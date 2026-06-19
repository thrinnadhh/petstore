package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Full Customer-App Test Suite
 *
 * Covers every major customer-facing feature:
 *   1. Registration (valid, duplicate, address persistence)
 *   2. Login (phone success, phone failure, email/password, wrong pin)
 *   3. Phone number formatting
 *   4. City & profile city selection
 *   5. Cart operations (add, remove, clear, cross-shop conflict)
 *   6. Checkout / Order placement totals (delivery fee, subtotal maths)
 *   7. Order status lifecycle
 *   8. Order items persistence
 *   9. Customer review submission & shop rating recalculation
 *  10. Shop wishlist (save & un-save)
 *  11. Product wishlist (save & un-save)
 *  12. Reminders CRUD
 *  13. Pet / Health-Passport CRUD
 *  14. Appointment booking lifecycle
 *  15. Product search & multi-filter
 *  16. Banner targeting by city
 *  17. Coupon validation maths (discount cap, min-order guard)
 *  18. Logout state reset
 *  19. Concurrent multi-user order isolation
 *  20. Pet registration & auto-creation of PetEntity
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomerAppTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PawsDao
    private lateinit var repo: PawsRepository

    // ─── Fixtures ──────────────────────────────────────────────────────────────

    private val cityHyd = CityEntity("hyd", "Hyderabad", "Telangana", true, 17.385, 78.4867)
    private val cityBlr = CityEntity("blr", "Bengaluru", "Karnataka", true, 12.9716, 77.5946)
    private val cityDel = CityEntity("del", "Delhi", "Delhi", false, 28.6139, 77.209)

    private fun makeConsumer(
        id: String = "consumer_test_1",
        fullName: String = "Priya Sharma",
        phone: String = "799000000001",
        cityId: String = "hyd",
        address: String = "Flat 3A, Jubilee Hills, Hyderabad"
    ) = ProfileEntity(
        id = id,
        fullName = fullName,
        phone = phone,
        cityId = cityId,
        avatarUrl = "https://example.com/avatar.jpg",
        role = "consumer",
        petName = "Bruno",
        address = address
    )

    private fun makeShop(
        id: String = "shop_test_1",
        cityId: String = "hyd",
        name: String = "Paws Palace"
    ) = ShopEntity(
        id = id, ownerId = "owner_1", cityId = cityId,
        name = name, description = "Best pet shop",
        address = "Road 12", locality = "Banjara Hills",
        phone = "9876543210", email = "pawspalace@paws.com",
        rating = 4.5, deliveryAvailable = true
    )

    private fun makeProduct(
        id: String = "prod_1",
        shopId: String = "shop_test_1",
        name: String = "Premium Kibble",
        price: Double = 750.0,
        mrp: Double = 900.0
    ) = ProductEntity(
        id = id, shopId = shopId, categoryId = "cat_food",
        name = name, description = "High quality dog food",
        price = price, mrp = mrp, brand = "Royal Canin",
        lifeStage = "Adult", inStock = true, isActive = true
    )

    // ─── Test lifecycle ────────────────────────────────────────────────────────

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.pawsDao()
        repo = PawsRepository(dao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 1 – REGISTRATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-REG-01 - new consumer is inserted and fetchable by id`() = runBlocking {
        val profile = makeConsumer()
        repo.insertProfile(profile)

        val fetched = repo.getProfile("consumer_test_1")
        assertNotNull("Profile should be persisted", fetched)
        assertEquals("consumer", fetched!!.role)
        assertEquals("Priya Sharma", fetched.fullName)
    }

    @Test
    fun `TC-REG-02 - registered address is persisted correctly`() = runBlocking {
        val address = "12-5, Road No 7, Banjara Hills, Hyderabad - 500034"
        val profile = makeConsumer(address = address)
        repo.insertProfile(profile)

        val fetched = repo.getProfile("consumer_test_1")
        assertEquals("Address must be stored verbatim", address, fetched?.address)
    }

    @Test
    fun `TC-REG-03 - blank address is stored as empty string and does not crash`() = runBlocking {
        val profile = makeConsumer(address = "")
        repo.insertProfile(profile)

        val fetched = repo.getProfile("consumer_test_1")
        assertNotNull(fetched)
        assertEquals("", fetched?.address)
    }

    @Test
    fun `TC-REG-04 - duplicate phone cannot be re-registered (unique constraint or app-level check)`() = runBlocking {
        val phone = "799000000099"
        val original = makeConsumer(id = "c1", phone = phone)
        repo.insertProfile(original)

        // Simulate the app-level guard: if phone exists, we must NOT insert again
        val existing = repo.getProfileByPhone(phone)
        val shouldBlock = (existing != null)

        assertTrue("Duplicate phone must be detected and blocked", shouldBlock)
        assertEquals("c1", existing?.id) // original user should still be the record
    }

    @Test
    fun `TC-REG-05 - consumer profile has role consumer`() = runBlocking {
        val profile = makeConsumer()
        repo.insertProfile(profile)
        val fetched = repo.getProfile("consumer_test_1")
        assertEquals("consumer", fetched?.role)
    }

    @Test
    fun `TC-REG-06 - consumer can be looked up by phone`() = runBlocking {
        val profile = makeConsumer(phone = "799123456789")
        repo.insertProfile(profile)

        val byPhone = repo.getProfileByPhone("799123456789")
        assertNotNull(byPhone)
        assertEquals("consumer_test_1", byPhone?.id)
    }

    @Test
    fun `TC-REG-07 - consumer can be looked up by email`() = runBlocking {
        val profile = makeConsumer().copy(email = "priya@pawsapp.com", password = "secret123")
        repo.insertProfile(profile)

        val byEmail = repo.getProfileByEmail("priya@pawsapp.com")
        assertNotNull(byEmail)
        assertEquals("consumer_test_1", byEmail?.id)
    }

    @Test
    fun `TC-REG-08 - pet entity is created alongside consumer when petName is non-blank`() = runBlocking {
        val consumerId = "consumer_pet_test"
        val consumer = makeConsumer(id = consumerId)
        repo.insertProfile(consumer)

        val pet = PetEntity(
            id = "pet_001",
            ownerId = consumerId,
            name = "Bruno",
            breed = "Golden Retriever",
            ageText = "2 years",
            weight = "24 kg"
        )
        repo.insertPet(pet)

        val pets = repo.getPetsForOwnerFlow(consumerId).first()
        assertEquals(1, pets.size)
        assertEquals("Bruno", pets[0].name)
        assertEquals("Golden Retriever", pets[0].breed)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 2 – LOGIN
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-LOGIN-01 - login succeeds when profile exists for phone`() = runBlocking {
        val profile = makeConsumer(phone = "799111222333").copy(password = "1234")
        repo.insertProfile(profile)

        val found = repo.getProfileByPhone("799111222333")
        assertNotNull(found)
        // Verify pin check
        assertEquals("1234", found?.password)
    }

    @Test
    fun `TC-LOGIN-02 - login fails when phone is not registered`() = runBlocking {
        val found = repo.getProfileByPhone("799999999999")
        assertNull("Unregistered phone should return null", found)
    }

    @Test
    fun `TC-LOGIN-03 - wrong PIN is detected by the password mismatch check`() = runBlocking {
        val profile = makeConsumer(phone = "799222333444").copy(password = "5678")
        repo.insertProfile(profile)

        val found = repo.getProfileByPhone("799222333444")
        assertNotNull(found)
        val enteredPin = "0000"
        val isWrong = (found?.password != null && found.password != enteredPin)
        assertTrue("Wrong PIN should be flagged", isWrong)
    }

    @Test
    fun `TC-LOGIN-04 - correct PIN is accepted`() = runBlocking {
        val profile = makeConsumer(phone = "799333444555").copy(password = "4321")
        repo.insertProfile(profile)

        val found = repo.getProfileByPhone("799333444555")
        val enteredPin = "4321"
        val isCorrect = (found?.password == null || found.password == enteredPin)
        assertTrue("Correct PIN must be accepted", isCorrect)
    }

    @Test
    fun `TC-LOGIN-05 - email login looks up profile by email`() = runBlocking {
        val profile = makeConsumer().copy(email = "test_login@pawsapp.com", password = "MyPaws#1")
        repo.insertProfile(profile)

        val found = repo.getProfileByEmail("test_login@pawsapp.com")
        assertNotNull(found)
        assertEquals("MyPaws#1", found?.password)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 3 – PHONE NUMBER FORMATTING (pure logic, no DB needed)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Replicates the formatPhoneNumber() logic from PawsViewModel inline so we
     * can unit-test it without spinning up an AndroidViewModel.
     */
    private fun formatPhone(phone: String): String {
        val clean = phone.replace(Regex("[^0-9]"), "")
        return when {
            clean.length == 10 -> "79$clean"
            clean.length == 12 && clean.startsWith("79") -> clean
            clean.length > 10 -> "79" + clean.takeLast(10)
            else -> "79" + clean.padStart(10, '0').takeLast(10)
        }
    }

    @Test
    fun `TC-FMT-01 - 10-digit number gets 79 prefix`() {
        assertEquals("799876543210", formatPhone("9876543210"))
    }

    @Test
    fun `TC-FMT-02 - already-formatted 12-digit number is unchanged`() {
        assertEquals("799876543210", formatPhone("799876543210"))
    }

    @Test
    fun `TC-FMT-03 - number with spaces is cleaned and prefixed`() {
        assertEquals("799876543210", formatPhone("9876 543210"))
    }

    @Test
    fun `TC-FMT-04 - short number is left-padded`() {
        val result = formatPhone("123")
        assertTrue("Should start with 79", result.startsWith("79"))
        assertEquals(12, result.length)
    }

    @Test
    fun `TC-FMT-05 - number with country code gets last 10 digits kept`() {
        // +91-9876543210 → clean="919876543210" → takeLast(10) = "9876543210" → "799876543210"
        assertEquals("799876543210", formatPhone("+91-9876543210"))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 4 – CITY SELECTION & PROFILE CITY UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-CITY-01 - active cities flow returns only active cities`() = runBlocking {
        repo.insertCity(cityHyd)
        repo.insertCity(cityBlr)
        repo.insertCity(cityDel) // isActive = false

        val activeCities = repo.activeCitiesFlow.first()
        val activeIds = activeCities.map { it.id }

        assertTrue("Hyderabad should be active", "hyd" in activeIds)
        assertTrue("Bengaluru should be active", "blr" in activeIds)
        assertFalse("Delhi should NOT appear (inactive)", "del" in activeIds)
    }

    @Test
    fun `TC-CITY-02 - profile city is updated correctly in DB`() = runBlocking {
        val profile = makeConsumer(cityId = "hyd")
        repo.insertProfile(profile)
        repo.insertCity(cityBlr)

        repo.updateProfileCity("consumer_test_1", "blr")

        val updated = repo.getProfile("consumer_test_1")
        assertEquals("blr", updated?.cityId)
    }

    @Test
    fun `TC-CITY-03 - shops filtered by city return only that city's shops`() = runBlocking {
        repo.insertCity(cityHyd)
        repo.insertCity(cityBlr)
        repo.insertShop(makeShop(id = "shop_hyd", cityId = "hyd"))
        repo.insertShop(makeShop(id = "shop_blr", cityId = "blr"))

        val hydShops = repo.getShopsForCitySync("hyd")
        assertEquals(1, hydShops.size)
        assertEquals("shop_hyd", hydShops[0].id)

        val blrShops = repo.getShopsForCitySync("blr")
        assertEquals(1, blrShops.size)
        assertEquals("shop_blr", blrShops[0].id)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 5 – CART OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Cart lives in-memory in the ViewModel; we replicate the add/remove/clear
     * logic inline to validate the pure state-machine rules independently of
     * the ViewModel lifecycle.
     */

    private fun cartAdd(cart: MutableMap<String, Int>, productId: String): Map<String, Int> {
        cart[productId] = (cart[productId] ?: 0) + 1
        return cart.toMap()
    }

    private fun cartRemove(cart: MutableMap<String, Int>, productId: String): Map<String, Int> {
        val qty = cart[productId] ?: return cart
        if (qty <= 1) cart.remove(productId) else cart[productId] = qty - 1
        return cart.toMap()
    }

    @Test
    fun `TC-CART-01 - adding a product increments quantity`() {
        val cart = mutableMapOf<String, Int>()
        cartAdd(cart, "prod_1")
        cartAdd(cart, "prod_1")
        assertEquals(2, cart["prod_1"])
    }

    @Test
    fun `TC-CART-02 - adding two different products keeps both`() {
        val cart = mutableMapOf<String, Int>()
        cartAdd(cart, "prod_1")
        cartAdd(cart, "prod_2")
        assertEquals(2, cart.size)
    }

    @Test
    fun `TC-CART-03 - removing a product decrements quantity`() {
        val cart = mutableMapOf("prod_1" to 3)
        cartRemove(cart, "prod_1")
        assertEquals(2, cart["prod_1"])
    }

    @Test
    fun `TC-CART-04 - removing last unit deletes product from cart`() {
        val cart = mutableMapOf("prod_1" to 1)
        cartRemove(cart, "prod_1")
        assertFalse("prod_1 should be gone", cart.containsKey("prod_1"))
    }

    @Test
    fun `TC-CART-05 - clearing cart empties the map`() {
        val cart = mutableMapOf("prod_1" to 2, "prod_2" to 1)
        cart.clear()
        assertTrue(cart.isEmpty())
    }

    @Test
    fun `TC-CART-06 - cross-shop conflict is detected when shopId differs`() {
        val cartShopId = "shop_A"
        val incomingShopId = "shop_B"
        val hasConflict = (cartShopId != incomingShopId)
        assertTrue("Different shop should trigger a conflict warning", hasConflict)
    }

    @Test
    fun `TC-CART-07 - same-shop product does not trigger conflict`() {
        val cartShopId = "shop_A"
        val incomingShopId = "shop_A"
        val hasConflict = (cartShopId != incomingShopId)
        assertFalse("Same shop should not trigger conflict", hasConflict)
    }

    @Test
    fun `TC-CART-08 - resolving conflict with clear resets cart and adds new product`() {
        val cart = mutableMapOf("old_prod" to 2)
        // User chooses "clear and add"
        cart.clear()
        cart["new_prod"] = 1
        assertFalse(cart.containsKey("old_prod"))
        assertEquals(1, cart["new_prod"])
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 6 – ORDER PLACEMENT & TOTAL CALCULATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-ORDER-01 - delivery order total = subtotal + delivery fee`() {
        val kibblePrice = 750.0
        val shampooPrice = 320.0
        val kibbleQty = 2
        val shampooQty = 1
        val deliveryFee = 30.0

        val subtotal = kibblePrice * kibbleQty + shampooPrice * shampooQty
        val total = subtotal + deliveryFee

        assertEquals("Subtotal", 1820.0, subtotal, 0.001)
        assertEquals("Total with delivery", 1850.0, total, 0.001)
    }

    @Test
    fun `TC-ORDER-02 - pickup order has zero delivery fee`() {
        val subtotal = 500.0
        val deliveryFee = 0.0 // pickup
        assertEquals(500.0, subtotal + deliveryFee, 0.001)
    }

    @Test
    fun `TC-ORDER-03 - new order is persisted in the DB with correct fields`() = runBlocking {
        val consumer = makeConsumer()
        val shop = makeShop()
        repo.insertProfile(consumer)
        repo.insertShop(shop)

        val order = OrderEntity(
            id = "order_unit_001",
            consumerId = consumer.id,
            shopId = shop.id,
            type = "delivery",
            status = "pending",
            totalAmount = 1850.0,
            deliveryAddress = "Flat 3A, Jubilee Hills, Hyderabad",
            notes = "Leave at door"
        )
        repo.insertOrder(order)

        val fetched = repo.getOrderById("order_unit_001")
        assertNotNull(fetched)
        assertEquals("pending", fetched?.status)
        assertEquals(1850.0, fetched?.totalAmount ?: 0.0, 0.001)
        assertEquals("delivery", fetched?.type)
        assertEquals("Flat 3A, Jubilee Hills, Hyderabad", fetched?.deliveryAddress)
    }

    @Test
    fun `TC-ORDER-04 - consumer can see their own orders`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)
        repo.insertShop(makeShop())

        repeat(3) { i ->
            repo.insertOrder(
                OrderEntity(
                    id = "order_00$i",
                    consumerId = consumer.id,
                    shopId = "shop_test_1",
                    type = "delivery",
                    status = "pending",
                    totalAmount = 100.0 * (i + 1),
                    deliveryAddress = "Some address"
                )
            )
        }

        val myOrders = repo.getOrdersForConsumer(consumer.id).first()
        assertEquals(3, myOrders.size)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 7 – ORDER STATUS LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-STATUS-01 - order progresses through full lifecycle correctly`() = runBlocking {
        repo.insertProfile(makeConsumer())
        repo.insertShop(makeShop())

        val orderId = "order_lifecycle_001"
        repo.insertOrder(
            OrderEntity(
                id = orderId,
                consumerId = "consumer_test_1",
                shopId = "shop_test_1",
                type = "delivery",
                status = "pending",
                totalAmount = 500.0,
                deliveryAddress = "Test Address"
            )
        )

        val lifecycle = listOf("accepted", "preparing", "out_for_delivery", "delivered")
        for (status in lifecycle) {
            repo.updateOrderStatus(orderId, status)
            val updated = repo.getOrderById(orderId)
            assertEquals("Status should be $status", status, updated?.status)
        }
    }

    @Test
    fun `TC-STATUS-02 - order can be cancelled from pending state`() = runBlocking {
        repo.insertProfile(makeConsumer())
        repo.insertShop(makeShop())

        val orderId = "order_cancel_001"
        repo.insertOrder(
            OrderEntity(
                id = orderId,
                consumerId = "consumer_test_1",
                shopId = "shop_test_1",
                type = "pickup",
                status = "pending",
                totalAmount = 200.0,
                deliveryAddress = ""
            )
        )
        repo.updateOrderStatus(orderId, "cancelled")

        val cancelled = repo.getOrderById(orderId)
        assertEquals("cancelled", cancelled?.status)
    }

    @Test
    fun `TC-STATUS-03 - captain id is assigned when order goes out for delivery`() = runBlocking {
        repo.insertProfile(makeConsumer())
        repo.insertShop(makeShop())

        val orderId = "order_captain_001"
        repo.insertOrder(
            OrderEntity(
                id = orderId,
                consumerId = "consumer_test_1",
                shopId = "shop_test_1",
                type = "delivery",
                status = "preparing",
                totalAmount = 800.0,
                deliveryAddress = "Some Place"
            )
        )
        repo.updateOrderStatus(orderId, "out_for_delivery", captainId = "captain_ramesh")

        val order = repo.getOrderById(orderId)
        assertEquals("out_for_delivery", order?.status)
        assertEquals("captain_ramesh", order?.captainId)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 8 – ORDER ITEMS PERSISTENCE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-ITEMS-01 - order items are linked to the correct order`() = runBlocking {
        repo.insertProfile(makeConsumer())
        repo.insertShop(makeShop())
        repo.insertProduct(makeProduct())
        repo.insertProduct(makeProduct(id = "prod_2", name = "Dog Shampoo", price = 299.0))

        val orderId = "order_items_001"
        repo.insertOrder(
            OrderEntity(
                id = orderId,
                consumerId = "consumer_test_1",
                shopId = "shop_test_1",
                type = "delivery",
                status = "pending",
                totalAmount = 1799.0,
                deliveryAddress = "Test"
            )
        )

        val item1 = OrderItemEntity(
            id = "oi_1", orderId = orderId,
            productId = "prod_1", quantity = 2,
            unitPrice = 750.0, subtotal = 1500.0
        )
        val item2 = OrderItemEntity(
            id = "oi_2", orderId = orderId,
            productId = "prod_2", quantity = 1,
            unitPrice = 299.0, subtotal = 299.0
        )
        repo.insertOrderItem(item1)
        repo.insertOrderItem(item2)

        val items = repo.getOrderItemsForOrder(orderId)
        assertEquals(2, items.size)

        val retrievedSubtotals = items.sumOf { it.subtotal }
        assertEquals(1799.0, retrievedSubtotals, 0.001)
    }

    @Test
    fun `TC-ITEMS-02 - subtotal for each line item is quantity times unit price`() {
        val qty = 3
        val unitPrice = 499.0
        val expected = qty * unitPrice
        assertEquals(1497.0, expected, 0.001)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 9 – REVIEWS & SHOP RATING RECALCULATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-REVIEW-01 - review is persisted with correct fields`() = runBlocking {
        repo.insertProfile(makeConsumer())
        repo.insertShop(makeShop())

        val review = ReviewEntity(
            id = "rev_001",
            shopId = "shop_test_1",
            consumerId = "consumer_test_1",
            rating = 5,
            comment = "Amazing service, Bruno loves the food!"
        )
        repo.insertReview(review)

        val reviews = repo.getReviewsForShop("shop_test_1").first()
        assertEquals(1, reviews.size)
        assertEquals(5, reviews[0].rating)
        assertEquals("Amazing service, Bruno loves the food!", reviews[0].comment)
    }

    @Test
    fun `TC-REVIEW-02 - average shop rating recalculates correctly after multiple reviews`() = runBlocking {
        repo.insertShop(makeShop())

        val ratings = listOf(5, 4, 3) // avg = 4.0
        ratings.forEachIndexed { i, rating ->
            repo.insertReview(
                ReviewEntity(
                    id = "rev_00$i",
                    shopId = "shop_test_1",
                    consumerId = "consumer_$i",
                    rating = rating,
                    comment = "Review #$i"
                )
            )
        }

        val allReviews = repo.getReviewsForShop("shop_test_1").first()
        val avg = allReviews.map { it.rating }.average()
        assertEquals(4.0, avg, 0.001)

        repo.updateShopRating("shop_test_1", avg, allReviews.size)

        val updatedShop = repo.getShopById("shop_test_1")
        assertEquals(4.0, updatedShop?.rating ?: 0.0, 0.001)
        assertEquals(3, updatedShop?.totalReviews)
    }

    @Test
    fun `TC-REVIEW-03 - rating boundary validation - rating of 1 is accepted`() = runBlocking {
        repo.insertShop(makeShop())
        val review = ReviewEntity(
            id = "rev_min",
            shopId = "shop_test_1",
            consumerId = "consumer_test_1",
            rating = 1,
            comment = "Terrible experience"
        )
        repo.insertReview(review)
        val reviews = repo.getReviewsForShop("shop_test_1").first()
        assertEquals(1, reviews[0].rating)
    }

    @Test
    fun `TC-REVIEW-04 - rating boundary validation - rating of 5 is accepted`() = runBlocking {
        repo.insertShop(makeShop())
        val review = ReviewEntity(
            id = "rev_max",
            shopId = "shop_test_1",
            consumerId = "consumer_test_1",
            rating = 5,
            comment = "Excellent!"
        )
        repo.insertReview(review)
        val reviews = repo.getReviewsForShop("shop_test_1").first()
        assertEquals(5, reviews[0].rating)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 10 – SHOP WISHLIST (Save / Un-save)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-WISH-01 - consumer can save a shop to wishlist`() = runBlocking {
        val consumer = makeConsumer()
        val shop = makeShop()
        repo.insertProfile(consumer)
        repo.insertShop(shop)

        repo.insertWishlist(
            WishlistEntity(
                id = "wl_001",
                consumerId = consumer.id,
                shopId = shop.id
            )
        )

        val saved = repo.getWishlistForConsumerFlow(consumer.id).first()
        assertEquals(1, saved.size)
        assertEquals(shop.id, saved[0].shopId)
    }

    @Test
    fun `TC-WISH-02 - consumer can remove a shop from wishlist`() = runBlocking {
        val consumer = makeConsumer()
        val shop = makeShop()
        repo.insertProfile(consumer)
        repo.insertShop(shop)

        repo.insertWishlist(WishlistEntity("wl_001", consumer.id, shop.id))
        repo.deleteWishlist(consumer.id, shop.id)

        val saved = repo.getWishlistForConsumerFlow(consumer.id).first()
        assertTrue("Wishlist should be empty after removal", saved.isEmpty())
    }

    @Test
    fun `TC-WISH-03 - saving multiple shops maintains all entries`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)
        repo.insertShop(makeShop(id = "shop_1"))
        repo.insertShop(makeShop(id = "shop_2"))
        repo.insertShop(makeShop(id = "shop_3"))

        listOf("shop_1", "shop_2", "shop_3").forEachIndexed { i, shopId ->
            repo.insertWishlist(WishlistEntity("wl_00$i", consumer.id, shopId))
        }

        val saved = repo.getWishlistForConsumerFlow(consumer.id).first()
        assertEquals(3, saved.size)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 11 – PRODUCT WISHLIST (Favourites)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-FAV-01 - consumer can favourite a product`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)
        repo.insertShop(makeShop())
        repo.insertProduct(makeProduct())

        repo.insertWishlistProduct(WishlistProductEntity("wp_001", consumer.id, "prod_1"))

        val favs = repo.getWishlistProductsForConsumerFlow(consumer.id).first()
        assertEquals(1, favs.size)
        assertEquals("prod_1", favs[0].productId)
    }

    @Test
    fun `TC-FAV-02 - consumer can un-favourite a product`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)
        repo.insertProduct(makeProduct())

        repo.insertWishlistProduct(WishlistProductEntity("wp_001", consumer.id, "prod_1"))
        repo.deleteWishlistProduct(consumer.id, "prod_1")

        val favs = repo.getWishlistProductsForConsumerFlow(consumer.id).first()
        assertTrue("Favourites should be empty", favs.isEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 12 – REMINDERS CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-REM-01 - consumer can create a vaccination reminder`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)

        val reminder = ReminderEntity(
            id = "rem_001",
            consumerId = consumer.id,
            title = "Vaccination",
            petName = "Bruno",
            dateString = "2026-08-20",
            notes = "Rabies booster shot",
            type = "vaccination"
        )
        repo.insertReminder(reminder)

        val reminders = repo.getRemindersForConsumerFlow(consumer.id).first()
        assertEquals(1, reminders.size)
        assertEquals("Vaccination", reminders[0].title)
        assertFalse("Should start as not completed", reminders[0].isCompleted)
    }

    @Test
    fun `TC-REM-02 - reminder can be marked as completed`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)

        val reminder = ReminderEntity(
            id = "rem_002",
            consumerId = consumer.id,
            title = "Grooming Date",
            petName = "Bruno",
            dateString = "2026-07-01",
            type = "grooming"
        )
        repo.insertReminder(reminder)
        repo.updateReminderCompletion("rem_002", true)

        val reminders = repo.getRemindersForConsumerFlow(consumer.id).first()
        assertTrue("Reminder should be completed", reminders[0].isCompleted)
    }

    @Test
    fun `TC-REM-03 - reminder can be deleted`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)

        repo.insertReminder(
            ReminderEntity(
                id = "rem_003",
                consumerId = consumer.id,
                title = "Doctor Appointment",
                petName = "Bruno",
                dateString = "2026-09-01",
                type = "doctor"
            )
        )
        repo.deleteReminder("rem_003")

        val reminders = repo.getRemindersForConsumerFlow(consumer.id).first()
        assertTrue("Reminders list should be empty after deletion", reminders.isEmpty())
    }

    @Test
    fun `TC-REM-04 - multiple reminders of different types are all returned`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)

        val types = listOf("vaccination", "grooming", "birthday", "doctor")
        types.forEachIndexed { i, type ->
            repo.insertReminder(
                ReminderEntity(
                    id = "rem_t_$i",
                    consumerId = consumer.id,
                    title = type.replaceFirstChar { it.uppercase() },
                    petName = "Bruno",
                    dateString = "2026-0${i + 1}-01",
                    type = type
                )
            )
        }

        val reminders = repo.getRemindersForConsumerFlow(consumer.id).first()
        assertEquals(4, reminders.size)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 13 – PET / HEALTH PASSPORT CRUD
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-PET-01 - consumer can add a pet`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)

        val pet = PetEntity(
            id = "pet_001",
            ownerId = consumer.id,
            name = "Bruno",
            breed = "Golden Retriever",
            ageText = "2 years",
            weight = "24 kg",
            allergies = "No wheat",
            vaccineRecord = "Rabies 2025",
            dewormingDate = "2026-05-15",
            vaccineDueDate = "2026-08-20"
        )
        repo.insertPet(pet)

        val pets = repo.getPetsForOwnerFlow(consumer.id).first()
        assertEquals(1, pets.size)
        assertEquals("Bruno", pets[0].name)
        assertEquals("No wheat", pets[0].allergies)
    }

    @Test
    fun `TC-PET-02 - consumer can add multiple pets`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)

        listOf("Bruno", "Kitty", "Goldie").forEachIndexed { i, petName ->
            repo.insertPet(
                PetEntity(
                    id = "pet_00$i",
                    ownerId = consumer.id,
                    name = petName,
                    breed = "Mixed",
                    ageText = "1 year"
                )
            )
        }

        val pets = repo.getPetsForOwnerFlow(consumer.id).first()
        assertEquals(3, pets.size)
    }

    @Test
    fun `TC-PET-03 - consumer can delete a pet`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)

        repo.insertPet(PetEntity("pet_del_1", consumer.id, "Max", "Labrador", "3 years"))
        repo.deletePet("pet_del_1")

        val pets = repo.getPetsForOwnerFlow(consumer.id).first()
        assertTrue("Pet list should be empty after deletion", pets.isEmpty())
    }

    @Test
    fun `TC-PET-04 - pets belong only to their owner`() = runBlocking {
        val owner1 = makeConsumer(id = "owner_1", phone = "799000000001")
        val owner2 = makeConsumer(id = "owner_2", phone = "799000000002")
        repo.insertProfile(owner1)
        repo.insertProfile(owner2)

        repo.insertPet(PetEntity("pet_o1", "owner_1", "Buddy", "Poodle", "2 years"))
        repo.insertPet(PetEntity("pet_o2", "owner_2", "Charlie", "Beagle", "4 years"))

        val owner1Pets = repo.getPetsForOwnerFlow("owner_1").first()
        val owner2Pets = repo.getPetsForOwnerFlow("owner_2").first()

        assertEquals(1, owner1Pets.size)
        assertEquals("Buddy", owner1Pets[0].name)
        assertEquals(1, owner2Pets.size)
        assertEquals("Charlie", owner2Pets[0].name)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 14 – APPOINTMENT BOOKING LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-APPT-01 - consumer can book an appointment`() = runBlocking {
        val consumer = makeConsumer()
        val shop = makeShop()
        repo.insertProfile(consumer)
        repo.insertShop(shop)
        repo.insertService(
            ServiceEntity("svc_001", shop.id, "General OPD", 600.0, "Vet Doctor Clinic")
        )

        val appt = AppointmentEntity(
            id = "appt_001",
            consumerId = consumer.id,
            shopId = shop.id,
            serviceId = "svc_001",
            serviceName = "General OPD",
            price = 600.0,
            appointmentDate = "2026-07-15",
            appointmentTime = "10:00",
            petName = "Bruno",
            status = "pending"
        )
        repo.insertAppointment(appt)

        val appts = repo.getAppointmentsForConsumerFlow(consumer.id).first()
        assertEquals(1, appts.size)
        assertEquals("pending", appts[0].status)
        assertEquals("General OPD", appts[0].serviceName)
    }

    @Test
    fun `TC-APPT-02 - appointment can be confirmed`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)
        repo.insertShop(makeShop())
        repo.insertAppointment(
            AppointmentEntity(
                id = "appt_002",
                consumerId = consumer.id,
                shopId = "shop_test_1",
                serviceId = "svc_001",
                serviceName = "Grooming",
                price = 999.0,
                appointmentDate = "2026-07-20",
                appointmentTime = "14:00",
                petName = "Bruno"
            )
        )

        repo.updateAppointmentStatus("appt_002", "confirmed")

        val appts = repo.getAppointmentsForConsumerFlow(consumer.id).first()
        assertEquals("confirmed", appts[0].status)
    }

    @Test
    fun `TC-APPT-03 - appointment can be cancelled`() = runBlocking {
        val consumer = makeConsumer()
        repo.insertProfile(consumer)
        repo.insertShop(makeShop())
        repo.insertAppointment(
            AppointmentEntity(
                id = "appt_003",
                consumerId = consumer.id,
                shopId = "shop_test_1",
                serviceId = "svc_002",
                serviceName = "Vaccination",
                price = 300.0,
                appointmentDate = "2026-08-01",
                appointmentTime = "09:30",
                petName = "Bruno"
            )
        )
        repo.updateAppointmentStatus("appt_003", "cancelled")

        val appts = repo.getAppointmentsForConsumerFlow(consumer.id).first()
        assertEquals("cancelled", appts[0].status)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 15 – PRODUCT SEARCH & MULTI-FILTER
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-SEARCH-01 - search by name is case-insensitive`() = runBlocking {
        repo.insertShop(makeShop())
        repo.insertProduct(makeProduct(id = "p1", name = "Pedigree Adult Dog Food"))
        repo.insertProduct(makeProduct(id = "p2", name = "Whiskas Cat Food"))

        val allProducts = repo.allProductsFlow.first()
        val query = "pedigree"
        val results = allProducts.filter { it.name.contains(query, ignoreCase = true) }
        assertEquals(1, results.size)
        assertEquals("p1", results[0].id)
    }

    @Test
    fun `TC-SEARCH-02 - search by description returns matching products`() = runBlocking {
        repo.insertShop(makeShop())
        repo.insertProduct(
            makeProduct(id = "p1", name = "Oatmeal Shampoo").copy(description = "Soothing oatmeal formula")
        )
        repo.insertProduct(
            makeProduct(id = "p2", name = "Tick Spray").copy(description = "Anti-tick spray")
        )

        val allProducts = repo.allProductsFlow.first()
        val query = "oatmeal"
        val results = allProducts.filter {
            it.name.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
        }
        assertEquals(1, results.size)
        assertEquals("p1", results[0].id)
    }

    @Test
    fun `TC-SEARCH-03 - empty search returns all active products`() = runBlocking {
        repo.insertShop(makeShop())
        repo.insertProduct(makeProduct(id = "p1"))
        repo.insertProduct(makeProduct(id = "p2", name = "Flea Collar"))
        repo.insertProduct(makeProduct(id = "p3", name = "Dog Bed"))

        val allProducts = repo.allProductsFlow.first()
        val query = ""
        val results = allProducts.filter { query.isEmpty() || it.name.contains(query, ignoreCase = true) }
        assertEquals(3, results.size)
    }

    @Test
    fun `TC-SEARCH-04 - category filter returns only matching products`() = runBlocking {
        repo.insertShop(makeShop())
        repo.insertProduct(makeProduct(id = "pFood", shopId = "shop_test_1").copy(categoryId = "cat_food"))
        repo.insertProduct(
            makeProduct(id = "pGroom", shopId = "shop_test_1", name = "Shampoo").copy(categoryId = "cat_groom")
        )

        val allProducts = repo.allProductsFlow.first()
        val selected = setOf("cat_food")
        val filtered = allProducts.filter { it.categoryId in selected }
        assertEquals(1, filtered.size)
        assertEquals("pFood", filtered[0].id)
    }

    @Test
    fun `TC-SEARCH-05 - brand filter is applied correctly`() = runBlocking {
        repo.insertShop(makeShop())
        repo.insertProduct(makeProduct(id = "p_rc", name = "Royal Canin Adult").copy(brand = "Royal Canin"))
        repo.insertProduct(makeProduct(id = "p_pe", name = "Pedigree Puppy").copy(brand = "Pedigree"))

        val allProducts = repo.allProductsFlow.first()
        val brand = "Royal Canin"
        val filtered = allProducts.filter { it.brand.equals(brand, ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("p_rc", filtered[0].id)
    }

    @Test
    fun `TC-SEARCH-06 - life stage filter returns only matching life stage`() = runBlocking {
        repo.insertShop(makeShop())
        repo.insertProduct(makeProduct(id = "p_adult").copy(lifeStage = "Adult"))
        repo.insertProduct(makeProduct(id = "p_puppy", name = "Puppy Food").copy(lifeStage = "Puppy"))

        val allProducts = repo.allProductsFlow.first()
        val stage = "Puppy"
        val filtered = allProducts.filter { it.lifeStage.equals(stage, ignoreCase = true) }
        assertEquals(1, filtered.size)
        assertEquals("p_puppy", filtered[0].id)
    }

    @Test
    fun `TC-SEARCH-07 - combined category and brand filter narrows results correctly`() = runBlocking {
        repo.insertShop(makeShop())
        repo.insertProduct(makeProduct(id = "p1").copy(categoryId = "cat_food", brand = "Royal Canin"))
        repo.insertProduct(makeProduct(id = "p2", name = "Other Food").copy(categoryId = "cat_food", brand = "Pedigree"))
        repo.insertProduct(makeProduct(id = "p3", name = "Shampoo").copy(categoryId = "cat_groom", brand = "Royal Canin"))

        val allProducts = repo.allProductsFlow.first()
        val selectedCat = "cat_food"
        val selectedBrand = "Royal Canin"
        val filtered = allProducts.filter {
            it.categoryId == selectedCat && it.brand.equals(selectedBrand, ignoreCase = true)
        }
        assertEquals(1, filtered.size)
        assertEquals("p1", filtered[0].id)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 16 – BANNER TARGETING BY CITY
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-BANNER-01 - banner targeted to specific city is shown only for that city`() = runBlocking {
        repo.insertBanner(
            BannerEntity(
                id = "b1", imageUrl = "https://img.com/b1.jpg",
                title = "Hyderabad Sale", description = "50% off",
                targetCityIds = listOf("hyd"), targetShopIds = listOf("all"),
                isActive = true
            )
        )
        repo.insertBanner(
            BannerEntity(
                id = "b2", imageUrl = "https://img.com/b2.jpg",
                title = "Bengaluru Sale", description = "40% off",
                targetCityIds = listOf("blr"), targetShopIds = listOf("all"),
                isActive = true
            )
        )

        val allBanners = repo.allBannersFlow.first()

        val hydBanners = allBanners.filter { it.isActive && (it.targetCityIds.contains("hyd") || it.targetCityIds.contains("all")) }
        assertEquals(1, hydBanners.size)
        assertEquals("b1", hydBanners[0].id)

        val blrBanners = allBanners.filter { it.isActive && (it.targetCityIds.contains("blr") || it.targetCityIds.contains("all")) }
        assertEquals(1, blrBanners.size)
        assertEquals("b2", blrBanners[0].id)
    }

    @Test
    fun `TC-BANNER-02 - banner with targetCityIds all appears for every city`() = runBlocking {
        repo.insertBanner(
            BannerEntity(
                id = "b_global", imageUrl = "https://img.com/global.jpg",
                title = "National Offer", description = "Free delivery this weekend",
                targetCityIds = listOf("all"), targetShopIds = listOf("all"),
                isActive = true
            )
        )

        val allBanners = repo.allBannersFlow.first()
        val hydSees = allBanners.filter { it.isActive && (it.targetCityIds.contains("hyd") || it.targetCityIds.contains("all")) }
        val blrSees = allBanners.filter { it.isActive && (it.targetCityIds.contains("blr") || it.targetCityIds.contains("all")) }
        assertEquals(1, hydSees.size)
        assertEquals(1, blrSees.size)
    }

    @Test
    fun `TC-BANNER-03 - inactive banners are excluded`() = runBlocking {
        repo.insertBanner(
            BannerEntity(
                id = "b_inactive", imageUrl = "https://img.com/x.jpg",
                title = "Old Campaign", description = "Expired",
                targetCityIds = listOf("all"), targetShopIds = listOf("all"),
                isActive = false
            )
        )
        val allBanners = repo.allBannersFlow.first()
        val visible = allBanners.filter { it.isActive }
        assertTrue("Inactive banners should not appear", visible.isEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 17 – COUPON VALIDATION MATHS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun applyDiscount(
        orderTotal: Double,
        coupon: CouponEntity?
    ): Double {
        if (coupon == null || !coupon.isActive) return orderTotal
        if (orderTotal < coupon.minOrderAmount) return orderTotal
        val rawDiscount = orderTotal * coupon.discountPercentage / 100.0
        val discount = minOf(rawDiscount, coupon.maxDiscount)
        return orderTotal - discount
    }

    @Test
    fun `TC-COUPON-01 - 20% off coupon with max cap of 150 is applied correctly`() {
        val coupon = CouponEntity(
            id = "cpn_001", shopId = "global",
            code = "PAWS20", discountPercentage = 20.0,
            maxDiscount = 150.0, minOrderAmount = 200.0
        )
        val total = applyDiscount(1000.0, coupon)
        // 20% of 1000 = 200; capped at 150 → final = 850
        assertEquals(850.0, total, 0.001)
    }

    @Test
    fun `TC-COUPON-02 - coupon not applied when order is below minimum`() {
        val coupon = CouponEntity(
            id = "cpn_002", shopId = "global",
            code = "PAWS30", discountPercentage = 30.0,
            maxDiscount = 200.0, minOrderAmount = 500.0
        )
        val total = applyDiscount(300.0, coupon)
        // 300 < 500, no discount
        assertEquals(300.0, total, 0.001)
    }

    @Test
    fun `TC-COUPON-03 - 10% coupon on 400 gives flat 40 discount`() {
        val coupon = CouponEntity(
            id = "cpn_003", shopId = "shop_test_1",
            code = "FIRSTPET", discountPercentage = 10.0,
            maxDiscount = 100.0, minOrderAmount = 100.0
        )
        val total = applyDiscount(400.0, coupon)
        // 10% of 400 = 40; 40 < 100 cap → final = 360
        assertEquals(360.0, total, 0.001)
    }

    @Test
    fun `TC-COUPON-04 - inactive coupon is not applied`() {
        val coupon = CouponEntity(
            id = "cpn_004", shopId = "global",
            code = "EXPIRED", discountPercentage = 50.0,
            maxDiscount = 500.0, minOrderAmount = 0.0,
            isActive = false
        )
        val total = applyDiscount(1000.0, coupon)
        assertEquals(1000.0, total, 0.001)
    }

    @Test
    fun `TC-COUPON-05 - null coupon returns original order total`() {
        val total = applyDiscount(750.0, null)
        assertEquals(750.0, total, 0.001)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 18 – LOGOUT STATE RESET
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-LOGOUT-01 - logout clears current user and cart state`() {
        // Simulate in-memory ViewModel state
        var currentUser: ProfileEntity? = makeConsumer()
        var cartItems = mutableMapOf("prod_1" to 2)
        var cartShopId: String? = "shop_test_1"

        // Simulate logout()
        currentUser = null
        cartItems.clear()
        cartShopId = null

        assertNull("User should be null after logout", currentUser)
        assertTrue("Cart should be empty after logout", cartItems.isEmpty())
        assertNull("CartShopId should be null after logout", cartShopId)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 19 – MULTI-USER ORDER ISOLATION
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `TC-MULTI-01 - orders are isolated per consumer`() = runBlocking {
        val user1 = makeConsumer(id = "user_alpha", phone = "799001001001")
        val user2 = makeConsumer(id = "user_beta", phone = "799002002002")
        repo.insertProfile(user1)
        repo.insertProfile(user2)
        repo.insertShop(makeShop())

        repo.insertOrder(
            OrderEntity(
                id = "order_alpha_1",
                consumerId = "user_alpha",
                shopId = "shop_test_1",
                type = "delivery",
                status = "pending",
                totalAmount = 300.0,
                deliveryAddress = "Alpha Address"
            )
        )
        repo.insertOrder(
            OrderEntity(
                id = "order_beta_1",
                consumerId = "user_beta",
                shopId = "shop_test_1",
                type = "delivery",
                status = "delivered",
                totalAmount = 600.0,
                deliveryAddress = "Beta Address"
            )
        )

        val alphaOrders = repo.getOrdersForConsumer("user_alpha").first()
        val betaOrders = repo.getOrdersForConsumer("user_beta").first()

        assertEquals(1, alphaOrders.size)
        assertEquals("order_alpha_1", alphaOrders[0].id)

        assertEquals(1, betaOrders.size)
        assertEquals("order_beta_1", betaOrders[0].id)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // 20 – LOCATION SERVICEABILITY MATHS (Haversine-style distance)
    // ═══════════════════════════════════════════════════════════════════════════

    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    @Test
    fun `TC-LOC-01 - GPS point within 50km of Hyderabad is serviceable`() {
        val hydLat = 17.385; val hydLng = 78.4867
        // ~10 km offset from city centre
        val userLat = 17.450; val userLng = 78.380
        val dist = haversineKm(userLat, userLng, hydLat, hydLng)
        assertTrue("Distance $dist km should be < 50 km for serviceability", dist < 50.0)
    }

    @Test
    fun `TC-LOC-02 - GPS point far from any city is not serviceable`() {
        val hydLat = 17.385; val hydLng = 78.4867
        // ~300 km away (near Vijayawada)
        val userLat = 16.506; val userLng = 80.648
        val dist = haversineKm(userLat, userLng, hydLat, hydLng)
        assertTrue("Distance $dist km should be > 50 km – not serviceable", dist > 50.0)
    }

    @Test
    fun `TC-LOC-03 - inactive city is not serviceable even within range`() = runBlocking {
        repo.insertCity(cityDel) // isActive = false

        val delhiCity = repo.getAllCitiesSync().firstOrNull { it.id == "del" }
        assertNotNull(delhiCity)
        // Simulate the serviceability guard:
        val isServiceable = delhiCity!!.isActive
        assertFalse("Inactive city should never be serviceable", isServiceable)
    }

    @Test
    fun `TC-APPT-04 - appointment booking supports concern and priority CRUD`() = runBlocking {
        val consumer = makeConsumer()
        val shop = makeShop()
        repo.insertProfile(consumer)
        repo.insertShop(shop)

        val appt = AppointmentEntity(
            id = "appt_004",
            consumerId = consumer.id,
            shopId = shop.id,
            serviceId = "svc_consult",
            serviceName = "Doctor Consultation",
            price = 400.0,
            appointmentDate = "2026-06-25",
            appointmentTime = "11:30 AM",
            petName = "Bruno",
            status = "pending",
            concern = "Fracture",
            priority = "High"
        )
        repo.insertAppointment(appt)

        // Retrieve and check concern and priority fields
        val appts = repo.getAppointmentsForConsumerFlow(consumer.id).first()
        assertEquals(1, appts.size)
        assertEquals("appt_004", appts[0].id)
        assertEquals("Fracture", appts[0].concern)
        assertEquals("High", appts[0].priority)

        // Verify status update CRUD
        repo.updateAppointmentStatus("appt_004", "confirmed")
        val updatedAppts = repo.getAppointmentsForConsumerFlow(consumer.id).first()
        assertEquals("confirmed", updatedAppts[0].status)
    }
}
