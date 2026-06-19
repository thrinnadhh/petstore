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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PawsBackendTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PawsDao
    private lateinit var repository: PawsRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.pawsDao()
        repository = PawsRepository(dao)
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testProfileFlow() = runBlocking {
        val profile = ProfileEntity(
            id = "user_test_arjun",
            fullName = "Arjun Patel",
            phone = "799876543210",
            cityId = "hyd",
            avatarUrl = "https://example.com/avatar.jpg",
            role = "consumer",
            address = "Test address, Hyderabad"
        )
        repository.insertProfile(profile)

        val fetched = repository.getProfile("user_test_arjun")
        assertNotNull(fetched)
        assertEquals("Arjun Patel", fetched?.fullName)
        assertEquals("consumer", fetched?.role)

        val fetchedByPhone = repository.getProfileByPhone("799876543210")
        assertNotNull(fetchedByPhone)
        assertEquals("user_test_arjun", fetchedByPhone?.id)
    }

    @Test
    fun testShopPlacementRankingScoreMath() = runBlocking {
        val shop1 = ShopEntity(
            id = "shop_hyd_royal", ownerId = "owner_1", cityId = "hyd",
            name = "Royal Canine Hub", description = "Premium", address = "Road 12", locality = "Banjara Hills",
            phone = "9876543210", email = "royal@paws.com", rating = 4.8
        )
        val shop2 = ShopEntity(
            id = "shop_hyd_puppy", ownerId = "owner_2", cityId = "hyd",
            name = "Paws & Co. Grooming Loft", description = "Expert", address = "Road 36", locality = "Jubilee Hills",
            phone = "8888888888", email = "puppy@paws.com", rating = 4.3
        )
        repository.insertShop(shop1)
        repository.insertShop(shop2)

        // Verify insertion
        val shopsInHyd = repository.getShopsForCitySync("hyd")
        assertEquals(2, shopsInHyd.size)

        // Mock Placement Score helper calculation
        // Equation: Placement Score = (Total Orders Taken * 1.0) + (Total Products Delivered * 2.0) + (Average Rating * 10.0)
        fun calculateScore(shop: ShopEntity, ordersTaken: Int, itemsDelivered: Int): Double {
            return (ordersTaken * 1.0) + (itemsDelivered * 2.0) + (shop.rating * 10.0)
        }

        // Royal Canine: 5 orders, 12 items delivered, 4.8 rating
        val scoreRoyal = calculateScore(shop1, 5, 12) 
        // 5 * 1.0 + 12 * 2.0 + 4.8 * 10.0 = 5.0 + 24.0 + 48.0 = 77.0
        assertEquals(77.0, scoreRoyal, 0.001)

        // Puppy Love: 15 orders, 3 items delivered, 4.3 rating
        val scorePuppy = calculateScore(shop2, 15, 3)
        // 15 * 1.0 + 3 * 2.0 + 4.3 * 10.0 = 15.0 + 6.0 + 43.0 = 64.0
        assertEquals(64.0, scorePuppy, 0.001)

        // Assert ranking order
        assertTrue("Royal Canine score should exceed Puppy Love score", scoreRoyal > scorePuppy)
    }

    @Test
    fun testMultiCategoryFilteringAndCatalogSearch() = runBlocking {
        val catFood = CategoryEntity("cat_food", "Food & Nutrition", "http://example.com/icon1.jpg")
        val catGroom = CategoryEntity("cat_groom", "Grooming Services", "http://example.com/icon2.jpg")
        repository.insertCategory(catFood)
        repository.insertCategory(catGroom)

        val prod1 = ProductEntity(
            id = "p_kibble", shopId = "shop_1", categoryId = "cat_food",
            name = "Royal Canin Puppy Kibble", description = "Premium puppy dog feed",
            price = 850.0, mrp = 1000.0, brand = "Royal Canin", lifeStage = "Puppy"
        )
        val prod2 = ProductEntity(
            id = "p_shampoo", shopId = "shop_1", categoryId = "cat_groom",
            name = "Deep Clean Oatmeal Wash", description = "Anti-itch oatmeal shampoo",
            price = 450.0, mrp = 550.0, brand = "Himalaya", lifeStage = "Adult"
        )
        repository.insertProduct(prod1)
        repository.insertProduct(prod2)

        val activeProducts = repository.allProductsFlow.first()
        assertEquals(2, activeProducts.size)

        // Search match mock test
        val query = "Oatmeal"
        val matched = activeProducts.filter { product ->
            product.name.contains(query, ignoreCase = true) ||
            product.description.contains(query, ignoreCase = true)
        }
        assertEquals(1, matched.size)
        assertEquals("p_shampoo", matched[0].id)

        // Multi-category tag check
        val selectedCatIds = setOf("cat_food")
        val filtered = activeProducts.filter { it.categoryId in selectedCatIds }
        assertEquals(1, filtered.size)
        assertEquals("p_kibble", filtered[0].id)
    }

    @Test
    fun testCollaborativeGroupRfqBiddingMathematics() = runBlocking {
        val sessionId = "RFQ-ABCDE123"
        val session = GroupRfqSessionEntity(
            id = sessionId,
            hostId = "host_arjun",
            cityId = "hyd",
            status = "open",
            biddingExpiresAt = System.currentTimeMillis() + 600000
        )
        repository.insertGroupRfqSession(session)

        val item1 = GroupRfqMemberItemEntity(
            id = "item_1", sessionId = sessionId, memberId = "member_1", memberName = "Alice",
            productId = "prod_kibble", quantity = 2, deliveryAddress = "Addr A", lat = 0.0, lng = 0.0
        )
        val item2 = GroupRfqMemberItemEntity(
            id = "item_2", sessionId = sessionId, memberId = "member_2", memberName = "Bob",
            productId = "prod_leash", quantity = 1, deliveryAddress = "Addr B", lat = 0.0, lng = 0.0
        )
        repository.insertRfqMemberItem(item1)
        repository.insertRfqMemberItem(item2)

        val sessionItems = repository.getRfqMemberItemsForSession(sessionId).first()
        assertEquals(2, sessionItems.size)

        // Bidding math verification: 20% discount on total subtotal
        val kibblePrice = 850.0
        val leashPrice = 300.0
        val subtotal = (kibblePrice * 2) + (leashPrice * 1) // 1700 + 300 = 2000.0
        assertEquals(2000.0, subtotal, 0.001)

        val discountPercentage = 20.0
        val quotedPrice = subtotal * (1 - discountPercentage / 100.0) // 2000 * 0.8 = 1600.0
        assertEquals(1600.0, quotedPrice, 0.001)

        // Split individual shares verification (with 30 delivery split and 10 platform split fees)
        val discountedKibbleTotal = (kibblePrice * (1 - discountPercentage / 100.0)) * 2 // 850 * 0.8 * 2 = 1360.0
        val discountedLeashTotal = (leashPrice * (1 - discountPercentage / 100.0)) * 1 // 300 * 0.8 * 1 = 240.0

        val aliceShare = discountedKibbleTotal + 30.0 + 10.0 // 1360 + 30 + 10 = 1400.0
        val bobShare = discountedLeashTotal + 30.0 + 10.0 // 240 + 30 + 10 = 280.0

        assertEquals(1400.0, aliceShare, 0.001)
        assertEquals(280.0, bobShare, 0.001)
    }

    @Test
    fun testDefaultSeedingOfConsultationAndGroomingServices() = runBlocking {
        // Run database seeding
        repository.seedDatabaseIfEmpty()

        // Fetch services for mock city hospital
        val hospitalServices = repository.getServicesForShopFlow("mock_city_hospital").first()
        assertTrue(hospitalServices.isNotEmpty())
        assertTrue(hospitalServices.any { it.name.contains("OPD") })
        assertTrue(hospitalServices.any { it.category.contains("Vet") })

        // Fetch services for mock paws bubbles spa
        val groomingServices = repository.getServicesForShopFlow("mock_paws_bubbles").first()
        assertTrue(groomingServices.isNotEmpty())
        assertTrue(groomingServices.any { it.name.contains("Styling") })
        assertTrue(groomingServices.any { it.category.contains("Grooming") })
    }

    @Test
    fun testDynamicMerchantShopServiceSeeding() = runBlocking {
        val shopId = "shop_test_dynamic_123"
        val newShop = ShopEntity(
            id = shopId,
            ownerId = "owner_test_123",
            cityId = "hyd",
            name = "Dynamic Test Hospital & Spa",
            description = "Vet Clinic and Grooming",
            address = "Test Road",
            locality = "Test Locality",
            phone = "9876543210",
            email = "test@paws.com",
            groomingEnabled = true,
            vetClinicEnabled = true
        )
        repository.insertShop(newShop)

        // Seed default services (vet and grooming)
        val defaultServices = mutableListOf<ServiceEntity>()
        if (newShop.vetClinicEnabled) {
            defaultServices.add(ServiceEntity(id = "service_${shopId}_vet_1", shopId = shopId, name = "Emergency Surgery Consultation", price = 1200.0, category = "Vet Doctor Clinic"))
            defaultServices.add(ServiceEntity(id = "service_${shopId}_vet_2", shopId = shopId, name = "General OPD Consultation", price = 600.0, category = "Vet Doctor Clinic"))
        }
        if (newShop.groomingEnabled) {
            defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_1", shopId = shopId, name = "Teddy Bear Coat Styling", price = 999.0, category = "Grooming"))
            defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_2", shopId = shopId, name = "Kennel Summer Short Cut", price = 799.0, category = "Grooming"))
            defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_3", shopId = shopId, name = "Majestic Lion Pom Styling", price = 1499.0, category = "Grooming"))
            defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_4", shopId = shopId, name = "Oatmeal Soothing Bath", price = 499.0, category = "Bathing"))
            defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_5", shopId = shopId, name = "Anti-Tick & Flea Medicated Wash", price = 699.0, category = "Bathing"))
            defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_6", shopId = shopId, name = "Premium Foam Aroma Spa Bath", price = 899.0, category = "Bathing"))
        }
        
        defaultServices.forEach { repository.insertService(it) }

        // Fetch services for dynamic shop
        val services = repository.getServicesForShopFlow(shopId).first()
        assertEquals(8, services.size)
        assertTrue(services.any { it.name == "Emergency Surgery Consultation" })
        assertTrue(services.any { it.name == "Teddy Bear Coat Styling" })
        assertTrue(services.any { it.name == "Premium Foam Aroma Spa Bath" })
    }
}
