package com.example.data

/**
 * Global configurations for Swiggy Paws Google Play Store release.
 * Easily toggle between Demo Mode (essential for Google Play Reviewers to test without real transactions)
 * and Production Mode (integrated with live servers & real payment gateways).
 */
object ProductionConfig {
    
    /**
     * Set to true to run in self-contained Demo/Mock mode.
     * Google Play Reviewers require all flows to function perfectly without charging real money.
     * Set to false for the final public release using real network backends.
     */
    const val IS_DEMO_MODE: Boolean = true

    /**
     * Production credentials placeholder for Razorpay.
     * Managed securely and separately from development sandbox tokens.
     */
    const val RAZORPAY_PROD_KEY: String = "rzp_live_production_token_placeholder"

    /**
     * Minimum API Level metadata verified for Google Play SDK compliance.
     */
    const val PLAY_STORE_MIN_SDK: Int = 24
    const val PLAY_STORE_TARGET_SDK: Int = 36
}
