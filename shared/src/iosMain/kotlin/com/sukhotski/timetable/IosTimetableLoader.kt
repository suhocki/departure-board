package com.sukhotski.timetable

import com.sukhotski.timetable.data.DefaultTimetableRepository
import com.sukhotski.timetable.data.NoOpErrorReporter
import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.data.remote.createTimetableHttpClient
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.domain.TimetableDefaults
import io.ktor.client.engine.darwin.Darwin
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers

/** The one entry point for Swift. It builds the Darwin client and loads the next two hours. */
class IosTimetableLoader(apiKey: String) {

    private val repository = DefaultTimetableRepository(
        TimetableApi(createTimetableHttpClient(Darwin.create()), apiKey),
        NoOpErrorReporter(),
        Dispatchers.Default,
    )

    /** The window starts at the current minute, same as on Android. */
    @Throws(CancellationException::class)
    suspend fun load(): LoadResult<Timetable> {
        val now = Clock.System.now()
        val from = Instant.fromEpochSeconds(now.epochSeconds / 60 * 60)
        return repository.departures(
            TimetableDefaults.STATION_ID,
            from,
            from + TimetableDefaults.WINDOW,
        )
    }
}
