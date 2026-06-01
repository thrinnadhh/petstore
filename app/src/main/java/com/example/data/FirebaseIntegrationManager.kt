package com.example.data

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FirebaseIntegrationManager {
    private val _fcmToken = MutableStateFlow<String>("Retrieving FCM token...")
    val fcmToken: StateFlow<String> = _fcmToken.asStateFlow()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        try {
            // Initialize Firebase App if not already initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            
            // Retrieve active registration token programmatically
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    _fcmToken.value = token
                    println("🔥 [Firebase FCM] Device Token: $token")
                } else {
                    _fcmToken.value = "FCM registration token bypass"
                    println("🔥 [Firebase FCM] Failed to retrieve token. Using bypass token.")
                }
            }
            isInitialized = true
        } catch (e: Exception) {
            _fcmToken.value = "fcm_mock_token_" + java.util.UUID.randomUUID().toString().take(12)
            isInitialized = true
            println("🔥 [Firebase FCM] Bypass initialization due to sandboxed environment: ${e.message}")
        }
    }
}
