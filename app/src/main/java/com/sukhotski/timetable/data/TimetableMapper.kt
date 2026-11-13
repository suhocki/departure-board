package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.DeviationDto
import com.sukhotski.timetable.data.remote.RideDto
import com.sukhotski.timetable.data.remote.TimetableResponse
import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.DepartureStatus
import com.sukhotski.timetable.domain.Station
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.domain.delayMinutes
import com.sukhotski.timetable.domain.departureOrder
import java.time.DateTimeException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

private const val CANCELLED = "CANCELLED"

/** Returns null when the response has no station name. Rides are not filtered by the request window. */
internal fun TimetableResponse.toTimetable(): Timetable? {
    val stationDto = station ?: return null
    val name = stationDto.name?.takeIf { it.isNotBlank() } ?: return null
    val zone = parseZoneOrNull(stationDto.timezone)
    return Timetable(
        station = Station(
            name = name,
            zone = zone ?: ZoneOffset.UTC,
            zoneIsFallback = zone == null,
        ),
        departures = rides
            .mapNotNull { it.toDepartureOrNull() }
            .distinctBy { it.id }
            .sortedWith(departureOrder),
    )
}

/** Returns null when a field the list needs is missing, blank or not parseable. */
internal fun RideDto.toDepartureOrNull(): Departure? {
    val code = line?.code
    val direction = line?.direction
    val scheduled = parseInstantOrNull(status?.scheduledTimestamp)
    if (id.isNullOrBlank() || code.isNullOrBlank() || direction.isNullOrBlank() ||
        scheduled == null
    ) {
        return null
    }
    return Departure(
        id = id,
        lineCode = code,
        direction = direction,
        scheduled = scheduled,
        status = status?.deviation.toStatus(scheduled),
    )
}

// The timestamp decides, not the class: an unknown class cannot break it.
private fun DeviationDto?.toStatus(scheduled: Instant): DepartureStatus {
    if (this == null) return DepartureStatus.Scheduled
    if (deviationClass == CANCELLED) return DepartureStatus.Cancelled
    val expected = parseInstantOrNull(deviationTimestamp) ?: return DepartureStatus.Scheduled
    return if (delayMinutes(scheduled, expected) >= 1) {
        DepartureStatus.Delayed(expected)
    } else {
        DepartureStatus.Scheduled
    }
}

private fun parseInstantOrNull(raw: String?): Instant? {
    if (raw == null) return null
    return try {
        OffsetDateTime.parse(raw).toInstant()
    } catch (_: DateTimeParseException) {
        null
    }
}

private fun parseZoneOrNull(raw: String?): ZoneId? {
    if (raw.isNullOrBlank()) return null
    return try {
        ZoneId.of(raw.trim())
    } catch (_: DateTimeException) {
        null
    }
}
