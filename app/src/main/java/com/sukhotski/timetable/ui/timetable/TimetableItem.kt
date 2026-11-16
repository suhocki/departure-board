package com.sukhotski.timetable.ui.timetable

import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.Timetable
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime

sealed interface TimetableItem {
    data class DateHeader(val date: LocalDate) : TimetableItem

    data class Ride(val departure: Departure) : TimetableItem
}

/**
 * Puts a date header before the first ride whose station-local date differs from the one before it,
 * starting from the station-local date of [now]. The device date is never used.
 */
fun timetableItems(timetable: Timetable, now: Instant): List<TimetableItem> {
    val zone = timetable.station.zone
    var previousDate = now.toLocalDateTime(zone).date
    return buildList {
        for (departure in timetable.departures) {
            val date = departure.scheduled.toLocalDateTime(zone).date
            if (date != previousDate) {
                add(TimetableItem.DateHeader(date))
                previousDate = date
            }
            add(TimetableItem.Ride(departure))
        }
    }
}
