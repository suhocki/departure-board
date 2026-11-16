package com.sukhotski.timetable.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.TimetableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.Job
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

    private var loadJob: Job? = null

    /** When the list on screen was loaded, to the second. updatedAt is cut to the minute. */
    private var loadedAt: Instant = Instant.fromEpochSeconds(0)

    init {
        load()
    }

    /** Pull to refresh, the Refresh action and Retry. */
    fun refresh() {
        load()
    }

    /**
     * Called when the app comes back to the foreground. An error is retried: the user may have
     * just turned the network on. Data older than [STALE_AFTER] is reloaded.
     */
    fun refreshIfStale() {
        when (_state.value) {
            TimetableUiState.Loading -> Unit

            is TimetableUiState.Error -> load()

            is TimetableUiState.Loaded ->
                if (clock.now() - loadedAt > STALE_AFTER) load()
        }
    }

    private fun load() {
        if (loadJob?.isActive == true) return
        val previous = _state.value as? TimetableUiState.Loaded
        _state.value =
            previous?.copy(isRefreshing = true, refreshError = null) ?: TimetableUiState.Loading
        loadJob = viewModelScope.launch {
            val startedAt = clock.now()
            val now = Instant.fromEpochSeconds(startedAt.epochSeconds / 60 * 60)
            _state.value =
                when (val result = repository.departures(STATION_ID, now, now + WINDOW)) {
                    is LoadResult.Success -> {
                        loadedAt = startedAt
                        TimetableUiState.Loaded(result.value, updatedAt = now, window = WINDOW)
                    }

                    is LoadResult.Failure ->
                        previous?.copy(isRefreshing = false, refreshError = result.error)
                            ?: TimetableUiState.Error(result.error)
                }
        }
    }

    companion object {
        /** Berlin central bus station. */
        const val STATION_ID = "dcbaaee6-9603-11e6-9066-549f350fcb0c"
        val WINDOW: Duration = 2.hours
        val STALE_AFTER: Duration = 1.minutes
    }
}
