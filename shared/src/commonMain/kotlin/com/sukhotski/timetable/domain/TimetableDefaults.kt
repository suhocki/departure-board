package com.sukhotski.timetable.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/** What both apps show: one station and the next two hours. */
object TimetableDefaults {
    /** Berlin central bus station. */
    const val STATION_ID = "dcbaaee6-9603-11e6-9066-549f350fcb0c"

    val WINDOW: Duration = 2.hours
}
