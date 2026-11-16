package com.sukhotski.timetable.di

import com.sukhotski.timetable.BuildConfig
import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.data.remote.createTimetableHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun httpClient(): HttpClient = createTimetableHttpClient(OkHttp.create())

    @Provides
    @Singleton
    fun timetableApi(client: HttpClient): TimetableApi =
        TimetableApi(client, BuildConfig.TIMETABLE_API_KEY)
}
