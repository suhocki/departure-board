package com.sukhotski.timetable.di

import com.sukhotski.timetable.BuildConfig
import com.sukhotski.timetable.data.remote.ApiKeyInterceptor
import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.data.remote.createTimetableApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

private const val BASE_URL = "https://global.api.flixbus.com/"

/** OkHttp limits each phase to 10 s but not the whole call. A slow body would hold the spinner. */
internal val CALL_TIMEOUT: Duration = 15.seconds

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiKeyInterceptor(BuildConfig.TIMETABLE_API_KEY))
        .callTimeout(CALL_TIMEOUT.toJavaDuration())
        .build()

    @Provides
    @Singleton
    fun timetableApi(client: OkHttpClient): TimetableApi =
        createTimetableApi(BASE_URL.toHttpUrl(), client)
}
