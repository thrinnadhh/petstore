package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val phone: String,
    val cityId: String,
    val avatarUrl: String,
    val role: String, // "consumer", "merchant", "admin"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val state: String,
    val isActive: Boolean = true,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val cityId: String,
    val name: String,
    val description: String,
    val address: String,
    val locality: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val phone: String,
    val email: String,
    val photos: List<String> = emptyList(),
    val isOpen: Boolean = true,
    val opensAt: String = "09:00",
    val closesAt: String = "21:00",
    val rating: Double = 4.5,
    val totalReviews: Int = 1,
    val deliveryAvailable: Boolean = true,
    val isVerified: Boolean = true,
    val isActive: Boolean = true,
    val isFeatured: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String, // "Food", "Grooming", "Accessories", "Vet Care"
    val iconUrl: String
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val price: Double,
    val mrp: Double,
    val photos: List<String> = emptyList(),
    val inStock: Boolean = true,
    val isActive: Boolean = true,
    val tags: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val consumerId: String,
    val shopId: String,
    val type: String, // "delivery" or "pickup"
    val status: String, // "pending", "accepted", "preparing", "out_for_delivery", "delivered", "rejected", "cancelled"
    val totalAmount: Double,
    val deliveryAddress: String, // JSON or formatted text
    val notes: String = "",
    val placedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productId: String,
    val quantity: Int,
    val unitPrice: Double,
    val subtotal: Double
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val consumerId: String,
    val rating: Int, // 1 to 5
    val comment: String,
    val photos: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "wishlists")
data class WishlistEntity(
    @PrimaryKey val id: String,
    val consumerId: String,
    val shopId: String,
    val createdAt: Long = System.currentTimeMillis()
)
