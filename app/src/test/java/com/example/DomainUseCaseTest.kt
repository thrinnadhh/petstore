package com.example

import com.example.data.AppDatabaseMigrations
import com.example.domain.common.IdGenerator
import com.example.domain.grooming.BookGroomingSlotCommand
import com.example.domain.grooming.BookGroomingSlotUseCase
import com.example.domain.grooming.GroomingBookingRepository
import com.example.domain.grooming.GroomingBookingRequest
import com.example.domain.orders.OrderStatusRepository
import com.example.domain.orders.UpdateOrderStatusUseCase
import com.example.domain.vet.BookDoctorAppointmentCommand
import com.example.domain.vet.BookDoctorAppointmentUseCase
import com.example.domain.vet.DoctorAppointmentRepository
import com.example.domain.vet.DoctorAppointmentRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainUseCaseTest {
    @Test
    fun `book grooming slot builds a sanitized pending booking request`() = runBlocking {
        val repository = FakeGroomingRepository()
        val useCase = BookGroomingSlotUseCase(
            repository = repository,
            idGenerator = FixedIdGenerator("gr_bk_fixed"),
            clockMillis = { 1234L }
        )

        val bookingId = useCase(
            BookGroomingSlotCommand(
                consumerId = "consumer_1",
                shopId = "shop_1",
                serviceId = "service_1",
                slotId = "slot_1",
                petId = "pet_1",
                petSizeCategory = "small",
                specialInstructions = "  trim nails gently  ",
                totalPrice = 450.0
            )
        )

        assertEquals("gr_bk_fixed", bookingId)
        assertEquals("gr_bk_fixed", repository.bookedRequest?.id)
        assertEquals("pending", repository.bookedRequest?.status)
        assertEquals("trim nails gently", repository.bookedRequest?.specialInstructions)
        assertEquals(1234L, repository.bookedRequest?.bookedAt)
    }

    @Test
    fun `book doctor appointment defaults blank pet name and forwards slot`() = runBlocking {
        val repository = FakeDoctorAppointmentRepository()
        val useCase = BookDoctorAppointmentUseCase(
            repository = repository,
            idGenerator = FixedIdGenerator("appt_fixed"),
            clockMillis = { 5678L }
        )

        val appointmentId = useCase(
            BookDoctorAppointmentCommand(
                consumerId = "consumer_1",
                shopId = "clinic_1",
                serviceId = "vet_consult",
                serviceName = "Vet Consultation",
                price = 600.0,
                date = "2026-07-01",
                time = "10:00 AM",
                petName = "   ",
                doctorId = "doctor_1",
                slotId = "slot_1",
                concern = "itching",
                priority = "High"
            )
        )

        assertEquals("appt_fixed", appointmentId)
        assertEquals("appt_fixed", repository.bookedRequest?.id)
        assertEquals("Buddy", repository.bookedRequest?.petName)
        assertEquals("pending", repository.bookedRequest?.status)
        assertEquals("slot_1", repository.bookedSlotId)
        assertEquals(5678L, repository.bookedRequest?.createdAt)
    }

    @Test
    fun `update order status use case forwards optional captain assignment`() = runBlocking {
        val repository = FakeOrderStatusRepository()
        val useCase = UpdateOrderStatusUseCase(repository)

        useCase("order_1", "preparing", "captain_1")

        assertEquals("order_1", repository.orderId)
        assertEquals("preparing", repository.status)
        assertEquals("captain_1", repository.captainId)
    }

    @Test
    fun `database migration registry includes current version migration`() {
        assertTrue(AppDatabaseMigrations.ALL.any { it.startVersion == 22 && it.endVersion == 23 })
    }

    private class FixedIdGenerator(private val value: String) : IdGenerator {
        override fun next(prefix: String): String = value
    }

    private class FakeGroomingRepository : GroomingBookingRepository {
        var bookedRequest: GroomingBookingRequest? = null

        override suspend fun bookGroomingSlot(request: GroomingBookingRequest): String {
            bookedRequest = request
            return request.id
        }

        override suspend fun cancelGroomingBooking(bookingId: String) = Unit

        override suspend fun updateGroomingBookingStatus(bookingId: String, status: String) = Unit
    }

    private class FakeDoctorAppointmentRepository : DoctorAppointmentRepository {
        var bookedRequest: DoctorAppointmentRequest? = null
        var bookedSlotId: String? = null

        override suspend fun bookDoctorAppointment(
            request: DoctorAppointmentRequest,
            slotId: String?
        ): String {
            bookedRequest = request
            bookedSlotId = slotId
            return request.id
        }

        override suspend fun cancelDoctorAppointment(appointmentId: String, slotId: String?) = Unit
    }

    private class FakeOrderStatusRepository : OrderStatusRepository {
        var orderId: String? = null
        var status: String? = null
        var captainId: String? = null

        override suspend fun updateOrderStatus(orderId: String, status: String, captainId: String?) {
            this.orderId = orderId
            this.status = status
            this.captainId = captainId
        }
    }
}
