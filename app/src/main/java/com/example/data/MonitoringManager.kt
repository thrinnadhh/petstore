package com.example.data

import android.util.Log

/**
 * Performance monitoring and security-aware logging utilities.
 *
 * Security notes:
 * - Phone numbers are always masked (only last 4 digits shown)
 * - Log.d calls are stripped from release APKs via ProGuard -assumenosideeffects
 * - Log.e (errors) are preserved so Crashlytics can capture them
 *
 * Note: measureQuery is a crossinline suspend wrapper to allow calling suspend fns from coroutines.
 */
object MonitoringManager {
    private const val TAG = "PawsMonitor"

    // Measures query performance using a suspend-compatible wrapper.
    // suspend keyword allows callers in coroutine context to pass suspend lambdas.
    suspend fun <T> measureQuery(queryName: String, queryBlock: suspend () -> T): T {
        val start = System.currentTimeMillis()
        val result = queryBlock()
        val duration = System.currentTimeMillis() - start
        // Log.d stripped in release builds by ProGuard -assumenosideeffects in proguard-rules.pro
        Log.d(TAG, "Query '$queryName' completed in ${duration}ms")
        return result
    }

    fun logAuthEvent(phone: String, eventType: String) {
        // Security: NEVER log the full phone number — mask all but last 4 digits
        val maskedPhone = if (phone.length > 4) {
            phone.takeLast(4).padStart(phone.length, '*')
        } else {
            "****"
        }
        // Log.d is stripped in release builds by ProGuard -assumenosideeffects
        Log.d(TAG, "Auth Event - Type: '$eventType', User: $maskedPhone")
    }

    fun logSyncError(errorMsg: String) {
        Log.e(TAG, "Sync Error: $errorMsg")
    }
}
