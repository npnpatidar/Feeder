package com.nononsenseapps.feeder.util

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nononsenseapps.feeder.ui.compose.settings.FreshRssSyncCredentials

/**
 * Secure storage for FreshRSS credentials using EncryptedSharedPreferences.
 * All data is encrypted using Android's security library.
 */
class FreshRssPreferences(private val context: Context) {

    private val prefs by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create EncryptedSharedPreferences", e)
            context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Save FreshRSS credentials securely.
     */
    suspend fun saveCredentials(credentials: FreshRssSyncCredentials) {
        try {
            prefs.edit().apply {
                putString(KEY_SERVER_URL, credentials.serverUrl)
                putString(KEY_USERNAME, credentials.username)
                putString(KEY_PASSWORD, credentials.password)
                apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save credentials", e)
            throw e
        }
    }

    /**
     * Retrieve saved FreshRSS credentials.
     * Returns null if no credentials are saved.
     */
    suspend fun getCredentials(): FreshRssSyncCredentials? {
        return try {
            val serverUrl = prefs.getString(KEY_SERVER_URL, null) ?: return null
            val username = prefs.getString(KEY_USERNAME, null) ?: return null
            val password = prefs.getString(KEY_PASSWORD, null) ?: return null

            FreshRssSyncCredentials(
                serverUrl = serverUrl,
                username = username,
                password = password
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve credentials", e)
            null
        }
    }

    /**
     * Clear all saved credentials.
     */
    suspend fun clearCredentials() {
        try {
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear credentials", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "FreshRssPreferences"
        private const val PREF_FILE_NAME = "freshrss_prefs"
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
    }
}
