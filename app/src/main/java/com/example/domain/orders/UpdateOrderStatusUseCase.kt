package com.example.domain.orders

class UpdateOrderStatusUseCase(
    private val repository: OrderStatusRepository
) {
    suspend operator fun invoke(orderId: String, status: String, captainId: String? = null) {
        repository.updateOrderStatus(orderId, status, captainId)
    }
}
