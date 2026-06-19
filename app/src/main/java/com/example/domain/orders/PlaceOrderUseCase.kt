package com.example.domain.orders

import com.example.domain.common.IdGenerator

class PlaceOrderUseCase(
    private val repository: CheckoutRepository,
    private val idGenerator: IdGenerator,
    private val clockMillis: () -> Long = System::currentTimeMillis
) {
    suspend operator fun invoke(command: PlaceOrderCommand): PlacedOrder {
        require(command.consumerId.isNotBlank()) { "A logged-in user is required to place an order." }
        require(command.shopId.isNotBlank()) { "A shop is required to place an order." }
        require(command.cartItems.isNotEmpty()) { "Cart is empty." }

        val orderId = idGenerator.next("order_")
        var subtotal = 0.0
        val lineItems = mutableListOf<PlaceOrderItemRequest>()

        command.cartItems.forEach { (productId, quantity) ->
            if (quantity > 0) {
                val product = repository.getCheckoutProduct(productId)
                if (product != null) {
                    val lineCost = product.price * quantity
                    subtotal += lineCost
                    lineItems.add(
                        PlaceOrderItemRequest(
                            id = idGenerator.next("item_"),
                            orderId = orderId,
                            productId = product.id,
                            quantity = quantity,
                            unitPrice = product.price,
                            subtotal = lineCost
                        )
                    )
                }
            }
        }

        require(lineItems.isNotEmpty()) { "Cart does not contain orderable products." }

        return repository.placeOrder(
            PlaceOrderRequest(
                orderId = orderId,
                consumerId = command.consumerId,
                shopId = command.shopId,
                deliveryType = command.deliveryType,
                totalAmount = subtotal + command.deliveryFee,
                deliveryAddress = command.deliveryAddress,
                notes = command.notes,
                placedAt = clockMillis(),
                items = lineItems
            )
        )
    }
}
