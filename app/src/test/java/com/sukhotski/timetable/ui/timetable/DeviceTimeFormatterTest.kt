package com.sukhotski.timetable.ui.timetable

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** rememberTimeFormatter() is the production path: device locale and 12/24 hour setting. */
@RunWith(AndroidJUnit4::class)
class DeviceTimeFormatterTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val saturday = LocalDate(2026, 11, 14)
    private val departure = Instant.parse("2026-11-13T19:35:00Z")
    private val berlin = TimeZone.of("Europe/Berlin")

    @Test
    @Config(qualifiers = "en-rUS")
    fun `US locale puts the month first in the date header`() {
        assertEquals("Saturday, November 14", deviceFormatter().date(saturday))
    }

    @Test
    @Config(qualifiers = "de-rDE")
    fun `German locale puts the day first in the date header`() {
        assertEquals("Samstag, 14. November", deviceFormatter().date(saturday))
    }

    @Test
    fun `24 hour setting gives 24 hour times`() {
        setTime1224("24")

        assertEquals("20:35", deviceFormatter().time(departure, berlin))
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `12 hour setting gives 12 hour times`() {
        setTime1224("12")

        assertEquals("8:35 PM", deviceFormatter().time(departure, berlin))
    }

    private fun setTime1224(value: String) {
        Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, value)
    }

    private fun deviceFormatter(): TimeFormatter {
        lateinit var formatter: TimeFormatter
        composeRule.setContent { formatter = rememberTimeFormatter() }
        composeRule.waitForIdle()
        return formatter
    }
}
