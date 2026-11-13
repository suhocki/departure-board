package com.sukhotski.timetable.data

import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.domain.TimetableRepository
import java.time.Instant

class DefaultTimetableRepository(private val api: TimetableApi) : TimetableRepository {

    override suspend fun departures(
        stationId: String,
        from: Instant,
        to: Instant,
    ): LoadResult<Timetable> {
        val response = api.departures(stationId, from.toString(), to.toString())
        val timetable = response.toTimetable() ?: return LoadResult.Failure(LoadError.Unexpected)
        return LoadResult.Success(timetable)
    }
}
