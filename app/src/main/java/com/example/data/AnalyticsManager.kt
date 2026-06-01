package com.example.data

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.BuildConfig
import com.posthog.PostHog
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

object AnalyticsManager {
    private const val TAG = "PawsAnalytics"
    private var isInitialized = false
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun init(context: Context) {
        if (isInitialized) return
        try {
            // PostHog setup
            val config = PostHogAndroidConfig(
                apiKey = "phc_petstore_posthog_sandbox_token_17798747",
                host = "https://us.i.posthog.com"
            ).apply {
                captureApplicationLifecycleEvents = true
                // Security: only enable verbose debug logging in debug builds
                debug = BuildConfig.DEBUG
            }
            PostHogAndroid.setup(context, config)
            if (BuildConfig.DEBUG) Log.d(TAG, "PostHog setup successfully initialized.")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "PostHog setup bypassed.")
        }

        try {
            // Firebase Analytics setup
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            if (BuildConfig.DEBUG) Log.d(TAG, "Firebase Analytics successfully initialized.")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Firebase Analytics setup bypassed.")
        }

        isInitialized = true
    }

    fun identifyUser(userId: String, properties: Map<String, Any> = emptyMap()) {
        // PostHog Identify
        try {
            PostHog.identify(userId, properties)
            if (BuildConfig.DEBUG) Log.d(TAG, "Identified user (PostHog): $userId")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "PostHog identify bypassed")
        }

        // Firebase Analytics / Crashlytics Identify
        try {
            firebaseAnalytics?.setUserId(userId)
            properties.forEach { (key, value) ->
                firebaseAnalytics?.setUserProperty(key, value.toString())
            }
            FirebaseCrashlytics.getInstance().setUserId(userId)
            if (BuildConfig.DEBUG) Log.d(TAG, "Identified user (Firebase): $userId")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Firebase identify bypassed")
        }
    }

    fun trackEvent(eventName: String, properties: Map<String, Any> = emptyMap()) {
        // PostHog Capture
        try {
            PostHog.capture(eventName, properties = properties)
            if (BuildConfig.DEBUG) Log.d(TAG, "Track (PostHog): $eventName")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "PostHog track bypassed: $eventName")
        }

        // Firebase Analytics Log Event
        try {
            val bundle = Bundle().apply {
                properties.forEach { (key, value) ->
                    putString(key, value.toString())
                }
            }
            firebaseAnalytics?.logEvent(eventName, bundle)
            if (BuildConfig.DEBUG) Log.d(TAG, "Track (Firebase): $eventName")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Firebase track bypassed: $eventName")
        }
    }

    // Capture exception metrics (e.g. from CircuitBreaker or remote network tripwires) directly in Crashlytics
    fun logException(throwable: Throwable, message: String? = null) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            message?.let { crashlytics.log(it) }
            crashlytics.recordException(throwable)
            if (BuildConfig.DEBUG) Log.e(TAG, "Exception logged to Crashlytics: ${throwable.message}")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Crashlytics log bypassed")
        }
    }
}
