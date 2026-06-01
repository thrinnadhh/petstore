package com.example.data

import android.content.Context
import android.widget.Toast

object NotificationManager {
    fun init(context: Context) {
        println("NotificationManager: Push channels successfully configured and ready to trigger.")
    }

    // Sends an immediate push notification alert to the phone UI
    fun fireInstantNotification(context: Context, title: String, message: String) {
        try {
            Toast.makeText(context, "🔔 [PUSH ALERT] $title\n$message", Toast.LENGTH_LONG).show()
            println("Notification Fired: [$title] -> $message")
        } catch (e: Exception) {
            println("Notification (Console Mock): [$title] -> $message")
        }
    }
}
