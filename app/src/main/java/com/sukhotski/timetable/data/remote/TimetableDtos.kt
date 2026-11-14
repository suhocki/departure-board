package com.sukhotski.timetable.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement

// Only the fields the app shows. Every field may be missing or null in a response.

@Serializable
data class TimetableResponse(
    @Serializable(with = LenientRideListSerializer::class)
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
data class StatusDto(
    @SerialName("scheduled_timestamp") val scheduledTimestamp: String? = null,
    val deviation: DeviationDto? = null,
)

@Serializable
data class DeviationDto(
    @SerialName("deviation_class") val deviationClass: String? = null,
    @SerialName("deviation_timestamp") val deviationTimestamp: String? = null,
)

/**
 * Decodes each ride on its own. A ride with a wrong type becomes an empty [RideDto],
 * so one bad ride does not fail the whole list. The mapper drops it and it is counted.
 */
object LenientRideListSerializer : KSerializer<List<RideDto>> {
    private val elements = ListSerializer(JsonElement.serializer())
    private val rides = ListSerializer(RideDto.serializer())

    override val descriptor = elements.descriptor

    override fun deserialize(decoder: Decoder): List<RideDto> {
        val json = (decoder as JsonDecoder).json
        return elements.deserialize(decoder).map {
            try {
                json.decodeFromJsonElement(RideDto.serializer(), it)
            } catch (_: SerializationException) {
                RideDto()
            }
        }
    }

    override fun serialize(encoder: Encoder, value: List<RideDto>) = rides.serialize(encoder, value)
}

val timetableJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}
