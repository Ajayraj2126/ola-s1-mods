package com.asjohn720.s1mods.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test fun comparesAjReleaseVersionsNumerically() {
        assertTrue(UpdateChecker.isNewer("v0.10.0-aj", "0.2.0-aj"))
        assertFalse(UpdateChecker.isNewer("v0.2.0-aj", "0.2.0-aj"))
        assertFalse(UpdateChecker.isNewer("v0.1.9-aj", "0.2.0-aj"))
        assertFalse(UpdateChecker.isNewer("v0.2", "0.2.0-aj"))
    }

    @Test fun neverTreatsAnOriginalOrUnrelatedApkAsAnAjUpdate() {
        assertFalse(UpdateChecker.isAjApk("S1Mods-v1.4.apk"))
        assertFalse(UpdateChecker.isAjApk("other-app.apk"))
        assertFalse(UpdateChecker.isAjApk("S1-Control-AJ-Edition.zip"))
        assertTrue(UpdateChecker.isAjApk("S1-Control-AJ-Edition-0.3.0-debug.apk"))
    }
}
