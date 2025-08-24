package com.nononsenseapps.feeder.freshrss

import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * Utility to build a FreshRssApi Retrofit client with Google Reader API authentication.
 */
object FreshRssApiClient {

    /**
     * Build a FreshRssApi client.
     *
     * @param baseUrl The FreshRSS server base URL, e.g. "https://example.com/"
     * @param authToken The "Auth" token from ClientLogin (for Google Reader API).
     * @return FreshRssApi instance.
     */
    fun create(
        baseUrl: String,
        authToken: String? = null
    ): FreshRssApi {
        val okHttpClient = OkHttpClient.Builder().apply {
            // Add auth header to all requests if token is provided
            if (!authToken.isNullOrBlank()) {
                addInterceptor { chain ->
                    val request = chain.request()
                    val newRequest = request.newBuilder()
                        .header("Authorization", "GoogleLogin auth=$authToken")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .build()
                    chain.proceed(newRequest)
                }
            }
            // Add logging interceptor for debugging
            addInterceptor { chain ->
                val request = chain.request()
                println("FreshRSS Request: ${request.method} ${request.url}")
                println("FreshRSS Headers: ${request.headers}")
                if (request.body != null) {
                    // Buffer the request body so we can read it multiple times
                    val buffer = okio.Buffer()
                    request.body!!.writeTo(buffer)
                    println("FreshRSS Request Body: ${buffer.readUtf8()}")
                }

                val response = chain.proceed(request)
                println("FreshRSS Response Code: ${response.code}")
                println("FreshRSS Response Headers: ${response.headers}")

                response.body?.let { responseBody ->
                    val contentType = responseBody.contentType()
                    val bodyString = responseBody.string()
                    println("FreshRSS Response Body: $bodyString")
                    println("FreshRSS Response Content-Type: $contentType")
                    // We need to recreate the response since we consumed the body
                    return@addInterceptor response.newBuilder()
                        .body(bodyString.toResponseBody(contentType))
                        .build()
                }
                response
            }
        }.build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(ensureApiBaseUrl(baseUrl))
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttpClient)
            .build()

        return retrofit.create(FreshRssApi::class.java)
    }

    /**
     * Ensures the base URL ends with a slash and points to the API root.
     */
    private fun ensureApiBaseUrl(baseUrl: String): String {
        println("Original base URL: $baseUrl")
        // Remove any trailing slashes from the base URL
        var cleanUrl = baseUrl.trimEnd('/')

        // If URL already contains /api/greader.php, use it as is
        if (!cleanUrl.contains("/api/greader.php")) {
            // Otherwise construct the proper URL
            cleanUrl = if (cleanUrl.endsWith("/api")) {
                "$cleanUrl/greader.php"
            } else {
                "$cleanUrl/api/greader.php"
            }
        }

        // Ensure URL ends with /
        val finalUrl = if (cleanUrl.endsWith("/")) cleanUrl else "$cleanUrl/"
        println("Final base URL: $finalUrl")
        return finalUrl
    }

    /**
     * OkHttp Interceptor to add Google Reader API auth header.
     */
    // AuthInterceptor removed in favor of inline interceptor
}
