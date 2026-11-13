package com.sukhotski.timetable.ui.timetable

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.sukhotski.timetable.ui.theme.TimetableTheme
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Goldens live in app/src/test/screenshots. Record them with ./gradlew recordRoborazziDebug. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
class TimetableScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun content() = capture("content", PreviewData.content)

    @Test
    fun empty() = capture("empty", PreviewData.empty)

    private fun capture(name: String, state: TimetableUiState) {
        composeRule.setContent {
            TimetableTheme {
                TimetableScreen(
                    state = state,
                    onRefresh = {},
                    formatter = TimeFormatter(is24Hour = true, Locale.US),
                )
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
