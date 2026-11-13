package com.sukhotski.timetable.ui.timetable

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sukhotski.timetable.R
import com.sukhotski.timetable.ui.theme.TimetableTheme
import java.util.Locale
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

    private fun setScreen(state: TimetableUiState) {
        composeRule.setContent {
            TimetableTheme {
                TimetableScreen(
                    state,
                    formatter = TimeFormatter(is24Hour = true, Locale.US),
                )
            }
        }
    }
}
