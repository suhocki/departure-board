package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.RideDto
import com.sukhotski.timetable.data.remote.TimetableResponse
import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.DepartureStatus
import com.sukhotski.timetable.domain.Station
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.domain.departureOrder
import java.time.DateTimeException
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/** Returns null without a station name or time zone. Rides are not filtered by the request window. */
internal fun TimetableResponse.toTimetable(): Timetable? {
    val stationDto = station ?: return null
    val name = stationDto.name?.takeIf { it.isNotBlank() } ?: return null
    val zone = parseZoneOrNull(stationDto.timezone) ?: return null
    return Timetable(
        station = Station(name = name, zone = zone, zoneIsFallback = false),
        departures = rides
            .mapNotNull { it.toDepartureOrNull() }
            .sortedWith(departureOrder),
    )
}

internal fun RideDto.toDepartureOrNull(): Departure? {
    val code = line?.code
    val direction = line?.direction
    val scheduled = parseInstantOrNull(status?.scheduledTimestamp)
    if (id == null || code == null || direction == null || scheduled == null) return null
    return Departure(
        id = id,
        lineCode = code,
        direction = direction,
        scheduled = scheduled,
        status = DepartureStatus.Scheduled,
    )
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
    if (raw == null) return null
    return try {
        ZoneId.of(raw)
    } catch (_: DateTimeException) {
        null
    }
}
