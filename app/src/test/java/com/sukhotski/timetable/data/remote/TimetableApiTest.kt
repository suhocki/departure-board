package com.sukhotski.timetable.data.remote

import com.sukhotski.timetable.testing.fixture
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TimetableApiTest {

    private val server = MockWebServer()
    private lateinit var api: TimetableApi

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor("test-key")).build()
        api = createTimetableApi(server.url("/"), client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `sends station id window and api key`() = runTest {
        server.enqueue(MockResponse.Builder().body("""{"rides":[]}""").build())

        api.departures("station-1", from = "2026-11-13T16:05:00Z", to = "2026-11-13T18:05:00Z")

        val url = server.takeRequest().url
        assertEquals("/gis/v2/timetable/station-1/departures", url.encodedPath)
        assertEquals("2026-11-13T16:05:00Z", url.queryParameter("from"))
        assertEquals("2026-11-13T18:05:00Z", url.queryParameter("to"))
        assertEquals("test-key", url.queryParameter("apiKey"))
    }

    @Test
    fun `decodes a full real response`() = runTest {
        server.enqueue(MockResponse.Builder().body(fixture("departures-full.json")).build())

        val response = api.departures(
            "station-1",
            from = "2026-11-13T16:05:00Z",
            to = "2026-11-13T18:05:00Z",
        )

        assertEquals(11, response.rides.size)
    }
}
