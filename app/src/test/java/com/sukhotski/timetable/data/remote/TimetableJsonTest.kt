package com.sukhotski.timetable.data.remote

import com.sukhotski.timetable.testing.fixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimetableJsonTest {

    @Test
    fun `parses a real response and ignores unknown fields`() {
        val response = decode(fixture("departures-normal.json"))

        assertEquals(11, response.rides.size)
        assertEquals("Berlin central bus station", response.station?.name)
        assertEquals("Europe/Berlin", response.station?.timezone)
        assertEquals("060", response.rides.first().line?.code)
    }

    @Test
    fun `missing and null fields do not fail parsing`() {
        assertEquals(TimetableResponse(), decode("{}"))
        assertEquals(TimetableResponse(), decode("""{"rides":null}"""))

        val partial = decode("""{"rides":[{"id":"a"},{}],"station":null}""")

        assertEquals(listOf(RideDto(id = "a"), RideDto()), partial.rides)
        assertNull(partial.station)
    }

    @Test
    fun `ride with a wrong type becomes an empty ride and the others stay`() {
        val response = decode(
            """
            {"rides":[
              {"id":123},
              {"id":"a","line":"N1"},
              {"id":"b","line":{"code":60}},
              {"id":"c","line":{"code":"N1","direction":"Hamburg"}}
            ]}
            """.trimIndent(),
        )

        assertEquals(
            listOf(RideDto(), RideDto(), RideDto()) +
                RideDto(id = "c", line = LineDto(code = "N1", direction = "Hamburg")),
            response.rides,
        )
    }

    private fun decode(json: String): TimetableResponse = timetableJson.decodeFromString(json)
}
