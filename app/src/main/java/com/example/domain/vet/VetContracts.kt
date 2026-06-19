package com.example.domain.vet

data class BookDoctorAppointmentCommand(
    val consumerId: String,
    val shopId: String,
    val serviceId: String,
    val serviceName: String,
    val price: Double,
    val date: String,
    val time: String,
    val petName: String,
    val doctorId: String?,
    val slotId: String?,
    val concern: String,
    val priority: String
)

data class DoctorAppointmentRequest(
    val id: String,
    val consumerId: String,
    val shopId: String,
    val serviceId: String,
    val serviceName: String,
    val price: Double,
    val appointmentDate: String,
    val appointmentTime: String,
    val petName: String,
    val status: String,
    val doctorId: String?,
    val createdAt: Long,
    val concern: String,
    val priority: String
)

interface DoctorAppointmentRepository {
    suspend fun bookDoctorAppointment(request: DoctorAppointmentRequest, slotId: String?): String
    suspend fun cancelDoctorAppointment(appointmentId: String, slotId: String?)
}
