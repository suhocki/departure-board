package com.sukhotski.timetable.ui.timetable

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
    fun `content shows station name and station time caption`() {
        setScreen(PreviewData.content)

        composeRule.onNode(
            hasText("Berlin central bus station") and isHeading(),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.header_caption, 2, 2, "UTC+1"),
        ).assertIsDisplayed()
    }

    @Test
    fun `unknown station zone shows the UTC caption`() {
        setScreen(PreviewData.contentZoneUnknown)

        composeRule.onNodeWithText(
            context.resources.getQuantityString(R.plurals.header_caption_zone_unknown, 2, 2),
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

    private fun setScreen(state: TimetableUiState, onRefresh: () -> Unit = {}) {
        composeRule.setContent {
            TimetableTheme {
                TimetableScreen(
                    state,
                    onRefresh = onRefresh,
                    formatter = TimeFormatter(is24Hour = true, Locale.US),
                )
            }
        }
    }
}
