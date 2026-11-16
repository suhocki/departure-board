package com.sukhotski.timetable.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val TIMETABLE_BASE_URL = "https://global.api.flixbus.com/"

/** The whole request is limited, not each phase. A slow body would hold the spinner. */
val TIMETABLE_REQUEST_TIMEOUT: Duration = 15.seconds

/**
 * The engine is a parameter: OkHttp on Android, Darwin on iOS, MockEngine in tests.
 * Error statuses are not thrown: [TimetableApi] checks the status itself.
 */
fun createTimetableHttpClient(
    engine: HttpClientEngine,
    baseUrl: String = TIMETABLE_BASE_URL,
    timeout: Duration = TIMETABLE_REQUEST_TIMEOUT,
): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(HttpTimeout) {
        requestTimeoutMillis = timeout.inWholeMilliseconds
    }
    defaultRequest {
        url(baseUrl)
    }
}
