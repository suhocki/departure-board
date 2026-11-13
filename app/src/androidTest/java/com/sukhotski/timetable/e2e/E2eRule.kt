package com.sukhotski.timetable.e2e

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import com.sukhotski.timetable.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import java.time.Instant
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** Starts the MockWebServer and the clock before Hilt builds the app, stops them after. */
private class EnvironmentRule(private val startTime: Instant) : ExternalResource() {
    override fun before() {
        TestEnvironment.server = MockWebServer().apply { start() }
        TestEnvironment.clock = TestClock(startTime)
    }

    override fun after() {
        TestEnvironment.server.close()
    }
}

/**
 * Environment, then Hilt, then Compose. Tests enqueue responses first and call [launch] after,
 * because the ViewModel loads as soon as the Activity exists.
 */
class E2eRule(testInstance: Any, startTime: Instant = Instant.parse("2026-11-14T12:00:30Z")) :
    TestRule {
    private val hilt = HiltAndroidRule(testInstance)
    val compose: ComposeTestRule = createEmptyComposeRule()
    val server: MockWebServer get() = TestEnvironment.server
    val clock: TestClock get() = TestEnvironment.clock

    private val chain = RuleChain
        .outerRule(EnvironmentRule(startTime))
        .around(hilt)
        .around(compose)

    override fun apply(base: Statement, description: Description): Statement =
        chain.apply(base, description)

    fun enqueueFixture(name: String) {
        server.enqueue(MockResponse.Builder().code(200).body(fixtureText(name)).build())
    }

    /** No answer at all: the server closes the connection, the app sees an IOException. */
    fun enqueueNoResponse() {
        server.enqueue(
            MockResponse.Builder().onRequestStart(SocketEffect.ShutdownConnection).build(),
        )
    }

    fun launch(): ActivityScenario<MainActivity> {
        hilt.inject()
        return ActivityScenario.launch(MainActivity::class.java)
    }

    private fun fixtureText(name: String): String {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "No fixture $name"
        }
        return stream.bufferedReader().use { it.readText() }
    }
}

/** A cold emulator can need a few seconds for the first request. Waiting ends as soon as it is true. */
const val TIMEOUT_MS = 10_000L
