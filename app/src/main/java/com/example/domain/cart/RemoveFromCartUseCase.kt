package com.example.domain.cart

class RemoveFromCartUseCase {
    operator fun invoke(state: CartState, productId: String): CartState {
        val quantity = state.items[productId] ?: return state
        val updatedItems = if (quantity <= 1) {
            state.items - productId
        } else {
            state.items + (productId to quantity - 1)
        }

        return state.copy(
            shopId = state.shopId.takeUnless { updatedItems.isEmpty() },
            items = updatedItems
        )
    }
}
