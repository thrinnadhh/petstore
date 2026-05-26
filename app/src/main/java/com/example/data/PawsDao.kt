package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PawsDao {
    // Profiles
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Query("UPDATE profiles SET cityId = :cityId WHERE id = :id")
    suspend fun updateProfileCity(id: String, cityId: String)

    // Cities
    @Query("SELECT * FROM cities WHERE isActive = 1")
    fun getActiveCitiesFlow(): Flow<List<CityEntity>>

    @Query("SELECT * FROM cities")
    suspend fun getAllCitiesSync(): List<CityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: CityEntity)

    // Shops
    @Query("SELECT * FROM shops WHERE cityId = :cityId AND isActive = 1 AND isVerified = 1")
    fun getShopsForCity(cityId: String): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE cityId = :cityId AND isActive = 1 AND isVerified = 1")
    suspend fun getShopsForCitySync(cityId: String): List<ShopEntity>

    @Query("SELECT * FROM shops")
    fun getAllShopsFlow(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE id = :id LIMIT 1")
    suspend fun getShopById(id: String): ShopEntity?

    @Query("SELECT * FROM shops WHERE id = :id")
    fun getShopByIdFlow(id: String): Flow<ShopEntity?>

    @Query("SELECT * FROM shops WHERE ownerId = :ownerId LIMIT 1")
    suspend fun getShopByOwnerId(ownerId: String): ShopEntity?

    @Query("SELECT * FROM shops WHERE ownerId = :ownerId")
    fun getShopByOwnerIdFlow(ownerId: String): Flow<ShopEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShop(shop: ShopEntity)

    @Query("UPDATE shops SET isOpen = :isOpen WHERE id = :id")
    suspend fun updateShopStatus(id: String, isOpen: Boolean)

    @Query("UPDATE shops SET rating = :rating, totalReviews = :totalReviews WHERE id = :id")
    suspend fun updateShopRating(id: String, rating: Double, totalReviews: Int)

    // Categories
    @Query("SELECT * FROM categories")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    // Products
    @Query("SELECT * FROM products WHERE shopId = :shopId AND isActive = 1")
    fun getProductsForShop(shopId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE shopId = :shopId")
    fun getAllProductsForShopUnfiltered(shopId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    // Orders
    @Query("SELECT * FROM orders WHERE consumerId = :consumerId ORDER BY placedAt DESC")
    fun getOrdersForConsumer(consumerId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE shopId = :shopId ORDER BY placedAt DESC")
    fun getOrdersForShop(shopId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getOrderByIdFlow(id: String): Flow<OrderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOrderStatus(id: String, status: String, updatedAt: Long = System.currentTimeMillis())

    // Order Items
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    suspend fun getOrderItemsForOrder(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItemsForOrderFlow(orderId: String): Flow<List<OrderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    // Reviews
    @Query("SELECT * FROM reviews WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun getReviewsForShop(shopId: String): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    // Wishlist (Favorites)
    @Query("SELECT * FROM wishlists WHERE consumerId = :consumerId")
    fun getWishlistForConsumerFlow(consumerId: String): Flow<List<WishlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlist(wishlist: WishlistEntity)

    @Query("DELETE FROM wishlists WHERE consumerId = :consumerId AND shopId = :shopId")
    suspend fun deleteWishlist(consumerId: String, shopId: String)
}
