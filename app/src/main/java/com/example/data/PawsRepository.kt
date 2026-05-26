package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PawsRepository(private val pawsDao: PawsDao) {

    // Profiles
    suspend fun getProfile(id: String): ProfileEntity? = pawsDao.getProfile(id)
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

    // Categories
    val allCategoriesFlow: Flow<List<CategoryEntity>> = pawsDao.getAllCategoriesFlow()
    suspend fun insertCategory(category: CategoryEntity) = pawsDao.insertCategory(category)

    // Products
    fun getProductsForShop(shopId: String): Flow<List<ProductEntity>> = pawsDao.getProductsForShop(shopId)
    fun getAllProductsForShopUnfiltered(shopId: String): Flow<List<ProductEntity>> = pawsDao.getAllProductsForShopUnfiltered(shopId)
    suspend fun getProductById(id: String): ProductEntity? = pawsDao.getProductById(id)
    suspend fun insertProduct(product: ProductEntity) = pawsDao.insertProduct(product)
    suspend fun deleteProductById(id: String) = pawsDao.deleteProductById(id)

    // Orders
    fun getOrdersForConsumer(consumerId: String): Flow<List<OrderEntity>> = pawsDao.getOrdersForConsumer(consumerId)
    fun getOrdersForShop(shopId: String): Flow<List<OrderEntity>> = pawsDao.getOrdersForShop(shopId)
    suspend fun getOrderById(id: String): OrderEntity? = pawsDao.getOrderById(id)
    fun getOrderByIdFlow(id: String): Flow<OrderEntity?> = pawsDao.getOrderByIdFlow(id)
    suspend fun insertOrder(order: OrderEntity) = pawsDao.insertOrder(order)
    suspend fun updateOrderStatus(id: String, status: String) = pawsDao.updateOrderStatus(id, status)

    // Order Items
    suspend fun getOrderItemsForOrder(orderId: String): List<OrderItemEntity> = pawsDao.getOrderItemsForOrder(orderId)
    fun getOrderItemsForOrderFlow(orderId: String): Flow<List<OrderItemEntity>> = pawsDao.getOrderItemsForOrderFlow(orderId)
    suspend fun insertOrderItem(item: OrderItemEntity) = pawsDao.insertOrderItem(item)
    suspend fun insertOrderItems(items: List<OrderItemEntity>) = pawsDao.insertOrderItems(items)

    // Reviews
    fun getReviewsForShop(shopId: String): Flow<List<ReviewEntity>> = pawsDao.getReviewsForShop(shopId)
    suspend fun insertReview(review: ReviewEntity) = pawsDao.insertReview(review)

    // Wishlist
    fun getWishlistForConsumerFlow(consumerId: String): Flow<List<WishlistEntity>> = pawsDao.getWishlistForConsumerFlow(consumerId)
    suspend fun insertWishlist(wishlist: WishlistEntity) = pawsDao.insertWishlist(wishlist)
    suspend fun deleteWishlist(consumerId: String, shopId: String) = pawsDao.deleteWishlist(consumerId, shopId)

    // Seed Database if empty
    suspend fun seedDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val existingCities = pawsDao.getAllCitiesSync()
        if (existingCities.isNotEmpty()) {
            return@withContext
        }

        // 1. Seed Cities
        val cities = listOf(
            CityEntity("hyd", "Hyderabad", "Telangana", true, 17.3850, 78.4867),
            CityEntity("blr", "Bengaluru", "Karnataka", true, 12.9716, 77.5946),
            CityEntity("maa", "Chennai", "Tamil Nadu", true, 13.0827, 80.2707)
        )
        cities.forEach { pawsDao.insertCity(it) }

        // 2. Seed Categories
        val categories = listOf(
            CategoryEntity("cat_food", "Dog Food", "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=100&auto=format&fit=crop&q=60"),
            CategoryEntity("cat_groom", "Grooming", "https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=100&auto=format&fit=crop&q=60"),
            CategoryEntity("cat_access", "Accessories", "https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=100&auto=format&fit=crop&q=60"),
            CategoryEntity("cat_vet", "Vet Care", "https://images.unsplash.com/photo-1628009368231-7bb7cfcb0def?w=100&auto=format&fit=crop&q=60")
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
                deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = true
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
                deliveryAvailable = false, isVerified = true, isActive = true, isFeatured = false
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
                deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = true
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
                deliveryAvailable = true, isVerified = true, isActive = true, isFeatured = true
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
                inStock = true, tags = listOf("kibble", "puppy", "grainfree", "food")
            ),
            ProductEntity(
                id = "p_hyd_2", shopId = "shop_hyd_1", categoryId = "cat_groom",
                name = "Royal Herbal Spa Therapy",
                description = "Luxurious full wash with organic neem & chamomile extracts, ear sanitation, hair trims & professional claw nail clipping.",
                price = 1499.0, mrp = 1800.0,
                photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("grooming", "bath", "spa")
            ),
            ProductEntity(
                id = "p_hyd_3", shopId = "shop_hyd_1", categoryId = "cat_access",
                name = "Orthopedic Memory Foam Bed",
                description = "Ergonomic pressure-relief memory foam sleep cushion with washable scratch-proof velour protective cover.",
                price = 2200.0, mrp = 2800.0,
                photos = listOf("https://images.unsplash.com/photo-1541599540903-216a46ca1da0?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("bed", "accessories", "comfort")
            ),
            ProductEntity(
                id = "p_hyd_4", shopId = "shop_hyd_1", categoryId = "cat_vet",
                name = "Joint Defense Chewable Tabs",
                description = "Daily glucosamine & MSM joint support chewable supplements, recommended by certified state veterinarians.",
                price = 690.0, mrp = 850.0,
                photos = listOf("https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("vet", "health", "vitamins")
            ),

            // Puppy Love Groomers
            ProductEntity(
                id = "p_hyd_5", shopId = "shop_hyd_2", categoryId = "cat_groom",
                name = "Deep Oatmeal Coat Wash",
                description = "Anti-itch organic hypoallergenic oatmeal bathes, blow dry, complete brush out & safe dog conditioning.",
                price = 999.0, mrp = 1200.0,
                photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("grooming", "wash", "oatmeal")
            ),
            ProductEntity(
                id = "p_hyd_6", shopId = "shop_hyd_2", categoryId = "cat_access",
                name = "Indestructible Rubber Bone",
                description = "Extremely resilient heavy-duty chewing toy with hollow channel to insert peanut butter treats.",
                price = 450.0, mrp = 600.0,
                photos = listOf("https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("toy", "accessories", "chewer")
            ),

            // Paws & Tails Elite (Bengaluru)
            ProductEntity(
                id = "p_blr_1", shopId = "shop_blr_1", categoryId = "cat_food",
                name = "Super Food Salmon Blend (3kg)",
                description = "Wild-caught salmon dry formula packed with omega fatty acids for brilliant coat radiance and optimal energy.",
                price = 1550.0, mrp = 1950.0,
                photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("food", "salmon", "premium")
            ),
            ProductEntity(
                id = "p_blr_2", shopId = "shop_blr_1", categoryId = "cat_access",
                name = "Padded Active Dog Harness",
                description = "Reflective nylon outdoor chest harness with security control handle and secure dual lead attachment loops.",
                price = 1150.0, mrp = 1400.0,
                photos = listOf("https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("harness", "accessories", "reflective")
            ),

            // The Dog Father (Chennai)
            ProductEntity(
                id = "p_maa_1", shopId = "shop_maa_1", categoryId = "cat_food",
                name = "Dehydrated Raw Beef Jerky",
                description = "All-natural human-grade meat snacks slowly dehydrated to lock in nutrients. Zero additives, grain-free.",
                price = 480.0, mrp = 600.0,
                photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("jerky", "treats", "beef")
            ),
            ProductEntity(
                id = "p_maa_2", shopId = "shop_maa_1", categoryId = "cat_access",
                name = "Interactive Snuffle Feeding Mat",
                description = "Plush intellectual puzzle play mat where your puppy searches for treats hidden inside fabric petal clusters.",
                price = 899.0, mrp = 1100.0,
                photos = listOf("https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=400&auto=format&fit=crop&q=70"),
                inStock = true, tags = listOf("snuffle", "toy", "accessories")
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
    }
}
