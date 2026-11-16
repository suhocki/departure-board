package com.sukhotski.timetable.di

import com.sukhotski.timetable.data.DefaultTimetableRepository
import com.sukhotski.timetable.data.NoOpErrorReporter
import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.data.remote.createTimetableHttpClient
import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.testing.fixture
import io.ktor.client.engine.okhttp.OkHttp
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** The shared logic is tested with MockEngine. These tests use the real OkHttp engine. */
class OkHttpEngineTest {

    private val server = MockWebServer()
    private val from = Instant.parse("2026-11-13T16:05:00Z")
    private val to = Instant.parse("2026-11-13T18:05:00Z")

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `sends the request to the host with the api key and reads the list`() = runTest {
        server.enqueue(MockResponse.Builder().body(fixture("departures-normal.json")).build())

        val result = repository().departures("station-1", from, to)

        check(result is LoadResult.Success) { "Expected success, got $result" }
        val url = server.takeRequest().url
        assertEquals("/gis/v2/timetable/station-1/departures", url.encodedPath)
        assertEquals("test-key", url.queryParameter("apiKey"))
    }

    @Test
    fun `maps connection failure to no connection`() = runTest {
        server.close()

        assertEquals(
            LoadResult.Failure(LoadError.NoConnection),
            repository().departures("station-1", from, to),
        )
    }

    @Test
    fun `maps timeout on a slow body to no connection`() = runTest {
        server.enqueue(
            MockResponse.Builder()
                .body(fixture("departures-full.json"))
                .throttleBody(1, 1, TimeUnit.SECONDS)
                .build(),
        )

        val result = repository(timeoutMillis = 200).departures("station-1", from, to)

        assertEquals(LoadResult.Failure(LoadError.NoConnection), result)
    }

    private fun repository(timeoutMillis: Long = 15_000): DefaultTimetableRepository {
        val client = createTimetableHttpClient(
            OkHttp.create(),
            baseUrl = server.url("/").toString(),
            timeout = timeoutMillis.milliseconds,
        )
        return DefaultTimetableRepository(
            TimetableApi(client, "test-key"),
            NoOpErrorReporter(),
            Dispatchers.Default,
        )
    }
}
