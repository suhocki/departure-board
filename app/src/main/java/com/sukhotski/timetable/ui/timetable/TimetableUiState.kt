package com.sukhotski.timetable.ui.timetable

import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.Timetable
import java.time.Instant

sealed interface TimetableUiState {
    data object Loading : TimetableUiState

    data class Error(val error: LoadError) : TimetableUiState

    data class Loaded(
        val timetable: Timetable,
        /** The start of the requested window. */
        val updatedAt: Instant,
    ) : TimetableUiState
}
