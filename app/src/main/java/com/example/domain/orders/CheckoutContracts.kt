package com.example.domain.orders

data class CheckoutProduct(
    val id: String,
    val price: Double
)

data class PlaceOrderCommand(
    val consumerId: String,
    val shopId: String,
    val cartItems: Map<String, Int>,
    val deliveryAddress: String,
    val notes: String,
    val deliveryType: String,
    val deliveryFee: Double
)

data class PlaceOrderRequest(
    val orderId: String,
    val consumerId: String,
    val shopId: String,
    val deliveryType: String,
    val totalAmount: Double,
    val deliveryAddress: String,
    val notes: String,
    val placedAt: Long,
    val items: List<PlaceOrderItemRequest>
)

data class PlaceOrderItemRequest(
    val id: String,
    val orderId: String,
    val productId: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)

data class PlacedOrder(
    val orderId: String,
    val totalAmount: Double,
    val itemCount: Int,
    val deliveryType: String
)

interface CheckoutRepository {
    suspend fun getCheckoutProduct(productId: String): CheckoutProduct?
    suspend fun placeOrder(request: PlaceOrderRequest): PlacedOrder
}
