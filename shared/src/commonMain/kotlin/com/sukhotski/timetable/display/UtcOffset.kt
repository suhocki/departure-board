package com.sukhotski.timetable.display

import kotlin.math.abs
import kotlinx.datetime.UtcOffset

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
