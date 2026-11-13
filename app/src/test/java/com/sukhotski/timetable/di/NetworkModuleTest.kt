package com.sukhotski.timetable.di

import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModuleTest {

    @Test
    fun `real client and api send the request to the timetable host with the api key`() = runTest {
        var sentUrl: HttpUrl? = null
        // Answers instead of the network, after the module's own interceptors.
        val client = NetworkModule.okHttpClient().newBuilder()
            .addInterceptor { chain ->
                sentUrl = chain.request().url
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"rides":[]}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        NetworkModule.timetableApi(client)
            .departures("station-1", from = "2026-11-13T16:05:00Z", to = "2026-11-13T18:05:00Z")

        val url = checkNotNull(sentUrl)
        assertEquals("https", url.scheme)
        assertEquals("global.api.flixbus.com", url.host)
        assertEquals("/gis/v2/timetable/station-1/departures", url.encodedPath)
        assertTrue("apiKey" in url.queryParameterNames)
    }

    @Test
    fun `real client limits the whole call`() {
        assertEquals(
            CALL_TIMEOUT.toMillis().toInt(),
            NetworkModule.okHttpClient().callTimeoutMillis,
        )
    }
}
