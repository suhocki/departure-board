package com.sukhotski.timetable.display

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset

class DisplayRulesTest {

    private val scheduled = Instant.parse("2026-11-13T16:20:00Z")

    @Test
    fun `formats utc offset`() {
        assertEquals("UTC+1", formatUtcOffset(UtcOffset(hours = 1)))
        assertEquals("UTC+5:30", formatUtcOffset(UtcOffset(5, 30)))
        assertEquals("UTC-5", formatUtcOffset(UtcOffset(hours = -5)))
        assertEquals("UTC", formatUtcOffset(UtcOffset.ZERO))
    }

    @Test
    fun `formats the offset of a zone at an instant`() {
        val berlin = TimeZone.of("Europe/Berlin")

        assertEquals("UTC+1", formatUtcOffset(berlin, Instant.parse("2026-11-13T16:20:00Z")))
        assertEquals("UTC+2", formatUtcOffset(berlin, Instant.parse("2026-07-13T16:20:00Z")))
    }

    @Test
    fun `delay up to 30 minutes is shown as minutes`() {
        for (late in listOf(1, 13, 30)) {
            assertEquals(
                DelayLabel.Minutes(late.toLong()),
                delayLabel(scheduled, scheduled + late.minutes),
            )
        }
    }

    @Test
    fun `delay above 30 minutes is shown as the expected time`() {
        val expected = scheduled + 31.minutes

        assertEquals(DelayLabel.ExpectedAt(expected), delayLabel(scheduled, expected))
    }
}
