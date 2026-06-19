package com.example.ui.grooming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.grooming.BookGroomingSlotCommand
import com.example.domain.grooming.BookGroomingSlotUseCase
import com.example.domain.grooming.CancelGroomingBookingUseCase
import com.example.domain.grooming.UpdateGroomingBookingStatusUseCase
import com.example.data.GroomingBookingEntity
import com.example.data.GroomingServiceEntity
import com.example.data.GroomingSlotEntity
import com.example.data.PawsRepository
import com.example.data.ProfileEntity
import com.example.data.ShopEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class GroomingViewModel(
    private val repository: PawsRepository,
    private val currentUserFlow: Flow<ProfileEntity?>,
    private val merchantShopFlow: Flow<ShopEntity?>,
    private val bookGroomingSlotUseCase: BookGroomingSlotUseCase,
    private val cancelGroomingBookingUseCase: CancelGroomingBookingUseCase,
    private val updateGroomingBookingStatusUseCase: UpdateGroomingBookingStatusUseCase
) : ViewModel() {

    fun getActiveGroomingServicesForShopFlow(shopId: String): Flow<List<GroomingServiceEntity>> =
        repository.getActiveGroomingServicesForShopFlow(shopId)

    fun getAllGroomingServicesForShopFlow(shopId: String): Flow<List<GroomingServiceEntity>> =
        repository.getAllGroomingServicesForShopFlow(shopId)

    fun getGroomingSlotsForShopAndDateFlow(shopId: String, date: String): Flow<List<GroomingSlotEntity>> =
        repository.getGroomingSlotsForShopAndDateFlow(shopId, date)

    fun getGroomingSlotsForDateRangeFlow(shopId: String, startDate: String, endDate: String): Flow<List<GroomingSlotEntity>> =
        repository.getGroomingSlotsForDateRangeFlow(shopId, startDate, endDate)

    val myGroomingBookings: Flow<List<GroomingBookingEntity>> = currentUserFlow.flatMapLatest { user ->
        if (user != null) repository.getGroomingBookingsForConsumerFlow(user.id)
        else flowOf(emptyList())
    }

    val merchantGroomingBookings: Flow<List<GroomingBookingEntity>> = merchantShopFlow.flatMapLatest { shop ->
        if (shop != null) repository.getGroomingBookingsForShopFlow(shop.id)
        else flowOf(emptyList())
    }

    fun getOrGenerateSlotsForDate(shopId: String, date: String, onResult: (List<GroomingSlotEntity>) -> Unit) {
        viewModelScope.launch {
            val slots = repository.getOrGenerateSlotsForDate(shopId, date)
            onResult(slots)
        }
    }

    fun bulkEditSlotCapacity(
        shopId: String,
        startDate: String,
        endDate: String,
        daysOfWeek: List<Int>,
        newCapacity: Int,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.bulkEditSlotCapacity(shopId, startDate, endDate, daysOfWeek, newCapacity)
            onResult(true)
        }
    }

    fun toggleSlotBlocked(slot: GroomingSlotEntity) {
        viewModelScope.launch {
            val updated = slot.copy(isBlocked = !slot.isBlocked)
            repository.insertGroomingSlot(updated)
        }
    }

    fun bookGroomingSlot(
        userId: String,
        shopId: String,
        serviceId: String,
        slotId: String,
        petId: String,
        petSizeCategory: String,
        specialInstructions: String?,
        totalPrice: Double,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val bookingId = bookGroomingSlotUseCase(
                    BookGroomingSlotCommand(
                    consumerId = userId,
                    shopId = shopId,
                    serviceId = serviceId,
                    slotId = slotId,
                    petId = petId,
                    petSizeCategory = petSizeCategory,
                        specialInstructions = specialInstructions,
                        totalPrice = totalPrice
                    )
                )
                onSuccess(bookingId)
            } catch (e: Exception) {
                onError(e.message ?: "Booking failed due to slot capacity or network issue.")
            }
        }
    }

    fun cancelGroomingBooking(bookingId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            cancelGroomingBookingUseCase(bookingId)
            onResult(true)
        }
    }

    fun updateGroomingBookingStatus(bookingId: String, status: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            updateGroomingBookingStatusUseCase(bookingId, status)
            onResult(true)
        }
    }

    fun getGroomingBookingById(bookingId: String, onResult: (GroomingBookingEntity?) -> Unit) {
        viewModelScope.launch {
            val booking = repository.getGroomingBookingById(bookingId)
            onResult(booking)
        }
    }

    fun getGroomingServiceById(serviceId: String, onResult: (GroomingServiceEntity?) -> Unit) {
        viewModelScope.launch {
            val service = repository.getGroomingServiceById(serviceId)
            onResult(service)
        }
    }

    fun saveGroomingService(
        shopId: String,
        serviceType: String,
        variantName: String,
        description: String,
        petSizeCategory: String,
        price: Double,
        durationMinutes: Int,
        imageUrls: List<String>,
        isActive: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = "gs_" + shopId + "_" + serviceType.replace("_", "") + "_" + variantName.replace(" ", "").lowercase() + "_" + petSizeCategory
            val service = GroomingServiceEntity(
                id = id,
                shopId = shopId,
                serviceType = serviceType,
                variantName = variantName,
                description = description,
                petSizeCategory = petSizeCategory,
                price = price,
                durationMinutes = durationMinutes,
                imageUrls = imageUrls,
                isActive = isActive,
                createdAt = System.currentTimeMillis()
            )
            repository.insertGroomingService(service)
            onResult(true)
        }
    }

    fun deleteGroomingService(serviceId: String) {
        viewModelScope.launch {
            repository.deleteGroomingService(serviceId)
        }
    }

    fun updateGroomingService(service: GroomingServiceEntity) {
        viewModelScope.launch {
            repository.insertGroomingService(service)
        }
    }

    fun proposeGroomingReschedule(booking: GroomingBookingEntity, newDate: String, newTime: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val updated = booking.copy(
                rescheduleDate = newDate,
                rescheduleTime = newTime,
                status = "reschedule_pending"
            )
            repository.insertGroomingBooking(updated)
            onResult(true)
        }
    }

    fun acceptGroomingReschedule(booking: GroomingBookingEntity, newSlotId: String, newDate: String, newTime: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.updateGroomingBookingStatus(booking.id, "cancelled")
            val updated = booking.copy(
                slotId = newSlotId,
                rescheduleDate = null,
                rescheduleTime = null,
                status = "cancelled"
            )
            repository.insertGroomingBooking(updated)
            repository.updateGroomingBookingStatus(booking.id, "confirmed")
            onResult(true)
        }
    }

    fun declineGroomingReschedule(booking: GroomingBookingEntity, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.updateGroomingBookingStatus(booking.id, "cancelled")
            val updated = booking.copy(
                rescheduleDate = null,
                rescheduleTime = null,
                status = "cancelled"
            )
            repository.insertGroomingBooking(updated)
            onResult(true)
        }
    }
}
