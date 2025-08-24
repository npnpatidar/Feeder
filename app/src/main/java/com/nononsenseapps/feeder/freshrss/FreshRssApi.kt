package com.nononsenseapps.feeder.freshrss

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Minimal FreshRSS Google Reader API for authentication and fetching subscriptions/feeds.
 * See: https://freshrss.github.io/FreshRSS/en/users/12_GoogleReaderAPI.html
 */
interface FreshRssApi {

    /**
     * Authenticate and get a session token.
     * Returns a response body with SID, LSID, Auth, etc.
     */
    @FormUrlEncoded
    @POST("/api/greader.php/accounts/ClientLogin")
    suspend fun login(
        @Field("Email") email: String,
        @Field("Passwd") password: String,
        @Field("service") service: String = "reader",
        @Field("accountType") accountType: String = "HOSTED",
        @Field("output") output: String = "text"
    ): Response<String>

    /**
     * Get a token for write operations (not always needed for read-only).
     */
    @GET("/api/greader.php/reader/api/0/token?output=text")
    suspend fun getToken(): Response<String>

    /**
     * Get list of subscriptions (feeds).
     * Requires "Authorization: GoogleLogin auth=<Auth token>" header.
     */
    @GET("/api/greader.php/reader/api/0/subscription/list?output=json")
    suspend fun getSubscriptions(): Response<SubscriptionListResponse>

    /**
     * Get unread items.
     * Requires "Authorization: GoogleLogin auth=<Auth token>" header.
     */
    @GET("/api/greader.php/reader/api/0/stream/contents/reading-list?output=json")
    suspend fun getUnreadItems(
        @Query("n") count: Int = 20 // Number of items to fetch
    ): Response<StreamContentsResponse>
}

/**
 * Data classes for FreshRSS API responses.
 * Only minimal fields for demonstration.
 */
data class SubscriptionListResponse(
    val subscriptions: List<Subscription>
)

data class Subscription(
    val id: String,
    val title: String,
    val url: String?
)

data class StreamContentsResponse(
    val items: List<FeedItem>
)

data class FeedItem(
    val id: String,
    val title: String?,
    val published: Long?,
    val updated: Long?,
    val summary: FeedItemSummary?,
    val canonical: List<FeedItemCanonical>?,
    val alternate: List<FeedItemAlternate>?
)

data class FeedItemSummary(
    val content: String?
)

data class FeedItemCanonical(
    val href: String?
)

data class FeedItemAlternate(
    val href: String?
)
