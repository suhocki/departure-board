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
    private val newerTimetable = Timetable(station, listOf(departure(id = "second")))

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

    @Test
    fun `retry after error loads again`() = runTest {
        repository.results += LoadResult.Failure(LoadError.NoConnection)
        repository.results += LoadResult.Success(timetable)
        val viewModel = createViewModel()

        viewModel.state.test {
            assertEquals(TimetableUiState.Loading, awaitItem())
            assertEquals(TimetableUiState.Error(LoadError.NoConnection), awaitItem())
            viewModel.refresh()
            assertEquals(TimetableUiState.Loading, awaitItem())
            assertEquals(TimetableUiState.Loaded(timetable, updatedAt = start), awaitItem())
        }
    }

    @Test
    fun `refresh keeps content while loading`() = runTest {
        repository.results += LoadResult.Success(timetable)
        repository.results += LoadResult.Success(newerTimetable)
        val viewModel = createViewModel()

        viewModel.state.test {
            skipItems(1)
            val loaded = TimetableUiState.Loaded(timetable, updatedAt = start)
            assertEquals(loaded, awaitItem())
            viewModel.refresh()
            assertEquals(loaded.copy(isRefreshing = true), awaitItem())
            assertEquals(TimetableUiState.Loaded(newerTimetable, updatedAt = start), awaitItem())
        }
    }

    @Test
    fun `refresh asks for a new window from the clock`() = runTest {
        repository.results += LoadResult.Success(timetable)
        repository.results += LoadResult.Success(newerTimetable)
        val viewModel = createViewModel()
        advanceUntilIdle()

        clock.advanceBy(Duration.ofMinutes(10))
        viewModel.refresh()
        advanceUntilIdle()

        val later = start + Duration.ofMinutes(10)
        assertEquals(
            FakeTimetableRepository.Call(
                TimetableViewModel.STATION_ID,
                later,
                later + Duration.ofHours(2),
            ),
            repository.calls.last(),
        )
        assertEquals(
            TimetableUiState.Loaded(newerTimetable, updatedAt = later),
            viewModel.state.value,
        )
    }

    @Test
    fun `ignores refresh while a request is running`() = runTest {
        repository.results += LoadResult.Success(timetable)
        repository.results += LoadResult.Success(newerTimetable)
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, repository.calls.size)
    }

    @Test
    fun `failed refresh keeps content and sets refresh error`() = runTest {
        repository.results += LoadResult.Success(timetable)
        repository.results += LoadResult.Failure(LoadError.NoConnection)
        val viewModel = createViewModel()
        advanceUntilIdle()

        clock.advanceBy(Duration.ofMinutes(10))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(
            TimetableUiState.Loaded(
                timetable,
                updatedAt = start,
                isRefreshing = false,
                refreshError = LoadError.NoConnection,
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun `successful refresh clears refresh error`() = runTest {
        repository.results += LoadResult.Success(timetable)
        repository.results += LoadResult.Failure(LoadError.NoConnection)
        repository.results += LoadResult.Success(newerTimetable)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.state.test {
            val failed = awaitItem()
            viewModel.refresh()
            assertEquals(
                (failed as TimetableUiState.Loaded).copy(isRefreshing = true, refreshError = null),
                awaitItem(),
            )
            assertEquals(TimetableUiState.Loaded(newerTimetable, updatedAt = start), awaitItem())
        }
    }

    @Test
    fun `refreshes stale data on return`() = runTest {
        repository.results += LoadResult.Success(timetable)
        repository.results += LoadResult.Success(newerTimetable)
        val viewModel = createViewModel()
        advanceUntilIdle()

        clock.advanceBy(Duration.ofSeconds(61))
        viewModel.refreshIfStale()
        advanceUntilIdle()

        assertEquals(2, repository.calls.size)
        assertEquals(start + Duration.ofMinutes(1), repository.calls.last().from)
    }

    @Test
    fun `does not refresh fresh data on return`() = runTest {
        repository.results += LoadResult.Success(timetable)
        val viewModel = createViewModel()
        advanceUntilIdle()
        clock.advanceBy(Duration.ofSeconds(59))

        viewModel.refreshIfStale()
        advanceUntilIdle()

        assertEquals(1, repository.calls.size)

        repository.results += LoadResult.Failure(LoadError.Unexpected)
        val failedViewModel = createViewModel()
        advanceUntilIdle()
        clock.advanceBy(Duration.ofHours(1))

        failedViewModel.refreshIfStale()
        advanceUntilIdle()

        assertEquals(2, repository.calls.size)
    }

    private fun createViewModel() = TimetableViewModel(repository, clock)
}
