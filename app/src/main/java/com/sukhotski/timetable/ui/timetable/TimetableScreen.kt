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
import androidx.compose.material3.TopAppBarDefaults
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
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
    val hours = state.window.toHours().toInt()
    Column(modifier) {
        state.refreshError?.let {
            RefreshErrorBanner(it, dataTime = formatter.time(state.updatedAt, station.zone))
        }
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            val items = remember(state.timetable, state.updatedAt) {
                timetableItems(state.timetable, state.updatedAt)
            }
            // The header scrolls with the rows, so large fonts and landscape keep room for the list.
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                item(key = "header") { StationHeader(station, state.updatedAt, hours, formatter) }
                if (items.isEmpty()) {
                    item {
                        EmptyContent(hours, Modifier.fillParentMaxWidth().fillParentMaxHeight(0.7f))
                    }
                }
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.key },
                    contentType = { _, item -> item::class },
                ) { index, item ->
                    if (index > 0) HorizontalDivider()
                    when (item) {
                        is TimetableItem.DateHeader -> DateHeader(formatter.date(item.date))

                        is TimetableItem.Ride -> DepartureRow(
                            item.departure,
                            station.zone,
                            formatter,
                        )
                    }
                }
            }
        }
    }
}

private val TimetableItem.key: String
    get() = when (this) {
        is TimetableItem.DateHeader -> "date:${date.toEpochDay()}"
        is TimetableItem.Ride -> departure.id
    }

@Composable
private fun StationHeader(
    station: Station,
    updatedAt: Instant,
    hours: Int,
    formatter: TimeFormatter,
) {
    // The window start is also the data time, so the user sees how old the list is.
    val from = formatter.time(updatedAt, station.zone)
    val caption = if (station.zoneIsFallback) {
        pluralStringResource(R.plurals.header_caption_zone_unknown, hours, hours, from)
    } else {
        pluralStringResource(
            R.plurals.header_caption,
            hours,
            hours,
            from,
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

@Composable
private fun DateHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
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
