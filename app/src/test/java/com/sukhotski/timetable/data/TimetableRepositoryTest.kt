package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.data.remote.createTimetableHttpClient
import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.testing.FakeErrorReporter
import com.sukhotski.timetable.testing.fixture
import com.sukhotski.timetable.testing.ridesJson
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private typealias Responder = suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData

class TimetableRepositoryTest {

    private val reporter = FakeErrorReporter()
    private val requests = mutableListOf<HttpRequestData>()
    private val responses = ArrayDeque<Responder>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mapping = UnconfinedTestDispatcher()
    private lateinit var repository: DefaultTimetableRepository

    private val from = Instant.parse("2026-11-13T16:05:00Z")
    private val to = from + 2.hours

    @Before
    fun setUp() {
        repository = repositoryWith(mapping)
    }

    @Test
    fun `requests two hour window in UTC with api key`() = runTest {
        enqueue(200, fixture("departures-empty.json"))

        repository.departures("station-1", from, to)

        val url = requests.single().url
        assertEquals("/gis/v2/timetable/station-1/departures", url.encodedPath)
        assertEquals("2026-11-13T16:05:00Z", url.parameters["from"])
        assertEquals("2026-11-13T18:05:00Z", url.parameters["to"])
        assertEquals("test-key", url.parameters["apiKey"])
    }

    @Test
    fun `parses full real response with unknown fields`() = runTest {
        enqueue(200, fixture("departures-full.json"))

        val timetable = success(repository.departures("station-1", from, to))

        assertEquals(11, timetable.departures.size)
        assertEquals("060", timetable.departures.first().lineCode)
        assertEquals(TimeZone.of("Europe/Berlin"), timetable.station.zone)
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
        responses += { respondError(HttpStatusCode.NoContent) }

        assertEquals(
            LoadResult.Failure(LoadError.Unexpected),
            repository.departures("station-1", from, to),
        )
    }

    @Test
    fun `maps connection failure to no connection`() = runTest {
        responses += { throw IOException("down") }

        assertEquals(
            LoadResult.Failure(LoadError.NoConnection),
            repository.departures("station-1", from, to),
        )
    }

    @Test
    fun `maps request timeout to no connection`() = runTest {
        responses += {
            delay(10.seconds)
            respond("")
        }
        val slowRepository = repositoryWith(mapping, timeout = 100.milliseconds)

        val result = slowRepository.departures("station-1", from, to)

        assertEquals(LoadResult.Failure(LoadError.NoConnection), result)
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

    @Test
    fun `reports the number of rides that were not shown`() = runTest {
        val good =
            """{"id":"a","status":{"scheduled_timestamp":"2026-11-13T16:20:00Z"},"line":{"code":"N1","direction":"A"}}"""
        enqueue(200, ridesJson(good, """{"id":"b"}""", """{"id":123}"""))

        val timetable = success(repository.departures("station-1", from, to))

        assertEquals(1, timetable.departures.size)
        assertEquals(listOf(2), reporter.dropped)
        assertTrue(reporter.errors.isEmpty())
    }

    @Test
    fun `reports nothing for a clean response or a connection failure`() = runTest {
        enqueue(200, fixture("departures-normal.json"))
        responses += { throw IOException("down") }
        repository.departures("station-1", from, to)
        repository.departures("station-1", from, to)

        assertTrue(reporter.dropped.isEmpty())
        assertTrue(reporter.errors.isEmpty())
    }

    @Test
    fun `reports unexpected failures without the url or the key`() = runTest {
        enqueue(401, fixture("error-401.txt"), contentType = "text/plain")
        enqueue(200, "not json")
        enqueue(200, """{"rides":[]}""")

        repeat(3) { repository.departures("station-1", from, to) }

        assertEquals(3, reporter.errors.size)
        for (error in reporter.errors) {
            val text = error.stackTraceToString()
            assertFalse(text, "test-key" in text || TEST_HOST in text)
        }
    }

    @Test
    fun `maps the response on the injected dispatcher`() = runTest {
        val dispatcher = CountingDispatcher()
        val repository = repositoryWith(dispatcher)
        enqueue(200, fixture("departures-normal.json"))

        success(repository.departures("station-1", from, to))

        assertEquals(1, dispatcher.dispatches)
    }

    private fun repositoryWith(
        dispatcher: CoroutineDispatcher,
        timeout: kotlin.time.Duration = 15.seconds,
    ): DefaultTimetableRepository {
        val engine = MockEngine { request ->
            requests += request
            responses.removeFirst()(request)
        }
        val client = createTimetableHttpClient(engine, "https://$TEST_HOST/", timeout)
        return DefaultTimetableRepository(TimetableApi(client, "test-key"), reporter, dispatcher)
    }

    private fun enqueue(code: Int, body: String, contentType: String = "application/json") {
        responses += {
            respond(
                content = body,
                status = HttpStatusCode.fromValue(code),
                headers = headersOf(HttpHeaders.ContentType, contentType),
            )
        }
    }

    private fun success(result: LoadResult<Timetable>): Timetable {
        check(result is LoadResult.Success) { "Expected success, got $result" }
        return result.value
    }

    /** Runs on Dispatchers.Default and counts how often work was sent to it. */
    private class CountingDispatcher : CoroutineDispatcher() {
        @Volatile
        var dispatches = 0

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatches++
            Dispatchers.Default.dispatch(context, block)
        }
    }

    private companion object {
        const val TEST_HOST = "timetable.test"
    }
}
