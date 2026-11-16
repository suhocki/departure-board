package com.sukhotski.timetable.di

import com.sukhotski.timetable.data.DefaultTimetableRepository
import com.sukhotski.timetable.data.ErrorReporter
import com.sukhotski.timetable.data.remote.TimetableApi
import com.sukhotski.timetable.domain.TimetableRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun timetableRepository(
        api: TimetableApi,
        errorReporter: ErrorReporter,
        @DefaultDispatcher mappingDispatcher: CoroutineDispatcher,
    ): TimetableRepository = DefaultTimetableRepository(api, errorReporter, mappingDispatcher)

    /** The only source of the current time in the app. */
    @Provides
    fun clock(): Clock = Clock.System
}
