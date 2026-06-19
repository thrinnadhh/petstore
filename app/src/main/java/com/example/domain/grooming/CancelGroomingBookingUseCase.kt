package com.example.domain.grooming

class CancelGroomingBookingUseCase(
    private val repository: GroomingBookingRepository
) {
    suspend operator fun invoke(bookingId: String) {
        repository.cancelGroomingBooking(bookingId)
    }
}
