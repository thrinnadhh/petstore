package com.example.domain.grooming

class UpdateGroomingBookingStatusUseCase(
    private val repository: GroomingBookingRepository
) {
    suspend operator fun invoke(bookingId: String, status: String) {
        repository.updateGroomingBookingStatus(bookingId, status)
    }
}
