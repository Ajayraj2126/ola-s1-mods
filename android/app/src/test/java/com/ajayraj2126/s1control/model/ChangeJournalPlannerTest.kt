package com.ajayraj2126.s1control.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangeJournalPlannerTest {
    private val fingerprint = PackageFingerprint("12", "2026-11-18 10:00:00")

    @Test fun removesOnlyAjInstalledPackageWithUnchangedFingerprint() {
        val record = DeviceChangeRecord("device", packages = listOf(
            RecordedPackage("aj.owned", "AJ owned", existedBefore = false, confirmed = true, fingerprint = fingerprint),
            RecordedPackage("pre.existing", "Pre-existing", existedBefore = true, confirmed = true, fingerprint = fingerprint),
            RecordedPackage("changed", "Changed", existedBefore = false, confirmed = true, fingerprint = fingerprint)
        ))
        val preview = ChangeJournalPlanner.preview(record, mapOf(
            "aj.owned" to PackageObservation(true, fingerprint),
            "pre.existing" to PackageObservation(true, fingerprint),
            "changed" to PackageObservation(true, fingerprint.copy(versionCode = "13"))
        ), emptyMap())

        assertEquals(listOf("aj.owned"), preview.packages.map { it.packageName })
        assertTrue(preview.warnings.any { it.contains("existed before AJ") })
        assertTrue(preview.warnings.any { it.contains("changed after AJ") })
    }

    @Test fun accessibilityRestoreRemovesOnlyServiceAddedByAj() {
        val address = SettingAddress(SettingKind.SETTING, "enabled_accessibility_services", "secure")
        val record = DeviceChangeRecord("device", settings = listOf(
            RecordedSetting(address, "other/Service", "other/Service:aj/Service", confirmed = true)
        ))
        val preview = ChangeJournalPlanner.preview(
            record,
            emptyMap(),
            mapOf(address.id to "other/Service:aj/Service:new/Service")
        )

        assertEquals("other/Service:new/Service", preview.settings.single().restoreValue)
    }

    @Test fun externallyChangedSettingIsPreserved() {
        val address = SettingAddress(SettingKind.SETTING, "accessibility_enabled", "secure")
        val record = DeviceChangeRecord("device", settings = listOf(
            RecordedSetting(address, "0", "1", confirmed = true)
        ))
        val preview = ChangeJournalPlanner.preview(record, emptyMap(), mapOf(address.id to "custom"))

        assertTrue(preview.settings.isEmpty())
        assertTrue(preview.warnings.any { it.contains("changed outside AJ") })
    }

    @Test fun diagnosticPropertyIsRestoredLast() {
        val setting = SettingAddress(SettingKind.SETTING, "accessibility_enabled", "secure")
        val property = SettingAddress(SettingKind.PROPERTY, "persist.ola.security.scootermode")
        val record = DeviceChangeRecord("device", settings = listOf(
            RecordedSetting(property, "prod", "serv", true),
            RecordedSetting(setting, "0", "1", true)
        ))
        val preview = ChangeJournalPlanner.preview(record, emptyMap(), mapOf(
            property.id to "serv",
            setting.id to "1"
        ))

        assertEquals(listOf(SettingKind.SETTING, SettingKind.PROPERTY), preview.settings.map { it.address.kind })
    }
}
