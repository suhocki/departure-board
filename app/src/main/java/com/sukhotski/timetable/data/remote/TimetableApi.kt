package com.sukhotski.timetable.data.remote

import com.sukhotski.timetable.data.UnexpectedResponseException
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class TimetableApi(private val client: HttpClient, private val apiKey: String) {

    /**
     * The response body as text, so the caller can decode it on its own dispatcher.
     * [from] and [to] are ISO 8601 instants in UTC with Z. The API ignores other offsets.
     * Error bodies are never read. Throws [UnexpectedResponseException] for a non-2xx status.
     */
    suspend fun departures(stationId: String, from: String, to: String): String {
        val response = client.get("gis/v2/timetable/$stationId/departures") {
            parameter("from", from)
            parameter("to", to)
            parameter("apiKey", apiKey)
        }
        if (!response.status.isSuccess()) {
            throw UnexpectedResponseException("HTTP ${response.status.value}")
        }
        return response.bodyAsText()
    }
}
