package com.sukhotski.timetable.ui.timetable

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TimeFormatterTest {

    private val berlin = ZoneId.of("Europe/Berlin")
    private val formatter24 = TimeFormatter(is24Hour = true, Locale.US)
    private val formatter12 = TimeFormatter(is24Hour = false, Locale.US)

    @Test
    fun `formats in station zone not device zone`() {
        val instant = Instant.parse("2026-11-13T16:20:00Z")
        // The test JVM runs in Asia/Kolkata, so reading the device zone would give 21:50.
        assertNotEquals(
            berlin.rules.getOffset(instant),
            ZoneId.systemDefault().rules.getOffset(instant),
        )

        assertEquals("17:20", formatter24.time(instant, berlin))
    }

    @Test
    fun `uses 12 hour format when device does`() {
        assertEquals("5:20 PM", formatter12.time(Instant.parse("2026-11-13T16:20:00Z"), berlin))
    }

    @Test
    fun `formats midnight hour`() {
        // departures-midnight-crossing sample
        val instant = Instant.parse("2026-11-13T23:05:00Z")

        assertEquals("00:05", formatter24.time(instant, berlin))
        assertEquals("12:05 AM", formatter12.time(instant, berlin))
    }

    @Test
    fun `formats summer time`() {
        // departures-far-future sample
        assertEquals("12:00", formatter24.time(Instant.parse("2027-06-01T10:00:00Z"), berlin))
    }

    @Test
    fun `formats across spring DST jump`() {
        // departures-dst-spring-forward sample: 02:00-03:00 local does not exist on 2027-03-28
        val times = listOf("00:20", "01:20", "02:00", "03:00")
            .map { formatter24.time(Instant.parse("2027-03-28T$it:00Z"), berlin) }

        assertEquals(listOf("01:20", "03:20", "04:00", "05:00"), times)
    }

    @Test
    fun `repeats the hour on autumn DST night`() {
        // 02:00-03:00 local happens twice on 2027-10-31
        assertEquals("02:30", formatter24.time(Instant.parse("2027-10-31T00:30:00Z"), berlin))
        assertEquals("02:30", formatter24.time(Instant.parse("2027-10-31T01:30:00Z"), berlin))
    }

    @Test
    fun `formats date header`() {
        assertEquals("Saturday, 14 November", formatter24.date(LocalDate.of(2026, 11, 14)))
        assertEquals("Friday, 13 November", formatter24.date(LocalDate.of(2026, 11, 13)))
    }

    @Test
    fun `formats date header with the given pattern`() {
        val us = TimeFormatter(is24Hour = false, Locale.US, datePattern = "EEEE, MMMM d")

        assertEquals("Saturday, November 14", us.date(LocalDate.of(2026, 11, 14)))
    }

    @Test
    fun `formats utc offset`() {
        assertEquals("UTC+1", formatUtcOffset(ZoneOffset.ofHours(1)))
        assertEquals("UTC+5:30", formatUtcOffset(ZoneOffset.ofHoursMinutes(5, 30)))
        assertEquals("UTC-5", formatUtcOffset(ZoneOffset.ofHours(-5)))
        assertEquals("UTC", formatUtcOffset(ZoneOffset.UTC))
    }
}
