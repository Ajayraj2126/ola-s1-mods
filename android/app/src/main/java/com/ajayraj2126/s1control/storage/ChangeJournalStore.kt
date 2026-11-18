package com.ajayraj2126.s1control.storage

import android.content.Context
import com.ajayraj2126.s1control.model.*
import org.json.JSONArray
import org.json.JSONObject

interface ChangeJournalStore {
    fun load(deviceId: String): DeviceChangeRecord
    fun save(record: DeviceChangeRecord)
}

/** App-private, synchronous commits: a before-state must be durable before any write. */
class PreferenceChangeJournalStore(context: Context) : ChangeJournalStore {
    private val preferences = context.getSharedPreferences("aj_device_change_journal_v1", Context.MODE_PRIVATE)
    override fun load(deviceId: String): DeviceChangeRecord {
        val raw = preferences.getString(deviceId, null) ?: return DeviceChangeRecord(deviceId)
        try {
            val root = JSONObject(raw)
            require(root.getString("deviceId") == deviceId)
            val packages = root.getJSONArray("packages").objects().map { item ->
                val fingerprint = item.optJSONObject("fingerprint")?.let {
                    PackageFingerprint(it.getString("versionCode"), it.getString("lastUpdateTime"))
                }
                RecordedPackage(item.getString("packageName"), item.getString("displayName"), item.getBoolean("existedBefore"), item.getBoolean("confirmed"), fingerprint)
            }
            val settings = root.getJSONArray("settings").objects().map { item ->
                val address = item.getJSONObject("address")
                RecordedSetting(SettingAddress(SettingKind.valueOf(address.getString("kind")), address.getString("name"), address.getString("namespace"), address.getString("packageName")), item.nullable("originalValue"), item.nullable("appliedValue"), item.getBoolean("confirmed"))
            }
            val warningArray = root.optJSONArray("warnings") ?: JSONArray()
            return DeviceChangeRecord(deviceId, packages, settings, (0 until warningArray.length()).map(warningArray::getString))
        } catch (failure: Exception) {
            // Corruption is not an empty journal: overwriting it would erase ownership evidence.
            throw IllegalStateException("AJ change journal could not be read. No automatic changes were made.", failure)
        }
    }
    override fun save(record: DeviceChangeRecord) {
        val root = JSONObject().put("deviceId", record.deviceId)
        root.put("packages", JSONArray().apply { record.packages.forEach { entry ->
            put(JSONObject().put("packageName", entry.packageName).put("displayName", entry.displayName).put("existedBefore", entry.existedBefore).put("confirmed", entry.confirmed).put("fingerprint", entry.fingerprint?.let { JSONObject().put("versionCode", it.versionCode).put("lastUpdateTime", it.lastUpdateTime) } ?: JSONObject.NULL))
        } })
        root.put("settings", JSONArray().apply { record.settings.forEach { entry ->
            put(JSONObject().put("address", JSONObject().put("kind", entry.address.kind.name).put("name", entry.address.name).put("namespace", entry.address.namespace).put("packageName", entry.address.packageName)).put("originalValue", entry.originalValue ?: JSONObject.NULL).put("appliedValue", entry.appliedValue ?: JSONObject.NULL).put("confirmed", entry.confirmed))
        } })
        root.put("warnings", JSONArray(record.warnings))
        check(preferences.edit().putString(record.deviceId, root.toString()).commit()) { "Cannot save AJ change journal; scooter mutation was stopped." }
    }
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map(::getJSONObject)
    private fun JSONObject.nullable(key: String): String? = if (isNull(key)) null else getString(key)
}

/** Pure repository logic; injected stores make ownership rules testable. */
class ChangeJournalRepository(private val store: ChangeJournalStore) {
    @Synchronized fun get(deviceId: String): DeviceChangeRecord = store.load(deviceId)
    @Synchronized fun beforePackage(deviceId: String, packageName: String, displayName: String, existedBefore: Boolean) {
        val record = store.load(deviceId)
        val old = record.packages.firstOrNull { it.packageName == packageName }
        val entry = RecordedPackage(packageName, displayName, old?.existedBefore ?: existedBefore)
        store.save(record.copy(packages = record.packages.filterNot { it.packageName == packageName } + entry))
    }
    @Synchronized fun afterPackage(deviceId: String, packageName: String, fingerprint: PackageFingerprint?) {
        val record = store.load(deviceId)
        store.save(record.copy(packages = record.packages.map { if (it.packageName == packageName) it.copy(confirmed = true, fingerprint = fingerprint) else it }))
    }
    @Synchronized fun beforeSetting(deviceId: String, address: SettingAddress, original: String?, applied: String?) {
        val record = store.load(deviceId)
        val old = record.settings.firstOrNull { it.address == address }
        if (old == null && original == applied) return
        val entry = RecordedSetting(address, old?.originalValue ?: original, applied)
        store.save(record.copy(settings = record.settings.filterNot { it.address == address } + entry))
    }
    @Synchronized fun afterSetting(deviceId: String, address: SettingAddress, observed: String?) {
        val record = store.load(deviceId)
        store.save(record.copy(settings = record.settings.map { if (it.address == address) it.copy(confirmed = observed == it.appliedValue) else it }))
    }
    @Synchronized fun removedPackage(deviceId: String, packageName: String) {
        val record = store.load(deviceId)
        store.save(record.copy(packages = record.packages.filterNot { it.packageName == packageName }, settings = record.settings.filterNot { it.address.packageName == packageName }))
    }
    @Synchronized fun restoredSetting(deviceId: String, address: SettingAddress) {
        val record = store.load(deviceId)
        store.save(record.copy(settings = record.settings.filterNot { it.address == address }))
    }
    @Synchronized fun warning(deviceId: String, message: String) {
        val record = store.load(deviceId)
        store.save(record.copy(warnings = (record.warnings + message).distinct()))
    }
}
