package com.sukhotski.timetable.ui.timetable

import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.DepartureStatus
import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.Station
import com.sukhotski.timetable.domain.Timetable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/**
 * One data set for previews, UI tests and screenshots. Friday 2026-11-13, 23:05 in Berlin.
 * It lives in main because previews need it. Nothing in the app uses it, so R8 removes it.
 */
object PreviewData {
    private val station =
        Station("Berlin central bus station", TimeZone.of("Europe/Berlin"), zoneIsFallback = false)

    val updatedAt: Instant = at("22:05:00")

    private val window: Duration = 2.hours

    val timetable = Timetable(
        station = station,
        departures = listOf(
            departure(
                "ride-1",
                "060",
                "Bremen Fernbusterminal",
                "21:10:00",
                DepartureStatus.Delayed(at("22:24:36")),
            ),
            departure("ride-2", "144", "Gorzów Wielkopolski, Main Station", "22:20:00"),
            departure(
                "ride-3",
                "N1338",
                "Medyka, Border Crossing",
                "22:20:00",
                DepartureStatus.Delayed(at("22:33:48")),
            ),
            departure("ride-4", "N1385", "Rome Tiburtina Bus station", "22:20:00"),
            departure(
                "ride-5",
                "N3207",
                "Berlin Airport BER (Berlin Brandenburg), Terminal 1/2",
                "22:30:00",
            ),
            departure(
                "ride-6",
                "N3212",
                "Bremen Fernbusterminal",
                "22:40:00",
                DepartureStatus.Cancelled,
            ),
            departure("ride-7", "N1325", "Bialystok, Bus Station", "23:00:00"),
        ),
    )

    val content = TimetableUiState.Loaded(timetable, updatedAt, window)

    val contentZoneUnknown = content.copy(
        timetable = timetable.copy(
            station = station.copy(zone = TimeZone.UTC, zoneIsFallback = true),
        ),
    )

    val errorNoConnection = TimetableUiState.Error(LoadError.NoConnection)

    val errorUnexpected = TimetableUiState.Error(LoadError.Unexpected)

    val empty = TimetableUiState.Loaded(Timetable(station, emptyList()), updatedAt, window)

    val refreshFailed = content.copy(refreshError = LoadError.NoConnection)

    private fun at(utcTime: String): Instant = Instant.parse("2026-11-13T${utcTime}Z")

    private fun departure(
        id: String,
        lineCode: String,
        direction: String,
        scheduledUtc: String,
        status: DepartureStatus = DepartureStatus.Scheduled,
    ) = Departure(id, lineCode, direction, at(scheduledUtc), status)
}
