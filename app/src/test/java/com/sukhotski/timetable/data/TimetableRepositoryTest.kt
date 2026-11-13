package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.ApiKeyInterceptor
import com.sukhotski.timetable.data.remote.createTimetableApi
import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.testing.fixture
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimetableRepositoryTest {

    private val server = MockWebServer()
    private lateinit var repository: DefaultTimetableRepository

    private val from = Instant.parse("2026-11-13T16:05:00Z")
    private val to = from + Duration.ofHours(2)

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor("test-key")).build()
        repository = DefaultTimetableRepository(createTimetableApi(server.url("/"), client))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `requests two hour window in UTC with api key`() = runTest {
        enqueue(200, fixture("departures-empty.json"))

        repository.departures("station-1", from, to)

        val url = server.takeRequest().url
        assertEquals("/gis/v2/timetable/station-1/departures", url.encodedPath)
        assertEquals("2026-11-13T16:05:00Z", url.queryParameter("from"))
        assertEquals("2026-11-13T18:05:00Z", url.queryParameter("to"))
        assertEquals("test-key", url.queryParameter("apiKey"))
    }

    @Test
    fun `parses full real response with unknown fields`() = runTest {
        enqueue(200, fixture("departures-full.json"))

        val timetable = success(repository.departures("station-1", from, to))

        assertEquals(11, timetable.departures.size)
        assertEquals("060", timetable.departures.first().lineCode)
        assertEquals(ZoneId.of("Europe/Berlin"), timetable.station.zone)
    }

    @Test
    fun `returns empty timetable for no rides`() = runTest {
        enqueue(200, fixture("departures-empty.json"))

        val timetable = success(repository.departures("station-1", from, to))

        assertTrue(timetable.departures.isEmpty())
        assertEquals("Berlin central bus station", timetable.station.name)
    }

    @Test
    fun `maps response without station to unexpected`() = runTest {
        enqueue(200, """{"rides":[]}""")

        assertEquals(
            LoadResult.Failure(LoadError.Unexpected),
            repository.departures("station-1", from, to),
        )
    }

    @Test
    fun `maps 401 to unexpected`() = runTest {
        enqueue(401, fixture("error-401.txt"), contentType = "text/plain")

        assertEquals(
            LoadResult.Failure(LoadError.Unexpected),
            repository.departures("station-1", from, to),
        )
    }

    @Test
    fun `maps 400 with json body to unexpected`() = runTest {
        enqueue(400, fixture("error-400.json"))

        assertEquals(
            LoadResult.Failure(LoadError.Unexpected),
            repository.departures("station-1", from, to),
        )
    }

    @Test
    fun `maps 404 with empty body to unexpected`() = runTest {
        enqueue(404, "")

        assertEquals(
            LoadResult.Failure(LoadError.Unexpected),
            repository.departures("station-1", from, to),
        )
    }

    @Test
    fun `maps 503 to unexpected`() = runTest {
        enqueue(503, "Service Unavailable", contentType = "text/plain")

        assertEquals(
            LoadResult.Failure(LoadError.Unexpected),
            repository.departures("station-1", from, to),
        )
    }

    @Test
    fun `maps 204 without body to unexpected`() = runTest {
        server.enqueue(MockResponse.Builder().code(204).build())

        assertEquals(
            LoadResult.Failure(LoadError.Unexpected),
            repository.departures("station-1", from, to),
        )
    }

    @Test
    fun `maps connection failure to no connection`() = runTest {
        server.close()

        assertEquals(
            LoadResult.Failure(LoadError.NoConnection),
            repository.departures("station-1", from, to),
        )
    }

    @Test
    fun `maps malformed body to unexpected`() = runTest {
        enqueue(200, "not json")
        enqueue(200, "")

        assertEquals(
            LoadResult.Failure(LoadError.Unexpected),
            repository.departures("station-1", from, to),
        )
        assertEquals(
            LoadResult.Failure(LoadError.Unexpected),
            repository.departures("station-1", from, to),
        )
    }

    private fun enqueue(code: Int, body: String, contentType: String = "application/json") {
        server.enqueue(
            MockResponse.Builder()
                .code(code)
                .setHeader("Content-Type", contentType)
                .body(body)
                .build(),
        )
    }

    private fun success(result: LoadResult<Timetable>): Timetable {
        check(result is LoadResult.Success) { "Expected success, got $result" }
        return result.value
    }
}
