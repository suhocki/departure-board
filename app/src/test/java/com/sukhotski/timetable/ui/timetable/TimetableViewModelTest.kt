package com.sukhotski.timetable.ui.timetable

import app.cash.turbine.test
import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.domain.LoadResult
import com.sukhotski.timetable.domain.Station
import com.sukhotski.timetable.domain.Timetable
import com.sukhotski.timetable.testing.FakeTimetableRepository
import com.sukhotski.timetable.testing.MainDispatcherRule
import com.sukhotski.timetable.testing.MutableClock
import com.sukhotski.timetable.testing.departure
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimetableViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val start = Instant.parse("2026-11-13T16:05:00Z")
    private val clock = MutableClock(start)
    private val repository = FakeTimetableRepository()

    private val station =
        Station("Berlin central bus station", ZoneId.of("Europe/Berlin"), zoneIsFallback = false)
    private val timetable = Timetable(station, listOf(departure(id = "first")))

    @Test
    fun `shows loading then content`() = runTest {
        repository.results += LoadResult.Success(timetable)

        createViewModel().state.test {
            assertEquals(TimetableUiState.Loading, awaitItem())
            assertEquals(TimetableUiState.Loaded(timetable, updatedAt = start), awaitItem())
        }
    }

    @Test
    fun `shows empty content`() = runTest {
        val empty = Timetable(station, emptyList())
        repository.results += LoadResult.Success(empty)
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(TimetableUiState.Loaded(empty, updatedAt = start), viewModel.state.value)
    }

    @Test
    fun `shows no connection error`() = runTest {
        repository.results += LoadResult.Failure(LoadError.NoConnection)
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(TimetableUiState.Error(LoadError.NoConnection), viewModel.state.value)
    }

    @Test
    fun `shows unexpected error`() = runTest {
        repository.results += LoadResult.Failure(LoadError.Unexpected)
        val viewModel = createViewModel()

        advanceUntilIdle()

        assertEquals(TimetableUiState.Error(LoadError.Unexpected), viewModel.state.value)
    }

    @Test
    fun `asks for a two hour window from the current minute`() = runTest {
        clock.advanceBy(Duration.ofSeconds(32).plusMillis(485))
        repository.results += LoadResult.Success(timetable)
        createViewModel()

        advanceUntilIdle()

        assertEquals(
            listOf(
                FakeTimetableRepository.Call(
                    stationId = TimetableViewModel.STATION_ID,
                    from = Instant.parse("2026-11-13T16:05:00Z"),
                    to = Instant.parse("2026-11-13T18:05:00Z"),
                ),
            ),
            repository.calls,
        )
    }

    private fun createViewModel() = TimetableViewModel(repository, clock)
}
