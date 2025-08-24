package com.nononsenseapps.feeder.ui.compose.settings

/**
 * Data class to hold FreshRSS sync credentials.
 * Used for secure storage and retrieval of authentication details.
 *
 * @property serverUrl The FreshRSS server URL
 * @property username The FreshRSS username/email
 * @property password The FreshRSS password
 */
data class FreshRssSyncCredentials(
    val serverUrl: String,
    val username: String,
    val password: String
) {
    /**
     * Check if all required fields are present and valid.
     */
    fun isValid(): Boolean {
        return serverUrl.isNotBlank() &&
                username.isNotBlank() &&
                password.isNotBlank() &&
                (serverUrl.startsWith("http://") || serverUrl.startsWith("https://"))
    }

    companion object {
        /**
         * Create an empty credentials object.
         */
        fun empty() = FreshRssSyncCredentials(
            serverUrl = "",
            username = "",
            password = ""
        )
    }
}
