package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.BuildConfig
import com.razorpay.Checkout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

object PaymentManager {
    private const val TAG = "PaymentManager"
    private val paymentScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Callback cache for production callbacks routed from the main Activity
    private var pendingSuccessCallback: ((PaymentResult) -> Unit)? = null
    private var pendingFailureCallback: ((String) -> Unit)? = null

    data class PaymentResult(
        val paymentId: String,
        val razorpayOrderId: String? = null,
        val signature: String? = null,
        val verifiedByServer: Boolean
    )

    fun onPaymentSuccess(paymentId: String, razorpayOrderId: String?, signature: String?) {
        val successCallback = pendingSuccessCallback
        val failureCallback = pendingFailureCallback
        if (successCallback == null || failureCallback == null) {
            clearCallbacks()
            return
        }

        if (ProductionConfig.IS_DEMO_MODE) {
            successCallback(
                PaymentResult(
                    paymentId = paymentId,
                    razorpayOrderId = razorpayOrderId,
                    signature = signature,
                    verifiedByServer = true
                )
            )
            clearCallbacks()
            return
        }

        if (paymentId.isBlank() || razorpayOrderId.isNullOrBlank() || signature.isNullOrBlank()) {
            failureCallback("Payment verification data was missing.")
            clearCallbacks()
            return
        }

        paymentScope.launch {
            val verification = SupabaseManager.verifyRazorpayPayment(
                razorpayOrderId = razorpayOrderId,
                paymentId = paymentId,
                signature = signature
            )
            verification.fold(
                onSuccess = {
                    successCallback(
                        PaymentResult(
                            paymentId = paymentId,
                            razorpayOrderId = razorpayOrderId,
                            signature = signature,
                            verifiedByServer = true
                        )
                    )
                },
                onFailure = {
                    failureCallback("Payment could not be verified. Please contact support if money was debited.")
                }
            )
            clearCallbacks()
        }
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
        onSuccess: (payment: PaymentResult) -> Unit,
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
                    onSuccess(
                        PaymentResult(
                            paymentId = mockPaymentId,
                            razorpayOrderId = orderId,
                            signature = "demo_signature",
                            verifiedByServer = true
                        )
                    )
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

            try {
                ProductionConfig.requireProductionBackendConfig()
            } catch (e: IllegalArgumentException) {
                onFailure(e.message ?: "Production payment configuration is incomplete.")
                return
            }

            Toast.makeText(context, "Launching Razorpay Production Gateway...", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Production Razorpay Request for ₹$amountInRupees - Order: $orderId")

            // Store callbacks to be triggered when the main activity routes Razorpay callbacks
            pendingSuccessCallback = onSuccess
            pendingFailureCallback = onFailure

            paymentScope.launch {
                val razorpayOrderResult = SupabaseManager.createRazorpayOrder(
                    amountInRupees = amountInRupees,
                    receipt = orderId
                )
                razorpayOrderResult.fold(
                    onSuccess = { razorpayOrderId ->
                        openProductionCheckout(
                            activity = activity,
                            amountInRupees = amountInRupees,
                            razorpayOrderId = razorpayOrderId,
                            email = email,
                            phone = phone,
                            onFailure = onFailure
                        )
                    },
                    onFailure = {
                        onFailure("Could not create a secure payment order. Please try again.")
                        clearCallbacks()
                    }
                )
            }
        }
    }

    private fun openProductionCheckout(
        activity: Activity,
        amountInRupees: Double,
        razorpayOrderId: String,
        email: String,
        phone: String,
        onFailure: (errorMessage: String) -> Unit
    ) {
            try {
                // Initialize Razorpay Checkout
                val checkout = Checkout()
                checkout.setKeyID(ProductionConfig.RAZORPAY_KEY_ID)

                // Configure checkout options
                val options = JSONObject().apply {
                    put("name", "Swiggy Paws")
                    put("description", "Secure Order Payment")
                    put("image", "https://irvskkigcxryxmdwylpt.supabase.co/storage/v1/object/public/photos/logo.png")
                    put("theme.color", "#FC8019") // Swiggy Orange
                    put("currency", "INR")
                    put("amount", (amountInRupees * 100).toInt()) // Amount in Paise (e.g. ₹10.00 = 1000 paise)
                    put("order_id", razorpayOrderId)
                    
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
