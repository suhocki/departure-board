package com.sukhotski.timetable.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.testing.HiltAndroidTest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
class TimetableE2eTest {

    @get:Rule
    val e2e = E2eRule(this)

    private fun waitForText(text: String) =
        e2e.compose.waitUntilExactlyOneExists(hasText(text, substring = true), TIMEOUT_MS)

    @Test
    fun coldStartShowsStationHeaderAndFirstRows() {
        e2e.enqueueFixture("departures-normal.json")

        e2e.launch()

        waitForText("Berlin central bus station")
        e2e.compose.onNodeWithText("Next 2 hours from", substring = true).assertExists()
        e2e.compose.onNodeWithText("(UTC+1)", substring = true).assertExists()
        e2e.compose.onNodeWithText("Bremen Fernbusterminal", useUnmergedTree = true)
            .assertExists()
        e2e.compose.onNodeWithText("Warsaw, Bus Station West", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun errorWithoutResponseThenRetryLoadsTheList() {
        e2e.enqueueNoResponse()
        e2e.launch()
        waitForText("No connection")

        e2e.enqueueFixture("departures-normal.json")
        e2e.compose.onNodeWithText("Retry").performClick()

        waitForText("Berlin central bus station")
        assertEquals(2, e2e.server.requestCount)
    }

    @Test
    fun pullToRefreshSendsNewRequestAndShowsNewData() {
        e2e.enqueueFixture("departures-normal.json")
        e2e.launch()
        waitForText("Berlin central bus station")

        e2e.enqueueFixture("departures-cancelled.json")
        e2e.compose.onNode(hasScrollAction()).performTouchInput {
            swipeDown(startY = height * 0.2f, endY = height * 0.9f)
        }

        waitForText("Kyiv (Central Bus Station)")
        assertEquals(2, e2e.server.requestCount)
    }

    @Test
    fun rotationKeepsTheListWithoutNewRequest() {
        e2e.enqueueFixture("departures-normal.json")
        val scenario = e2e.launch()
        waitForText("Berlin central bus station")

        scenario.recreate()

        waitForText("Berlin central bus station")
        e2e.compose.waitForIdle()
        assertEquals(1, e2e.server.requestCount)
    }

    @Test
    fun returnAfterStaleTimeReloads() {
        e2e.enqueueFixture("departures-normal.json")
        val scenario = e2e.launch()
        waitForText("Berlin central bus station")

        // A short trip to the background keeps the data.
        scenario.moveToState(Lifecycle.State.CREATED)
        e2e.clock.advanceBy(30.seconds)
        scenario.moveToState(Lifecycle.State.RESUMED)
        e2e.compose.waitForIdle()
        assertEquals(1, e2e.server.requestCount)

        scenario.moveToState(Lifecycle.State.CREATED)
        e2e.clock.advanceBy(31.seconds)
        e2e.enqueueFixture("departures-cancelled.json")
        scenario.moveToState(Lifecycle.State.RESUMED)

        waitForText("Kyiv (Central Bus Station)")
        assertEquals(2, e2e.server.requestCount)
    }

    @Test
    fun relaunchedActivityLoadsAgain() {
        e2e.enqueueFixture("departures-normal.json")
        val first = e2e.launch()
        waitForText("Berlin central bus station")

        // A closed Activity loses its ViewModel, so the new one starts empty and loads.
        first.close()
        e2e.enqueueFixture("departures-cancelled.json")
        e2e.launch()

        waitForText("Kyiv (Central Bus Station)")
        assertEquals(2, e2e.server.requestCount)
    }

    @Test
    fun delayedRowsShowMinutesOrExpectedTime() {
        e2e.enqueueFixture("departures-normal.json")
        e2e.launch()
        waitForText("Berlin central bus station")

        // 74 min late is above the 30 min limit, 29 and 13 are not.
        e2e.compose.onNodeWithText("+29 min", useUnmergedTree = true).assertExists()
        e2e.compose.onNodeWithText("+13 min", useUnmergedTree = true).assertExists()
        e2e.compose.onAllNodesWithText("Expected ", substring = true, useUnmergedTree = true)
            .onFirst().assertExists()
        e2e.compose.onNodeWithText("+74 min", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun cancelledRideShowsTheWordCancelled() {
        e2e.enqueueFixture("departures-cancelled.json")
        e2e.launch()
        waitForText("Berlin central bus station")

        // The cancelled ride is the seventh row, below the first screenful on a small device.
        e2e.compose.onNode(hasScrollAction())
            .performScrollToNode(hasContentDescription("cancelled", substring = true))

        e2e.compose.onNodeWithText("Cancelled", useUnmergedTree = true).assertExists()
        e2e.compose.onNode(hasContentDescription("Line N3212", substring = true)).assertExists()
    }
}
