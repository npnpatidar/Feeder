package com.nononsenseapps.feeder.ui.compose.settings

/**
 * Status of the FreshRSS sync operation.
 */
sealed class FreshRssSyncStatus {
    /**
     * Initial state, no sync has been attempted.
     */
    object None : FreshRssSyncStatus()

    /**
     * Sync is currently in progress.
     */
    object Loading : FreshRssSyncStatus()

    /**
     * Sync completed successfully.
     */
    object Success : FreshRssSyncStatus()

    /**
     * Sync failed with an error.
     * @param message Error message describing what went wrong.
     */
    data class Error(val message: String?) : FreshRssSyncStatus()
}
