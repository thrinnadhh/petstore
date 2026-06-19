package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppointmentEntity
import com.example.data.CityEntity
import com.example.data.CouponEntity
import com.example.data.DoctorEntity
import com.example.data.GroomingBookingEntity
import com.example.data.GroomingServiceEntity
import com.example.data.GroomingSlotEntity
import com.example.data.OrderEntity
import com.example.data.PawsRepository
import com.example.data.ProductEntity
import com.example.data.ProfileEntity
import com.example.data.ShopEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HardcoreRegressionTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: PawsRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PawsRepository(db.pawsDao())
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `active shop listing excludes inactive unverified and declined shops`() = runBlocking {
        repository.insertCity(CityEntity("hyd", "Hyderabad", "Telangana", true))
        repository.insertShop(makeShop(id = "shop_active"))
        repository.insertShop(makeShop(id = "shop_inactive", isActive = false))
        repository.insertShop(makeShop(id = "shop_unverified", isVerified = false))
        repository.insertShop(makeShop(id = "shop_declined", status = "declined"))
        repository.insertShop(makeShop(id = "shop_pending", status = "pending"))

        val visibleShopIds = repository.getShopsForCitySync("hyd").map { it.id }

        assertEquals(listOf("shop_active"), visibleShopIds)
    }

    @Test
    fun `inactive products never appear in active product feeds`() = runBlocking {
        repository.insertProduct(makeProduct(id = "prod_active", isActive = true))
        repository.insertProduct(makeProduct(id = "prod_inactive", isActive = false))

        val productIds = repository.allProductsFlow.first().map { it.id }

        assertEquals(listOf("prod_active"), productIds)
    }

    @Test
    fun `coupon lookup excludes inactive global and inactive shop-specific coupons`() = runBlocking {
        repository.insertCoupon(
            CouponEntity(
                id = "coupon_active_shop",
                shopId = "shop_1",
                code = "SHOP10",
                discountPercentage = 10.0,
                maxDiscount = 100.0,
                minOrderAmount = 300.0,
                isActive = true
            )
        )
        repository.insertCoupon(
            CouponEntity(
                id = "coupon_inactive_shop",
                shopId = "shop_1",
                code = "DEADSHOP",
                discountPercentage = 90.0,
                maxDiscount = 900.0,
                minOrderAmount = 1.0,
                isActive = false
            )
        )
        repository.insertCoupon(
            CouponEntity(
                id = "coupon_inactive_global",
                shopId = "global",
                code = "DEADGLOBAL",
                discountPercentage = 90.0,
                maxDiscount = 900.0,
                minOrderAmount = 1.0,
                isActive = false
            )
        )

        val coupons = repository.getCouponsForShopFlow("shop_1").first()

        assertEquals(listOf("SHOP10"), coupons.map { it.code })
    }

    @Test
    fun `order status updates preserve assigned captain unless a replacement is supplied`() = runBlocking {
        val order = OrderEntity(
            id = "order_1",
            consumerId = "consumer_1",
            shopId = "shop_1",
            type = "delivery",
            status = "accepted",
            totalAmount = 500.0,
            deliveryAddress = "Hyderabad",
            captainId = "captain_1"
        )
        repository.insertOrder(order)

        repository.updateOrderStatus("order_1", "delivered")

        val deliveredOrder = repository.getOrderById("order_1")
        assertNotNull(deliveredOrder)
        assertEquals("delivered", deliveredOrder?.status)
        assertEquals("captain_1", deliveredOrder?.captainId)
    }

    @Test
    fun `grooming slot cannot be overbooked beyond capacity`() = runBlocking {
        val shop = makeShop(id = "shop_grooming", groomingEnabled = true)
        repository.insertShop(shop)
        repository.insertGroomingService(
            GroomingServiceEntity(
                id = "service_small_bath",
                shopId = shop.id,
                serviceType = "bath",
                variantName = "Small Bath",
                description = "Gentle wash",
                petSizeCategory = "small",
                price = 400.0,
                durationMinutes = 30,
                imageUrls = emptyList(),
                isActive = true
            )
        )
        repository.insertGroomingSlot(
            GroomingSlotEntity(
                id = "slot_1",
                shopId = shop.id,
                slotDate = "2026-07-01",
                slotTime = "10:00",
                capacity = 1
            )
        )

        repository.bookGroomingSlot(makeGroomingBooking(id = "booking_1"))
        val error = expectFailure {
            repository.bookGroomingSlot(makeGroomingBooking(id = "booking_2"))
        }

        assertTrue(error.message.orEmpty().contains("fully booked", ignoreCase = true))
        assertEquals(1, repository.getGroomingSlotById("slot_1")?.bookedCount)
    }

    @Test
    fun `grooming booking cancellation is idempotent and never creates negative capacity`() = runBlocking {
        repository.insertShop(makeShop(id = "shop_grooming", groomingEnabled = true))
        repository.insertGroomingService(
            GroomingServiceEntity(
                id = "service_small_bath",
                shopId = "shop_grooming",
                serviceType = "bath",
                variantName = "Small Bath",
                description = "Gentle wash",
                petSizeCategory = "small",
                price = 400.0,
                durationMinutes = 30,
                imageUrls = emptyList(),
                isActive = true
            )
        )
        repository.insertGroomingSlot(
            GroomingSlotEntity(
                id = "slot_1",
                shopId = "shop_grooming",
                slotDate = "2026-07-01",
                slotTime = "10:00",
                capacity = 1
            )
        )
        repository.bookGroomingSlot(makeGroomingBooking(id = "booking_1"))

        repository.cancelGroomingBooking("booking_1")
        repository.cancelGroomingBooking("booking_1")

        val slot = repository.getGroomingSlotById("slot_1")
        assertEquals(0, slot?.bookedCount)
        assertEquals("cancelled", repository.getGroomingBookingById("booking_1")?.status)
    }

    @Test
    fun `doctor appointment slot cannot be overbooked beyond capacity`() = runBlocking {
        repository.insertShop(makeShop(id = "shop_hospital", vetClinicEnabled = true))
        repository.insertDoctor(
            DoctorEntity(
                id = "doctor_1",
                shopId = "shop_hospital",
                name = "Dr Test",
                photoUrl = "",
                qualification = "DVM",
                specialization = "General",
                activeSlots = listOf("10:00 AM")
            )
        )
        db.pawsDao().insertDoctorSlot(
            com.example.data.DoctorSlotEntity(
                id = "doctor_slot_1",
                doctorId = "doctor_1",
                shopId = "shop_hospital",
                slotDate = "2026-07-01",
                slotTime = "10:00 AM",
                capacity = 1
            )
        )

        repository.bookDoctorAppointment(makeAppointment(id = "appt_1"), "doctor_slot_1")
        val error = expectFailure {
            repository.bookDoctorAppointment(makeAppointment(id = "appt_2"), "doctor_slot_1")
        }

        assertTrue(error.message.orEmpty().contains("fully booked", ignoreCase = true))
        assertEquals(1, db.pawsDao().getDoctorSlotById("doctor_slot_1")?.bookedCount)
    }

    @Test
    fun `doctor appointment cancellation is idempotent and never creates negative capacity`() = runBlocking {
        repository.insertShop(makeShop(id = "shop_hospital", vetClinicEnabled = true))
        repository.insertDoctor(
            DoctorEntity(
                id = "doctor_1",
                shopId = "shop_hospital",
                name = "Dr Test",
                photoUrl = "",
                qualification = "DVM",
                specialization = "General",
                activeSlots = listOf("10:00 AM")
            )
        )
        db.pawsDao().insertDoctorSlot(
            com.example.data.DoctorSlotEntity(
                id = "doctor_slot_1",
                doctorId = "doctor_1",
                shopId = "shop_hospital",
                slotDate = "2026-07-01",
                slotTime = "10:00 AM",
                capacity = 1
            )
        )
        repository.bookDoctorAppointment(makeAppointment(id = "appt_1"), "doctor_slot_1")

        repository.cancelDoctorAppointment("appt_1", "doctor_slot_1")
        repository.cancelDoctorAppointment("appt_1", "doctor_slot_1")

        assertEquals(0, db.pawsDao().getDoctorSlotById("doctor_slot_1")?.bookedCount)
    }

    @Test
    fun `database seeding preserves pre-existing merchant shops and products`() = runBlocking {
        repository.insertShop(makeShop(id = "merchant_shop_before_seed", ownerId = "merchant_1"))
        repository.insertProduct(
            makeProduct(
                id = "merchant_product_before_seed",
                shopId = "merchant_shop_before_seed"
            )
        )

        repository.seedDatabaseIfEmpty()

        assertNotNull(repository.getShopById("merchant_shop_before_seed"))
        assertNotNull(repository.getProductById("merchant_product_before_seed"))
    }

    @Test
    fun `profile passwords should not be persisted as plain text credentials`() = runBlocking {
        repository.insertProfile(
            ProfileEntity(
                id = "consumer_sensitive",
                fullName = "Sensitive User",
                phone = "799999999999",
                cityId = "hyd",
                avatarUrl = "",
                role = "consumer",
                password = "1234"
            )
        )

        val storedProfile = repository.getProfile("consumer_sensitive")

        assertNotNull(storedProfile)
        assertFalse(
            "Passwords should be hashed or delegated to an auth provider before persistence.",
            storedProfile?.password == "1234"
        )
    }

    private fun makeShop(
        id: String,
        ownerId: String = "owner_$id",
        isActive: Boolean = true,
        isVerified: Boolean = true,
        status: String = "active",
        groomingEnabled: Boolean = false,
        vetClinicEnabled: Boolean = false
    ) = ShopEntity(
        id = id,
        ownerId = ownerId,
        cityId = "hyd",
        name = "Shop $id",
        description = "Regression test shop",
        address = "Road 1",
        locality = "Banjara Hills",
        phone = "9876543210",
        email = "$id@example.com",
        isActive = isActive,
        isVerified = isVerified,
        status = status,
        groomingEnabled = groomingEnabled,
        vetClinicEnabled = vetClinicEnabled
    )

    private fun makeProduct(
        id: String,
        shopId: String = "shop_1",
        isActive: Boolean = true
    ) = ProductEntity(
        id = id,
        shopId = shopId,
        categoryId = "cat_food",
        name = "Product $id",
        description = "Regression test product",
        price = 100.0,
        mrp = 120.0,
        isActive = isActive
    )

    private fun makeGroomingBooking(id: String) = GroomingBookingEntity(
        id = id,
        consumerId = "consumer_1",
        shopId = "shop_grooming",
        serviceId = "service_small_bath",
        slotId = "slot_1",
        petId = "pet_1",
        petSizeCategory = "small",
        status = "pending",
        totalPrice = 400.0
    )

    private fun makeAppointment(id: String) = AppointmentEntity(
        id = id,
        consumerId = "consumer_1",
        shopId = "shop_hospital",
        serviceId = "service_vet",
        serviceName = "Vet Consultation",
        price = 600.0,
        appointmentDate = "2026-07-01",
        appointmentTime = "10:00 AM",
        petName = "Buddy",
        status = "pending",
        doctorId = "doctor_1"
    )

    private suspend fun expectFailure(block: suspend () -> Unit): Throwable {
        try {
            block()
        } catch (throwable: Throwable) {
            return throwable
        }
        fail("Expected operation to fail, but it completed successfully.")
        throw AssertionError("Unreachable")
    }
}
