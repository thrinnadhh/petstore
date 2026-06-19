package com.example.domain.cart

class AddToCartUseCase {
    operator fun invoke(state: CartState, productId: String, shopId: String): AddToCartResult {
        val currentShopId = state.shopId
        if (currentShopId != null && currentShopId != shopId) {
            return AddToCartResult.ShopConflict(
                currentShopId = currentShopId,
                pendingProductId = productId
            )
        }

        val quantity = state.items[productId] ?: 0
        return AddToCartResult.Updated(
            state.copy(
                shopId = shopId,
                items = state.items + (productId to quantity + 1)
            )
        )
    }
}
