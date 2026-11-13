package com.sukhotski.timetable.ui.timetable

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sukhotski.timetable.R
import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.domain.DepartureStatus
import com.sukhotski.timetable.testing.departure
import com.sukhotski.timetable.ui.theme.TimetableTheme
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DepartureRowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.of("Europe/Berlin")
    private val formatter = TimeFormatter(is24Hour = true, Locale.US)

    // 23:30 in Berlin, with the 53 character direction.
    private val airportRide = PreviewData.timetable.departures[4]

    @Test
    fun `shows direction line code and scheduled time`() {
        setRows(airportRide)

        composeRule.onNodeWithText(
            "Berlin Airport BER (Berlin Brandenburg), Terminal 1/2",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("N3207").assertIsDisplayed()
        composeRule.onNodeWithText("23:30").assertIsDisplayed()
    }

    @Test
    fun `shows 12 hour time when the device uses it`() {
        setRows(airportRide, formatter = TimeFormatter(is24Hour = false, Locale.US))

        composeRule.onNodeWithText("11:30 PM").assertIsDisplayed()
    }

    @Test
    fun `delay up to 30 minutes shows minutes`() {
        setRows(
            delayed(id = "a", minutes = 1),
            delayed(id = "b", minutes = 13),
            delayed(id = "c", minutes = 30),
        )

        for (minutes in listOf(1, 13, 30)) {
            composeRule.onNodeWithText(
                context.getString(R.string.departure_delay, minutes),
            ).assertIsDisplayed()
        }
    }

    @Test
    fun `delay above 30 minutes shows expected time`() {
        // Scheduled 17:20 in Berlin, 31 minutes late.
        val late = delayed(id = "a", minutes = 31)
        val veryLate = departure(
            id = "b",
            lineCode = "060",
            scheduled = Instant.parse("2026-11-13T15:10:00Z"),
            status = DepartureStatus.Delayed(Instant.parse("2026-11-13T16:24:36Z")),
        )
        setRows(late, veryLate)

        composeRule.onNodeWithText(
            context.getString(R.string.departure_expected, "17:51"),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.departure_delay, 31),
        ).assertDoesNotExist()
        composeRule.onNodeWithText("16:10").assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.departure_expected, "17:24"),
        ).assertIsDisplayed()
    }

    @Test
    fun `scheduled ride shows no status`() {
        setRows(departure())

        composeRule.onNodeWithText("min", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText(
            context.getString(R.string.departure_expected, ""),
            substring = true,
        ).assertDoesNotExist()
    }

    private fun setRows(vararg departures: Departure, formatter: TimeFormatter = this.formatter) {
        composeRule.setContent {
            TimetableTheme {
                Column {
                    departures.forEach { DepartureRow(it, zone, formatter) }
                }
            }
        }
    }

    /** A ride scheduled at 17:20 in Berlin, [minutes] late. */
    private fun delayed(id: String, minutes: Long): Departure {
        val scheduled = Instant.parse("2026-11-13T16:20:00Z")
        return departure(
            id = id,
            scheduled = scheduled,
            status = DepartureStatus.Delayed(scheduled + Duration.ofMinutes(minutes)),
        )
    }
}
