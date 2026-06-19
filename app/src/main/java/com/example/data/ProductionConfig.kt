package com.example.data

import com.example.BuildConfig

/**
 * Global configurations for Swiggy Paws Google Play Store release.
 * Build types control demo vs production behavior. Debug builds stay self-contained for
 * development and review; release builds fail closed when required backend config is missing.
 */
object ProductionConfig {
    val IS_DEMO_MODE: Boolean = BuildConfig.DEMO_MODE

    val RAZORPAY_KEY_ID: String = BuildConfig.RAZORPAY_KEY_ID

    /**
     * Minimum API Level metadata verified for Google Play SDK compliance.
     */
    const val PLAY_STORE_MIN_SDK: Int = 24
    const val PLAY_STORE_TARGET_SDK: Int = 36

    fun requireProductionBackendConfig() {
        if (IS_DEMO_MODE) return
        require(BuildConfig.SUPABASE_URL.isNotBlank()) { "SUPABASE_URL is required for production builds." }
        require(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) { "SUPABASE_ANON_KEY is required for production builds." }
        require(RAZORPAY_KEY_ID.isNotBlank()) { "RAZORPAY_KEY_ID is required for production payment builds." }
    }
}
