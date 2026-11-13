package com.sukhotski.timetable.ui.timetable

import android.text.format.DateFormat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.sukhotski.timetable.R
import com.sukhotski.timetable.ui.theme.TimetableTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    state: TimetableUiState,
    modifier: Modifier = Modifier,
    formatter: TimeFormatter = rememberTimeFormatter(),
) {
    val listState = rememberLazyListState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.timetable_title)) })
        },
    ) { padding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(padding)
        when (state) {
            TimetableUiState.Loading, is TimetableUiState.Error -> Unit

            is TimetableUiState.Loaded -> LoadedContent(
                state,
                listState,
                formatter,
                contentModifier,
            )
        }
    }
}

@Composable
private fun LoadedContent(
    state: TimetableUiState.Loaded,
    listState: LazyListState,
    formatter: TimeFormatter,
    modifier: Modifier = Modifier,
) {
    val station = state.timetable.station
    LazyColumn(state = listState, modifier = modifier) {
        itemsIndexed(
            items = state.timetable.departures,
            key = { _, departure -> departure.id },
        ) { index, departure ->
            if (index > 0) HorizontalDivider()
            DepartureRow(departure, station.zone, formatter)
        }
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
        TimetableScreen(PreviewData.content, formatter = TimeFormatter(is24Hour = true, Locale.US))
    }
}
