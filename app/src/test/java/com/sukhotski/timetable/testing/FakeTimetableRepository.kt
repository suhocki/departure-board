package com.sukhotski.timetable.testing

import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.domain.TimetableRepository
import java.time.Instant

class FakeTimetableRepository : TimetableRepository {
    data class Call(val stationId: String, val from: Instant, val to: Instant)

    /** Returned one by one, in order. */
    val results = ArrayDeque<LoadResult<Timetable>>()
    val calls = mutableListOf<Call>()

    override suspend fun departures(
        stationId: String,
        from: Instant,
        to: Instant,
    ): LoadResult<Timetable> {
        calls += Call(stationId, from, to)
        return results.removeFirst()
    }
}
