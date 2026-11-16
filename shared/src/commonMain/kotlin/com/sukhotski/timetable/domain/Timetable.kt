package com.sukhotski.timetable.domain

import kotlin.time.Instant
import kotlinx.datetime.TimeZone

data class Timetable(val station: Station, val departures: List<Departure>)

/** [zone] is UTC and [zoneIsFallback] is true when the API gave no usable time zone. */
data class Station(val name: String, val zone: TimeZone, val zoneIsFallback: Boolean)

data class Departure(
    val id: String,
    val lineCode: String,
    val direction: String,
    val scheduled: Instant,
    val status: DepartureStatus,
)

sealed interface DepartureStatus {
    /** No live data, not LATE, or less than one whole minute late. */
    data object Scheduled : DepartureStatus

    data class Delayed(val expected: Instant) : DepartureStatus

    data object Cancelled : DepartureStatus
}

val departureOrder: Comparator<Departure> = compareBy({ it.scheduled }, { it.lineCode }, { it.id })

/** Whole minutes from [scheduled] to [expected], rounded down and never negative. */
fun delayMinutes(scheduled: Instant, expected: Instant): Long =
    (expected - scheduled).inWholeMinutes.coerceAtLeast(0)
