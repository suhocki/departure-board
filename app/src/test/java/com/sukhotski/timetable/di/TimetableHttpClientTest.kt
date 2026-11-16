package com.sukhotski.timetable.di

import com.sukhotski.timetable.data.remote.TIMETABLE_REQUEST_TIMEOUT
import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.data.remote.createTimetableHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TimetableHttpClientTest {

    @Test
    fun `default client and api send the request to the timetable host with the api key`() =
        runTest {
            var sent: HttpRequestData? = null
            val engine = MockEngine { request ->
                sent = request
                respond("""{"rides":[]}""")
            }

            TimetableApi(createTimetableHttpClient(engine), "k")
                .departures("station-1", from = "2026-11-13T16:05:00Z", to = "2026-11-13T18:05:00Z")

            val url = checkNotNull(sent).url
            assertEquals("https", url.protocol.name)
            assertEquals("global.api.flixbus.com", url.host)
            assertEquals("/gis/v2/timetable/station-1/departures", url.encodedPath)
            assertEquals("k", url.parameters["apiKey"])
        }

    @Test
    fun `limits the whole request`() = runTest {
        assertEquals(15.seconds, TIMETABLE_REQUEST_TIMEOUT)
        val engine = MockEngine {
            delay(10.seconds)
            respond("")
        }
        val client = createTimetableHttpClient(engine, timeout = 50.milliseconds)

        try {
            client.get("x")
            error("Expected a timeout")
        } catch (_: HttpRequestTimeoutException) {
        }
    }
}
