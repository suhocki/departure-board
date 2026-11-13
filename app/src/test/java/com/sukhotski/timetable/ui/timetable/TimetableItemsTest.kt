package com.sukhotski.timetable.ui.timetable

import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.Station
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.testing.departure
import com.sukhotski.timetable.ui.timetable.TimetableItem.DateHeader
import com.sukhotski.timetable.ui.timetable.TimetableItem.Ride
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class TimetableItemsTest {

    @Test
    fun `no header when all rides are today`() {
        val first = ride("2026-11-13T16:20:00Z")
        val second = ride("2026-11-13T18:00:00Z")

        val items =
            timetableItems(timetable(first, second), now = Instant.parse("2026-11-13T16:05:00Z"))

        assertEquals(listOf(Ride(first), Ride(second)), items)
    }

    @Test
    fun `adds header before first ride of next day`() {
        val lateEvening = ride("2026-11-13T22:20:00Z")
        val afterMidnight = ride("2026-11-14T00:00:00Z")

        val items =
            timetableItems(
                timetable(lateEvening, afterMidnight),
                now = Instant.parse("2026-11-13T22:00:00Z"),
            )

        assertEquals(
            listOf(Ride(lateEvening), DateHeader(LocalDate.of(2026, 11, 14)), Ride(afterMidnight)),
            items,
        )
    }

    @Test
    fun `adds headers for a late ride from yesterday and for today after it`() {
        // 00:30 on Saturday in Berlin. The late ride was scheduled for 23:50 on Friday.
        val yesterday = ride("2026-11-13T22:50:00Z")
        val today = ride("2026-11-13T23:40:00Z")

        val items =
            timetableItems(timetable(yesterday, today), now = Instant.parse("2026-11-13T23:30:00Z"))

        assertEquals(
            listOf(
                DateHeader(LocalDate.of(2026, 11, 13)),
                Ride(yesterday),
                DateHeader(LocalDate.of(2026, 11, 14)),
                Ride(today),
            ),
            items,
        )
    }

    @Test
    fun `today is taken in station zone`() {
        // 22:30 on Friday in New York, already Saturday in UTC, Berlin and the test JVM zone.
        val newYork = ZoneId.of("America/New_York")
        val friday = ride("2026-11-14T04:30:00Z")
        val saturday = ride("2026-11-14T05:30:00Z")

        val items =
            timetableItems(
                timetable(friday, saturday, zone = newYork),
                now = Instant.parse("2026-11-14T03:30:00Z"),
            )

        assertEquals(
            listOf(Ride(friday), DateHeader(LocalDate.of(2026, 11, 14)), Ride(saturday)),
            items,
        )
    }

    private fun ride(scheduled: String) =
        departure(id = scheduled, scheduled = Instant.parse(scheduled))

    private fun timetable(vararg rides: Departure, zone: ZoneId = ZoneId.of("Europe/Berlin")) =
        Timetable(Station("Station", zone, zoneIsFallback = false), rides.toList())
}
