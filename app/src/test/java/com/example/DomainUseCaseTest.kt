package com.example

import com.example.data.AppDatabaseMigrations
import com.example.domain.cart.AddToCartResult
import com.example.domain.cart.AddToCartUseCase
import com.example.domain.cart.CartState
import com.example.domain.cart.RemoveFromCartUseCase
import com.example.domain.common.IdGenerator
import com.example.domain.grooming.BookGroomingSlotCommand
import com.example.domain.grooming.BookGroomingSlotUseCase
import com.example.domain.grooming.GroomingBookingRepository
import com.example.domain.grooming.GroomingBookingRequest
import com.example.domain.orders.CheckoutProduct
import com.example.domain.orders.CheckoutRepository
import com.example.domain.orders.OrderStatusRepository
import com.example.domain.orders.PlaceOrderCommand
import com.example.domain.orders.PlaceOrderRequest
import com.example.domain.orders.PlaceOrderUseCase
import com.example.domain.orders.PlacedOrder
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
    fun `add to cart increments same shop items and rejects cross shop add`() {
        val useCase = AddToCartUseCase()

        val first = useCase(CartState(shopId = null, items = emptyMap()), "product_1", "shop_1")
        val second = useCase((first as AddToCartResult.Updated).state, "product_1", "shop_1")
        val conflict = useCase((second as AddToCartResult.Updated).state, "product_2", "shop_2")

        assertEquals(mapOf("product_1" to 2), second.state.items)
        assertTrue(conflict is AddToCartResult.ShopConflict)
    }

    @Test
    fun `remove from cart decrements quantity and clears shop when empty`() {
        val useCase = RemoveFromCartUseCase()

        val once = useCase(CartState("shop_1", mapOf("product_1" to 2)), "product_1")
        val empty = useCase(once, "product_1")

        assertEquals(mapOf("product_1" to 1), once.items)
        assertEquals(null, empty.shopId)
        assertTrue(empty.items.isEmpty())
    }

    @Test
    fun `place order calculates line totals and delivery fee outside ViewModel`() = runBlocking {
        val repository = FakeCheckoutRepository(
            products = mapOf(
                "product_1" to CheckoutProduct("product_1", 100.0),
                "product_2" to CheckoutProduct("product_2", 50.0)
            )
        )
        val useCase = PlaceOrderUseCase(
            repository = repository,
            idGenerator = SequenceIdGenerator(listOf("order_fixed", "item_1", "item_2")),
            clockMillis = { 999L }
        )

        val result = useCase(
            PlaceOrderCommand(
                consumerId = "consumer_1",
                shopId = "shop_1",
                cartItems = mapOf("product_1" to 2, "product_2" to 1),
                deliveryAddress = "Hyderabad",
                notes = "Leave at door",
                deliveryType = "delivery",
                deliveryFee = 30.0
            )
        )

        assertEquals("order_fixed", result.orderId)
        assertEquals(280.0, result.totalAmount, 0.0)
        assertEquals(2, result.itemCount)
        assertEquals(999L, repository.placedRequest?.placedAt)
        assertEquals(listOf(200.0, 50.0), repository.placedRequest?.items?.map { it.subtotal })
    }

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

    private class SequenceIdGenerator(values: List<String>) : IdGenerator {
        private val ids = values.toMutableList()

        override fun next(prefix: String): String = ids.removeAt(0)
    }

    private class FakeCheckoutRepository(
        private val products: Map<String, CheckoutProduct>
    ) : CheckoutRepository {
        var placedRequest: PlaceOrderRequest? = null

        override suspend fun getCheckoutProduct(productId: String): CheckoutProduct? {
            return products[productId]
        }

        override suspend fun placeOrder(request: PlaceOrderRequest): PlacedOrder {
            placedRequest = request
            return PlacedOrder(
                orderId = request.orderId,
                totalAmount = request.totalAmount,
                itemCount = request.items.size,
                deliveryType = request.deliveryType
            )
        }
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
