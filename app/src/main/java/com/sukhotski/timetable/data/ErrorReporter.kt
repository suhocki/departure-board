package com.sukhotski.timetable.data

import javax.inject.Inject

/**
 * Where non-fatal problems go. A crash reporter (Crashlytics) would implement it.
 * Never gets a URL or a request: the API key is in the query.
 */
interface ErrorReporter {
    /** A failure the user sees as "Something went wrong". */
    fun report(error: Throwable)

    /** Rides that came in the response but could not be shown. */
    fun droppedRides(count: Int)
}

class NoOpErrorReporter @Inject constructor() : ErrorReporter {
    override fun report(error: Throwable) = Unit

    override fun droppedRides(count: Int) = Unit
}
