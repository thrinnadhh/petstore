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
import com.razorpay.PaymentResultListener

class MainActivity : ComponentActivity(), PaymentResultListener {
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

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        PaymentManager.onPaymentSuccess(razorpayPaymentId ?: "")
    }

    override fun onPaymentError(code: Int, response: String?) {
        PaymentManager.onPaymentError(code, response ?: "")
    }
}
