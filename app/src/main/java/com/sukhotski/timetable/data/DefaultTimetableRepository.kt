package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.di.DefaultDispatcher
import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.domain.TimetableRepository
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class DefaultTimetableRepository @Inject constructor(
    private val api: TimetableApi,
    private val errorReporter: ErrorReporter,
    @param:DefaultDispatcher private val mappingDispatcher: CoroutineDispatcher,
) : TimetableRepository {

    // Error bodies are never read: the API sends plain text, JSON or nothing.
    // Anything other than a connection failure is Unexpected: HTTP errors, a body that does not
    // parse, a 2xx without a body. Only cancellation is rethrown. Connection failures are not
    // reported, they are the user's network, not a bug.
    override suspend fun departures(
        stationId: String,
        from: Instant,
        to: Instant,
    ): LoadResult<Timetable> {
        val response = try {
            api.departures(stationId, from.toString(), to.toString())
        } catch (_: IOException) {
            return LoadResult.Failure(LoadError.NoConnection)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            // HttpException keeps the request, and the request URL has the key.
            return unexpected(UnexpectedResponseException("HTTP ${e.code()}"))
        } catch (e: Exception) {
            return unexpected(e)
        }
        // Retrofit parses JSON off the main thread, but the dates and the sort run here.
        val timetable = withContext(mappingDispatcher) { response.toTimetable() }
            ?: return unexpected(UnexpectedResponseException("No station name"))
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
