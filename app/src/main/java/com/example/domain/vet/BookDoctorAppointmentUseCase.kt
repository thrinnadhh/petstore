package com.example.domain.vet

import com.example.domain.common.IdGenerator

class BookDoctorAppointmentUseCase(
    private val repository: DoctorAppointmentRepository,
    private val idGenerator: IdGenerator,
    private val clockMillis: () -> Long = System::currentTimeMillis
) {
    suspend operator fun invoke(command: BookDoctorAppointmentCommand): String {
        val appointmentId = idGenerator.next("appt_")
        return repository.bookDoctorAppointment(
            DoctorAppointmentRequest(
                id = appointmentId,
                consumerId = command.consumerId,
                shopId = command.shopId,
                serviceId = command.serviceId,
                serviceName = command.serviceName,
                price = command.price,
                appointmentDate = command.date,
                appointmentTime = command.time,
                petName = command.petName.trim().ifEmpty { "Buddy" },
                status = "pending",
                doctorId = command.doctorId,
                createdAt = clockMillis(),
                concern = command.concern,
                priority = command.priority
            ),
            command.slotId
        )
    }
}
