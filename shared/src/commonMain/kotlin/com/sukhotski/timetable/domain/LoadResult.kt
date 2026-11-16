package com.sukhotski.timetable.domain

sealed interface LoadResult<out T> {
    data class Success<T>(val value: T) : LoadResult<T>

    data class Failure(val error: LoadError) : LoadResult<Nothing>
}

enum class LoadError { NoConnection, Unexpected }
