package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.data.remote.TimetableResponse
import com.sukhotski.timetable.data.remote.timetableJson
import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.domain.TimetableRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.io.IOException

class DefaultTimetableRepository(
    private val api: TimetableApi,
    private val errorReporter: ErrorReporter,
    private val mappingDispatcher: CoroutineDispatcher,
) : TimetableRepository {

    // Error bodies are never read: the API sends plain text, JSON or nothing.
    // Any IOException (connection, timeout) is NoConnection. Anything else is Unexpected: HTTP
    // errors, a body that does not parse, a 2xx without a body. Only cancellation is rethrown.
    // Connection failures are not reported, they are the user's network, not a bug.
    // Ktor messages have the request URL and the URL has the key, so only a short text is reported.
    override suspend fun departures(
        stationId: String,
        from: Instant,
        to: Instant,
    ): LoadResult<Timetable> {
        val body = try {
            api.departures(stationId, from.toString(), to.toString())
        } catch (_: IOException) {
            return LoadResult.Failure(LoadError.NoConnection)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnexpectedResponseException) {
            return unexpected(e)
        } catch (e: Exception) {
            return unexpected(UnexpectedResponseException(e::class.simpleName ?: "Exception"))
        }
        // JSON, dates and the sort are CPU work: all of it runs on the mapping dispatcher.
        val (response, timetable) = try {
            withContext(mappingDispatcher) {
                val decoded = timetableJson.decodeFromString<TimetableResponse>(body)
                decoded to decoded.toTimetable()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return unexpected(UnexpectedResponseException(e::class.simpleName ?: "Exception"))
        }
        if (timetable == null) return unexpected(UnexpectedResponseException("No station name"))
        val dropped = response.rides.size - timetable.departures.size
        if (dropped > 0) errorReporter.droppedRides(dropped)
        return LoadResult.Success(timetable)
    }

    private fun unexpected(error: Throwable): LoadResult<Nothing> {
        errorReporter.report(error)
        return LoadResult.Failure(LoadError.Unexpected)
    }
}

class UnexpectedResponseException(message: String) : Exception(message)
