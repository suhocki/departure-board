package com.sukhotski.timetable.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Only the fields the app shows. Every field may be missing or null in a response.

@Serializable
data class TimetableResponse(
    val rides: List<RideDto> = emptyList(),
    val station: StationDto? = null,
)

@Serializable
data class StationDto(val name: String? = null, val timezone: String? = null)

@Serializable
data class RideDto(
    val id: String? = null,
    val status: StatusDto? = null,
    val line: LineDto? = null,
)

@Serializable
data class LineDto(val code: String? = null, val direction: String? = null)

@Serializable
data class StatusDto(@SerialName("scheduled_timestamp") val scheduledTimestamp: String? = null)

val TimetableJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
