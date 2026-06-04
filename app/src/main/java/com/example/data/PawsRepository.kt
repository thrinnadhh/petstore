package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PawsRepository(private val pawsDao: PawsDao) {

    // Profiles
    suspend fun getProfile(id: String): ProfileEntity? = pawsDao.getProfile(id)
    suspend fun getProfileByPhone(phone: String): ProfileEntity? = pawsDao.getProfileByPhone(phone)
    suspend fun getProfileByEmail(email: String): ProfileEntity? = pawsDao.getProfileByEmail(email)
    suspend fun insertProfile(profile: ProfileEntity) = pawsDao.insertProfile(profile)
    suspend fun updateProfileCity(id: String, cityId: String) = pawsDao.updateProfileCity(id, cityId)

    // Cities
    val activeCitiesFlow: Flow<List<CityEntity>> = pawsDao.getActiveCitiesFlow()
    suspend fun getAllCitiesSync(): List<CityEntity> = pawsDao.getAllCitiesSync()
    suspend fun insertCity(city: CityEntity) = pawsDao.insertCity(city)

    // Shops
    fun getShopsForCity(cityId: String): Flow<List<ShopEntity>> = pawsDao.getShopsForCity(cityId)
    suspend fun getShopsForCitySync(cityId: String): List<ShopEntity> = pawsDao.getShopsForCitySync(cityId)
    val allShopsFlow: Flow<List<ShopEntity>> = pawsDao.getAllShopsFlow()
    suspend fun getShopById(id: String): ShopEntity? = pawsDao.getShopById(id)
    fun getShopByIdFlow(id: String): Flow<ShopEntity?> = pawsDao.getShopByIdFlow(id)
    suspend fun getShopByOwnerId(ownerId: String): ShopEntity? = pawsDao.getShopByOwnerId(ownerId)
    fun getShopByOwnerIdFlow(ownerId: String): Flow<ShopEntity?> = pawsDao.getShopByOwnerIdFlow(ownerId)
    suspend fun insertShop(shop: ShopEntity) = pawsDao.insertShop(shop)
    suspend fun updateShopStatus(id: String, isOpen: Boolean) = pawsDao.updateShopStatus(id, isOpen)
    suspend fun updateShopRating(id: String, rating: Double, totalReviews: Int) = pawsDao.updateShopRating(id, rating, totalReviews)
    suspend fun updateShopApprovalStatus(id: String, status: String) = pawsDao.updateShopApprovalStatus(id, status)

    // Categories
    val allCategoriesFlow: Flow<List<CategoryEntity>> = pawsDao.getAllCategoriesFlow()
    suspend fun insertCategory(category: CategoryEntity) = pawsDao.insertCategory(category)

    // Products
    val allProductsFlow: Flow<List<ProductEntity>> = pawsDao.getAllProductsFlow()
    fun getProductsForShop(shopId: String): Flow<List<ProductEntity>> = pawsDao.getProductsForShop(shopId)
    fun getAllProductsForShopUnfiltered(shopId: String): Flow<List<ProductEntity>> = pawsDao.getAllProductsForShopUnfiltered(shopId)
    suspend fun getProductById(id: String): ProductEntity? = pawsDao.getProductById(id)
    suspend fun insertProduct(product: ProductEntity) = pawsDao.insertProduct(product)
    suspend fun deleteProductById(id: String) = pawsDao.deleteProductById(id)
    suspend fun updateProductStock(id: String, stockCount: Int) = pawsDao.updateProductStock(id, stockCount)

    // Orders
    val allOrdersFlow: Flow<List<OrderEntity>> = pawsDao.getAllOrdersFlow()
    val allOrderItemsFlow: Flow<List<OrderItemEntity>> = pawsDao.getAllOrderItemsFlow()
    fun getOrdersForConsumer(consumerId: String): Flow<List<OrderEntity>> = pawsDao.getOrdersForConsumer(consumerId)
    fun getOrdersForShop(shopId: String): Flow<List<OrderEntity>> = pawsDao.getOrdersForShop(shopId)
    suspend fun getOrderById(id: String): OrderEntity? = pawsDao.getOrderById(id)
    fun getOrderByIdFlow(id: String): Flow<OrderEntity?> = pawsDao.getOrderByIdFlow(id)
    suspend fun insertOrder(order: OrderEntity) {
        pawsDao.insertOrder(order)
        if (!ProductionConfig.IS_DEMO_MODE) {
            SupabaseManager.insertOrderToCloud(
                orderId = order.id,
                consumerId = order.consumerId,
                shopId = order.shopId,
                type = order.type,
                status = order.status,
                totalAmount = order.totalAmount,
                deliveryAddress = order.deliveryAddress,
                notes = order.notes
            )
        }
    }
    suspend fun updateOrderStatus(id: String, status: String, captainId: String? = null) {
        pawsDao.updateOrderStatus(id, status, captainId)
        if (!ProductionConfig.IS_DEMO_MODE) {
            SupabaseManager.updateOrderStatusInCloud(id, status)
        }
    }

    // Order Items
    suspend fun getOrderItemsForOrder(orderId: String): List<OrderItemEntity> = pawsDao.getOrderItemsForOrder(orderId)
    fun getOrderItemsForOrderFlow(orderId: String): Flow<List<OrderItemEntity>> = pawsDao.getOrderItemsForOrderFlow(orderId)
    suspend fun insertOrderItem(item: OrderItemEntity) {
        pawsDao.insertOrderItem(item)
        if (!ProductionConfig.IS_DEMO_MODE) {
            SupabaseManager.insertOrderItemToCloud(
                itemId = item.id,
                orderId = item.orderId,
                productId = item.productId,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                subtotal = item.subtotal
            )
        }
    }
    suspend fun insertOrderItems(items: List<OrderItemEntity>) {
        pawsDao.insertOrderItems(items)
        if (!ProductionConfig.IS_DEMO_MODE) {
            items.forEach { item ->
                SupabaseManager.insertOrderItemToCloud(
                    itemId = item.id,
                    orderId = item.orderId,
                    productId = item.productId,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    subtotal = item.subtotal
                )
            }
        }
    }

    // Reviews
    fun getReviewsForShop(shopId: String): Flow<List<ReviewEntity>> = pawsDao.getReviewsForShop(shopId)
    suspend fun insertReview(review: ReviewEntity) = pawsDao.insertReview(review)

    // Wishlist
    fun getWishlistForConsumerFlow(consumerId: String): Flow<List<WishlistEntity>> = pawsDao.getWishlistForConsumerFlow(consumerId)
    suspend fun insertWishlist(wishlist: WishlistEntity) = pawsDao.insertWishlist(wishlist)
    suspend fun deleteWishlist(consumerId: String, shopId: String) = pawsDao.deleteWishlist(consumerId, shopId)

    // Banners
    val allBannersFlow: Flow<List<BannerEntity>> = pawsDao.getAllBannersFlow()
    suspend fun insertBanner(banner: BannerEntity) = pawsDao.insertBanner(banner)
    suspend fun deleteBanner(bannerId: String) = pawsDao.deleteBanner(bannerId)

    // Chats
    fun getMessagesForConversationFlow(shopId: String): Flow<List<ChatMessageEntity>> = pawsDao.getMessagesForConversationFlow(shopId)
    fun getMessagesForUserFlow(userId: String): Flow<List<ChatMessageEntity>> = pawsDao.getMessagesForUserFlow(userId)
    suspend fun insertChatMessage(message: ChatMessageEntity) = pawsDao.insertChatMessage(message)
    suspend fun markMessagesAsRead(shopId: String, userId: String) = pawsDao.markMessagesAsRead(shopId, userId)

    // Product Wishlist
    fun getWishlistProductsForConsumerFlow(consumerId: String): Flow<List<WishlistProductEntity>> = pawsDao.getWishlistProductsForConsumerFlow(consumerId)
    suspend fun insertWishlistProduct(wishlistProduct: WishlistProductEntity) = pawsDao.insertWishlistProduct(wishlistProduct)
    suspend fun deleteWishlistProduct(consumerId: String, productId: String) = pawsDao.deleteWishlistProduct(consumerId, productId)

    // Services
    fun getServicesForShopFlow(shopId: String): Flow<List<ServiceEntity>> = pawsDao.getServicesForShopFlow(shopId)
    suspend fun insertService(service: ServiceEntity) = pawsDao.insertService(service)
    suspend fun deleteService(serviceId: String) = pawsDao.deleteService(serviceId)

    // Appointments
    fun getAppointmentsForConsumerFlow(consumerId: String): Flow<List<AppointmentEntity>> = pawsDao.getAppointmentsForConsumerFlow(consumerId)
    fun getAppointmentsForShopFlow(shopId: String): Flow<List<AppointmentEntity>> = pawsDao.getAppointmentsForShopFlow(shopId)
    suspend fun insertAppointment(appointment: AppointmentEntity) = pawsDao.insertAppointment(appointment)
    suspend fun updateAppointmentStatus(appointmentId: String, status: String) = pawsDao.updateAppointmentStatus(appointmentId, status)

    // Reminders
    fun getRemindersForConsumerFlow(consumerId: String): Flow<List<ReminderEntity>> = pawsDao.getRemindersForConsumerFlow(consumerId)
    suspend fun insertReminder(reminder: ReminderEntity) = pawsDao.insertReminder(reminder)
    suspend fun updateReminderCompletion(reminderId: String, isCompleted: Boolean) = pawsDao.updateReminderCompletion(reminderId, isCompleted)
    suspend fun deleteReminder(reminderId: String) = pawsDao.deleteReminder(reminderId)

    // Product Specs
    fun getSpecsForProductFlow(productId: String): Flow<List<ProductSpecEntity>> = pawsDao.getSpecsForProductFlow(productId)
    suspend fun getSpecsForProductSync(productId: String): List<ProductSpecEntity> = pawsDao.getSpecsForProductSync(productId)
    fun getAllProductSpecsFlow(): Flow<List<ProductSpecEntity>> = pawsDao.getAllProductSpecsFlow()
    suspend fun insertProductSpec(spec: ProductSpecEntity) = pawsDao.insertProductSpec(spec)
    suspend fun deleteProductSpec(specId: String) = pawsDao.deleteProductSpec(specId)

    // Pet Problems & Dynamic Recommendations
    val allProblemsFlow: Flow<List<ProblemEntity>> = pawsDao.getAllProblemsFlow()
    suspend fun insertProblem(problem: ProblemEntity) = pawsDao.insertProblem(problem)
    suspend fun deleteProblemById(id: String) = pawsDao.deleteProblemById(id)

    // Pets & Health Passports
    fun getPetsForOwnerFlow(ownerId: String): Flow<List<PetEntity>> = pawsDao.getPetsForOwnerFlow(ownerId)
    suspend fun insertPet(pet: PetEntity) = pawsDao.insertPet(pet)
    suspend fun deletePet(id: String) = pawsDao.deletePet(id)

    // Captains
    val pendingCaptainsFlow: Flow<List<CaptainEntity>> = pawsDao.getPendingCaptainsFlow()
    fun getCaptainByUserIdFlow(userId: String): Flow<CaptainEntity?> = pawsDao.getCaptainByUserIdFlow(userId)
    suspend fun insertCaptain(captain: CaptainEntity) = pawsDao.insertCaptain(captain)
    suspend fun updateCaptainStatus(id: String, status: String, isActive: Boolean) = pawsDao.updateCaptainStatus(id, status, isActive)
    suspend fun getCaptainById(id: String): CaptainEntity? = pawsDao.getCaptainById(id)

    // Group RFQ Sessions & Bidding Auction
    fun getGroupRfqSessionsForCity(cityId: String): Flow<List<GroupRfqSessionEntity>> = pawsDao.getGroupRfqSessionsForCity(cityId)
    suspend fun getGroupRfqSessionById(id: String): GroupRfqSessionEntity? = pawsDao.getGroupRfqSessionById(id)
    fun getGroupRfqSessionByIdFlow(id: String): Flow<GroupRfqSessionEntity?> = pawsDao.getGroupRfqSessionByIdFlow(id)
    suspend fun insertGroupRfqSession(session: GroupRfqSessionEntity) = pawsDao.insertGroupRfqSession(session)
    suspend fun updateGroupRfqSessionStatus(id: String, status: String) = pawsDao.updateGroupRfqSessionStatus(id, status)
    suspend fun acceptGroupRfqQuotation(id: String, chosenQuotationId: String) = pawsDao.acceptGroupRfqQuotation(id, chosenQuotationId)

    // Group RFQ Member Items
    fun getRfqMemberItemsForSession(sessionId: String): Flow<List<GroupRfqMemberItemEntity>> = pawsDao.getRfqMemberItemsForSession(sessionId)
    suspend fun getRfqMemberItemsForSessionSync(sessionId: String): List<GroupRfqMemberItemEntity> = pawsDao.getRfqMemberItemsForSessionSync(sessionId)
    suspend fun insertRfqMemberItem(item: GroupRfqMemberItemEntity) = pawsDao.insertRfqMemberItem(item)
    suspend fun markMemberItemsAsPaid(sessionId: String, memberId: String) = pawsDao.markMemberItemsAsPaid(sessionId, memberId)
    suspend fun deleteRfqMemberItem(id: String) = pawsDao.deleteRfqMemberItem(id)
    suspend fun clearRfqMemberItems(sessionId: String) = pawsDao.clearRfqMemberItems(sessionId)

    // Merchant Quotations (Bids)
    fun getQuotationsForSession(sessionId: String): Flow<List<MerchantQuotationEntity>> = pawsDao.getQuotationsForSession(sessionId)
    suspend fun insertMerchantQuotation(quotation: MerchantQuotationEntity) = pawsDao.insertMerchantQuotation(quotation)

    // Seed Database if empty
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val existingCities = pawsDao.getAllCitiesSync()
        val cities = listOf(
            CityEntity("hyd", "Hyderabad", "Telangana", true, 17.3850, 78.4867),
            CityEntity("blr", "Bengaluru", "Karnataka", true, 12.9716, 77.5946),
            CityEntity("maa", "Chennai", "Tamil Nadu", true, 13.0827, 80.2707),
            CityEntity("del", "Delhi", "Delhi", false, 28.6139, 77.2090),
            CityEntity("bom", "Mumbai", "Maharashtra", false, 19.0760, 72.8777),
            CityEntity("ccu", "Kolkata", "West Bengal", false, 22.5726, 88.3639)
        )
        cities.forEach { pawsDao.insertCity(it) }

        val hasNew = pawsDao.hasNewCategories()
        if (hasNew == 0) {
            pawsDao.clearCategories()
            pawsDao.clearShops()
            pawsDao.clearProducts()
            pawsDao.clearServices()
        } else {
            // New database structure already seeded, check if we need to seed appointments/reminders
            val existingAppts = pawsDao.getAppointmentsForConsumerSync("consumer_arjun")
            if (existingAppts.none { it.id.startsWith("appt_seed_") }) {
                val seededAppointments = listOf(
                    AppointmentEntity(
                        id = "appt_seed_1",
                        consumerId = "consumer_arjun",
                        shopId = "shop_hyd_1",
                        serviceId = "service_seed_1",
                        serviceName = "Annual Wellness Exam",
                        price = 800.0,
                        appointmentDate = "2026-10-24",
                        appointmentTime = "10:30 AM",
                        petName = "Bella",
                        status = "pending",
                        createdAt = System.currentTimeMillis()
                    ),
                    AppointmentEntity(
                        id = "appt_seed_2",
                        consumerId = "consumer_arjun",
                        shopId = "shop_hyd_2",
                        serviceId = "service_seed_2",
                        serviceName = "Vaccination Update",
                        price = 500.0,
                        appointmentDate = "2026-11-12",
                        appointmentTime = "02:00 PM",
                        petName = "Luna",
                        status = "pending",
                        createdAt = System.currentTimeMillis()
                    )
                )
                seededAppointments.forEach { pawsDao.insertAppointment(it) }
            }

            val existingReminders = pawsDao.getRemindersForConsumerSync("consumer_arjun")
            if (existingReminders.none { it.id.startsWith("rem_med_") || it.id.startsWith("rem_vacc_") }) {
                val seededMedReminders = listOf(
                    ReminderEntity(
                        id = "rem_med_1",
                        consumerId = "consumer_arjun",
                        title = "Heartgard Plus",
                        petName = "Buddy",
                        dateString = "2026-10-24",
                        notes = "1 Chewable | Monthly | Due",
                        isCompleted = false,
                        type = "medication",
                        createdAt = System.currentTimeMillis()
                    ),
                    ReminderEntity(
                        id = "rem_med_2",
                        consumerId = "consumer_arjun",
                        title = "NexGard",
                        petName = "Buddy",
                        dateString = "2026-11-10",
                        notes = "1 Chew (68mg) | Monthly | Flea & Tick | Last given: Oct 10 | Next: Nov 10",
                        isCompleted = false,
                        type = "medication",
                        createdAt = System.currentTimeMillis()
                    ),
                    ReminderEntity(
                        id = "rem_med_3",
                        consumerId = "consumer_arjun",
                        title = "Apoquel",
                        petName = "Buddy",
                        dateString = "2026-10-25",
                        notes = "1/2 Tablet | Daily | Allergy Relief | Last given: Today 8am | Next: Tmrw 8am",
                        isCompleted = false,
                        type = "medication",
                        createdAt = System.currentTimeMillis()
                    ),
                    ReminderEntity(
                        id = "rem_med_4",
                        consumerId = "consumer_arjun",
                        title = "Deworming Liquid",
                        petName = "Buddy",
                        dateString = "2026-08-15",
                        notes = "3 doses | Completed | Aug 15",
                        isCompleted = true,
                        type = "medication",
                        createdAt = System.currentTimeMillis()
                    ),
                    ReminderEntity(
                        id = "rem_med_5",
                        consumerId = "consumer_arjun",
                        title = "Antibiotic Ointment",
                        petName = "Buddy",
                        dateString = "2026-07-02",
                        notes = "14 days | Completed | Jul 02",
                        isCompleted = true,
                        type = "medication",
                        createdAt = System.currentTimeMillis()
                    ),
                    ReminderEntity(
                        id = "rem_vacc_1",
                        consumerId = "consumer_arjun",
                        title = "Rabies (1 Year)",
                        petName = "Buddy",
                        dateString = "2024-10-15",
                        notes = "Administered: Oct 15, 2023 | Due: Oct 15, 2024 | Dr. Sarah Jenkins | City Vet Clinic | cert",
                        isCompleted = false,
                        type = "vaccination",
                        createdAt = System.currentTimeMillis()
                    ),
                    ReminderEntity(
                        id = "rem_vacc_2",
                        consumerId = "consumer_arjun",
                        title = "DHPP (Distemper/Parvo)",
                        petName = "Buddy",
                        dateString = "2026-06-10",
                        notes = "Administered: Jun 10, 2023 | Valid 3 Years | Dr. Michael Chen | Downtown Pet Hospital",
                        isCompleted = true,
                        type = "vaccination",
                        createdAt = System.currentTimeMillis()
                    ),
                    ReminderEntity(
                        id = "rem_vacc_3",
                        consumerId = "consumer_arjun",
                        title = "Bordetella (Kennel Cough)",
                        petName = "Buddy",
                        dateString = "2023-03-22",
                        notes = "Administered: Mar 22, 2023 | Dr. Sarah Jenkins",
                        isCompleted = true,
                        type = "vaccination",
                        createdAt = System.currentTimeMillis()
                    )
                )
                seededMedReminders.forEach { pawsDao.insertReminder(it) }
            }
            return@withContext
        }

        // 2. Seed Categories
        val categories = listOf(
            CategoryEntity("cat_food", "Food & Nutrition", "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=100&auto=format&fit=crop&q=60"),
            CategoryEntity("cat_treats", "Treats & Chews", "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=100&auto=format&fit=crop&q=60"),
            CategoryEntity("cat_toys", "Toys & Mental Enrichment", "https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=100&auto=format&fit=crop&q=60"),
            CategoryEntity("cat_travel", "Travel, Leashes & Apparel", "https://images.unsplash.com/photo-1541599540903-216a46ca1da0?w=100&auto=format&fit=crop&q=60"),
            CategoryEntity("cat_furniture", "Furniture & Sleep", "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=100&auto=format&fit=crop&q=60"),
            CategoryEntity("cat_waste", "Waste Management & Litter", "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=100&auto=format&fit=crop&q=60"),
            CategoryEntity("cat_groom", "Grooming Services", "https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=100&auto=format&fit=crop&q=60")
        )
        categories.forEach { pawsDao.insertCategory(it) }

        // 3. Seed Shops
        val shops = listOf(
            ShopEntity(
                id = "shop_hyd_1",
                ownerId = "merchant_hyd_1",
                cityId = "hyd",
                name = "Royal Canine Hub",
                description = "Premium dog nutritional cuisine & styling spa",
                address = "Level 2, Road No 12, Banjara Hills, Hyderabad, 500034",
                locality = "Banjara Hills",
                lat = 17.4150, lng = 78.4410,
                phone = "+91 98765 43210",
                email = "contact@royalcaninehub.com",
                photos = listOf("https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=600&auto=format&fit=crop&q=80"),
                isOpen = true, opensAt = "09:00", closesAt = "21:30",
                rating = 4.8, totalReviews = 3,
                deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = true,
                status = "active",
                groomingEnabled = true,
                vetClinicEnabled = true
            ),
            ShopEntity(
                id = "shop_hyd_2",
                ownerId = "merchant_hyd_2",
                cityId = "hyd",
                name = "Puppy Love Groomers",
                description = "Expert coat grooming, therapy bathes & dog toys",
                address = "Jubilee Square, Road No 36, Jubilee Hills, Hyderabad, 500033",
                locality = "Jubilee Hills",
                lat = 17.4300, lng = 78.4000,
                phone = "+91 99999 88888",
                email = "contact@puppylove.com",
                photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=600&auto=format&fit=crop&q=80"),
                isOpen = true, opensAt = "10:00", closesAt = "20:00",
                rating = 4.3, totalReviews = 1,
                deliveryAvailable = false, isVerified = true, isActive = true, isFeatured = false,
                status = "active",
                groomingEnabled = true,
                vetClinicEnabled = true
            ),
            ShopEntity(
                id = "shop_blr_1",
                ownerId = "merchant_blr_1",
                cityId = "blr",
                name = "Paws & Tails Elite",
                description = "Indiranagar's premium multi-activity pet shopping arena",
                address = "100 Feet Rd, Hal 2nd Stage, Indiranagar, Bengaluru, 560038",
                locality = "Indiranagar",
                lat = 12.9716, lng = 77.6412,
                phone = "+91 91234 56789",
                email = "contact@pawstails.com",
                photos = listOf("https://images.unsplash.com/photo-1541599540903-216a46ca1da0?w=600&auto=format&fit=crop&q=80"),
                isOpen = true, opensAt = "08:30", closesAt = "22:00",
                rating = 4.9, totalReviews = 2,
                deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = true,
                status = "active",
                groomingEnabled = true,
                vetClinicEnabled = true
            ),
            ShopEntity(
                id = "shop_maa_1",
                ownerId = "merchant_maa_1",
                cityId = "maa",
                name = "The Dog Father",
                description = "Interactive training accessories & fresh organic food options",
                address = "Gandhi Nagar, Adyar, Chennai, 600020",
                locality = "Adyar",
                lat = 13.0033, lng = 80.2550,
                phone = "+91 90000 12345",
                email = "sales@dogfather.com",
                photos = listOf("https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=600&auto=format&fit=crop&q=80"),
                isOpen = true, opensAt = "09:00", closesAt = "21:00",
                rating = 4.6, totalReviews = 2,
                deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = true,
                status = "active",
                groomingEnabled = true,
                vetClinicEnabled = true
            )
        )
        shops.forEach { pawsDao.insertShop(it) }

        // 4. Seed Products
        val products = listOf(
            // Royal Canine Hub
            ProductEntity(
                id = "p_hyd_1", shopId = "shop_hyd_1", categoryId = "cat_food",
                name = "Premium Puppy Kibble (2kg)",
                description = "Grain-free nutrition enriched with prebiotic fiber, DHA & wholesome chicken protein, optimal for young retrievers.",
                price = 850.0, mrp = 1100.0,
                photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("kibble", "puppy", "grainfree", "food"),
                brand = "Royal Canin", lifeStage = "Puppy", stockCount = 12
            ),
            ProductEntity(
                id = "p_pedigree_dry", shopId = "shop_hyd_1", categoryId = "cat_food",
                name = "Pedigree Pro Dry Adult Dog Food",
                description = "Nutritious dry kibble formula specially formulated for adult dogs, packed with protein and key vitamins.",
                price = 750.0, mrp = 950.0,
                photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400"),
                inStock = true, tags = listOf("pedigree", "kibble", "dryfood", "dogfood"),
                brand = "Pedigree", lifeStage = "Adult", stockCount = 25
            ),
            ProductEntity(
                id = "p_cat_whiskas", shopId = "shop_hyd_1", categoryId = "cat_food",
                name = "Whiskas Premium Cat Kibble (Mackerel)",
                description = "Crunchy pockets filled with mackerel and premium sea protein, customized for adult cats.",
                price = 399.0, mrp = 490.0,
                photos = listOf("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400"),
                inStock = true, tags = listOf("cat", "whiskas", "catfood", "mackerel"),
                brand = "Whiskas", lifeStage = "Adult", stockCount = 30
            ),
            ProductEntity(
                id = "p_cattle_feed", shopId = "shop_hyd_1", categoryId = "cat_food",
                name = "Super Growth Cattle Blend (20kg)",
                description = "Premium nutritional feed optimized for lactating cows and cattle, rich in calcium and prebiotics.",
                price = 1200.0, mrp = 1500.0,
                photos = listOf("https://images.unsplash.com/photo-1570042225831-d98fa7577f1e?w=400"),
                inStock = true, tags = listOf("cattle", "cow", "feed", "bulk"),
                brand = "Bovishield", lifeStage = "Adult", stockCount = 10
            ),
            ProductEntity(
                id = "p_hamster_mix", shopId = "shop_hyd_1", categoryId = "cat_food",
                name = "Gourmet Hamster & Gerbil Crunchy Mix",
                description = "Fortified seed, grain, and fruit blend optimized for hamsters, gerbils, and small rodents.",
                price = 220.0, mrp = 299.0,
                photos = listOf("https://images.unsplash.com/photo-1452857297128-d9c29adba80b?w=400"),
                inStock = true, tags = listOf("hamster", "gerbil", "rodent", "seeds"),
                brand = "Kaytee", lifeStage = "Adult", stockCount = 15
            ),
            ProductEntity(
                id = "p_rabbit_pellets", shopId = "shop_hyd_1", categoryId = "cat_food",
                name = "Timothy Gold Premium Rabbit Feed",
                description = "Premium high-fiber Timothy hay pellets specifically designed for active adult rabbits.",
                price = 450.0, mrp = 550.0,
                photos = listOf("https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?w=400"),
                inStock = true, tags = listOf("rabbit", "bunny", "timothy", "pellets"),
                brand = "Oxbow", lifeStage = "Adult", stockCount = 18
            ),
            ProductEntity(
                id = "p_bird_seed", shopId = "shop_hyd_1", categoryId = "cat_food",
                name = "Wagner Wild Bird Feed Mix",
                description = "Premium mixed sunflower seeds and grains for wild birds, parakeets, and finches.",
                price = 280.0, mrp = 350.0,
                photos = listOf("https://images.unsplash.com/photo-1522850959076-58d7c244737a?w=400"),
                inStock = true, tags = listOf("bird", "parakeet", "finch", "seeds"),
                brand = "Wagner", lifeStage = "Adult", stockCount = 40
            ),
            ProductEntity(
                id = "p_hyd_2", shopId = "shop_hyd_1", categoryId = "cat_groom",
                name = "Royal Herbal Spa Therapy",
                description = "Luxurious full wash with organic neem & chamomile extracts, ear sanitation, hair trims & professional claw nail clipping.",
                price = 1499.0, mrp = 1800.0,
                photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("grooming", "bath", "spa"),
                brand = "Royal Canin", lifeStage = "Adult", stockCount = 8
            ),
            ProductEntity(
                id = "p_hyd_3", shopId = "shop_hyd_1", categoryId = "cat_access",
                name = "Orthopedic Memory Foam Bed",
                description = "Ergonomic pressure-relief memory foam sleep cushion with washable scratch-proof velour protective cover.",
                price = 2200.0, mrp = 2800.0,
                photos = listOf("https://images.unsplash.com/photo-1541599540903-216a46ca1da0?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("bed", "accessories", "comfort"),
                brand = "Generic", lifeStage = "Adult", stockCount = 15
            ),
            ProductEntity(
                id = "p_hyd_4", shopId = "shop_hyd_1", categoryId = "cat_furniture",
                name = "Joint Defense Chewable Tabs",
                description = "Daily glucosamine & MSM joint support chewable supplements, recommended by certified state veterinarians.",
                price = 690.0, mrp = 850.0,
                photos = listOf("https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("vet", "health", "vitamins"),
                brand = "Purina", lifeStage = "Senior", stockCount = 3
            ),
            ProductEntity(
                id = "p_shampoo_itch", shopId = "shop_hyd_1", categoryId = "cat_furniture",
                name = "Anti-Itch Oatmeal Shampoo",
                description = "Relieves severe itching and dry skin. Hypoallergenic formula enriched with natural oatmeal & aloe vera.",
                price = 350.0, mrp = 450.0,
                photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400"),
                inStock = true, tags = listOf("shampoo", "itching", "skin", "grooming"),
                brand = "Himalaya", lifeStage = "Adult", stockCount = 15
            ),
            ProductEntity(
                id = "p_shampoo_ticks", shopId = "shop_hyd_1", categoryId = "cat_furniture",
                name = "Tick & Flea Defense Shampoo",
                description = "Kills ticks, fleas, and lice on contact. Long-lasting protection with fresh neem extracts.",
                price = 399.0, mrp = 499.0,
                photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400"),
                inStock = true, tags = listOf("shampoo", "ticks", "fleas", "grooming"),
                brand = "Himalaya", lifeStage = "Adult", stockCount = 20
            ),
            ProductEntity(
                id = "p_shampoo_dandruff", shopId = "shop_hyd_1", categoryId = "cat_furniture",
                name = "Anti-Dandruff Tea Tree Shampoo",
                description = "Eliminates flakes and dandruff (like white powder) quickly. Soothes irritated skin.",
                price = 380.0, mrp = 480.0,
                photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400"),
                inStock = true, tags = listOf("shampoo", "dandruff", "dandruf", "flakes", "grooming"),
                brand = "Wahl", lifeStage = "Adult", stockCount = 12
            ),
            ProductEntity(
                id = "p_shampoo_fungal", shopId = "shop_hyd_1", categoryId = "cat_furniture",
                name = "Fungal Control Medicated Shampoo",
                description = "Effective against fungal infections and yeast dermatitis. Enriched with neem & ketoconazole.",
                price = 420.0, mrp = 520.0,
                photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400"),
                inStock = true, tags = listOf("shampoo", "fungal", "infections", "neem", "grooming"),
                brand = "Wahl", lifeStage = "Adult", stockCount = 8
            ),

            // Puppy Love Groomers
            ProductEntity(
                id = "p_hyd_5", shopId = "shop_hyd_2", categoryId = "cat_groom",
                name = "Deep Oatmeal Coat Wash",
                description = "Anti-itch organic hypoallergenic oatmeal bathes, blow dry, complete brush out & safe dog conditioning.",
                price = 999.0, mrp = 1200.0,
                photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400&auto=format&fit=crop&q=70"),
                inStock = false, tags = listOf("grooming", "wash", "oatmeal"),
                brand = "Pedigree", lifeStage = "Adult", stockCount = 0
            ),
            ProductEntity(
                id = "p_hyd_6", shopId = "shop_hyd_2", categoryId = "cat_access",
                name = "Indestructible Rubber Bone",
                description = "Extremely resilient heavy-duty chewing toy with hollow channel to insert peanut butter treats.",
                price = 450.0, mrp = 600.0,
                photos = listOf("https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("toy", "accessories", "chewer"),
                brand = "Generic", lifeStage = "Puppy", stockCount = 20
            ),

            // Paws & Tails Elite (Bengaluru)
            ProductEntity(
                id = "p_blr_1", shopId = "shop_blr_1", categoryId = "cat_food",
                name = "Super Food Salmon Blend (3kg)",
                description = "Wild-caught salmon dry formula packed with omega fatty acids for brilliant coat radiance and optimal energy.",
                price = 1550.0, mrp = 1950.0,
                photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("food", "salmon", "premium"),
                brand = "Acana", lifeStage = "Adult", stockCount = 9
            ),
            ProductEntity(
                id = "p_blr_2", shopId = "shop_blr_1", categoryId = "cat_access",
                name = "Padded Active Dog Harness",
                description = "Reflective nylon outdoor chest harness with security control handle and secure dual lead attachment loops.",
                price = 1150.0, mrp = 1400.0,
                photos = listOf("https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("harness", "accessories", "reflective"),
                brand = "Generic", lifeStage = "Adult", stockCount = 14
            ),

            // The Dog Father (Chennai)
            ProductEntity(
                id = "p_maa_1", shopId = "shop_maa_1", categoryId = "cat_food",
                name = "Dehydrated Raw Beef Jerky",
                description = "All-natural human-grade meat snacks slowly dehydrated to lock in nutrients. Zero additives, grain-free.",
                price = 480.0, mrp = 600.0,
                photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("jerky", "treats", "beef"),
                brand = "Pedigree", lifeStage = "Adult", stockCount = 6
            ),
            ProductEntity(
                id = "p_maa_2", shopId = "shop_maa_1", categoryId = "cat_access",
                name = "Interactive Snuffle Feeding Mat",
                description = "Plush intellectual puzzle play mat where your puppy searches for treats hidden inside fabric petal clusters.",
                price = 899.0, mrp = 1100.0,
                photos = listOf("https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("snuffle", "toy", "accessories"),
                brand = "Generic", lifeStage = "Puppy", stockCount = 5
            )
        )
        products.forEach { pawsDao.insertProduct(it) }

        // 5. Seed Reviews for Royal Canine Hub
        val reviews = listOf(
            ReviewEntity(
                id = "rev_1", shopId = "shop_hyd_1", consumerId = "consumer_arjun",
                rating = 5, comment = "Absolutely amazing groomers! Arjun loved his fresh trim and the dog spa treatment is fantastic. Highly recommend Road No 12 hub!",
                createdAt = System.currentTimeMillis() - 250000000L
            ),
            ReviewEntity(
                id = "rev_2", shopId = "shop_hyd_1", consumerId = "consumer_priya",
                rating = 5, comment = "Royal Canine is my go-to shop. Their memory foam bed is premium quality and their staff is extremely sweet to nervous puppies.",
                createdAt = System.currentTimeMillis() - 120000000L
            ),
            ReviewEntity(
                id = "rev_3", shopId = "shop_hyd_1", consumerId = "consumer_suresh",
                rating = 4, comment = "Excellent premium puppy kibble and dry foods. Spoke directly with the owner, very helpful. Delivery was quick but slightly delayed due to rains.",
                createdAt = System.currentTimeMillis() - 60000000L
            )
        )
        reviews.forEach { pawsDao.insertReview(it) }

        // 6. Seed Banners
        val banners = listOf(
            BannerEntity(
                id = "b1",
                imageUrl = "https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=800&auto=format&fit=crop&q=80",
                title = "Summer Pet Carnival!",
                description = "Get flat 50% discount on grooming packages at Royal Canine Hub. Limited slots available, book now!",
                targetCityIds = listOf("hyd", "all"),
                targetShopIds = listOf("shop_hyd_1")
            ),
            BannerEntity(
                id = "b2",
                imageUrl = "https://images.unsplash.com/photo-1583511655857-d19b40a7a54e?w=800&auto=format&fit=crop&q=80",
                title = "Premium Nutrition Week",
                description = "Seeded with love: Pedigree, Royal Canin, and Acana premium dog blends are on sale at Bengaluru's top boutiques.",
                targetCityIds = listOf("blr", "all"),
                targetShopIds = listOf("shop_blr_1")
            ),
            BannerEntity(
                id = "b3",
                imageUrl = "https://images.unsplash.com/photo-1541599540903-216a46ca1ad0?w=800&auto=format&fit=crop&q=80",
                title = "Ultimate snuffle toys",
                description = "Unlock the mental potential of your pet with Snuffle Feeding mats and chewing bones now available in Chennai and Hyderabad.",
                targetCityIds = listOf("hyd", "maa", "all"),
                targetShopIds = listOf("all")
            )
        )
        banners.forEach { pawsDao.insertBanner(it) }

        // 7. Seed Chat Messages for a vibrant demo
        val chats = listOf(
            ChatMessageEntity(
                id = "chat_seed_1",
                senderId = "consumer_arjun",
                recipientId = "merchant_hyd_1",
                shopId = "shop_hyd_1",
                message = "Hi Royal Canine Hub! Do you have the Premium Orthopedic Memory Foam Bed in stock today?",
                senderName = "Arjun Patel",
                timestamp = System.currentTimeMillis() - 72000000L,
                isRead = true
            ),
            ChatMessageEntity(
                id = "chat_seed_2",
                senderId = "merchant_hyd_1",
                recipientId = "consumer_arjun",
                shopId = "shop_hyd_1",
                message = "Hello Arjun! Yes, we have 4 units left in stock at our Banjara Hills boutique. You can buy it right through the app!",
                senderName = "Royal Canine Hub",
                timestamp = System.currentTimeMillis() - 71000000L,
                isRead = true
            )
        )
        chats.forEach { pawsDao.insertChatMessage(it) }

        // 8. Seed Default Service Offerings
        val seededServices = listOf(
            ServiceEntity(
                id = "service_seed_1",
                shopId = "shop_hyd_1",
                name = "Full Pet Styling & Grooming",
                price = 799.0,
                category = "Grooming"
            ),
            ServiceEntity(
                id = "service_seed_2",
                shopId = "shop_hyd_1",
                name = "Deep Clean Oatmeal Bath",
                price = 499.0,
                category = "Bathing"
            ),
            ServiceEntity(
                id = "service_seed_3",
                shopId = "shop_hyd_1",
                name = "Vet Doctor Clinic Consultation",
                price = 590.0,
                category = "Vet Doctor Clinic"
            ),
            ServiceEntity(
                id = "service_seed_4",
                shopId = "shop_blr_1",
                name = "Standard Grooming session",
                price = 650.0,
                category = "Grooming"
            ),
            ServiceEntity(
                id = "service_seed_5",
                shopId = "shop_blr_1",
                name = "Vet General Checkup",
                price = 500.0,
                category = "Vet Doctor Clinic"
            )
        )
        seededServices.forEach { pawsDao.insertService(it) }

        // 9. Seed Date Reminders for Arjun
        val seededReminders = listOf(
            ReminderEntity(
                id = "rem_seed_1",
                consumerId = "consumer_arjun",
                title = "Doctor Appointment",
                petName = "Buddy",
                dateString = "2026-05-30",
                notes = "General health checkup at Royal Canine Hub Vet Clinic.",
                type = "doctor"
            ),
            ReminderEntity(
                id = "rem_seed_2",
                consumerId = "consumer_arjun",
                title = "Pet Birthday",
                petName = "Buddy",
                dateString = "2026-06-05",
                notes = "Buddy's 2nd Birthday! Get special meat pastries.",
                type = "birthday"
            ),
            ReminderEntity(
                id = "rem_seed_3",
                consumerId = "consumer_arjun",
                title = "Vaccination Details",
                petName = "Buddy",
                dateString = "2026-05-29",
                notes = "DHPP Booster vaccination due.",
                type = "vaccination"
            )
        )
        seededReminders.forEach { pawsDao.insertReminder(it) }

        // 10. Seed Product Specs for Diverse Animal Categories
        val seededSpecs = listOf(
            // Dog - Pedigree (3 kg & 10 kg)
            ProductSpecEntity(
                id = "spec_pedigree_3",
                productId = "p_pedigree_dry",
                weightText = "3 kg",
                petCategory = "dog",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400",
                    "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=400",
                    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400",
                    "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400"
                ),
                description1 = "Premium protein sourced from quality ingredients supporting strong, lean muscles.",
                description2 = "Rich in Omega-6 fatty acids and zinc for a visibly radiant skin and healthy coat.",
                description3 = "Fortified with dietary fiber to support digestion and maximum nutrient absorption.",
                description4 = "Specially shaped crunchy kibble designed to help clean teeth and keep gums healthy."
            ),
            ProductSpecEntity(
                id = "spec_pedigree_10",
                productId = "p_pedigree_dry",
                weightText = "10 kg",
                petCategory = "dog",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=400",
                    "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400",
                    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400",
                    "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400"
                ),
                description1 = "Bulk active family size package providing long-lasting puppy energy and nutrition.",
                description2 = "Optimized calcium and phosphorus ratio to support healthy bone structure and active joints.",
                description3 = "Infused with organic prebiotics to promote a balanced gut flora in growing dogs.",
                description4 = "Fortified with natural vitamin E and minerals to boost strong immune defenses."
            ),
            
            // Cat - Whiskas (3 kg & 5 kg)
            ProductSpecEntity(
                id = "spec_whiskas_3",
                productId = "p_cat_whiskas",
                weightText = "3 kg",
                petCategory = "cat",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400",
                    "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400",
                    "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400",
                    "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=400"
                ),
                description1 = "Enriched with real mackerel proteins for shiny fur and overall cellular vitality.",
                description2 = "Provides optimal taurine levels to promote brilliant eyesight and strong heart health.",
                description3 = "Specially designed dental crunchy kibble pockets to reduce tartar build-up.",
                description4 = "Fortified with organic zinc and Omega-3 acids for premium immunity support."
            ),
            ProductSpecEntity(
                id = "spec_whiskas_5",
                productId = "p_cat_whiskas",
                weightText = "5 kg",
                petCategory = "cat",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400",
                    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400",
                    "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400",
                    "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=400"
                ),
                description1 = "Family active size packaging keeping your kittens and cats nourished and full of vitality.",
                description2 = "Low magnesium levels customized to support robust kidney and urinary tract health.",
                description3 = "Packed with clean vitamins and vital minerals for muscle retention and active plays.",
                description4 = "Naturally preserved without any artificial flavorings or colored dyes."
            ),

            // Cattle - Bovishield (20 kg)
            ProductSpecEntity(
                id = "spec_cattle_20",
                productId = "p_cattle_feed",
                weightText = "20 kg",
                petCategory = "cattle",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1570042225831-d98fa7577f1e?w=400",
                    "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400",
                    "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=400",
                    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400"
                ),
                description1 = "Bulk high-nutrition cattle blend formulated for premium milk production.",
                description2 = "Highly enriched with bone-building calcium and phosphate minerals.",
                description3 = "Infused with organic prebiotics to maintain balanced rumen health in milch cows.",
                description4 = "Clean grains and soy proteins to maximize energy levels and physical health."
            ),

            // Hamster - Kaytee (500 g)
            ProductSpecEntity(
                id = "spec_hamster_500",
                productId = "p_hamster_mix",
                weightText = "500 g",
                petCategory = "hamster",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1452857297128-d9c29adba80b?w=400",
                    "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400",
                    "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=400",
                    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400"
                ),
                description1 = "Gourmet seed, grain, and dried fruit mix tailored for hamsters and gerbils.",
                description2 = "Promotes natural foraging behavior and active small animal play.",
                description3 = "Fortified with natural vitamin C and minerals for strong teeth and claws.",
                description4 = "Packed in a premium zip-lock air-tight bag to retain freshness."
            ),

            // Rabbit - Timothy Feed (2 kg)
            ProductSpecEntity(
                id = "spec_rabbit_2",
                productId = "p_rabbit_pellets",
                weightText = "2 kg",
                petCategory = "rabbits",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?w=400",
                    "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400",
                    "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=400",
                    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400"
                ),
                description1 = "Premium high-fiber Timothy hay pellets for small rabbits and bunnies.",
                description2 = "Aids standard digestion and maintains optimal dental wear.",
                description3 = "Rich in vitamins, essential amino acids, and minerals.",
                description4 = "Soy-free and wheat-free natural timothy grass fibers."
            ),

            // Bird - Wagner Mix (1 kg)
            ProductSpecEntity(
                id = "spec_bird_1",
                productId = "p_bird_seed",
                weightText = "1 kg",
                petCategory = "birds",
                imageUrls = listOf(
                    "https://images.unsplash.com/photo-1522850959076-58d7c244737a?w=400",
                    "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400",
                    "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=400",
                    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400"
                ),
                description1 = "Premium wild bird seed mix including high-grade sunflower seeds.",
                description2 = "Optimized nutrient density to attract colorful finches, parrots, and wild birds.",
                description3 = "Fortified with amino acids and essential bird health trace minerals.",
                description4 = "Clean, dirt-free sifted seeds packaging for easy bird feeder placement."
            )
        )
        seededSpecs.forEach { pawsDao.insertProductSpec(it) }

        // 11. Seed Pet Health Passport for Arjun
        val seededPets = listOf(
            PetEntity(
                id = "pet_buddy",
                ownerId = "consumer_arjun",
                name = "Buddy",
                breed = "Golden Retriever",
                ageText = "2 years",
                weight = "24 kg",
                avatarUrl = "https://images.unsplash.com/photo-1552053831-71594a27632d?w=400", // Beautiful Retriever headshot
                allergies = "Grain-sensitive, no chicken by-products, prefers raw vegetables",
                vaccineRecord = "Rabies vaccine (2025-08-20), DHPP booster (2025-10-15), Dewormed",
                dewormingDate = "2026-05-15",
                vaccineDueDate = "2026-08-20"
            )
        )
        seededPets.forEach { pawsDao.insertPet(it) }

        // 12. Seed Dynamic Pet targeted Problems & Recommendations
        val seededProblems = listOf(
            ProblemEntity(
                id = "prob_hair",
                title = "Full Hair Growth 🦁",
                description = "Combat pet hair shedding, stimulate strong roots, and promote thick, lustrous coat density using premium salmon foods and natural grooming oils.",
                solution = "Feed salmon-based premium kibbles rich in Omega 3 & 6 fatty acids to nourish the hair follicles from the inside. Bathe your pet with natural conditioning formula.",
                howToUse = "1. Feed 150g salmon kibble daily.\n2. Apply grooming conditioner once a week during bath.\n3. Brush the fur daily for 5-10 minutes.",
                emoji = "🦁",
                productIds = listOf("p_blr_1", "p_hyd_1")
            ),
            ProblemEntity(
                id = "prob_itching",
                title = "Itching Relief 🧼",
                description = "Instantly soothe inflamed skin, treat seasonal hot spots, and reduce allergies/redness with medicated fungal and tea tree formulas recommended by veterinarians.",
                solution = "Use medicated anti-fungal shampoo with organic tea tree and oatmeal extracts to reduce skin redness, clean out allergens, and heal active fungal spores.",
                howToUse = "1. Wet coat thoroughly with lukewarm water.\n2. Massage anti-fungal shampoo deeply into the skin.\n3. Leave it on for 5-10 minutes before rinsing completely.",
                emoji = "🧼",
                productIds = listOf("p_shampoo_fungal", "p_shampoo_dandruff")
            ),
            ProblemEntity(
                id = "prob_dandruff",
                title = "Dandruff Control ❄️",
                description = "Say goodbye to annoying white flakes and skin dryness. Hydrate your pet's epidermal skin barrier with tea tree conditioning shampoos.",
                solution = "Apply moisturizing coal-tar and salicylic acid dandruff shampoo to hydrate the deep dry epidermal barrier, dissolve scales, and restore natural skin oils.",
                howToUse = "1. Massage dandruff shampoo into damp hair.\n2. Work into a rich lather and allow to sit for 3-5 minutes.\n3. Rinse thoroughly and towel dry.",
                emoji = "❄️",
                productIds = listOf("p_shampoo_dandruff", "p_shampoo_fungal")
            )
        )
        seededProblems.forEach { pawsDao.insertProblem(it) }

        // 13. Seed Default Profiles with Emails & Passwords
        val superAdminProfile = ProfileEntity(
            id = "admin_super",
            fullName = "Super Admin",
            phone = "79999999999",
            cityId = "hyd",
            avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&auto=format&fit=crop",
            role = "superadmin",
            email = "trinadhbandapalli@gmail.com",
            password = "thrinnadhh@Paws"
        )
        val defaultCustomerProfile = ProfileEntity(
            id = "consumer_arjun",
            fullName = "Arjun Kumar",
            phone = "799876543210",
            cityId = "hyd",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop",
            role = "consumer",
            email = "arjun@gmail.com",
            password = "password123"
        )
        pawsDao.insertProfile(superAdminProfile)
        pawsDao.insertProfile(defaultCustomerProfile)

        // Seed Appointments and Reminders for Arjun
        val existingAppts = pawsDao.getAppointmentsForConsumerSync("consumer_arjun")
        if (existingAppts.none { it.id.startsWith("appt_seed_") }) {
            val seededAppointments = listOf(
                AppointmentEntity(
                    id = "appt_seed_1",
                    consumerId = "consumer_arjun",
                    shopId = "shop_hyd_1",
                    serviceId = "service_seed_1",
                    serviceName = "Annual Wellness Exam",
                    price = 800.0,
                    appointmentDate = "2026-10-24",
                    appointmentTime = "10:30 AM",
                    petName = "Bella",
                    status = "pending",
                    createdAt = System.currentTimeMillis()
                ),
                AppointmentEntity(
                    id = "appt_seed_2",
                    consumerId = "consumer_arjun",
                    shopId = "shop_hyd_2",
                    serviceId = "service_seed_2",
                    serviceName = "Vaccination Update",
                    price = 500.0,
                    appointmentDate = "2026-11-12",
                    appointmentTime = "02:00 PM",
                    petName = "Luna",
                    status = "pending",
                    createdAt = System.currentTimeMillis()
                )
            )
            seededAppointments.forEach { pawsDao.insertAppointment(it) }
        }

        val existingReminders = pawsDao.getRemindersForConsumerSync("consumer_arjun")
        if (existingReminders.none { it.id.startsWith("rem_med_") || it.id.startsWith("rem_vacc_") }) {
            val seededMedReminders = listOf(
                ReminderEntity(
                    id = "rem_med_1",
                    consumerId = "consumer_arjun",
                    title = "Heartgard Plus",
                    petName = "Buddy",
                    dateString = "2026-10-24",
                    notes = "1 Chewable | Monthly | Due",
                    isCompleted = false,
                    type = "medication",
                    createdAt = System.currentTimeMillis()
                ),
                ReminderEntity(
                    id = "rem_med_2",
                    consumerId = "consumer_arjun",
                    title = "NexGard",
                    petName = "Buddy",
                    dateString = "2026-11-10",
                    notes = "1 Chew (68mg) | Monthly | Flea & Tick | Last given: Oct 10 | Next: Nov 10",
                    isCompleted = false,
                    type = "medication",
                    createdAt = System.currentTimeMillis()
                ),
                ReminderEntity(
                    id = "rem_med_3",
                    consumerId = "consumer_arjun",
                    title = "Apoquel",
                    petName = "Buddy",
                    dateString = "2026-10-25",
                    notes = "1/2 Tablet | Daily | Allergy Relief | Last given: Today 8am | Next: Tmrw 8am",
                    isCompleted = false,
                    type = "medication",
                    createdAt = System.currentTimeMillis()
                ),
                ReminderEntity(
                    id = "rem_med_4",
                    consumerId = "consumer_arjun",
                    title = "Deworming Liquid",
                    petName = "Buddy",
                    dateString = "2026-08-15",
                    notes = "3 doses | Completed | Aug 15",
                    isCompleted = true,
                    type = "medication",
                    createdAt = System.currentTimeMillis()
                ),
                ReminderEntity(
                    id = "rem_med_5",
                    consumerId = "consumer_arjun",
                    title = "Antibiotic Ointment",
                    petName = "Buddy",
                    dateString = "2026-07-02",
                    notes = "14 days | Completed | Jul 02",
                    isCompleted = true,
                    type = "medication",
                    createdAt = System.currentTimeMillis()
                ),
                ReminderEntity(
                    id = "rem_vacc_1",
                    consumerId = "consumer_arjun",
                    title = "Rabies (1 Year)",
                    petName = "Buddy",
                    dateString = "2024-10-15",
                    notes = "Administered: Oct 15, 2023 | Due: Oct 15, 2024 | Dr. Sarah Jenkins | City Vet Clinic | cert",
                    isCompleted = false,
                    type = "vaccination",
                    createdAt = System.currentTimeMillis()
                ),
                ReminderEntity(
                    id = "rem_vacc_2",
                    consumerId = "consumer_arjun",
                    title = "DHPP (Distemper/Parvo)",
                    petName = "Buddy",
                    dateString = "2026-06-10",
                    notes = "Administered: Jun 10, 2023 | Valid 3 Years | Dr. Michael Chen | Downtown Pet Hospital",
                    isCompleted = true,
                    type = "vaccination",
                    createdAt = System.currentTimeMillis()
                ),
                ReminderEntity(
                    id = "rem_vacc_3",
                    consumerId = "consumer_arjun",
                    title = "Bordetella (Kennel Cough)",
                    petName = "Buddy",
                    dateString = "2023-03-22",
                    notes = "Administered: Mar 22, 2023 | Dr. Sarah Jenkins",
                    isCompleted = true,
                    type = "vaccination",
                    createdAt = System.currentTimeMillis()
                )
            )
            seededMedReminders.forEach { pawsDao.insertReminder(it) }
        }
    }
}
