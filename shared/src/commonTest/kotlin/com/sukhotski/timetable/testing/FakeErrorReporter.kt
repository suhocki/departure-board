package com.sukhotski.timetable.testing

import com.sukhotski.timetable.data.ErrorReporter

class FakeErrorReporter : ErrorReporter {
    val errors = mutableListOf<Throwable>()
    val dropped = mutableListOf<Int>()

    override fun report(error: Throwable) {
        errors += error
    }

    override fun droppedRides(count: Int) {
        dropped += count
    }
}
