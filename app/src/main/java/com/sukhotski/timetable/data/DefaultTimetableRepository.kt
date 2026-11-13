package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.domain.TimetableRepository
import java.io.IOException
import java.time.Instant
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

class DefaultTimetableRepository(private val api: TimetableApi) : TimetableRepository {

    // Error bodies are never read: the API sends plain text, JSON or nothing.
    override suspend fun departures(
        stationId: String,
        from: Instant,
        to: Instant,
    ): LoadResult<Timetable> {
        val response = try {
            api.departures(stationId, from.toString(), to.toString())
        } catch (_: IOException) {
            return LoadResult.Failure(LoadError.NoConnection)
        } catch (_: HttpException) {
            return LoadResult.Failure(LoadError.Unexpected)
        } catch (_: SerializationException) {
            return LoadResult.Failure(LoadError.Unexpected)
        }
        val timetable = response.toTimetable() ?: return LoadResult.Failure(LoadError.Unexpected)
        return LoadResult.Success(timetable)
    }
}
