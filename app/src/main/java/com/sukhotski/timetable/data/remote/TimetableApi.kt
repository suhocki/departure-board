package com.sukhotski.timetable.data.remote

import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TimetableApi {
    /** [from] and [to] are ISO 8601 instants in UTC with Z. The API ignores other offsets. */
    @GET("gis/v2/timetable/{stationId}/departures")
    suspend fun departures(
        @Path("stationId") stationId: String,
        @Query("from") from: String,
        @Query("to") to: String,
    ): TimetableResponse
}

fun createTimetableApi(baseUrl: HttpUrl, client: OkHttpClient): TimetableApi = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(client)
    .addConverterFactory(TimetableJson.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(TimetableApi::class.java)
