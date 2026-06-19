package com.example.domain.grooming

import com.example.domain.common.IdGenerator

class BookGroomingSlotUseCase(
    private val repository: GroomingBookingRepository,
    private val idGenerator: IdGenerator,
    private val clockMillis: () -> Long = System::currentTimeMillis
) {
    suspend operator fun invoke(command: BookGroomingSlotCommand): String {
        val bookingId = idGenerator.next("gr_bk_")
        return repository.bookGroomingSlot(
            GroomingBookingRequest(
                id = bookingId,
                consumerId = command.consumerId,
                shopId = command.shopId,
                serviceId = command.serviceId,
                slotId = command.slotId,
                petId = command.petId,
                petSizeCategory = command.petSizeCategory,
                status = "pending",
                specialInstructions = command.specialInstructions?.trim()?.takeIf { it.isNotEmpty() },
                totalPrice = command.totalPrice,
                bookedAt = clockMillis()
            )
        )
    }
}
