package com.example.ui.doctor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppointmentEntity
import com.example.data.DoctorEntity
import com.example.data.DoctorSlotEntity
import com.example.data.PawsRepository
import com.example.data.ProfileEntity
import com.example.domain.vet.BookDoctorAppointmentCommand
import com.example.domain.vet.BookDoctorAppointmentUseCase
import com.example.domain.vet.CancelDoctorAppointmentUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class DoctorViewModel(
    private val repository: PawsRepository,
    private val currentUserFlow: Flow<ProfileEntity?>,
    private val bookDoctorAppointmentUseCase: BookDoctorAppointmentUseCase,
    private val cancelDoctorAppointmentUseCase: CancelDoctorAppointmentUseCase
) : ViewModel() {
    
    fun getDoctorsForShopFlow(shopId: String): Flow<List<DoctorEntity>> =
        repository.getDoctorsForShopFlow(shopId)

    fun getDoctorById(id: String, onResult: (DoctorEntity?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getDoctorById(id))
        }
    }

    fun getDoctorSlotsFlow(shopId: String, doctorId: String, date: String): Flow<List<DoctorSlotEntity>> =
        repository.getDoctorSlotsFlow(shopId, doctorId, date)

    fun getOrGenerateDoctorSlotsForDate(shopId: String, doctorId: String, date: String, onResult: (List<DoctorSlotEntity>) -> Unit) {
        viewModelScope.launch {
            val slots = repository.getOrGenerateDoctorSlotsForDate(shopId, doctorId, date)
            onResult(slots)
        }
    }

    fun toggleDoctorSlotBlocked(slot: DoctorSlotEntity) {
        viewModelScope.launch {
            repository.toggleDoctorSlotBlocked(slot)
        }
    }

    fun updateDoctorSlotCapacity(slotId: String, capacity: Int) {
        viewModelScope.launch {
            repository.updateDoctorSlotCapacity(slotId, capacity)
        }
    }

    fun saveDoctor(
        id: String?,
        shopId: String,
        name: String,
        photoUrl: String,
        qualification: String,
        specialization: String,
        workingDays: List<String>,
        activeSlots: List<String>,
        isAvailable: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val docId = id ?: ("doc_" + UUID.randomUUID().toString().substring(0, 8))
            val doctor = DoctorEntity(
                id = docId,
                shopId = shopId,
                name = name.trim(),
                photoUrl = photoUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400" },
                qualification = qualification.trim(),
                specialization = specialization.trim(),
                workingDays = workingDays,
                activeSlots = activeSlots,
                isAvailable = isAvailable
            )
            repository.insertDoctor(doctor)
            onResult(true)
        }
    }

    fun deleteDoctor(doctorId: String) {
        viewModelScope.launch {
            repository.deleteDoctor(doctorId)
        }
    }

    fun bookDoctorAppointment(
        shopId: String,
        serviceId: String,
        serviceName: String,
        price: Double,
        date: String,
        time: String,
        petName: String,
        doctorId: String?,
        slotId: String?,
        concern: String = "",
        priority: String = "Normal",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val user = currentUserFlow.firstOrNull()
            if (user == null) {
                onError("User not logged in.")
                return@launch
            }
            try {
                bookDoctorAppointmentUseCase(
                    BookDoctorAppointmentCommand(
                        consumerId = user.id,
                        shopId = shopId,
                        serviceId = serviceId,
                        serviceName = serviceName,
                        price = price,
                        date = date,
                        time = time,
                        petName = petName.trim().ifEmpty { "Buddy" },
                        doctorId = doctorId,
                        slotId = slotId,
                        concern = concern,
                        priority = priority
                    )
                )
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Booking failed.")
            }
        }
    }

    fun proposeReschedule(appointment: AppointmentEntity, newDate: String, newTime: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val updated = appointment.copy(
                rescheduleDate = newDate,
                rescheduleTime = newTime,
                status = "reschedule_pending"
            )
            repository.insertAppointment(updated)
            onResult(true)
        }
    }

    fun acceptReschedule(appointment: AppointmentEntity, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val updated = appointment.copy(
                appointmentDate = appointment.rescheduleDate ?: appointment.appointmentDate,
                appointmentTime = appointment.rescheduleTime ?: appointment.appointmentTime,
                rescheduleDate = null,
                rescheduleTime = null,
                status = "confirmed"
            )
            repository.insertAppointment(updated)
            onResult(true)
        }
    }

    fun declineReschedule(appointment: AppointmentEntity, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val updated = appointment.copy(
                rescheduleDate = null,
                rescheduleTime = null,
                status = "cancelled"
            )
            repository.insertAppointment(updated)
            onResult(true)
        }
    }

    fun cancelAppointmentWithRefund(appointment: AppointmentEntity, slotId: String?, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            cancelDoctorAppointmentUseCase(appointment.id, slotId)
            onResult(true)
        }
    }
}
