package com.sukhotski.timetable.ui.timetable

import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.Timetable
import java.time.Duration
import java.time.Instant

sealed interface TimetableUiState {
    data object Loading : TimetableUiState

    data class Error(val error: LoadError) : TimetableUiState

    data class Loaded(
        val timetable: Timetable,
        /**
         * The start of the requested window. Also "today" for date headers
         * and the data time in the refresh banner.
         */
        val updatedAt: Instant,
        /** Length of the requested window, shown in the header. */
        val window: Duration,
        val isRefreshing: Boolean = false,
        /** Set when a refresh failed and the list on screen is older. */
        val refreshError: LoadError? = null,
    ) : TimetableUiState
}
