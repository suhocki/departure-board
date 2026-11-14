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
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient

/** The clock the app reads in e2e tests. A test moves it to simulate time passing. */
class TestClock(@Volatile private var now: Instant) : Clock() {
    fun advanceBy(duration: Duration) {
        now += duration
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
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
 * Replaces only the base URL and the clock. OkHttp, the key interceptor, Retrofit, the repository
 * and the ViewModel are the production ones.
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
        .callTimeout(Duration.ofSeconds(15))
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
