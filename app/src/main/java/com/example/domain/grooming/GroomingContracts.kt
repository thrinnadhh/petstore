package com.example.domain.grooming

data class BookGroomingSlotCommand(
    val consumerId: String,
    val shopId: String,
    val serviceId: String,
    val slotId: String,
    val petId: String,
    val petSizeCategory: String,
    val specialInstructions: String?,
    val totalPrice: Double
)

data class GroomingBookingRequest(
    val id: String,
    val consumerId: String,
    val shopId: String,
    val serviceId: String,
    val slotId: String,
    val petId: String,
    val petSizeCategory: String,
    val status: String,
    val specialInstructions: String?,
    val totalPrice: Double,
    val bookedAt: Long
)

interface GroomingBookingRepository {
    suspend fun bookGroomingSlot(request: GroomingBookingRequest): String
    suspend fun cancelGroomingBooking(bookingId: String)
    suspend fun updateGroomingBookingStatus(bookingId: String, status: String)
}
