package com.sukhotski.timetable.testing

import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.DepartureStatus
import kotlin.time.Instant

fun departure(
    id: String = "ride-1",
    lineCode: String = "N1338",
    direction: String = "Medyka, Border Crossing",
    scheduled: Instant = Instant.parse("2026-11-13T16:20:00Z"),
    status: DepartureStatus = DepartureStatus.Scheduled,
) = Departure(id, lineCode, direction, scheduled, status)
