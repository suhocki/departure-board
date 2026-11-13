package com.sukhotski.timetable.ui.timetable

import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.Timetable
import java.time.Instant
import java.time.LocalDate

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
    var previousDate = now.atZone(zone).toLocalDate()
    return buildList {
        for (departure in timetable.departures) {
            val date = departure.scheduled.atZone(zone).toLocalDate()
            if (date != previousDate) {
                add(TimetableItem.DateHeader(date))
                previousDate = date
            }
            add(TimetableItem.Ride(departure))
        }
    }
}
