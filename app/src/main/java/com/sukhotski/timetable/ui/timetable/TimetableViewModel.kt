package com.sukhotski.timetable.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TimetableViewModel @Inject constructor(
    private val repository: TimetableRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow<TimetableUiState>(TimetableUiState.Loading)
    val state: StateFlow<TimetableUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val now = clock.instant().truncatedTo(ChronoUnit.MINUTES)
            _state.value =
                when (val result = repository.departures(STATION_ID, now, now + WINDOW)) {
                    is LoadResult.Success -> TimetableUiState.Loaded(result.value, updatedAt = now)
                    is LoadResult.Failure -> TimetableUiState.Error(result.error)
                }
        }
    }

    companion object {
        /** Berlin central bus station. */
        const val STATION_ID = "dcbaaee6-9603-11e6-9066-549f350fcb0c"
        val WINDOW: Duration = Duration.ofHours(2)
    }
}
