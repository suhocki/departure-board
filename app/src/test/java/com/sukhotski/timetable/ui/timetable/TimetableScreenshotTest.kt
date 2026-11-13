package com.sukhotski.timetable.ui.timetable

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
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

    @Test
    fun content_dark() = capture("content_dark", PreviewData.content, darkTheme = true)

    @Test
    fun content_font_2x() = capture("content_font_2x", PreviewData.content, fontScale = 2f)

    @Test
    fun error_no_connection() = capture("error_no_connection", PreviewData.errorNoConnection)

    private fun capture(
        name: String,
        state: TimetableUiState,
        darkTheme: Boolean = false,
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                TimetableTheme(darkTheme = darkTheme) {
                    TimetableScreen(
                        state = state,
                        onRefresh = {},
                        formatter = TimeFormatter(is24Hour = true, Locale.US),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}
