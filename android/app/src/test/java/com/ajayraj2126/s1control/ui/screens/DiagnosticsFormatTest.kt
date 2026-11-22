package com.ajayraj2126.s1control.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticsFormatTest {
    @Test
    fun uptimeFormatsHoursAndMinutes() {
        assertEquals("1h 1m", DiagnosticsFormat.uptime("3661.25 9000.00"))
    }

    @Test
    fun uptimeFormatsDays() {
        assertEquals("2d 1h 0m", DiagnosticsFormat.uptime("176400.1 12345"))
    }

    @Test
    fun uptimeRejectsMalformedResponses() {
        assertEquals("Unavailable", DiagnosticsFormat.uptime(""))
        assertEquals("Unavailable", DiagnosticsFormat.uptime("error"))
        assertEquals("Unavailable", DiagnosticsFormat.uptime("-99.0"))
    }

    @Test
    fun systemLoadFormatsOnlyThreeNumbers() {
        assertEquals("0.10 / 0.20 / 0.30 (1/5/15 min)",
            DiagnosticsFormat.load("0.10 0.20 0.30 1/99 101"))
        assertEquals("Unavailable", DiagnosticsFormat.load("not available"))
    }
}
