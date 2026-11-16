package com.sukhotski.timetable.testing

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class MutableClock(private var now: Instant) : Clock {
    fun advanceBy(duration: Duration) {
        now += duration
    }

    override fun now(): Instant = now
}
