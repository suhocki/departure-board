package com.sukhotski.timetable.ui.timetable

import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaZoneId

/**
 * Explicit time patterns, not localized styles: they follow the device 12/24 hour switch
 * and give the same text on every JDK and device. The date pattern comes from the device
 * locale (see rememberTimeFormatter). The default is the en-US result, for tests and previews.
 */
class TimeFormatter(is24Hour: Boolean, locale: Locale, datePattern: String = "EEEE, MMMM d") {
    private val timeFormat = DateTimeFormatter.ofPattern(
        if (is24Hour) "HH:mm" else "h:mm a",
        locale,
    )
    private val dateFormat = DateTimeFormatter.ofPattern(datePattern, locale)

    fun time(instant: Instant, zone: TimeZone): String =
        timeFormat.format(instant.toJavaInstant().atZone(zone.toJavaZoneId()))

    fun date(date: LocalDate): String = dateFormat.format(date.toJavaLocalDate())
}

/** "UTC+1", "UTC+5:30", "UTC-5" or "UTC". Zone names like CET differ between platforms and locales. */
fun formatUtcOffset(offset: UtcOffset): String {
    val totalMinutes = offset.totalSeconds / 60
    if (totalMinutes == 0) return "UTC"
    val sign = if (totalMinutes > 0) "+" else "-"
    val hours = abs(totalMinutes) / 60
    val minutes = abs(totalMinutes) % 60
    val text = "UTC$sign$hours"
    return if (minutes == 0) text else "$text:${minutes.toString().padStart(2, '0')}"
}
