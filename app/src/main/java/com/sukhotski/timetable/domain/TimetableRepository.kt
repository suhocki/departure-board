package com.sukhotski.timetable.domain

import java.time.Instant

interface TimetableRepository {
    suspend fun departures(stationId: String, from: Instant, to: Instant): LoadResult<Timetable>
}
