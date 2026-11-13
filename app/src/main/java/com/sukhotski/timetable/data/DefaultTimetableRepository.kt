package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.domain.TimetableRepository
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class DefaultTimetableRepository @Inject constructor(private val api: TimetableApi) :
    TimetableRepository {

    // Error bodies are never read: the API sends plain text, JSON or nothing.
    // Anything other than a connection failure is Unexpected: HTTP errors, a body that does not
    // parse, a 2xx without a body. Only cancellation is rethrown.
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
        } catch (_: Exception) {
            return LoadResult.Failure(LoadError.Unexpected)
        }
        val timetable = response.toTimetable() ?: return LoadResult.Failure(LoadError.Unexpected)
        return LoadResult.Success(timetable)
    }
}
