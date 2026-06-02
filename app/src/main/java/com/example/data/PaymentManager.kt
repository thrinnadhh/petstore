package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.BuildConfig
import com.razorpay.Checkout
import org.json.JSONObject

object PaymentManager {
    private const val TAG = "PaymentManager"

    // Callback cache for production callbacks routed from the main Activity
    private var pendingSuccessCallback: ((String) -> Unit)? = null
    private var pendingFailureCallback: ((String) -> Unit)? = null

    fun onPaymentSuccess(paymentId: String) {
        pendingSuccessCallback?.invoke(paymentId)
        clearCallbacks()
    }

    fun onPaymentError(code: Int, response: String) {
        pendingFailureCallback?.invoke("Error $code: $response")
        clearCallbacks()
    }

    private fun clearCallbacks() {
        pendingSuccessCallback = null
        pendingFailureCallback = null
    }

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
        if (ProductionConfig.IS_DEMO_MODE) {
            Toast.makeText(context, "Initiating Secure Razorpay Checkout of ₹$amountInRupees...", Toast.LENGTH_SHORT).show()

            // ⚠️ SECURITY NOTE: This is a mock implementation for demo purposes only.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val mockPaymentId = "pay_razorpay_" + java.util.UUID.randomUUID().toString().take(12)
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Mock payment successful for Order: $orderId")
                    }
                    onSuccess(mockPaymentId)
                } catch (e: Exception) {
                    onFailure("Payment processing error: ${e.message}")
                }
            }, 1500)
        } else {
            val activity = context as? Activity
            if (activity == null) {
                onFailure("Payment context must be an Activity context.")
                return
            }

            Toast.makeText(context, "Launching Razorpay Production Gateway...", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Production Razorpay Request for ₹$amountInRupees - Order: $orderId")

            // Store callbacks to be triggered when the main activity routes Razorpay callbacks
            pendingSuccessCallback = onSuccess
            pendingFailureCallback = onFailure

            try {
                // Initialize Razorpay Checkout
                val checkout = Checkout()
                checkout.setKeyID(ProductionConfig.RAZORPAY_PROD_KEY)

                // Configure checkout options
                val options = JSONObject().apply {
                    put("name", "Swiggy Paws")
                    put("description", "Secure Order Payment for $orderId")
                    put("image", "https://irvskkigcxryxmdwylpt.supabase.co/storage/v1/object/public/photos/logo.png")
                    put("theme.color", "#FC8019") // Swiggy Orange
                    put("currency", "INR")
                    put("amount", (amountInRupees * 100).toInt()) // Amount in Paise (e.g. ₹10.00 = 1000 paise)
                    
                    val prefill = JSONObject().apply {
                        put("email", email)
                        put("contact", phone)
                    }
                    put("prefill", prefill)

                    val retry = JSONObject().apply {
                        put("enabled", true)
                        put("max_count", 3)
                    }
                    put("retry", retry)
                }

                // Launch checkout interface
                checkout.open(activity, options)
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating Razorpay checkout: ${e.message}", e)
                onFailure("Checkout initialization failed: ${e.localizedMessage}")
                clearCallbacks()
            }
        }
    }
}
