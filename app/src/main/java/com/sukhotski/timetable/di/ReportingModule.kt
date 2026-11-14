package com.sukhotski.timetable.di

import com.sukhotski.timetable.data.ErrorReporter
import com.sukhotski.timetable.data.NoOpErrorReporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportingModule {

    /** Swap for a Crashlytics implementation in a real app. */
    @Binds
    abstract fun errorReporter(reporter: NoOpErrorReporter): ErrorReporter
}
