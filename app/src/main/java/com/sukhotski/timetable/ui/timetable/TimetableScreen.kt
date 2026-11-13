package com.sukhotski.timetable.ui.timetable

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sukhotski.timetable.R
import com.sukhotski.timetable.domain.Station
import com.sukhotski.timetable.ui.theme.TimetableTheme
import java.time.Instant
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    state: TimetableUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    formatter: TimeFormatter = rememberTimeFormatter(),
) {
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timetable_title)) },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            painterResource(R.drawable.ic_refresh),
                            contentDescription = stringResource(R.string.action_refresh),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when (state) {
            TimetableUiState.Loading -> LoadingContent(contentModifier)

            is TimetableUiState.Error ->
                ErrorContent(state.error, onRetry = onRefresh, modifier = contentModifier)

            is TimetableUiState.Loaded ->
                LoadedContent(state, onRefresh, listState, formatter, contentModifier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedContent(
    state: TimetableUiState.Loaded,
    onRefresh: () -> Unit,
    listState: LazyListState,
    formatter: TimeFormatter,
    modifier: Modifier = Modifier,
) {
    val station = state.timetable.station
    val hours = TimetableViewModel.WINDOW.toHours().toInt()
    Column(modifier) {
        StationHeader(station, state.updatedAt, hours)
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                if (state.timetable.departures.isEmpty()) {
                    item { EmptyContent(hours, Modifier.fillParentMaxSize()) }
                }
                itemsIndexed(
                    items = state.timetable.departures,
                    key = { _, departure -> departure.id },
                ) { index, departure ->
                    if (index > 0) HorizontalDivider()
                    DepartureRow(departure, station.zone, formatter)
                }
            }
        }
    }
}

@Composable
private fun StationHeader(station: Station, updatedAt: Instant, hours: Int) {
    val caption = if (station.zoneIsFallback) {
        pluralStringResource(R.plurals.header_caption_zone_unknown, hours, hours)
    } else {
        pluralStringResource(
            R.plurals.header_caption,
            hours,
            hours,
            formatUtcOffset(station.zone.rules.getOffset(updatedAt)),
        )
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = station.name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The only place that reads the device 12/24 hour setting and locale. */
@Composable
fun rememberTimeFormatter(): TimeFormatter {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val locale: Locale = LocalConfiguration.current.locales[0]
    return remember(is24Hour, locale) { TimeFormatter(is24Hour, locale) }
}

@Preview
@Composable
private fun TimetableScreenPreview() {
    TimetableTheme {
        TimetableScreen(PreviewData.content, onRefresh = {
        }, formatter = TimeFormatter(is24Hour = true, Locale.US))
    }
}
