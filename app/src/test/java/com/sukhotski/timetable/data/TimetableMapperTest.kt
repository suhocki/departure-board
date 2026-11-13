package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.TimetableJson
import com.sukhotski.timetable.data.remote.TimetableResponse
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.testing.fixture
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

    private fun mapOrNull(json: String): Timetable? =
        TimetableJson.decodeFromString<TimetableResponse>(json).toTimetable()

    private fun map(json: String): Timetable = checkNotNull(mapOrNull(json))

    private companion object {
        const val GOOD_RIDE =
            """{"id":"good","status":{"scheduled_timestamp":"2026-11-13T16:20:00Z"},"line":{"code":"N1338","direction":"Medyka, Border Crossing"}}"""
    }
}
