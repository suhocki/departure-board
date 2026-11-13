package com.sukhotski.timetable.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class TimetableE2eTest {

    @get:Rule
    val e2e = E2eRule(this)

    @Test
    fun coldStartShowsStationHeaderAndFirstRows() {
        e2e.enqueueFixture("departures-normal.json")

        e2e.launch()

        e2e.compose.waitUntilExactlyOneExists(hasText("Berlin central bus station"), TIMEOUT_MS)
        e2e.compose.onNodeWithText("Next 2 hours from", substring = true).assertExists()
        e2e.compose.onNodeWithText("(UTC+1)", substring = true).assertExists()
        e2e.compose.onNodeWithText("Bremen Fernbusterminal", useUnmergedTree = true)
            .assertExists()
        e2e.compose.onNodeWithText("Warsaw, Bus Station West", useUnmergedTree = true)
            .assertExists()
    }
}
