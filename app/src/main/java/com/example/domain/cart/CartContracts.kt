package com.example.domain.cart

data class CartState(
    val shopId: String?,
    val items: Map<String, Int>
)

sealed interface AddToCartResult {
    data class Updated(val state: CartState) : AddToCartResult
    data class ShopConflict(val currentShopId: String, val pendingProductId: String) : AddToCartResult
}
