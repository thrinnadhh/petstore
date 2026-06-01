package com.example.data

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.BuildConfig

object PaymentManager {
    private const val TAG = "PaymentManager"

    // Starts payments session (Razorpay SDK overlay wrapper)
    fun startRazorpayCheckout(
        context: Context,
        amountInRupees: Double,
        orderId: String,
        email: String,
        phone: String,
        onSuccess: (paymentId: String) -> Unit,
        onFailure: (errorMessage: String) -> Unit
    ) {
        Toast.makeText(context, "Initiating Secure Razorpay Checkout of ₹$amountInRupees...", Toast.LENGTH_SHORT).show()

        // ⚠️ SECURITY NOTE: This is a mock implementation for demo purposes only.
        // In production with real Razorpay integration:
        // 1. Create an order on YOUR SERVER via Razorpay Orders API (never client-side)
        // 2. Pass the server-generated order ID to the Razorpay checkout
        // 3. After onSuccess, send razorpay_payment_id + razorpay_order_id + razorpay_signature
        //    to YOUR BACKEND for HMAC-SHA256 signature verification BEFORE marking order as paid
        // 4. Never trust client-side payment success callbacks alone — they can be faked

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                val mockPaymentId = "pay_razorpay_" + java.util.UUID.randomUUID().toString().take(12)
                // Security: only log payment IDs in debug builds
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Mock payment successful for Order: $orderId")
                }
                onSuccess(mockPaymentId)
            } catch (e: Exception) {
                onFailure("Payment processing error: ${e.message}")
            }
        }, 1500)
    }
}
