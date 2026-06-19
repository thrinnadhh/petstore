package com.example.domain.cart

class ClearCartUseCase {
    operator fun invoke(): CartState = CartState(shopId = null, items = emptyMap())
}
