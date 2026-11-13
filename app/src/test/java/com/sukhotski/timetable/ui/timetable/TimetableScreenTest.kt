package com.sukhotski.timetable.ui.timetable

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sukhotski.timetable.R
import com.sukhotski.timetable.domain.LoadError
import com.sukhotski.timetable.ui.theme.TimetableTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimetableScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `content shows direction line code and time`() {
        setScreen(PreviewData.content)

        composeRule.onNodeWithText(context.getString(R.string.timetable_title)).assertIsDisplayed()
        composeRule.onNodeWithText("Medyka, Border Crossing").assertIsDisplayed()
        composeRule.onNodeWithText("N1338").assertIsDisplayed()
        composeRule.onNodeWithText("23:30").assertIsDisplayed()
        PreviewData.timetable.departures.forEach {
            composeRule.onNodeWithText(it.lineCode).assertExists()
        }
    }

    @Test
    fun `content shows station name and caption with window start`() {
        setScreen(PreviewData.content)

        composeRule.onNode(
            hasText("Berlin central bus station") and isHeading(),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.header_caption, 2, 2, "23:05", "UTC+1"),
        ).assertIsDisplayed()
    }

    @Test
    fun `station header scrolls with the list`() {
        setScreen(PreviewData.content)

        composeRule.onNode(
            hasText("Berlin central bus station") and hasAnyAncestor(hasScrollToIndexAction()),
        ).assertExists()
    }

    @Test
    fun `unknown station zone shows the UTC caption`() {
        setScreen(PreviewData.contentZoneUnknown)

        composeRule.onNodeWithText(
            context.resources.getQuantityString(
                R.plurals.header_caption_zone_unknown,
                2,
                2,
                "22:05",
            ),
        ).assertIsDisplayed()
        composeRule.onNodeWithText("22:30").assertIsDisplayed()
    }

    @Test
    fun `loading shows progress`() {
        setScreen(TimetableUiState.Loading)

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.loading_description),
        ).assertExists()
        composeRule.onNodeWithText("Berlin central bus station").assertDoesNotExist()
    }

    @Test
    fun `refresh action is disabled only while loading`() {
        var state by mutableStateOf<TimetableUiState>(TimetableUiState.Loading)
        composeRule.setContent {
            TimetableTheme {
                TimetableScreen(
                    state = state,
                    onRefresh = {},
                    formatter = TimeFormatter(is24Hour = true, Locale.US),
                )
            }
        }
        val refresh = composeRule.onNodeWithContentDescription(
            context.getString(R.string.action_refresh),
        )

        refresh.assertIsNotEnabled()
        state = PreviewData.content
        refresh.assertIsEnabled()
    }

    @Test
    fun `error shows message and retry calls back`() {
        var refreshes = 0
        setScreen(PreviewData.errorNoConnection, onRefresh = { refreshes++ })

        composeRule.onNodeWithText(
            context.getString(R.string.error_no_connection_title),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.error_no_connection_body),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.action_retry)).performClick()

        assertEquals(1, refreshes)
    }

    @Test
    fun `unexpected error shows its own message`() {
        setScreen(PreviewData.errorUnexpected)

        composeRule.onNodeWithText(
            context.getString(R.string.error_unexpected_title),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.error_unexpected_body),
        ).assertIsDisplayed()
    }

    @Test
    fun `refresh action calls back`() {
        var refreshes = 0
        setScreen(PreviewData.content, onRefresh = { refreshes++ })

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.action_refresh),
        ).performClick()

        assertEquals(1, refreshes)
    }

    @Test
    fun `empty shows message`() {
        setScreen(PreviewData.empty)

        composeRule.onNodeWithText(context.getString(R.string.empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.empty_body, 2, 2),
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Berlin central bus station").assertIsDisplayed()
    }

    @Test
    fun `failed refresh shows banner above the list`() {
        setScreen(PreviewData.refreshFailed)

        val banner = composeRule.onNodeWithText(
            context.getString(R.string.banner_no_connection, "23:05"),
        )
        banner.assertIsDisplayed()
        val firstRow = composeRule.onNodeWithText("060")
        firstRow.assertIsDisplayed()
        assertTrue(
            banner.getUnclippedBoundsInRoot().bottom <= firstRow.getUnclippedBoundsInRoot().top,
        )
        composeRule.onNodeWithText("Medyka, Border Crossing").assertIsDisplayed()
    }

    @Test
    fun `unexpected refresh error shows its own banner text`() {
        setScreen(PreviewData.content.copy(refreshError = LoadError.Unexpected))

        composeRule.onNodeWithText(
            context.getString(R.string.banner_unexpected, "23:05"),
        ).assertIsDisplayed()
    }

    @Test
    fun `delayed and cancelled rows show status and are described for TalkBack`() {
        setScreen(PreviewData.content)

        composeRule.onNodeWithText(
            context.getString(R.string.departure_delay, 13),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.departure_expected, "23:24"),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.departure_cancelled),
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.departure_a11y_delayed,
                "060",
                "Bremen Fernbusterminal",
                "22:10",
                context.resources.getQuantityString(R.plurals.delay_minutes_late, 74, 74),
                "23:24",
            ),
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.departure_a11y_delayed,
                "N1338",
                "Medyka, Border Crossing",
                "23:20",
                context.resources.getQuantityString(R.plurals.delay_minutes_late, 13, 13),
                "23:33",
            ),
        ).assertExists()
        composeRule.onNodeWithContentDescription(
            context.getString(
                R.string.departure_a11y_cancelled,
                "N3212",
                "Bremen Fernbusterminal",
                "23:40",
            ),
        ).assertExists()
    }

    @Test
    fun `shows date header before rides of the next day`() {
        setScreen(PreviewData.content)

        val header = composeRule.onNode(hasText("Saturday, November 14") and isHeading())
        header.assertIsDisplayed()
        val headerBounds = header.getUnclippedBoundsInRoot()
        assertTrue(
            composeRule.onNodeWithText("23:40").getUnclippedBoundsInRoot().bottom <=
                headerBounds.top,
        )
        assertTrue(
            headerBounds.bottom <=
                composeRule.onNodeWithText("Bialystok, Bus Station").getUnclippedBoundsInRoot().top,
        )
        composeRule.onAllNodesWithText("November", substring = true).assertCountEquals(1)
    }

    @Test
    fun `error content scrolls`() {
        setScreen(PreviewData.errorNoConnection)

        composeRule.onNode(
            hasScrollAction() and
                hasAnyDescendant(hasText(context.getString(R.string.action_retry))),
        ).assertExists()
    }

    @Test
    fun `missing api key shows a hint on the unexpected error`() {
        setScreen(PreviewData.errorUnexpected, apiKeyMissing = true)

        composeRule.onNodeWithText(context.getString(R.string.error_api_key_missing))
            .assertIsDisplayed()
    }

    @Test
    fun `no api key hint on the no connection error`() {
        setScreen(PreviewData.errorNoConnection, apiKeyMissing = true)

        composeRule.onNodeWithText(context.getString(R.string.error_api_key_missing))
            .assertDoesNotExist()
    }

    private fun setScreen(
        state: TimetableUiState,
        onRefresh: () -> Unit = {},
        apiKeyMissing: Boolean = false,
    ) {
        composeRule.setContent {
            TimetableTheme {
                TimetableScreen(
                    state,
                    onRefresh = onRefresh,
                    formatter = TimeFormatter(is24Hour = true, Locale.US),
                    apiKeyMissing = apiKeyMissing,
                )
            }
        }
    }
}
