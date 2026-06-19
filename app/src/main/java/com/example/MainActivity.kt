package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.PaymentManager
import com.example.ui.PawsApp
import com.example.ui.PawsViewModel
import com.example.ui.theme.MyApplicationTheme
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private val viewModel: PawsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PawsApp(viewModel = viewModel)
            }
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        PaymentManager.onPaymentSuccess(
            paymentId = razorpayPaymentId.orEmpty(),
            razorpayOrderId = paymentData?.orderId,
            signature = paymentData?.signature
        )
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        PaymentManager.onPaymentError(code, response ?: "")
    }
}
