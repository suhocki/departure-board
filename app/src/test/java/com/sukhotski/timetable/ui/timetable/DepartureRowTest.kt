package com.sukhotski.timetable.ui.timetable

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sukhotski.timetable.domain.Departure
import com.sukhotski.timetable.ui.theme.TimetableTheme
import java.time.ZoneId
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DepartureRowTest {

    @get:Rule
    val composeRule = createComposeRule()

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

    private fun setRows(vararg departures: Departure, formatter: TimeFormatter = this.formatter) {
        composeRule.setContent {
            TimetableTheme {
                Column {
                    departures.forEach { DepartureRow(it, zone, formatter) }
                }
            }
        }
    }
}
