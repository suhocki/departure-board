package com.sukhotski.timetable.display

import kotlin.math.abs
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt

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

/** The offset of [zone] at [at], as text. Swift cannot call the extension [offsetAt]. */
fun formatUtcOffset(zone: TimeZone, at: Instant): String = formatUtcOffset(zone.offsetAt(at))
