package com.sukhotski.timetable.domain

import com.sukhotski.timetable.testing.departure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TimetableTest {

    @Test
    fun `orders by scheduled time then line code then id`() {
        val earlier =
            departure(id = "a", lineCode = "N1385", scheduled = at("2026-11-13T16:00:00Z"))
        val code144 = departure(id = "b", lineCode = "144", scheduled = at("2026-11-13T16:20:00Z"))
        val code144SecondId =
            departure(id = "c", lineCode = "144", scheduled = at("2026-11-13T16:20:00Z"))
        val n1338 = departure(id = "d", lineCode = "N1338", scheduled = at("2026-11-13T16:20:00Z"))
        val n1385 = departure(id = "e", lineCode = "N1385", scheduled = at("2026-11-13T16:20:00Z"))
        // Autumn DST night in Berlin: both show 02:30, the real order is by Instant.
        val dstFirst = departure(id = "f", lineCode = "N9", scheduled = at("2027-10-31T00:30:00Z"))
        val dstSecond = departure(id = "g", lineCode = "N1", scheduled = at("2027-10-31T01:30:00Z"))

        val sorted = listOf(dstSecond, n1385, code144SecondId, earlier, n1338, dstFirst, code144)
            .sortedWith(departureOrder)

        assertEquals(
            listOf(earlier, code144, code144SecondId, n1338, n1385, dstFirst, dstSecond),
            sorted,
        )
    }

    @Test
    fun `delay is floored to whole minutes`() {
        val scheduled = at("2026-11-13T15:10:00Z")

        assertEquals(0, delayMinutes(scheduled, scheduled))
        assertEquals(0, delayMinutes(scheduled, scheduled.plus(59.seconds)))
        assertEquals(1, delayMinutes(scheduled, scheduled.plus(60.seconds)))
        assertEquals(1, delayMinutes(scheduled, scheduled.plus(119.seconds)))
        assertEquals(74, delayMinutes(scheduled, at("2026-11-13T16:24:36Z")))
    }

    @Test
    fun `delay is never negative`() {
        val scheduled = at("2026-11-13T15:10:00Z")

        assertEquals(0, delayMinutes(scheduled, scheduled.minus(5.minutes)))
    }

    private fun at(text: String): Instant = Instant.parse(text)
}
