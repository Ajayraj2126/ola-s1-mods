package com.ajayraj2126.s1control.model

/** Exact observed package identity used to avoid removing a subsequently replaced app. */
data class PackageFingerprint(val versionCode: String, val lastUpdateTime: String)
data class PackageObservation(val installed: Boolean, val fingerprint: PackageFingerprint? = null)
data class RecordedPackage(
    val packageName: String,
    val displayName: String,
    val existedBefore: Boolean,
    val confirmed: Boolean = false,
    val fingerprint: PackageFingerprint? = null
)

enum class SettingKind { SETTING, PROPERTY, APP_OP, PERMISSION }
data class SettingAddress(
    val kind: SettingKind,
    val name: String,
    val namespace: String = "",
    val packageName: String = ""
) {
    val id: String get() = "${kind.name}:$namespace:$packageName:$name"
    val label: String get() = when {
        kind == SettingKind.SETTING && name == "enabled_accessibility_services" -> "Accessibility services"
        kind == SettingKind.SETTING && name == "accessibility_enabled" -> "Accessibility enabled"
        kind == SettingKind.PROPERTY -> "Diagnostic service mode"
        else -> "$packageName · $name"
    }
}
data class RecordedSetting(
    val address: SettingAddress,
    val originalValue: String?,
    val appliedValue: String?,
    val confirmed: Boolean = false
)
data class DeviceChangeRecord(
    val deviceId: String,
    val packages: List<RecordedPackage> = emptyList(),
    val settings: List<RecordedSetting> = emptyList(),
    val warnings: List<String> = emptyList()
)

data class PackageRemoval(
    val packageName: String,
    val displayName: String,
    val expectedFingerprint: PackageFingerprint
)
/** beforeValue is the live value shown in the preview, not the historical original. */
data class SettingRestoration(
    val address: SettingAddress,
    val label: String,
    val beforeValue: String?,
    val restoreValue: String?
)
data class RemovalPreview(
    val deviceId: String,
    val packages: List<PackageRemoval> = emptyList(),
    val settings: List<SettingRestoration> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    val hasChanges: Boolean get() = packages.isNotEmpty() || settings.isNotEmpty()
    val totalChanges: Int get() = packages.size + settings.size
}
data class RemovalProgress(val completed: Int, val total: Int, val message: String)
data class RemovalResult(
    val removedPackages: List<String> = emptyList(),
    val restoredSettings: List<String> = emptyList(),
    val skipped: List<String> = emptyList(),
    val failures: List<String> = emptyList()
) {
    val successful: Boolean get() = failures.isEmpty() && skipped.isEmpty()
}

/** Pure functions: no Android or device access. */
object ChangeJournalPlanner {
    fun services(value: String?): List<String> = value.orEmpty().split(':')
        .map(String::trim).filter { it.isNotEmpty() && it != "null" }.distinct()

    fun mergeAccessibilityServices(current: String?, service: String): String =
        (services(current) + service).distinct().joinToString(":")

    fun preview(
        record: DeviceChangeRecord,
        packages: Map<String, PackageObservation>,
        settings: Map<String, String?>
    ): RemovalPreview {
        val removals = mutableListOf<PackageRemoval>()
        val restorations = mutableListOf<SettingRestoration>()
        val warnings = record.warnings.toMutableList()
        record.packages.forEach { entry ->
            val live = packages[entry.packageName]
            when {
                entry.existedBefore -> warnings += "${entry.displayName} existed before AJ; it will be preserved. Its previous APK/data cannot be reconstructed."
                !entry.confirmed -> warnings += "${entry.displayName}: installation outcome was not verified; automatic removal is disabled."
                live == null -> warnings += "${entry.displayName}: package state could not be verified."
                !live.installed -> Unit
                entry.fingerprint == null || live.fingerprint == null -> warnings += "${entry.displayName}: version/update identity is unavailable; it will be preserved."
                entry.fingerprint != live.fingerprint -> warnings += "${entry.displayName} changed after AJ installed it; it will be preserved."
                else -> removals += PackageRemoval(entry.packageName, entry.displayName, entry.fingerprint)
            }
        }
        record.settings.forEach { entry ->
            val address = entry.address
            if (!entry.confirmed) {
                warnings += "${address.label}: mutation outcome was not verified; automatic restoration is disabled."
                return@forEach
            }
            if (!settings.containsKey(address.id)) {
                warnings += "${address.label}: current value could not be read."
                return@forEach
            }
            val current = settings[address.id]
            if (address.kind == SettingKind.SETTING && address.name == "enabled_accessibility_services") {
                // Only remove services AJ added. Preserve every unrelated current service;
                // do not re-enable a service the user disabled after installation.
                val added = services(entry.appliedValue) - services(entry.originalValue).toSet()
                val remaining = services(current).filterNot { it in added }
                if (remaining != services(current)) {
                    val target = remaining.joinToString(":").ifEmpty {
                        if (entry.originalValue == null) null else ""
                    }
                    restorations += SettingRestoration(address, address.label, current, target)
                }
            } else if (current == entry.originalValue) {
                Unit // Already restored.
            } else if (current == entry.appliedValue) {
                // Do not disable accessibility globally when another enabled service remains.
                if (address.kind == SettingKind.SETTING && address.name == "accessibility_enabled" && entry.originalValue != "1") {
                    val serviceEntry = record.settings.firstOrNull { it.address.name == "enabled_accessibility_services" }
                    val liveServices = serviceEntry?.let { settings[it.address.id] }
                    val added = serviceEntry?.let { services(it.appliedValue) - services(it.originalValue).toSet() }.orEmpty()
                    if (services(liveServices).any { it !in added }) {
                        warnings += "Accessibility remains enabled because other services are active."
                        return@forEach
                    }
                }
                restorations += SettingRestoration(address, address.label, current, entry.originalValue)
            } else {
                warnings += "${address.label} changed outside AJ; its current value will be preserved."
            }
        }
        // Return to the original diagnostic mode only after all other work.
        return RemovalPreview(record.deviceId, removals, restorations.sortedBy { it.address.kind == SettingKind.PROPERTY }, warnings.distinct())
    }
}
