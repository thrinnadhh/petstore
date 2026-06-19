package com.example.domain.orders

interface OrderStatusRepository {
    suspend fun updateOrderStatus(orderId: String, status: String, captainId: String? = null)
}
