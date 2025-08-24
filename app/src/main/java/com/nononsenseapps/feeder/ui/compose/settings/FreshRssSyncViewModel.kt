package com.nononsenseapps.feeder.ui.compose.settings

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.base.DIAwareViewModel
import com.nononsenseapps.feeder.db.room.Feed
import com.nononsenseapps.feeder.db.room.FeedItem
import com.nononsenseapps.feeder.freshrss.FreshRssApiClient
import com.nononsenseapps.feeder.util.FreshRssPreferences
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance
import retrofit2.HttpException
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * ViewModel for FreshRSS Sync screen.
 * Handles authentication, sync operations and state management.
 */
class FreshRssSyncViewModel(
    override val di: DI
) : DIAwareViewModel(di) {

    private val repository: Repository by instance()
    private val appContext: Application by instance()
    private val prefs = FreshRssPreferences(appContext)

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    init {
        viewModelScope.launch {
            // Load saved credentials if they exist
            prefs.getCredentials()?.let { credentials ->
                _serverUrl.value = credentials.serverUrl
                _username.value = credentials.username
                _password.value = credentials.password
            }
        }
    }

    private val _syncStatus = MutableStateFlow<FreshRssSyncStatus>(FreshRssSyncStatus.None)
    val syncStatus: StateFlow<FreshRssSyncStatus> = _syncStatus.asStateFlow()

    fun setServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun setUsername(username: String) {
        _username.value = username
    }

    fun setPassword(password: String) {
        _password.value = password
    }

    fun saveCredentials(url: String, username: String, password: String) {
        viewModelScope.launch {
            val credentials = FreshRssSyncCredentials(
                serverUrl = url.trim(),
                username = username.trim(),
                password = password.trim()
            )

            if (!credentials.isValid()) {
                _syncStatus.value = FreshRssSyncStatus.Error("Please fill all fields correctly")
            } else {
                // Save to secure storage
                prefs.saveCredentials(credentials)

                // Update UI state
                _serverUrl.value = credentials.serverUrl
                _username.value = credentials.username
                _password.value = credentials.password

                _syncStatus.value = FreshRssSyncStatus.Success
            }
        }
    }

    fun syncWithFreshRss() {
        val url = _serverUrl.value.trim()
        val user = _username.value.trim()
        val pass = _password.value

        if (url.isBlank() || user.isBlank() || pass.isBlank()) {
            _syncStatus.value = FreshRssSyncStatus.Error("Please fill all fields")
            return
        }

        // Ensure URL is properly formatted
        var serverUrl = url.trimEnd('/')
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            _syncStatus.value = FreshRssSyncStatus.Error("URL must start with http:// or https://")
            return
        }

        // Remove /api/greader.php if it's already in the URL to avoid duplication
        if (serverUrl.endsWith("/api/greader.php")) {
            serverUrl = serverUrl.removeSuffix("/api/greader.php")
        }

        _syncStatus.value = FreshRssSyncStatus.Loading

        viewModelScope.launch {
            try {
                println("Starting FreshRSS sync with URL: $url")
                // 1. Create initial API client and login
                var api = FreshRssApiClient.create(serverUrl)
                println("Attempting login for user: $user")
                // Remove any leading/trailing whitespace from credentials
                val cleanUser = user.trim()
                val cleanPass = pass.trim()
                val loginResp = api.login(
                    email = cleanUser,
                    password = cleanPass,
                    service = "reader",
                    accountType = "HOSTED",
                    output = "text"
                )

                if (!loginResp.isSuccessful || loginResp.body().isNullOrBlank()) {
                    val errorBody = loginResp.errorBody()?.string() ?: "No error body"
                    val errorMsg = when (loginResp.code()) {
                        404 -> "Login failed: API endpoint not found (404). Please check the server URL."
                        401 -> "Login failed: Invalid credentials. Please verify your username and password."
                        403 -> "Login failed: Access forbidden. Please verify that:\n1. Google Reader API is enabled\n2. Your credentials are correct"
                        else -> "Login failed with code ${loginResp.code()}\nServer response: $errorBody"
                    }
                    println("Login failed: $errorMsg")
                    _syncStatus.value = FreshRssSyncStatus.Error(errorMsg)
                    return@launch
                }

                // Parse Auth token from response
                val responseBody = loginResp.body()!!
                println("Login response:\n$responseBody")
                val auth = parseAuthToken(responseBody)
                if (auth == null) {
                    println("Failed to parse auth token from response")
                    _syncStatus.value = FreshRssSyncStatus.Error("Invalid auth response: Unable to extract auth token. Response format may be incorrect.")
                    return@launch
                }
                println("Successfully obtained auth token")

                // Create new client with auth token
                api = FreshRssApiClient.create(serverUrl, auth)

                // 2. Get action token for write operations
                val tokenResp = api.getToken()
                if (!tokenResp.isSuccessful || tokenResp.body().isNullOrBlank()) {
                    val errorBody = tokenResp.errorBody()?.string() ?: "No error body"
                    println("Failed to get token: ${tokenResp.code()} - $errorBody")
                    _syncStatus.value = FreshRssSyncStatus.Error("Failed to get token: ${tokenResp.code()} - $errorBody")
                    return@launch
                }
                println("Successfully obtained action token")

                // 3. Get subscriptions using authenticated client
                val subsResp = api.getSubscriptions()
                if (!subsResp.isSuccessful || subsResp.body() == null) {
                    val errorBody = subsResp.errorBody()?.string() ?: "No error body"
                    println("Failed to fetch subscriptions: ${subsResp.code()} - $errorBody")
                    _syncStatus.value = FreshRssSyncStatus.Error(
                        "Failed to fetch subscriptions: ${subsResp.code()} - $errorBody"
                    )
                    return@launch
                }
                println("Successfully fetched subscriptions")

                val subscriptions = subsResp.body()!!.subscriptions
                val feedUrlToId = mutableMapOf<String, Long>()

                // Store feeds in local database
                for (sub in subscriptions) {
                    try {
                        val feedUrl = sub.url ?: continue
                        val existingFeed = repository.getFeed(URL(feedUrl))
                        val feed = Feed(
                            id = existingFeed?.id ?: 0L,
                            title = sub.title,
                            url = URL(feedUrl),
                            tag = existingFeed?.tag ?: "",
                            customTitle = existingFeed?.customTitle ?: "",
                            notify = existingFeed?.notify ?: false,
                            lastSync = Instant.now()
                        )
                        val feedId = repository.upsertFeed(feed)
                        feedUrlToId[feedUrl] = feedId
                    } catch (e: Exception) {
                        println("Error processing feed ${sub.title}: ${e.message}")
                    }
                }

                println("Stored ${feedUrlToId.size} feeds")

                // 4. Get unread items
                val unreadResp = api.getUnreadItems(50)
                if (!unreadResp.isSuccessful || unreadResp.body() == null) {
                    _syncStatus.value = FreshRssSyncStatus.Error(
                        "Failed to fetch unread items: ${unreadResp.code()} - ${unreadResp.errorBody()?.string()}"
                    )
                    return@launch
                }

                val unreadItems = unreadResp.body()!!.items

                // Process items
                val itemsWithText = unreadItems.mapNotNull { item ->
                    val feedUrl = item.alternate?.firstOrNull()?.href ?: return@mapNotNull null
                    val feedId = feedUrlToId[feedUrl] ?: return@mapNotNull null

                    val pubDate = item.published?.let {
                        Instant.ofEpochSecond(it)
                    } ?: Instant.now()
                    val pubZoned = pubDate.atZone(ZoneId.systemDefault())

                    val feedItem = FeedItem(
                        guid = item.id,
                        plainTitle = item.title ?: "",
                        title = item.title ?: "",
                        plainSnippet = item.summary?.content ?: "",
                        pubDate = pubZoned,
                        link = item.alternate?.firstOrNull()?.href,
                        feedId = feedId,
                        primarySortTime = pubDate,
                        firstSyncedTime = Instant.now()
                    )
                    feedItem to (item.summary?.content ?: "")
                }

                try {
                    // Insert items in batches to avoid transaction size limits
                    var totalInserted = 0
                    itemsWithText.chunked(50).forEach { batch ->
                        repository.upsertFeedItems(batch) { _, _ -> }
                        totalInserted += batch.size
                    }
                    println("Successfully stored $totalInserted items")
                    _syncStatus.value = FreshRssSyncStatus.Success
                } catch (e: Exception) {
                    val error = "Failed to store items: ${e.message}"
                    println(error)
                    e.printStackTrace()
                    _syncStatus.value = FreshRssSyncStatus.Error(error)
                }
            } catch (e: HttpException) {
                val errorMsg = when (e.code()) {
                    404 -> "API endpoint not found (404). Please check the server URL"
                    401 -> "Authentication failed (401). Please check your credentials"
                    403 -> "Access forbidden (403). Please check if Google Reader API is enabled"
                    else -> "Network error ${e.code()}: ${e.message()}"
                }
                _syncStatus.value = FreshRssSyncStatus.Error(errorMsg)
            } catch (e: Exception) {
                _syncStatus.value = FreshRssSyncStatus.Error("Sync failed: ${e.message}")
            }
        }
    }

    private fun parseAuthToken(response: String): String? {
        println("Parsing auth token from response")
        val authLine = response.lineSequence()
            .firstOrNull { it.startsWith("Auth=") }
        println("Found auth line: $authLine")
        return authLine?.substringAfter("Auth=")?.trim()?.also {
            println("Extracted auth token length: ${it.length}")
        }
    }

    fun clearSyncStatus() {
        _syncStatus.value = FreshRssSyncStatus.None
    }
}
