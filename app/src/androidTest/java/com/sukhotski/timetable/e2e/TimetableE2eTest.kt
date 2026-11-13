package com.sukhotski.timetable.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Duration
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
        e2e.clock.advanceBy(Duration.ofSeconds(30))
        scenario.moveToState(Lifecycle.State.RESUMED)
        e2e.compose.waitForIdle()
        assertEquals(1, e2e.server.requestCount)

        scenario.moveToState(Lifecycle.State.CREATED)
        e2e.clock.advanceBy(Duration.ofSeconds(31))
        e2e.enqueueFixture("departures-cancelled.json")
        scenario.moveToState(Lifecycle.State.RESUMED)

        waitForText("Kyiv (Central Bus Station)")
        assertEquals(2, e2e.server.requestCount)
    }
}
