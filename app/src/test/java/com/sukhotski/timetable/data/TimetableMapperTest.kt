package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.TimetableResponse
import com.sukhotski.timetable.data.remote.timetableJson
import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.DepartureStatus
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.testing.fixture
import com.sukhotski.timetable.testing.ridesJson
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableMapperTest {

    @Test
    fun `maps all rides of a real response`() {
        val timetable = map(fixture("departures-normal.json"))

        assertEquals("Berlin central bus station", timetable.station.name)
        assertEquals(TimeZone.of("Europe/Berlin"), timetable.station.zone)
        assertEquals(
            listOf("060", "1324", "N160", "144", "N1338", "N1385") +
                listOf("050", "1327", "N1326", "N260", "1926"),
            timetable.departures.map { it.lineCode },
        )
        val first = timetable.departures.first()
        assertEquals("Bremen Fernbusterminal", first.direction)
        assertEquals(Instant.parse("2026-11-13T15:10:00Z"), first.scheduled)
    }

    @Test
    fun `keeps delayed ride scheduled before the window start`() {
        val windowStart = Instant.parse("2026-11-13T16:05:00Z")

        val departures = map(fixture("departures-normal.json")).departures

        assertTrue(departures.any { it.lineCode == "060" && it.scheduled < windowStart })
    }

    @Test
    fun `returns null when station is missing`() {
        assertNull(mapOrNull("""{"rides":[$GOOD_RIDE],"station":null}"""))
        assertNull(
            mapOrNull(
                """{"rides":[$GOOD_RIDE],"station":{"name":" ","timezone":"Europe/Berlin"}}""",
            ),
        )
    }

    @Test
    fun `drops ride without id`() {
        val departures = departures(
            GOOD_RIDE,
            """{"status":{"scheduled_timestamp":"2026-11-13T16:20:00Z"},"line":{"code":"N1","direction":"A"}}""",
            """{"id":null,"status":{"scheduled_timestamp":"2026-11-13T16:20:00Z"},"line":{"code":"N1","direction":"A"}}""",
            """{"id":" ","status":{"scheduled_timestamp":"2026-11-13T16:20:00Z"},"line":{"code":"N1","direction":"A"}}""",
        )

        assertEquals(listOf("good"), departures.map { it.id })
    }

    @Test
    fun `drops ride with unparseable scheduled time`() {
        val departures = departures(
            GOOD_RIDE,
            """{"id":"a","status":{"scheduled_timestamp":"2026-11-13T16:20:00"},"line":{"code":"N1","direction":"A"}}""",
            """{"id":"b","status":{"scheduled_timestamp":""},"line":{"code":"N1","direction":"A"}}""",
            """{"id":"c","status":{"scheduled_timestamp":null},"line":{"code":"N1","direction":"A"}}""",
            """{"id":"d","status":{"scheduled_timestamp":"2026-02-30T10:00:00Z"},"line":{"code":"N1","direction":"A"}}""",
            """{"id":"e","line":{"code":"N1","direction":"A"}}""",
        )

        assertEquals(listOf("good"), departures.map { it.id })
    }

    @Test
    fun `drops ride with blank line code or direction`() {
        val departures = departures(
            GOOD_RIDE,
            """{"id":"a","status":{"scheduled_timestamp":"2026-11-13T16:20:00Z"}}""",
            """{"id":"b","status":{"scheduled_timestamp":"2026-11-13T16:20:00Z"},"line":{"code":null,"direction":"A"}}""",
            """{"id":"c","status":{"scheduled_timestamp":"2026-11-13T16:20:00Z"},"line":{"code":"N1","direction":" "}}""",
        )

        assertEquals(listOf("good"), departures.map { it.id })
    }

    @Test
    fun `removes duplicate ride ids`() {
        val departures = departures(
            GOOD_RIDE,
            """{"id":"good","status":{"scheduled_timestamp":"2026-11-13T17:00:00Z"},"line":{"code":"N2","direction":"B"}}""",
        )

        assertEquals(1, departures.size)
        assertEquals("N1338", departures.single().lineCode)
    }

    @Test
    fun `maps later deviation timestamp to delayed`() {
        val departures = map(fixture("departures-normal.json")).departures

        assertEquals(
            DepartureStatus.Delayed(Instant.parse("2026-11-13T16:24:36Z")),
            departures.byCode("060").status,
        )
        assertEquals(5, departures.count { it.status is DepartureStatus.Delayed })
    }

    @Test
    fun `treats missing deviation and zero delay as scheduled`() {
        val departures = map(fixture("departures-normal.json")).departures

        assertEquals(DepartureStatus.Scheduled, departures.byCode("050").status)
        assertEquals(DepartureStatus.Scheduled, departures.byCode("N1385").status)
        assertEquals(DepartureStatus.Scheduled, statusOf("""{"deviation_class":"LATE"}"""))
        assertEquals(
            DepartureStatus.Scheduled,
            statusOf("""{"deviation_class":"LATE","deviation_timestamp":"soon"}"""),
        )
    }

    @Test
    fun `ignores delay under one minute and early departure`() {
        assertEquals(DepartureStatus.Scheduled, statusOf(deviation("LATE", "2026-11-13T16:20:59Z")))
        assertEquals(
            DepartureStatus.Scheduled,
            statusOf(deviation("ON_TIME", "2026-11-13T16:15:00Z")),
        )
    }

    @Test
    fun `on time class with a later timestamp shows no delay`() {
        assertEquals(
            DepartureStatus.Scheduled,
            statusOf(deviation("ON_TIME", "2026-11-13T16:24:06Z")),
        )
    }

    @Test
    fun `unknown or missing class is scheduled whatever the timestamp`() {
        assertEquals(
            DepartureStatus.Scheduled,
            statusOf(deviation("SOMETHING_NEW", "2026-11-13T16:25:00Z")),
        )
        assertEquals(
            DepartureStatus.Scheduled,
            statusOf("""{"deviation_timestamp":"2026-11-13T16:25:00Z"}"""),
        )
    }

    @Test
    fun `late class takes the expected time from the timestamp`() {
        assertEquals(
            DepartureStatus.Delayed(Instant.parse("2026-11-13T16:25:00Z")),
            statusOf(deviation("LATE", "2026-11-13T16:25:00Z")),
        )
        assertEquals(DepartureStatus.Scheduled, statusOf(deviation("LATE", "2026-11-13T16:20:00Z")))
    }

    @Test
    fun `marks cancelled ride as cancelled`() {
        val departures = map(fixture("departures-cancelled.json")).departures
        val cancelled = departures.byCode("N3212")

        assertEquals(DepartureStatus.Cancelled, cancelled.status)
        assertEquals(Instant.parse("2026-11-14T13:20:00Z"), cancelled.scheduled)
        assertEquals(15, departures.size)
        assertTrue(departures.indexOf(cancelled) in 1 until departures.lastIndex)
        assertEquals(
            DepartureStatus.Cancelled,
            statusOf(deviation("CANCELLED", "2026-11-13T16:40:00Z")),
        )
    }

    @Test
    fun `falls back to UTC when station time zone is missing or invalid`() {
        for (timezone in listOf("null", "\"\"", "\"Europe/Berln\"", "\"PST\"")) {
            val timetable =
                map(
                    """{"rides":[$GOOD_RIDE],"station":{"name":"Berlin central bus station","timezone":$timezone}}""",
                )

            assertEquals(timezone, TimeZone.UTC, timetable.station.zone)
            assertTrue(timezone, timetable.station.zoneIsFallback)
            assertEquals(timezone, 1, timetable.departures.size)
        }
        val padded =
            map(
                """{"rides":[],"station":{"name":"Berlin central bus station","timezone":" Europe/Berlin "}}""",
            )

        assertEquals(TimeZone.of("Europe/Berlin"), padded.station.zone)
        assertFalse(padded.station.zoneIsFallback)
    }

    private fun mapOrNull(json: String): Timetable? =
        timetableJson.decodeFromString<TimetableResponse>(json).toTimetable()

    private fun map(json: String): Timetable = checkNotNull(mapOrNull(json))

    private fun departures(vararg rides: String): List<Departure> =
        map(ridesJson(*rides)).departures

    private fun List<Departure>.byCode(code: String): Departure = single { it.lineCode == code }

    /** Status of a ride scheduled at 16:20Z with the given deviation object. */
    private fun statusOf(deviation: String): DepartureStatus = departures(
        """{"id":"r","status":{"scheduled_timestamp":"2026-11-13T16:20:00Z","deviation":$deviation},"line":{"code":"N1","direction":"A"}}""",
    ).single().status

    private fun deviation(deviationClass: String, timestamp: String) =
        """{"deviation_class":"$deviationClass","deviation_timestamp":"$timestamp"}"""

    private companion object {
        const val GOOD_RIDE =
            """{"id":"good","status":{"scheduled_timestamp":"2026-11-13T16:20:00Z"},"line":{"code":"N1338","direction":"Medyka, Border Crossing"}}"""
    }
}
