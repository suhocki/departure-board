package com.sukhotski.timetable.e2e

import com.sukhotski.timetable.data.DefaultTimetableRepository
import com.sukhotski.timetable.data.ErrorReporter
import com.sukhotski.timetable.data.remote.ApiKeyInterceptor
import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.data.remote.createTimetableApi
import com.sukhotski.timetable.di.DataModule
import com.sukhotski.timetable.di.DefaultDispatcher
import com.sukhotski.timetable.di.NetworkModule
import com.sukhotski.timetable.domain.TimetableRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineDispatcher
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient

/** The clock the app reads in e2e tests. A test moves it to simulate time passing. */
class TestClock(@Volatile private var now: Instant) : Clock {
    fun advanceBy(duration: Duration) {
        now += duration
    }

    override fun now(): Instant = now
}

/**
 * Test doubles that outlive one Hilt component. The rule sets them up before the Activity starts,
 * the replaced modules below hand them to the real app. Tests run one at a time.
 */
object TestEnvironment {
    lateinit var server: MockWebServer
    lateinit var clock: TestClock
}

/**
 * Replaces all of NetworkModule and DataModule. The OkHttpClient is built again here with the
 * production key interceptor and the same 15 s call timeout, so an interceptor added to
 * NetworkModule is not seen here. Retrofit points at the mock server, the clock is the test clock,
 * and the repository binding is redone with the production class. The ViewModel, ErrorReporter
 * and dispatcher are the production ones.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class, DataModule::class],
)
object TestAppModule {

    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiKeyInterceptor("test-key"))
        .callTimeout(15.seconds.toJavaDuration())
        .build()

    @Provides
    @Singleton
    fun timetableApi(client: OkHttpClient): TimetableApi =
        createTimetableApi(TestEnvironment.server.url("/"), client)

    @Provides
    fun clock(): Clock = TestEnvironment.clock

    @Provides
    fun repository(
        api: TimetableApi,
        errorReporter: ErrorReporter,
        @DefaultDispatcher mappingDispatcher: CoroutineDispatcher,
    ): TimetableRepository = DefaultTimetableRepository(api, errorReporter, mappingDispatcher)
}
