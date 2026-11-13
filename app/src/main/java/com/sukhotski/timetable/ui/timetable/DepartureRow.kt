package com.sukhotski.timetable.ui.timetable

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sukhotski.timetable.R
import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.DepartureStatus
import com.sukhotski.timetable.domain.delayMinutes
import com.sukhotski.timetable.ui.theme.TimetableTheme
import java.time.ZoneId
import java.util.Locale

/** Above this delay the row shows the expected clock time instead of minutes. */
private const val EXPECTED_TIME_THRESHOLD_MINUTES = 30

private val STATUS_MAX_WIDTH = 120.dp

@Composable
fun DepartureRow(
    departure: Departure,
    zone: ZoneId,
    formatter: TimeFormatter,
    modifier: Modifier = Modifier,
) {
    val isCancelled = departure.status is DepartureStatus.Cancelled
    // The word "Cancelled" carries the meaning. Color and strike-through only support it.
    val textColor = if (isCancelled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        Color.Unspecified
    }
    ListItem(
        headlineContent = { Text(departure.direction, color = textColor) },
        supportingContent = { Text(departure.lineCode, color = textColor) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatter.time(departure.scheduled, zone),
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (isCancelled) TextDecoration.LineThrough else null,
                )
                statusText(departure, zone, formatter)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.End,
                        // Wraps at large font sizes instead of squeezing the direction.
                        modifier = Modifier.widthIn(max = STATUS_MAX_WIDTH),
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier,
    )
}

@Composable
private fun statusText(departure: Departure, zone: ZoneId, formatter: TimeFormatter): String? =
    when (val status = departure.status) {
        DepartureStatus.Scheduled -> null

        DepartureStatus.Cancelled -> stringResource(R.string.departure_cancelled)

        is DepartureStatus.Delayed -> {
            val minutes = delayMinutes(departure.scheduled, status.expected)
            if (minutes > EXPECTED_TIME_THRESHOLD_MINUTES) {
                stringResource(R.string.departure_expected, formatter.time(status.expected, zone))
            } else {
                stringResource(R.string.departure_delay, minutes)
            }
        }
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
