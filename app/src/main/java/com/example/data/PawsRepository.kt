package com.example.data

import com.example.domain.grooming.GroomingBookingRepository
import com.example.domain.grooming.GroomingBookingRequest
import com.example.domain.orders.CheckoutProduct
import com.example.domain.orders.CheckoutRepository
import com.example.domain.orders.OrderStatusRepository
import com.example.domain.orders.PlaceOrderItemRequest
import com.example.domain.orders.PlaceOrderRequest
import com.example.domain.orders.PlacedOrder
import com.example.domain.vet.DoctorAppointmentRepository
import com.example.domain.vet.DoctorAppointmentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PawsRepository(private val pawsDao: PawsDao) :
    GroomingBookingRepository,
    DoctorAppointmentRepository,
    CheckoutRepository,
    OrderStatusRepository {

    // Profiles
    suspend fun getProfile(id: String): ProfileEntity? = pawsDao.getProfile(id)
    suspend fun getProfileByPhone(phone: String): ProfileEntity? = pawsDao.getProfileByPhone(phone)
    suspend fun getProfileByEmail(email: String): ProfileEntity? = pawsDao.getProfileByEmail(email)
    suspend fun insertProfile(profile: ProfileEntity) {
        val rawPassword = profile.password
        val finalPassword = when {
            rawPassword.isNullOrBlank() -> null
            BCryptHelper.isHashedPassword(rawPassword) -> rawPassword
            else -> BCryptHelper.hashPassword(rawPassword)
        }
        pawsDao.insertProfile(profile.copy(password = finalPassword))
    }
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

    override suspend fun getCheckoutProduct(productId: String): CheckoutProduct? {
        return pawsDao.getProductById(productId)?.let { product ->
            CheckoutProduct(id = product.id, price = product.price)
        }
    }

    override suspend fun placeOrder(request: PlaceOrderRequest): PlacedOrder {
        insertOrder(
            OrderEntity(
                id = request.orderId,
                consumerId = request.consumerId,
                shopId = request.shopId,
                type = request.deliveryType,
                status = "pending",
                totalAmount = request.totalAmount,
                deliveryAddress = request.deliveryAddress,
                notes = request.notes,
                placedAt = request.placedAt
            )
        )
        insertOrderItems(request.items.map { it.toEntity() })
        return PlacedOrder(
            orderId = request.orderId,
            totalAmount = request.totalAmount,
            itemCount = request.items.size,
            deliveryType = request.deliveryType
        )
    }
    override suspend fun updateOrderStatus(orderId: String, status: String, captainId: String?) {
        val existingOrder = pawsDao.getOrderById(orderId)
        val finalCaptainId = captainId ?: existingOrder?.captainId
        pawsDao.updateOrderStatus(orderId, status, finalCaptainId)
        if (!ProductionConfig.IS_DEMO_MODE) {
            SupabaseManager.updateOrderStatusInCloud(orderId, status)
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        updateOrderStatus(orderId, status, null)
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
    suspend fun insertAppointment(appointment: AppointmentEntity) {
        pawsDao.insertAppointment(appointment)
        if (!ProductionConfig.IS_DEMO_MODE) {
            SupabaseManager.insertAppointmentToCloud(
                appointmentId = appointment.id,
                consumerId = appointment.consumerId,
                shopId = appointment.shopId,
                serviceId = appointment.serviceId,
                serviceName = appointment.serviceName,
                price = appointment.price,
                appointmentDate = appointment.appointmentDate,
                appointmentTime = appointment.appointmentTime,
                petName = appointment.petName,
                status = appointment.status
            )
        }
    }
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
    suspend fun getProblemById(id: String): ProblemEntity? = pawsDao.getProblemById(id)
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

        // Ensure mock_posh_paws and its products are always present
        val poshShop = pawsDao.getShopById("mock_posh_paws")
        if (poshShop == null) {
            pawsDao.insertShop(
                ShopEntity(
                    id = "mock_posh_paws",
                    ownerId = "merchant_posh_paws",
                    cityId = "hyd",
                    name = "The Posh Paws",
                    description = "Premium Pet Supplies & Accessories",
                    address = "123 Pet Avenue, Suite 4B, Metropolis, NY 10001",
                    locality = "Banjara Hills",
                    lat = 17.4150, lng = 78.4410,
                    phone = "(555) 123-4567",
                    email = "posh@paws.com",
                    photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuDO-FLWT7iQKhtxei9MNj4zRn2Giyn_JLl-A7mFm14gNsSAeh5ZQIPsRHCcYiDaBMgn4OvvvYNsn2hJAGB4NJqgsCZLmfZXT1t_I0OW5B9ERTOzyv9XW-sKjBz4N3uEweZFAIoMUmBW-aTLY6bu1WNxdHdZNuJ0kS8SEc2OEVnf0y6K56nEFOuyvzkPwL3dy743debiOCvJJvce4R8i5PUGfzN8BrQkGHuTEzwSZzEtL7zRhZEPQ54M-79FGR3NHN58I-ApJ8m6BlA"),
                    isOpen = true, opensAt = "09:00", closesAt = "20:00",
                    rating = 4.8, totalReviews = 42,
                    deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = true,
                    status = "active",
                    groomingEnabled = false,
                    vetClinicEnabled = false
                )
            )
            val poshProducts = listOf(
                ProductEntity(
                    id = "p_posh_1", shopId = "mock_posh_paws", categoryId = "cat_food",
                    name = "Wilderness Grain-Free Salmon Recipe",
                    description = "Wild-caught salmon dry formula packed with omega fatty acids for brilliant coat radiance and optimal energy.",
                    price = 45.99, mrp = 52.00,
                    photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuCtlcUGdknbnxqoGx69Rd9xnPKdeUvT17jW2kk-D96gocFI4_VYLuy59ahTw1ZyVd-4ycyYp_u4l8gw2Zazsl9mBWfFSUwzW5nC9jGpJujfxYG0Sd1mrtuJ5RX2IzJBg7g1Xc5oxNUMGjw4ByKGrmpuoNizrd9CX4iiRPXG1-OJrNvdgP1aXYFHbIMvTyn2MZmPN8rvShuhR1YrzYoZEy1y_yvKuYvEPQVx8H0YcL6LOrE_27ZDcKosz3D0H96yQXSzi1OVjTyxgIE"),
                    inStock = true, tags = listOf("salmon", "grainfree", "food", "dog"),
                    brand = "Wilderness", lifeStage = "Adult", stockCount = 12
                ),
                ProductEntity(
                    id = "p_posh_2", shopId = "mock_posh_paws", categoryId = "cat_toys",
                    name = "Tough-Chew Dino Plushie (Large)",
                    description = "Extremely resilient heavy-duty chewing toy with hollow channel to insert peanut butter treats.",
                    price = 14.50, mrp = 18.00,
                    photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuDlRTEgVVNx6kefg68afWQDOnsCUZhRJ2RVQbLchxXc5olgmAbed5oEcwOU8UhkAzUIPzkET36t3AD-RZXd-PSASKIpS1quqpgvu22hmMLDcc4wGZfRa197N9W33_ceAV6v_xrE4Rea8XdxremyWdjUnb8QA9J_qTVo4rxOGXixh93fTRFT2BMwCL-iZJ6wirBmOjGyGjbDMIauUdZ5doSPDdwtlwNLUrYe8_L9R8WB1svZrg5Ss7W6Ttk9vj1iV7RuzqtBY2cZLMw"),
                    inStock = true, tags = listOf("toy", "plush", "dino", "chew"),
                    brand = "Tough-Chew", lifeStage = "Adult", stockCount = 20
                ),
                ProductEntity(
                    id = "p_posh_3", shopId = "mock_posh_paws", categoryId = "cat_treats",
                    name = "Organic Peanut Butter Biscuits",
                    description = "Artisanal, bone-shaped dog treats arranged elegantly. Wholesome, natural ingredients.",
                    price = 8.99, mrp = 10.99,
                    photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuD-SLjtkvZYh5_rI2HWJm0-m6i3KVAYNCIKguMo9gmbmyZciyCU_UViB7koR6qiMQo1Pa6fyulmb43UIWd8jvm3A0ITpUx0J02UpJ1fN1DphqhCDcN35scbFFjeVnaXXXrehFNwNlQXEUM5vo7XytTIT-cpT8BF2hjO0cL65gJ837mbyHrUfGNBv7dNdiGA9bEistsBXYnm2VpzyviTq2u0gaWzTSVkxmJW93dwmFQ1MFfTLxFZRluaf12hD1GXTdXQEwO0FLaTkiA"),
                    inStock = true, tags = listOf("treats", "biscuits", "peanutbutter", "dog"),
                    brand = "Organic", lifeStage = "Adult", stockCount = 15
                ),
                ProductEntity(
                    id = "p_posh_4", shopId = "mock_posh_paws", categoryId = "cat_furniture",
                    name = "Orthopedic Cloud Lounger (Medium)",
                    description = "Soft, luxurious dog bed sitting in a bright, modern room corner. Plush fabric pressure-relief sleep cushion.",
                    price = 85.00, mrp = 95.00,
                    photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuD5NFqFPnKMEFbvAuBD_55W4KxuuMtl5E7NCkfO8WmnXzlrRKqFOk6qa9SrsQ-wXEP9OTFTlfm-8f-FcCOx-r98_E_CAXnWX0SdIWDmL4enTgZ_7UiZjDZa20nJ2OKAMdx5StNQlJeJzlH_DfcrzdDpdFGd2LPagPjuwUropMvzCvfJo2aDmGVH7scmv5Y93vt8D_D-dUNFIeTAd8RL3PgX9VEOahGTKa0kb3zwag3EJadUduENpj93ElQvaien90GMX8sN60-HRIE"),
                    inStock = false, tags = listOf("bed", "furniture", "sleep", "dog"),
                    brand = "Orthopedic", lifeStage = "Adult", stockCount = 0
                )
            )
            poshProducts.forEach { pawsDao.insertProduct(it) }
        }

        // Ensure mock_healthy_hounds is seeded
        val healthyShop = pawsDao.getShopById("mock_healthy_hounds")
        if (healthyShop == null) {
            pawsDao.insertShop(
                ShopEntity(
                    id = "mock_healthy_hounds",
                    ownerId = "merchant_healthy_hounds",
                    cityId = "hyd",
                    name = "Healthy Hounds Pantry",
                    description = "Organic & Raw Diet Specialist",
                    address = "Road No 12, Banjara Hills, Hyderabad",
                    locality = "Banjara Hills",
                    lat = 17.4120, lng = 78.4480,
                    phone = "9876543211",
                    email = "healthy@paws.com",
                    photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuASfM4FE2gFaBN0OhgeTBOjES2tuJHOL72sgaRGgO-tENBpVYDnBud9une2vRaHplLerDL25aSx0vh9cJz69DTuFIW1egWGJvltzY6_RQn4GF_mmvas_iU801N87_y6-JFB3H3zQFxvQwyYXfEgQuQ8JQuV0F3BI5heqbe6Fn_zOitcCR1esBTCKNBI4NVMHkzRxgVe8mC0fGuNb2htuR3f91sz8odhN4x_vfPmxh9MBA5fDQuWEqnrBtDvcw7nJsN8Qi7g7AzJId8"),
                    isOpen = true, opensAt = "09:00", closesAt = "21:00",
                    rating = 4.5, totalReviews = 88,
                    deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = true,
                    status = "active",
                    groomingEnabled = false,
                    vetClinicEnabled = false
                )
            )
        }

        // Ensure mock_city_hospital is seeded
        val cityHospital = pawsDao.getShopById("mock_city_hospital")
        if (cityHospital == null) {
            pawsDao.insertShop(
                ShopEntity(
                    id = "mock_city_hospital",
                    ownerId = "system",
                    cityId = "hyd",
                    name = "City Pet Hospital",
                    description = "Emergency & General Care",
                    address = "Metro Station Road, Madhapur, Hyderabad",
                    locality = "Madhapur",
                    lat = 17.4350, lng = 78.3880,
                    phone = "+91 90000 55555",
                    email = "cityhospital@paws.com",
                    photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuB5z2g3IHBH5gz3oR6QqQl6XDHPXhUN4b482F_jJ_bPPyD_OnMLA-gnGMdyNXz7v-jaFvfwW2nZgw5KX9NdTC9YFXzkoNU1GbbdvagvvRSdasnjCk7_elM2rSKuGbzmVkaxSgZdguhWDkjbumkNBU7ppWfcO0BHE2XmNjU2nF4ild_5dbokZ4jck5r_IU4B0KaW73XkasFSbOjZBQL9xAMihZ9AWDirYg99ysJl5RAKEqRVNyjhtIeMcQILmQFS97_A-HBozb9Kz-k"),
                    isOpen = true, opensAt = "07:00", closesAt = "23:00",
                    rating = 4.9, totalReviews = 19,
                    deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = false,
                    status = "active",
                    groomingEnabled = false,
                    vetClinicEnabled = true
                )
            )
        }

        // Ensure mock_petcare_wellness is seeded
        val wellnessCenter = pawsDao.getShopById("mock_petcare_wellness")
        if (wellnessCenter == null) {
            pawsDao.insertShop(
                ShopEntity(
                    id = "mock_petcare_wellness",
                    ownerId = "system",
                    cityId = "hyd",
                    name = "PetCare Wellness Center",
                    description = "Specialized Veterinary Services",
                    address = "Gachibowli, Hyderabad",
                    locality = "Gachibowli",
                    lat = 17.4480, lng = 78.3740,
                    phone = "+91 90000 66666",
                    email = "wellness@paws.com",
                    photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuDYKJG83KcL1yNh-w9EyZpJJHjgLNuCQIwoxOy4oxO9897FscAQj38VOtNLWetFhV0UcGvbpvYFMlMNisc1N7np5cd_0qaZcKNYGqSiaBeZDsParI4mxGmOxyw6mMU4RnJGckXQcWZv9-HU08XqZzmVBHFvSqAiJicfb1bes3T14Iv-yfAJJflwwAUl-CIk_HMUPFxRcCa1f_RtBSqklHewyESVhtAzbgZgixnF5Psbz6VhIkMXq-m2KovO2SB4RSYINa5KONreaS8"),
                    isOpen = true, opensAt = "08:00", closesAt = "22:00",
                    rating = 4.7, totalReviews = 34,
                    deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = false,
                    status = "active",
                    groomingEnabled = false,
                    vetClinicEnabled = true
                )
            )
        }

        // Ensure mock_paws_bubbles is seeded
        val bubblesSpa = pawsDao.getShopById("mock_paws_bubbles")
        if (bubblesSpa == null) {
            pawsDao.insertShop(
                ShopEntity(
                    id = "mock_paws_bubbles",
                    ownerId = "system",
                    cityId = "hyd",
                    name = "Fur & Fluff Boutique Spa",
                    description = "Luxury Grooming & Styling",
                    address = "Kondapur, Hyderabad",
                    locality = "Kondapur",
                    lat = 17.4620, lng = 78.3560,
                    phone = "9876543216",
                    email = "bubbles@paws.com",
                    photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuCLDcsiQzTJ35jcCpCNHSC0CPGtsB--0Xdb-LVHpAoteDtktABgPSTQMMPGcfAgwvMEa22Twz_PWoxMANUVHDlfmcOgn53ytuQl7eHMq2kD2oBJX8mNowGEJjxAIHOdSyARgHYwDg6TFxoXYoYnVogC8c3QqEQxzKXQHBhPxhv1VK3mWc1o8kwr-eyteIwsACN_yi3C9LZwRdXcVVbk_7sQFr6t-JFQsx7yaIuZTVNVZeEEPbhBBDvdW00lu99huqxwo4ClJpdhVnY"),
                    isOpen = true, opensAt = "09:00", closesAt = "21:00",
                    rating = 4.8, totalReviews = 42,
                    deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = false,
                    status = "active",
                    groomingEnabled = true,
                    vetClinicEnabled = false
                )
            )
        }

        // Ensure mock_grooming_room is seeded
        val groomingRoom = pawsDao.getShopById("mock_grooming_room")
        if (groomingRoom == null) {
            pawsDao.insertShop(
                ShopEntity(
                    id = "mock_grooming_room",
                    ownerId = "system",
                    cityId = "hyd",
                    name = "The Dapper Dog Salon",
                    description = "Professional Pet Grooming",
                    address = "Jubilee Hills, Hyderabad",
                    locality = "Jubilee Hills",
                    lat = 17.4300, lng = 78.4000,
                    phone = "9876543217",
                    email = "groomingroom@paws.com",
                    photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuA8-OnbYbH6ervRc4iDKjRxKLt6mO6wKvK8uA3YF7QqP3s6MzG7DILE7cEzhjoG1QhhOujkvk6kROOkrlX_HL2AqoacPYkIXR9PWO8eOCuNrkd24m2rUzV3v_SsO_Tt-eng-sTQpDJE-rHj2Ksx8Qw8uGaUZB-6jpIsSfhmFTkAVrxBXvue6givMDI98jjybom420pH3sbIUeml2Io6RygcKD0Xk279U3oRRXPXcZSjpIgZMptmDBLqWFDLWZce7mlSIJJ-aZXYgOs"),
                    isOpen = true, opensAt = "09:00", closesAt = "20:00",
                    rating = 4.6, totalReviews = 27,
                    deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = false,
                    status = "active",
                    groomingEnabled = true,
                    vetClinicEnabled = false
                )
            )
        }

        // Additive seeding only: never clear user or merchant-created records during startup.
        // Existing seed rows are protected by REPLACE semantics and id-prefix checks below.
        run {
            // Check if we need to seed appointments/reminders
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
                name = "Paws & Co. Grooming Loft",
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
                vetClinicEnabled = false
            ),
            ShopEntity(
                id = "shop_blr_1",
                ownerId = "merchant_blr_1",
                cityId = "blr",
                name = "Shampooch Luxury Spa",
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

            // Paws & Co. Grooming Loft
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

            // Shampooch Luxury Spa (Bengaluru)
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
            ),
            ServiceEntity(
                id = "service_seed_city_1",
                shopId = "mock_city_hospital",
                name = "Emergency Surgery Consultation",
                price = 1200.0,
                category = "Vet Doctor Clinic"
            ),
            ServiceEntity(
                id = "service_seed_city_2",
                shopId = "mock_city_hospital",
                name = "General OPD Consultation",
                price = 600.0,
                category = "Vet Doctor Clinic"
            ),
            ServiceEntity(
                id = "service_seed_city_3",
                shopId = "mock_city_hospital",
                name = "In-house Lab Diagnostics Checkup",
                price = 1500.0,
                category = "Vet Doctor Clinic"
            ),
            ServiceEntity(
                id = "service_seed_wellness_1",
                shopId = "mock_petcare_wellness",
                name = "Routine Health Examination",
                price = 400.0,
                category = "Vet Doctor Clinic"
            ),
            ServiceEntity(
                id = "service_seed_wellness_2",
                shopId = "mock_petcare_wellness",
                name = "Nutritional Consultation",
                price = 300.0,
                category = "Vet Doctor Clinic"
            ),
            ServiceEntity(
                id = "service_seed_wellness_3",
                shopId = "mock_petcare_wellness",
                name = "Comprehensive Vaccination Package",
                price = 800.0,
                category = "Vet Doctor Clinic"
            )
        )
        seededServices.forEach { pawsDao.insertService(it) }

        // Fallback to ensure services are present in existing DB instances
        val hospitalServicesList = listOf(
            ServiceEntity(id = "service_seed_city_1", shopId = "mock_city_hospital", name = "Emergency Surgery Consultation", price = 1200.0, category = "Vet Doctor Clinic"),
            ServiceEntity(id = "service_seed_city_2", shopId = "mock_city_hospital", name = "General OPD Consultation", price = 600.0, category = "Vet Doctor Clinic"),
            ServiceEntity(id = "service_seed_city_3", shopId = "mock_city_hospital", name = "In-house Lab Diagnostics Checkup", price = 1500.0, category = "Vet Doctor Clinic"),
            ServiceEntity(id = "service_seed_wellness_1", shopId = "mock_petcare_wellness", name = "Routine Health Examination", price = 400.0, category = "Vet Doctor Clinic"),
            ServiceEntity(id = "service_seed_wellness_2", shopId = "mock_petcare_wellness", name = "Nutritional Consultation", price = 300.0, category = "Vet Doctor Clinic"),
            ServiceEntity(id = "service_seed_wellness_3", shopId = "mock_petcare_wellness", name = "Comprehensive Vaccination Package", price = 800.0, category = "Vet Doctor Clinic"),
            ServiceEntity(id = "service_seed_bubbles_1", shopId = "mock_paws_bubbles", name = "Teddy Bear Coat Styling", price = 999.0, category = "Grooming"),
            ServiceEntity(id = "service_seed_bubbles_2", shopId = "mock_paws_bubbles", name = "Kennel Summer Short Cut", price = 799.0, category = "Grooming"),
            ServiceEntity(id = "service_seed_bubbles_3", shopId = "mock_paws_bubbles", name = "Majestic Lion Pom Styling", price = 1499.0, category = "Grooming"),
            ServiceEntity(id = "service_seed_bubbles_4", shopId = "mock_paws_bubbles", name = "Oatmeal Soothing Bath", price = 499.0, category = "Bathing"),
            ServiceEntity(id = "service_seed_bubbles_5", shopId = "mock_paws_bubbles", name = "Anti-Tick & Flea Medicated Wash", price = 699.0, category = "Bathing"),
            ServiceEntity(id = "service_seed_bubbles_6", shopId = "mock_paws_bubbles", name = "Premium Foam Aroma Spa Bath", price = 899.0, category = "Bathing"),
            ServiceEntity(id = "service_seed_grooming_room_1", shopId = "mock_grooming_room", name = "Teddy Bear Coat Styling", price = 999.0, category = "Grooming"),
            ServiceEntity(id = "service_seed_grooming_room_2", shopId = "mock_grooming_room", name = "Kennel Summer Short Cut", price = 799.0, category = "Grooming"),
            ServiceEntity(id = "service_seed_grooming_room_3", shopId = "mock_grooming_room", name = "Majestic Lion Pom Styling", price = 1499.0, category = "Grooming"),
            ServiceEntity(id = "service_seed_grooming_room_4", shopId = "mock_grooming_room", name = "Oatmeal Soothing Bath", price = 499.0, category = "Bathing"),
            ServiceEntity(id = "service_seed_grooming_room_5", shopId = "mock_grooming_room", name = "Anti-Tick & Flea Medicated Wash", price = 699.0, category = "Bathing"),
            ServiceEntity(id = "service_seed_grooming_room_6", shopId = "mock_grooming_room", name = "Premium Foam Aroma Spa Bath", price = 899.0, category = "Bathing")
        )
        hospitalServicesList.forEach { pawsDao.insertService(it) }

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
            password = BCryptHelper.hashPassword("thrinnadhh@Paws"),
            address = "Super Admin Headquarters, Hyderabad"
        )
        val defaultCustomerProfile = ProfileEntity(
            id = "consumer_arjun",
            fullName = "Arjun Kumar",
            phone = "799876543210",
            cityId = "hyd",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop",
            role = "consumer",
            email = "arjun@gmail.com",
            password = BCryptHelper.hashPassword("password123"),
            address = "Villa 42, Road No 5, Banjara Hills, Hyderabad"
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

        // Seed default Grooming services for mock_paws_bubbles and mock_grooming_room
        val existingGroomingServices = pawsDao.getGroomingServicesForShopSync("mock_paws_bubbles")
        if (existingGroomingServices.isEmpty()) {
            val sampleServices = listOf(
                // Bath variant: Basic Bath
                GroomingServiceEntity(
                    id = "gs_bubbles_basic_bath_small",
                    shopId = "mock_paws_bubbles",
                    serviceType = "bath",
                    variantName = "Basic Bath",
                    description = "Regular bath with quality organic shampoo, blow dry, and brushing.",
                    petSizeCategory = "small",
                    price = 300.0,
                    durationMinutes = 30,
                    imageUrls = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400"),
                    isActive = true
                ),
                GroomingServiceEntity(
                    id = "gs_bubbles_basic_bath_medium",
                    shopId = "mock_paws_bubbles",
                    serviceType = "bath",
                    variantName = "Basic Bath",
                    description = "Regular bath with quality organic shampoo, blow dry, and brushing.",
                    petSizeCategory = "medium",
                    price = 450.0,
                    durationMinutes = 45,
                    imageUrls = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400"),
                    isActive = true
                ),
                GroomingServiceEntity(
                    id = "gs_bubbles_basic_bath_large",
                    shopId = "mock_paws_bubbles",
                    serviceType = "bath",
                    variantName = "Basic Bath",
                    description = "Regular bath with quality organic shampoo, blow dry, and brushing.",
                    petSizeCategory = "large",
                    price = 600.0,
                    durationMinutes = 60,
                    imageUrls = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400"),
                    isActive = true
                ),

                // Haircut variant: Puppy Cut
                GroomingServiceEntity(
                    id = "gs_bubbles_puppy_cut_small",
                    shopId = "mock_paws_bubbles",
                    serviceType = "haircut",
                    variantName = "Puppy Cut",
                    description = "Short all-over even clip for active low-maintenance style.",
                    petSizeCategory = "small",
                    price = 500.0,
                    durationMinutes = 45,
                    imageUrls = listOf("https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=400"),
                    isActive = true
                ),
                GroomingServiceEntity(
                    id = "gs_bubbles_puppy_cut_medium",
                    shopId = "mock_paws_bubbles",
                    serviceType = "haircut",
                    variantName = "Puppy Cut",
                    description = "Short all-over even clip for active low-maintenance style.",
                    petSizeCategory = "medium",
                    price = 750.0,
                    durationMinutes = 60,
                    imageUrls = listOf("https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=400"),
                    isActive = true
                ),

                // Combo variant: Full Groom Package
                GroomingServiceEntity(
                    id = "gs_bubbles_full_groom_small",
                    shopId = "mock_paws_bubbles",
                    serviceType = "bath_and_haircut",
                    variantName = "Full Groom Package",
                    description = "Comprehensive grooming including Basic Bath + Puppy Cut with discounted price.",
                    petSizeCategory = "small",
                    price = 700.0, // Calculated: (300 + 500) - 100
                    durationMinutes = 75,
                    imageUrls = listOf("https://images.unsplash.com/photo-1534361960057-19889db9621e?w=400"),
                    isActive = true
                ),
                GroomingServiceEntity(
                    id = "gs_bubbles_full_groom_medium",
                    shopId = "mock_paws_bubbles",
                    serviceType = "bath_and_haircut",
                    variantName = "Full Groom Package",
                    description = "Comprehensive grooming including Basic Bath + Puppy Cut with discounted price.",
                    petSizeCategory = "medium",
                    price = 1100.0, // Calculated: (450 + 750) - 100
                    durationMinutes = 105,
                    imageUrls = listOf("https://images.unsplash.com/photo-1534361960057-19889db9621e?w=400"),
                    isActive = true
                ),

                // Other: Nail Trim
                GroomingServiceEntity(
                    id = "gs_bubbles_nail_trim_small",
                    shopId = "mock_paws_bubbles",
                    serviceType = "nail_trim",
                    variantName = "Nail Trim",
                    description = "Clip claws, file sharp edges, apply conditioning balm to pads.",
                    petSizeCategory = "small",
                    price = 150.0,
                    durationMinutes = 15,
                    imageUrls = listOf("https://images.unsplash.com/photo-1596492784531-6e6eb5ea9993?w=400"),
                    isActive = true
                ),
                GroomingServiceEntity(
                    id = "gs_bubbles_nail_trim_medium",
                    shopId = "mock_paws_bubbles",
                    serviceType = "nail_trim",
                    variantName = "Nail Trim",
                    description = "Clip claws, file sharp edges, apply conditioning balm to pads.",
                    petSizeCategory = "medium",
                    price = 200.0,
                    durationMinutes = 15,
                    imageUrls = listOf("https://images.unsplash.com/photo-1596492784531-6e6eb5ea9993?w=400"),
                    isActive = true
                ),
                GroomingServiceEntity(
                    id = "gs_bubbles_nail_trim_large",
                    shopId = "mock_paws_bubbles",
                    serviceType = "nail_trim",
                    variantName = "Nail Trim",
                    description = "Clip claws, file sharp edges, apply conditioning balm to pads.",
                    petSizeCategory = "large",
                    price = 250.0,
                    durationMinutes = 20,
                    imageUrls = listOf("https://images.unsplash.com/photo-1596492784531-6e6eb5ea9993?w=400"),
                    isActive = true
                ),

                // Other: Ear Cleaning
                GroomingServiceEntity(
                    id = "gs_bubbles_ear_cleaning_small",
                    shopId = "mock_paws_bubbles",
                    serviceType = "ear_cleaning",
                    variantName = "Ear Cleaning",
                    description = "Clean ear canals with antiseptic soothing botanical drops.",
                    petSizeCategory = "small",
                    price = 120.0,
                    durationMinutes = 15,
                    imageUrls = listOf("https://images.unsplash.com/photo-1601758228041-f3b2795255f1?w=400"),
                    isActive = true
                )
            )
            sampleServices.forEach { pawsDao.insertGroomingService(it) }
        }

        // Seed Doctors
        val existingDoctors = pawsDao.getDoctorsForShopSync("shop_hyd_1")
        if (existingDoctors.isEmpty()) {
            val sampleDoctors = listOf(
                DoctorEntity(
                    id = "doc_sarah",
                    shopId = "shop_hyd_1",
                    name = "Dr. Sarah Jenkins",
                    photoUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAXx7J-sz-fV3Y_VS7BUI6xREHCtu84yd5izEqlWLTC8qY3WVnPddQFvYVf1Uguayi9ZhyWEQqeq7VVZ8JBry3BUH8fQZe9pYp60LQlAR5WW6EqklajhvhlVd5UfSEbAEEdBxS2KPuNI2uYuzXZDjavgaQPJdIW6hobWpxBmcbsK-_aYymv4nOhGWpCChcYO1573y__YOHNSOI3lnkq3dbHgykjeQTBzaa7j5UA6cZ14CiudAIZpXucpgIZNwKuerA1rpKJ82bpAXk",
                    qualification = "DVM, DACVS",
                    specialization = "Emergency Surgery",
                    workingDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri"),
                    activeSlots = listOf("09:00 AM", "10:30 AM", "01:30 PM", "03:00 PM"),
                    isAvailable = true
                ),
                DoctorEntity(
                    id = "doc_elena",
                    shopId = "shop_hyd_1",
                    name = "Dr. Elena Rossi",
                    photoUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAn2LMbNYuqvAlSJ3Vsv3UiJVRa9B9YUYi39ShyxYeRMqbb6gwbm_4Iotbu_QZm-5pxwrOvETau5MJVPL3XcKNg6qJVbEke8w6buRQkmUgagREG15ZIZTBzIbtKGwCVgqAao1NSa_ZZsJpM9KgiC_VrK9O7aF2eMr6Gc5ES_fCKqnlSM8pjQ43E5RjzkRFt3L-lFoX38HEPBd99oYiAvt9cFIUV6eaqq44WCx2QdvoFE-V8XtAwCc_uwI9A53BXcQdxiXfZv7X09sw",
                    qualification = "DVM, CVA",
                    specialization = "Holistic Medicine & General Care",
                    workingDays = listOf("Mon", "Wed", "Fri"),
                    activeSlots = listOf("09:30 AM", "11:00 AM", "02:00 PM", "04:30 PM"),
                    isAvailable = true
                ),
                DoctorEntity(
                    id = "doc_michael",
                    shopId = "shop_hyd_1",
                    name = "Dr. Michael Chang",
                    photoUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAKwIVhg0jtyF9l-ZpnLelxtm7dPs0cNiFMAyw_WbKRSfJ-i0uLTr96yc3YnkabFFIt0-hMNtQHjQbpSi78KFLnVwvykST98Q7q0yq-VpxoJsmDMPV-o5KN9bm1J7OvZmRRt5brgcoASNXXfKdmBQQQhUfDC321lcBVF-97a8rpivLlEdfo5BOwTnpqhCSm8jeONbNd_hZQynrJzMvK0xF-OekAviLapXfyl7_GJi-uwVhc2mIOs3PLunPO4AZkbC_lh_X_RficrVc",
                    qualification = "DVM, DACVIM",
                    specialization = "Internal Medicine",
                    workingDays = listOf("Tue", "Thu", "Sat"),
                    activeSlots = listOf("10:00 AM", "12:00 PM", "03:30 PM", "05:00 PM"),
                    isAvailable = true
                )
            )
            sampleDoctors.forEach { pawsDao.insertDoctor(it) }
        }

        // Seed Coupons
        val existingCoupons = pawsDao.getCouponsForShopSync("shop_hyd_1")
        if (existingCoupons.isEmpty()) {
            val sampleCoupons = listOf(
                CouponEntity("c_welcome20", "global", "WELCOME20", 20.0, 200.0, 500.0, true),
                CouponEntity("c_flat10", "global", "FLAT10", 10.0, 100.0, 300.0, true)
            )
            sampleCoupons.forEach { pawsDao.insertCoupon(it) }
        }

        // Update default profiles with 4-digit passwords
        val arjun = pawsDao.getProfile("consumer_arjun")
        if (arjun != null) {
            pawsDao.insertProfile(arjun.copy(password = BCryptHelper.hashPassword("1234")))
        }
        val superAdmin = pawsDao.getProfile("admin_super")
        if (superAdmin != null) {
            pawsDao.insertProfile(superAdmin.copy(password = BCryptHelper.hashPassword("0000")))
        }

        // Attach freebie sample to Pedigree dry dog food
        val pedigree = pawsDao.getProductById("p_pedigree_dry")
        if (pedigree != null) {
            pawsDao.insertProduct(
                pedigree.copy(
                    sampleAttachedProductId = "p_hamster_mix",
                    sampleDescription = "Free Sample Timothy Premium Mix attached!"
                )
            )
        }
    }

    // --- Grooming Services Repository Methods ---
    fun getActiveGroomingServicesForShopFlow(shopId: String): Flow<List<GroomingServiceEntity>> = pawsDao.getActiveGroomingServicesForShopFlow(shopId)
    fun getAllGroomingServicesForShopFlow(shopId: String): Flow<List<GroomingServiceEntity>> = pawsDao.getAllGroomingServicesForShopFlow(shopId)
    suspend fun getGroomingServicesForShopSync(shopId: String): List<GroomingServiceEntity> = pawsDao.getGroomingServicesForShopSync(shopId)
    suspend fun getGroomingServiceById(id: String): GroomingServiceEntity? = pawsDao.getGroomingServiceById(id)
    suspend fun insertGroomingService(service: GroomingServiceEntity) = pawsDao.insertGroomingService(service)
    suspend fun deleteGroomingService(id: String) = pawsDao.deleteGroomingService(id)
    suspend fun clearGroomingServicesForShop(shopId: String) = pawsDao.clearGroomingServicesForShop(shopId)

    // --- Grooming Slots Repository Methods ---
    fun getGroomingSlotsForShopAndDateFlow(shopId: String, date: String): Flow<List<GroomingSlotEntity>> = pawsDao.getGroomingSlotsForShopAndDateFlow(shopId, date)
    suspend fun getGroomingSlotsForShopAndDateSync(shopId: String, date: String): List<GroomingSlotEntity> = pawsDao.getGroomingSlotsForShopAndDateSync(shopId, date)
    suspend fun getGroomingSlotById(id: String): GroomingSlotEntity? = pawsDao.getGroomingSlotById(id)
    suspend fun insertGroomingSlot(slot: GroomingSlotEntity) = pawsDao.insertGroomingSlot(slot)
    suspend fun insertGroomingSlots(slots: List<GroomingSlotEntity>) = pawsDao.insertGroomingSlots(slots)
    fun getGroomingSlotsForDateRangeFlow(shopId: String, startDate: String, endDate: String): Flow<List<GroomingSlotEntity>> = pawsDao.getGroomingSlotsForDateRangeFlow(shopId, startDate, endDate)
    suspend fun getGroomingSlotsForDateRangeSync(shopId: String, startDate: String, endDate: String): List<GroomingSlotEntity> = pawsDao.getGroomingSlotsForDateRangeSync(shopId, startDate, endDate)

    // Helper to auto-generate slots for a date if they don't exist
    suspend fun getOrGenerateSlotsForDate(shopId: String, dateStr: String): List<GroomingSlotEntity> = withContext(Dispatchers.IO) {
        val existing = pawsDao.getGroomingSlotsForShopAndDateSync(shopId, dateStr)
        if (existing.isNotEmpty()) {
            return@withContext existing
        }

        val shop = pawsDao.getShopById(shopId)
        val opensAt = shop?.opensAt ?: "09:00"
        val closesAt = shop?.closesAt ?: "21:00"

        val openHour = try { opensAt.split(":")[0].toInt() } catch(e: Exception) { 9 }
        val openMin = try { opensAt.split(":")[1].toInt() } catch(e: Exception) { 0 }
        val closeHour = try { closesAt.split(":")[0].toInt() } catch(e: Exception) { 21 }
        val closeMin = try { closesAt.split(":")[1].toInt() } catch(e: Exception) { 0 }

        val slots = mutableListOf<GroomingSlotEntity>()
        var currHour = openHour
        var currMin = openMin

        while (currHour < closeHour || (currHour == closeHour && currMin < closeMin)) {
            val timeStr = String.format("%02d:%02d", currHour, currMin)
            val slotId = "${shopId}_${dateStr}_${timeStr}"
            slots.add(GroomingSlotEntity(
                id = slotId,
                shopId = shopId,
                slotDate = dateStr,
                slotTime = timeStr,
                capacity = 1,
                bookedCount = 0,
                isBlocked = false
            ))

            currMin += 30
            if (currMin >= 60) {
                currHour += 1
                currMin = 0
            }
        }

        if (slots.isNotEmpty()) {
            pawsDao.insertGroomingSlots(slots)
        }
        return@withContext pawsDao.getGroomingSlotsForShopAndDateSync(shopId, dateStr)
    }

    // Bulk edit slot capacity
    suspend fun bulkEditSlotCapacity(
        shopId: String,
        startDate: String,
        endDate: String,
        daysOfWeek: List<Int>, // 1 = Sunday, 2 = Monday, ... 7 = Saturday
        newCapacity: Int
    ) = withContext(Dispatchers.IO) {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        
        try {
            val startCal = java.util.Calendar.getInstance()
            val parsedStart = format.parse(startDate) ?: return@withContext
            startCal.time = parsedStart

            val endCal = java.util.Calendar.getInstance()
            val parsedEnd = format.parse(endDate) ?: return@withContext
            endCal.time = parsedEnd

            val tempCal = java.util.Calendar.getInstance()
            tempCal.time = startCal.time

            while (!tempCal.after(endCal)) {
                val dayOfWeek = tempCal.get(java.util.Calendar.DAY_OF_WEEK)
                if (daysOfWeek.contains(dayOfWeek)) {
                    val dateStr = format.format(tempCal.time)
                    val slots = getOrGenerateSlotsForDate(shopId, dateStr)
                    val updatedSlots = slots.map { it.copy(capacity = newCapacity) }
                    pawsDao.insertGroomingSlots(updatedSlots)
                }
                tempCal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Grooming Bookings Repository Methods ---
    fun getGroomingBookingsForConsumerFlow(consumerId: String): Flow<List<GroomingBookingEntity>> = pawsDao.getGroomingBookingsForConsumerFlow(consumerId)
    fun getGroomingBookingsForShopFlow(shopId: String): Flow<List<GroomingBookingEntity>> = pawsDao.getGroomingBookingsForShopFlow(shopId)
    suspend fun getGroomingBookingById(id: String): GroomingBookingEntity? = pawsDao.getGroomingBookingById(id)
    suspend fun insertGroomingBooking(booking: GroomingBookingEntity) = withContext(Dispatchers.IO) {
        pawsDao.insertGroomingBooking(booking)
    }

    suspend fun bookGroomingSlot(booking: GroomingBookingEntity) = withContext(Dispatchers.IO) {
        val slot = pawsDao.getGroomingSlotById(booking.slotId)
            ?: throw Exception("The selected slot was not found.")

        if (slot.isBlocked) {
            throw Exception("The selected slot is currently blocked by the shop.")
        }
        if (slot.bookedCount >= slot.capacity) {
            throw Exception("The selected slot is fully booked. Please select another slot.")
        }

        val service = pawsDao.getGroomingServiceById(booking.serviceId)
            ?: throw Exception("Service not found.")

        if (service.petSizeCategory != booking.petSizeCategory) {
            throw Exception("The pet size category does not match the service size tier.")
        }

        pawsDao.insertGroomingBooking(booking)
        pawsDao.incrementSlotBookedCount(booking.slotId)
    }

    override suspend fun bookGroomingSlot(request: GroomingBookingRequest): String {
        val booking = GroomingBookingEntity(
            id = request.id,
            consumerId = request.consumerId,
            shopId = request.shopId,
            serviceId = request.serviceId,
            slotId = request.slotId,
            petId = request.petId,
            petSizeCategory = request.petSizeCategory,
            status = request.status,
            specialInstructions = request.specialInstructions,
            totalPrice = request.totalPrice,
            bookedAt = request.bookedAt
        )
        bookGroomingSlot(booking)
        return request.id
    }

    override suspend fun cancelGroomingBooking(bookingId: String) = withContext(Dispatchers.IO) {
        val booking = pawsDao.getGroomingBookingById(bookingId) ?: return@withContext
        if (booking.status == "cancelled") return@withContext

        pawsDao.updateGroomingBookingStatus(bookingId, "cancelled")
        pawsDao.decrementSlotBookedCount(booking.slotId)
    }

    override suspend fun updateGroomingBookingStatus(bookingId: String, status: String) = withContext(Dispatchers.IO) {
        val booking = pawsDao.getGroomingBookingById(bookingId) ?: return@withContext
        val oldStatus = booking.status
        
        pawsDao.updateGroomingBookingStatus(bookingId, status)
        
        if ((status == "cancelled" || status == "no_show") && oldStatus != "cancelled" && oldStatus != "no_show") {
            pawsDao.decrementSlotBookedCount(booking.slotId)
        } else if ((oldStatus == "cancelled" || oldStatus == "no_show") && status != "cancelled" && status != "no_show") {
            pawsDao.incrementSlotBookedCount(booking.slotId)
        }
    }

    // --- Doctor Repository Methods ---
    fun getDoctorsForShopFlow(shopId: String): Flow<List<DoctorEntity>> = pawsDao.getDoctorsForShopFlow(shopId)
    suspend fun getDoctorById(id: String): DoctorEntity? = pawsDao.getDoctorById(id)
    suspend fun insertDoctor(doctor: DoctorEntity) = withContext(Dispatchers.IO) { pawsDao.insertDoctor(doctor) }
    suspend fun deleteDoctor(id: String) = withContext(Dispatchers.IO) { pawsDao.deleteDoctor(id) }

    // Doctor Slots
    fun getDoctorSlotsFlow(shopId: String, doctorId: String, date: String): Flow<List<DoctorSlotEntity>> =
        pawsDao.getDoctorSlotsFlow(shopId, doctorId, date)

    suspend fun getOrGenerateDoctorSlotsForDate(shopId: String, doctorId: String, dateStr: String): List<DoctorSlotEntity> = withContext(Dispatchers.IO) {
        val existing = pawsDao.getDoctorSlotsSync(shopId, doctorId, dateStr)
        if (existing.isNotEmpty()) {
            return@withContext existing
        }

        val doctor = pawsDao.getDoctorById(doctorId) ?: return@withContext emptyList()
        val slots = mutableListOf<DoctorSlotEntity>()

        for (time in doctor.activeSlots) {
            val slotId = "doc_slot_${doctorId}_${dateStr}_${time.replace(" ", "").replace(":", "")}"
            slots.add(
                DoctorSlotEntity(
                    id = slotId,
                    doctorId = doctorId,
                    shopId = shopId,
                    slotDate = dateStr,
                    slotTime = time,
                    capacity = 1,
                    bookedCount = 0,
                    isBlocked = false
                )
            )
        }

        if (slots.isNotEmpty()) {
            pawsDao.insertDoctorSlots(slots)
        }
        return@withContext pawsDao.getDoctorSlotsSync(shopId, doctorId, dateStr)
    }

    suspend fun toggleDoctorSlotBlocked(slot: DoctorSlotEntity) = withContext(Dispatchers.IO) {
        pawsDao.insertDoctorSlot(slot.copy(isBlocked = !slot.isBlocked))
    }

    suspend fun updateDoctorSlotCapacity(slotId: String, capacity: Int) = withContext(Dispatchers.IO) {
        val slot = pawsDao.getDoctorSlotById(slotId) ?: return@withContext
        pawsDao.insertDoctorSlot(slot.copy(capacity = capacity))
    }

    suspend fun bookDoctorAppointment(appointment: AppointmentEntity, slotId: String) = withContext(Dispatchers.IO) {
        val slot = pawsDao.getDoctorSlotById(slotId)
            ?: throw Exception("The selected doctor slot was not found.")

        if (slot.isBlocked) {
            throw Exception("The selected doctor slot is currently blocked.")
        }
        if (slot.bookedCount >= slot.capacity) {
            throw Exception("The selected slot is fully booked. Please select another slot.")
        }

        pawsDao.insertAppointment(appointment)
        pawsDao.incrementDoctorSlotBookedCount(slotId)
    }

    override suspend fun bookDoctorAppointment(request: DoctorAppointmentRequest, slotId: String?): String {
        val appointment = AppointmentEntity(
            id = request.id,
            consumerId = request.consumerId,
            shopId = request.shopId,
            serviceId = request.serviceId,
            serviceName = request.serviceName,
            price = request.price,
            appointmentDate = request.appointmentDate,
            appointmentTime = request.appointmentTime,
            petName = request.petName,
            status = request.status,
            doctorId = request.doctorId,
            createdAt = request.createdAt,
            concern = request.concern,
            priority = request.priority
        )
        if (slotId != null) {
            bookDoctorAppointment(appointment, slotId)
        } else {
            insertAppointment(appointment)
        }
        return request.id
    }

    suspend fun getAppointmentById(id: String): AppointmentEntity? = pawsDao.getAppointmentById(id)

    override suspend fun cancelDoctorAppointment(appointmentId: String, slotId: String?) = withContext(Dispatchers.IO) {
        val appt = pawsDao.getAppointmentById(appointmentId)
        if (appt == null || appt.status == "cancelled" || appt.status == "no_show") {
            return@withContext
        }

        pawsDao.updateAppointmentStatus(appointmentId, "cancelled")
        if (slotId != null) {
            pawsDao.decrementDoctorSlotBookedCount(slotId)
        }
    }

    // --- Coupons Repository Methods ---
    fun getCouponsForShopFlow(shopId: String): Flow<List<CouponEntity>> = pawsDao.getCouponsForShopFlow(shopId)
    suspend fun getCouponByCode(code: String): CouponEntity? = pawsDao.getCouponByCode(code)
    suspend fun insertCoupon(coupon: CouponEntity) = withContext(Dispatchers.IO) { pawsDao.insertCoupon(coupon) }
    suspend fun deleteCoupon(id: String) = withContext(Dispatchers.IO) { pawsDao.deleteCoupon(id) }
}

private fun PlaceOrderItemRequest.toEntity(): OrderItemEntity {
    return OrderItemEntity(
        id = id,
        orderId = orderId,
        productId = productId,
        quantity = quantity,
        unitPrice = unitPrice,
        subtotal = subtotal
    )
}
