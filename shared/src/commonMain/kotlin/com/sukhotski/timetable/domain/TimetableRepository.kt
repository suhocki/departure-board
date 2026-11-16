package com.sukhotski.timetable.domain

import kotlin.time.Instant

interface TimetableRepository {
    suspend fun departures(stationId: String, from: Instant, to: Instant): LoadResult<Timetable>
}
