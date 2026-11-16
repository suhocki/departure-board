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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sukhotski.timetable.R
import com.sukhotski.timetable.display.DelayLabel
import com.sukhotski.timetable.display.delayLabel
import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.DepartureStatus
import com.sukhotski.timetable.domain.delayMinutes
import com.sukhotski.timetable.ui.theme.TimetableTheme
import java.util.Locale
import kotlinx.datetime.TimeZone

/** Above this delay the row shows the expected clock time instead of minutes. */

private val STATUS_MAX_WIDTH = 120.dp

@Composable
fun DepartureRow(
    departure: Departure,
    zone: TimeZone,
    formatter: TimeFormatter,
    modifier: Modifier = Modifier,
) {
    val scheduledTime = formatter.time(departure.scheduled, zone)
    val isCancelled = departure.status is DepartureStatus.Cancelled
    val description = departureDescription(departure, scheduledTime, zone, formatter)
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
                    text = scheduledTime,
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
        // One node for screen readers, with a full sentence instead of three fragments.
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
    )
}

@Composable
private fun statusText(departure: Departure, zone: TimeZone, formatter: TimeFormatter): String? =
    when (val status = departure.status) {
        DepartureStatus.Scheduled -> null

        DepartureStatus.Cancelled -> stringResource(R.string.departure_cancelled)

        is DepartureStatus.Delayed -> {
            when (val label = delayLabel(departure.scheduled, status.expected)) {
                is DelayLabel.ExpectedAt ->
                    stringResource(
                        R.string.departure_expected,
                        formatter.time(label.expected, zone),
                    )

                is DelayLabel.Minutes -> stringResource(R.string.departure_delay, label.minutes)
            }
        }
    }

@Composable
private fun departureDescription(
    departure: Departure,
    scheduledTime: String,
    zone: TimeZone,
    formatter: TimeFormatter,
): String = when (val status = departure.status) {
    DepartureStatus.Scheduled ->
        stringResource(
            R.string.departure_a11y,
            departure.lineCode,
            departure.direction,
            scheduledTime,
        )

    DepartureStatus.Cancelled ->
        stringResource(
            R.string.departure_a11y_cancelled,
            departure.lineCode,
            departure.direction,
            scheduledTime,
        )

    is DepartureStatus.Delayed -> {
        val minutes = delayMinutes(departure.scheduled, status.expected).toInt()
        stringResource(
            R.string.departure_a11y_delayed,
            departure.lineCode,
            departure.direction,
            scheduledTime,
            pluralStringResource(R.plurals.delay_minutes_late, minutes, minutes),
            formatter.time(status.expected, zone),
        )
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
