package com.sukhotski.timetable.di

import com.sukhotski.timetable.data.ErrorReporter
import com.sukhotski.timetable.data.NoOpErrorReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ReportingModule {

    /** Swap for a Crashlytics implementation in a real app. */
    @Provides
    fun errorReporter(): ErrorReporter = NoOpErrorReporter()
}
