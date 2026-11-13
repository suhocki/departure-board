package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.TimetableJson
import com.sukhotski.timetable.data.remote.TimetableResponse
import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.testing.fixture
import com.sukhotski.timetable.testing.ridesJson
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableMapperTest {

    @Test
    fun `maps all rides of a real response`() {
        val timetable = map(fixture("departures-normal.json"))

        assertEquals("Berlin central bus station", timetable.station.name)
        assertEquals(ZoneId.of("Europe/Berlin"), timetable.station.zone)
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

    private fun mapOrNull(json: String): Timetable? =
        TimetableJson.decodeFromString<TimetableResponse>(json).toTimetable()

    private fun map(json: String): Timetable = checkNotNull(mapOrNull(json))

    private fun departures(vararg rides: String): List<Departure> =
        map(ridesJson(*rides)).departures

    private companion object {
        const val GOOD_RIDE =
            """{"id":"good","status":{"scheduled_timestamp":"2026-11-13T16:20:00Z"},"line":{"code":"N1338","direction":"Medyka, Border Crossing"}}"""
    }
}
