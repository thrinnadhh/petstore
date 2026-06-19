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
    val petName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val email: String? = null,
    val password: String? = null,
    val address: String = ""
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
    val status: String = "active",
    val groomingEnabled: Boolean = true,
    val vetClinicEnabled: Boolean = true,
    val shopEnabled: Boolean = true,
    val vetLicenseNumber: String = "",
    val isVetVerified: Boolean = false,
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
    val brand: String = "Generic",
    val lifeStage: String = "Adult",
    val stockCount: Int = 10,
    val sampleAttachedProductId: String? = null,
    val sampleDescription: String = "",
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
    val captainId: String? = null,
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

@Entity(tableName = "banners")
data class BannerEntity(
    @PrimaryKey val id: String,
    val imageUrl: String,
    val title: String,
    val description: String,
    val targetCityIds: List<String>, // "all" or specific city IDs
    val targetShopIds: List<String>, // "all" or specific shop IDs
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val recipientId: String,
    val shopId: String,
    val message: String,
    val senderName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "wishlist_products")
data class WishlistProductEntity(
    @PrimaryKey val id: String,
    val consumerId: String,
    val productId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val name: String,
    val price: Double,
    val category: String = "Grooming", // "Grooming" | "Vet Care" | "Food" | "Other"
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val consumerId: String,
    val shopId: String,
    val serviceId: String,
    val serviceName: String,
    val price: Double,
    val appointmentDate: String,
    val appointmentTime: String,
    val petName: String = "Buddy",
    val status: String = "pending", // "pending", "confirmed", "completed", "cancelled", "reschedule_pending", "no_show"
    val doctorId: String? = null,
    val rescheduleDate: String? = null,
    val rescheduleTime: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val concern: String = "",
    val priority: String = "Normal"
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val consumerId: String,
    val title: String, // "Doctor Appointment", "Pet Birthday", "Vaccination", "Grooming Date", or "Custom Alert"
    val petName: String,
    val dateString: String,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val type: String = "general", // "doctor", "birthday", "vaccination", "grooming"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "product_specs")
data class ProductSpecEntity(
    @PrimaryKey val id: String,
    val productId: String, // Associated main product ID (e.g. for Pedigree)
    val weightText: String, // Weight tag (e.g. "3 kg", "10 kg")
    val petCategory: String = "dog", // "cat", "dog", "cattle", "kitten", "puppy", "hamster", "rabbits", "birds"
    val imageUrls: List<String> = emptyList(), // Store up to 4 package images
    val description1: String = "",
    val description2: String = "",
    val description3: String = "",
    val description4: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val name: String,
    val breed: String,
    val ageText: String, // e.g. "2 years"
    val weight: String = "", // e.g. "24 kg"
    val avatarUrl: String = "",
    val allergies: String = "",
    val vaccineRecord: String = "", // e.g. "Dewormed, Rabies vaccine"
    val dewormingDate: String = "", // e.g. "2026-05-15"
    val vaccineDueDate: String = "", // e.g. "2026-08-20"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "captains")
data class CaptainEntity(
    @PrimaryKey val id: String,
    val userId: String, // Associated profile ID
    val fullName: String,
    val phone: String,
    val vehicleNumber: String,
    val panCard: String,
    val bankDetails: String,
    val aadharNumber: String,
    val panCardUrl: String = "",
    val aadharCardUrl: String = "",
    val licenseUrl: String = "",
    val selfieUrl: String = "",
    val status: String = "pending", // "pending", "approved", "rejected"
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "pet_problems")
data class ProblemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val solution: String = "",
    val howToUse: String = "",
    val emoji: String,
    val productIds: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "group_rfq_sessions")
data class GroupRfqSessionEntity(
    @PrimaryKey val id: String,
    val hostId: String,
    val cityId: String,
    val status: String = "open", // "open" (adding items) | "bidding" (merchants quoting) | "accepted" (quote chosen) | "completed"
    val chosenQuotationId: String? = null,
    val biddingExpiresAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_rfq_member_items")
data class GroupRfqMemberItemEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val memberId: String,
    val memberName: String,
    val productId: String,
    val quantity: Int,
    val deliveryAddress: String,
    val lat: Double,
    val lng: Double,
    val hasPaid: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "merchant_quotations")
data class MerchantQuotationEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val shopId: String,
    val shopName: String,
    val discountPercentage: Double,
    val quotedPrice: Double,
    val isAccepted: Boolean = false,
    val submittedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "grooming_services")
data class GroomingServiceEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val serviceType: String, // "bath" | "haircut" | "bath_and_haircut" | "nail_trim" | "ear_cleaning" | "full_grooming"
    val variantName: String,
    val description: String,
    val petSizeCategory: String, // "small" | "medium" | "large"
    val price: Double,
    val durationMinutes: Int,
    val imageUrls: List<String>,
    val isActive: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "grooming_slots")
data class GroomingSlotEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val slotDate: String, // YYYY-MM-DD
    val slotTime: String, // HH:mm
    val capacity: Int = 1,
    val bookedCount: Int = 0,
    val isBlocked: Boolean = false
)

@Entity(tableName = "grooming_bookings")
data class GroomingBookingEntity(
    @PrimaryKey val id: String,
    val consumerId: String,
    val shopId: String,
    val serviceId: String,
    val slotId: String,
    val petId: String,
    val petSizeCategory: String,
    val status: String, // "pending" | "confirmed" | "in_progress" | "completed" | "cancelled" | "no_show" | "reschedule_pending"
    val specialInstructions: String? = null,
    val totalPrice: Double,
    val rescheduleDate: String? = null,
    val rescheduleTime: String? = null,
    val bookedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val name: String,
    val photoUrl: String,
    val qualification: String,
    val specialization: String,
    val workingDays: List<String> = emptyList(),
    val activeSlots: List<String> = emptyList(),
    val isAvailable: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "doctor_slots")
data class DoctorSlotEntity(
    @PrimaryKey val id: String,
    val doctorId: String,
    val shopId: String,
    val slotDate: String, // YYYY-MM-DD
    val slotTime: String, // HH:mm
    val capacity: Int = 1,
    val bookedCount: Int = 0,
    val isBlocked: Boolean = false
)

@Entity(tableName = "coupons")
data class CouponEntity(
    @PrimaryKey val id: String,
    val shopId: String, // "global" or specific shopId
    val code: String,
    val discountPercentage: Double,
    val maxDiscount: Double,
    val minOrderAmount: Double,
    val isActive: Boolean = true
)





