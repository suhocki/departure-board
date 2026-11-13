package com.sukhotski.timetable.ui.timetable

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.ui.theme.TimetableTheme
import java.time.ZoneId
import java.util.Locale

@Composable
fun DepartureRow(
    departure: Departure,
    zone: ZoneId,
    formatter: TimeFormatter,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(departure.direction) },
        supportingContent = { Text(departure.lineCode) },
        trailingContent = {
            Text(
                text = formatter.time(departure.scheduled, zone),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun DepartureRowPreview() {
    TimetableTheme {
        DepartureRow(
            departure = PreviewData.timetable.departures.first(),
            zone = PreviewData.timetable.station.zone,
            formatter = TimeFormatter(is24Hour = true, Locale.US),
        )
    }
}
