package com.example.domain.vet

class CancelDoctorAppointmentUseCase(
    private val repository: DoctorAppointmentRepository
) {
    suspend operator fun invoke(appointmentId: String, slotId: String?) {
        repository.cancelDoctorAppointment(appointmentId, slotId)
    }
}
