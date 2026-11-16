package com.sukhotski.timetable.display

import com.sukhotski.timetable.domain.delayMinutes
import kotlin.time.Instant

/** Up to this many minutes late the row shows "+N min". Later than that it shows the expected time. */
const val EXPECTED_TIME_THRESHOLD_MINUTES = 30

sealed interface DelayLabel {
    data class Minutes(val minutes: Long) : DelayLabel

    data class ExpectedAt(val expected: Instant) : DelayLabel
}

/** The text of the label is the job of each platform. The rule is here. */
fun delayLabel(scheduled: Instant, expected: Instant): DelayLabel {
    val minutes = delayMinutes(scheduled, expected)
    return if (minutes > EXPECTED_TIME_THRESHOLD_MINUTES) {
        DelayLabel.ExpectedAt(expected)
    } else {
        DelayLabel.Minutes(minutes)
    }
}
