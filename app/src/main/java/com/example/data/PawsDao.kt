package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PawsDao {
    // Profiles
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfile(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE phone = :phone LIMIT 1")
    suspend fun getProfileByPhone(phone: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE email = :email LIMIT 1")
    suspend fun getProfileByEmail(email: String): ProfileEntity?

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
    @Query("SELECT * FROM shops WHERE cityId = :cityId AND isActive = 1 AND isVerified = 1 AND status = 'active'")
    fun getShopsForCity(cityId: String): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE cityId = :cityId AND isActive = 1 AND isVerified = 1 AND status = 'active'")
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

    @Query("UPDATE shops SET status = :status WHERE id = :id")
    suspend fun updateShopApprovalStatus(id: String, status: String)

    // Categories
    @Query("SELECT * FROM categories")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoriesCount(): Int

    @Query("SELECT COUNT(*) FROM categories WHERE id = 'cat_food'")
    suspend fun hasNewCategories(): Int

    @Query("DELETE FROM categories")
    suspend fun clearCategories()

    @Query("DELETE FROM shops")
    suspend fun clearShops()

    @Query("DELETE FROM products")
    suspend fun clearProducts()

    @Query("DELETE FROM services")
    suspend fun clearServices()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    // Products
    @Query("SELECT * FROM products WHERE isActive = 1")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

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

    @Query("UPDATE products SET stockCount = :stockCount WHERE id = :id")
    suspend fun updateProductStock(id: String, stockCount: Int)

    // Orders
    @Query("SELECT * FROM orders WHERE consumerId = :consumerId ORDER BY placedAt DESC")
    fun getOrdersForConsumer(consumerId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE shopId = :shopId ORDER BY placedAt DESC")
    fun getOrdersForShop(shopId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getOrderByIdFlow(id: String): Flow<OrderEntity?>

    @Query("SELECT * FROM orders")
    fun getAllOrdersFlow(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM order_items")
    fun getAllOrderItemsFlow(): Flow<List<OrderItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status, captainId = :captainId, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOrderStatus(id: String, status: String, captainId: String?, updatedAt: Long = System.currentTimeMillis())

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

    // Banners
    @Query("SELECT * FROM banners ORDER BY createdAt DESC")
    fun getAllBannersFlow(): Flow<List<BannerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanner(banner: BannerEntity)

    @Query("DELETE FROM banners WHERE id = :bannerId")
    suspend fun deleteBanner(bannerId: String)

    // Chats
    @Query("SELECT * FROM chat_messages WHERE shopId = :shopId ORDER BY timestamp ASC")
    fun getMessagesForConversationFlow(shopId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE senderId = :userId OR recipientId = :userId ORDER BY timestamp DESC")
    fun getMessagesForUserFlow(userId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET isRead = 1 WHERE shopId = :shopId AND recipientId = :userId")
    suspend fun markMessagesAsRead(shopId: String, userId: String)

    // Product Wishlist
    @Query("SELECT * FROM wishlist_products WHERE consumerId = :consumerId")
    fun getWishlistProductsForConsumerFlow(consumerId: String): Flow<List<WishlistProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistProduct(wishlistProduct: WishlistProductEntity)

    @Query("DELETE FROM wishlist_products WHERE consumerId = :consumerId AND productId = :productId")
    suspend fun deleteWishlistProduct(consumerId: String, productId: String)

    // Services
    @Query("SELECT * FROM services WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun getServicesForShopFlow(shopId: String): Flow<List<ServiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertService(service: ServiceEntity)

    @Query("DELETE FROM services WHERE id = :serviceId")
    suspend fun deleteService(serviceId: String)

    // Appointments
    @Query("SELECT * FROM appointments WHERE consumerId = :consumerId ORDER BY createdAt DESC")
    fun getAppointmentsForConsumerFlow(consumerId: String): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE consumerId = :consumerId")
    suspend fun getAppointmentsForConsumerSync(consumerId: String): List<AppointmentEntity>

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    suspend fun getAppointmentById(id: String): AppointmentEntity?

    @Query("SELECT * FROM appointments WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun getAppointmentsForShopFlow(shopId: String): Flow<List<AppointmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(appointment: AppointmentEntity)

    @Query("UPDATE appointments SET status = :status WHERE id = :appointmentId")
    suspend fun updateAppointmentStatus(appointmentId: String, status: String)

    // Reminders
    @Query("SELECT * FROM reminders WHERE consumerId = :consumerId ORDER BY dateString ASC")
    fun getRemindersForConsumerFlow(consumerId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE consumerId = :consumerId")
    suspend fun getRemindersForConsumerSync(consumerId: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isCompleted = :isCompleted WHERE id = :reminderId")
    suspend fun updateReminderCompletion(reminderId: String, isCompleted: Boolean)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminder(reminderId: String)

    // Product Specs (Weights and Descriptions)
    @Query("SELECT * FROM product_specs WHERE productId = :productId AND isActive = 1")
    fun getSpecsForProductFlow(productId: String): Flow<List<ProductSpecEntity>>

    @Query("SELECT * FROM product_specs WHERE productId = :productId AND isActive = 1")
    suspend fun getSpecsForProductSync(productId: String): List<ProductSpecEntity>

    @Query("SELECT * FROM product_specs")
    fun getAllProductSpecsFlow(): Flow<List<ProductSpecEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductSpec(spec: ProductSpecEntity)

    @Query("DELETE FROM product_specs WHERE id = :specId")
    suspend fun deleteProductSpec(specId: String)

    // Pets & Health Passports
    @Query("SELECT * FROM pets WHERE ownerId = :ownerId ORDER BY name ASC")
    fun getPetsForOwnerFlow(ownerId: String): Flow<List<PetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPet(pet: PetEntity)

    @Query("DELETE FROM pets WHERE id = :id")
    suspend fun deletePet(id: String)

    // Captains
    @Query("SELECT * FROM captains WHERE status = 'pending' ORDER BY createdAt DESC")
    fun getPendingCaptainsFlow(): Flow<List<CaptainEntity>>

    @Query("SELECT * FROM captains WHERE userId = :userId LIMIT 1")
    fun getCaptainByUserIdFlow(userId: String): Flow<CaptainEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaptain(captain: CaptainEntity)

    @Query("UPDATE captains SET status = :status, isActive = :isActive WHERE id = :id")
    suspend fun updateCaptainStatus(id: String, status: String, isActive: Boolean)

    @Query("SELECT * FROM captains WHERE id = :id LIMIT 1")
    suspend fun getCaptainById(id: String): CaptainEntity?

    // Pet Problems & Dynamic Recommendations
    @Query("SELECT * FROM pet_problems ORDER BY createdAt DESC")
    fun getAllProblemsFlow(): Flow<List<ProblemEntity>>

    @Query("SELECT * FROM pet_problems WHERE id = :id LIMIT 1")
    suspend fun getProblemById(id: String): ProblemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProblem(problem: ProblemEntity)

    @Query("DELETE FROM pet_problems WHERE id = :id")
    suspend fun deleteProblemById(id: String)

    // Group RFQ Sessions & Bidding Auction
    @Query("SELECT * FROM group_rfq_sessions WHERE cityId = :cityId ORDER BY createdAt DESC")
    fun getGroupRfqSessionsForCity(cityId: String): Flow<List<GroupRfqSessionEntity>>

    @Query("SELECT * FROM group_rfq_sessions WHERE id = :id LIMIT 1")
    suspend fun getGroupRfqSessionById(id: String): GroupRfqSessionEntity?

    @Query("SELECT * FROM group_rfq_sessions WHERE id = :id")
    fun getGroupRfqSessionByIdFlow(id: String): Flow<GroupRfqSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupRfqSession(session: GroupRfqSessionEntity)

    @Query("UPDATE group_rfq_sessions SET status = :status WHERE id = :id")
    suspend fun updateGroupRfqSessionStatus(id: String, status: String)

    @Query("UPDATE group_rfq_sessions SET chosenQuotationId = :chosenQuotationId, status = 'accepted' WHERE id = :id")
    suspend fun acceptGroupRfqQuotation(id: String, chosenQuotationId: String)

    // Group RFQ Member Items
    @Query("SELECT * FROM group_rfq_member_items WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun getRfqMemberItemsForSession(sessionId: String): Flow<List<GroupRfqMemberItemEntity>>

    @Query("SELECT * FROM group_rfq_member_items WHERE sessionId = :sessionId")
    suspend fun getRfqMemberItemsForSessionSync(sessionId: String): List<GroupRfqMemberItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRfqMemberItem(item: GroupRfqMemberItemEntity)

    @Query("UPDATE group_rfq_member_items SET hasPaid = 1 WHERE sessionId = :sessionId AND memberId = :memberId")
    suspend fun markMemberItemsAsPaid(sessionId: String, memberId: String)

    @Query("DELETE FROM group_rfq_member_items WHERE id = :id")
    suspend fun deleteRfqMemberItem(id: String)

    @Query("DELETE FROM group_rfq_member_items WHERE sessionId = :sessionId")
    suspend fun clearRfqMemberItems(sessionId: String)

    // Merchant Quotations (Bids)
    @Query("SELECT * FROM merchant_quotations WHERE sessionId = :sessionId ORDER BY discountPercentage DESC, submittedAt ASC")
    fun getQuotationsForSession(sessionId: String): Flow<List<MerchantQuotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchantQuotation(quotation: MerchantQuotationEntity)

    // Grooming Services
    @Query("SELECT * FROM grooming_services WHERE shopId = :shopId AND isActive = 1 ORDER BY createdAt DESC")
    fun getActiveGroomingServicesForShopFlow(shopId: String): Flow<List<GroomingServiceEntity>>

    @Query("SELECT * FROM grooming_services WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun getAllGroomingServicesForShopFlow(shopId: String): Flow<List<GroomingServiceEntity>>

    @Query("SELECT * FROM grooming_services WHERE shopId = :shopId")
    suspend fun getGroomingServicesForShopSync(shopId: String): List<GroomingServiceEntity>

    @Query("SELECT * FROM grooming_services WHERE id = :id LIMIT 1")
    suspend fun getGroomingServiceById(id: String): GroomingServiceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroomingService(service: GroomingServiceEntity)

    @Query("DELETE FROM grooming_services WHERE id = :id")
    suspend fun deleteGroomingService(id: String)

    @Query("DELETE FROM grooming_services WHERE shopId = :shopId")
    suspend fun clearGroomingServicesForShop(shopId: String)

    // Grooming Slots
    @Query("SELECT * FROM grooming_slots WHERE shopId = :shopId AND slotDate = :date ORDER BY slotTime ASC")
    fun getGroomingSlotsForShopAndDateFlow(shopId: String, date: String): Flow<List<GroomingSlotEntity>>

    @Query("SELECT * FROM grooming_slots WHERE shopId = :shopId AND slotDate = :date ORDER BY slotTime ASC")
    suspend fun getGroomingSlotsForShopAndDateSync(shopId: String, date: String): List<GroomingSlotEntity>

    @Query("SELECT * FROM grooming_slots WHERE id = :id LIMIT 1")
    suspend fun getGroomingSlotById(id: String): GroomingSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroomingSlot(slot: GroomingSlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroomingSlots(slots: List<GroomingSlotEntity>)

    @Query("UPDATE grooming_slots SET bookedCount = bookedCount + 1 WHERE id = :slotId")
    suspend fun incrementSlotBookedCount(slotId: String)

    @Query("UPDATE grooming_slots SET bookedCount = bookedCount - 1 WHERE id = :slotId AND bookedCount > 0")
    suspend fun decrementSlotBookedCount(slotId: String)

    @Query("SELECT * FROM grooming_slots WHERE shopId = :shopId AND slotDate >= :startDate AND slotDate <= :endDate ORDER BY slotDate ASC, slotTime ASC")
    suspend fun getGroomingSlotsForDateRangeSync(shopId: String, startDate: String, endDate: String): List<GroomingSlotEntity>

    @Query("SELECT * FROM grooming_slots WHERE shopId = :shopId AND slotDate >= :startDate AND slotDate <= :endDate ORDER BY slotDate ASC, slotTime ASC")
    fun getGroomingSlotsForDateRangeFlow(shopId: String, startDate: String, endDate: String): Flow<List<GroomingSlotEntity>>

    // Grooming Bookings
    @Query("SELECT * FROM grooming_bookings WHERE consumerId = :consumerId ORDER BY bookedAt DESC")
    fun getGroomingBookingsForConsumerFlow(consumerId: String): Flow<List<GroomingBookingEntity>>

    @Query("SELECT * FROM grooming_bookings WHERE shopId = :shopId ORDER BY bookedAt DESC")
    fun getGroomingBookingsForShopFlow(shopId: String): Flow<List<GroomingBookingEntity>>

    @Query("SELECT * FROM grooming_bookings WHERE id = :id LIMIT 1")
    suspend fun getGroomingBookingById(id: String): GroomingBookingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroomingBooking(booking: GroomingBookingEntity)

    @Query("UPDATE grooming_bookings SET status = :status WHERE id = :id")
    suspend fun updateGroomingBookingStatus(id: String, status: String)

    // Doctors
    @Query("SELECT * FROM doctors WHERE shopId = :shopId ORDER BY createdAt DESC")
    fun getDoctorsForShopFlow(shopId: String): Flow<List<DoctorEntity>>

    @Query("SELECT * FROM doctors WHERE shopId = :shopId")
    suspend fun getDoctorsForShopSync(shopId: String): List<DoctorEntity>

    @Query("SELECT * FROM doctors WHERE id = :id LIMIT 1")
    suspend fun getDoctorById(id: String): DoctorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctor(doctor: DoctorEntity)

    @Query("DELETE FROM doctors WHERE id = :id")
    suspend fun deleteDoctor(id: String)

    @Query("DELETE FROM doctors WHERE shopId = :shopId")
    suspend fun clearDoctorsForShop(shopId: String)

    // Doctor Slots
    @Query("SELECT * FROM doctor_slots WHERE shopId = :shopId AND doctorId = :doctorId AND slotDate = :date ORDER BY slotTime ASC")
    fun getDoctorSlotsFlow(shopId: String, doctorId: String, date: String): Flow<List<DoctorSlotEntity>>

    @Query("SELECT * FROM doctor_slots WHERE shopId = :shopId AND doctorId = :doctorId AND slotDate = :date ORDER BY slotTime ASC")
    suspend fun getDoctorSlotsSync(shopId: String, doctorId: String, date: String): List<DoctorSlotEntity>

    @Query("SELECT * FROM doctor_slots WHERE id = :id LIMIT 1")
    suspend fun getDoctorSlotById(id: String): DoctorSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorSlot(slot: DoctorSlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoctorSlots(slots: List<DoctorSlotEntity>)

    @Query("UPDATE doctor_slots SET bookedCount = bookedCount + 1 WHERE id = :slotId")
    suspend fun incrementDoctorSlotBookedCount(slotId: String)

    @Query("UPDATE doctor_slots SET bookedCount = bookedCount - 1 WHERE id = :slotId AND bookedCount > 0")
    suspend fun decrementDoctorSlotBookedCount(slotId: String)

    @Query("SELECT * FROM doctor_slots WHERE shopId = :shopId AND doctorId = :doctorId AND slotDate >= :startDate AND slotDate <= :endDate ORDER BY slotDate ASC, slotTime ASC")
    fun getDoctorSlotsForDateRangeFlow(shopId: String, doctorId: String, startDate: String, endDate: String): Flow<List<DoctorSlotEntity>>

    @Query("SELECT * FROM doctor_slots WHERE shopId = :shopId AND doctorId = :doctorId AND slotDate >= :startDate AND slotDate <= :endDate ORDER BY slotDate ASC, slotTime ASC")
    suspend fun getDoctorSlotsForDateRangeSync(shopId: String, doctorId: String, startDate: String, endDate: String): List<DoctorSlotEntity>

    // Coupons
    @Query("SELECT * FROM coupons WHERE (shopId = :shopId OR shopId = 'global') AND isActive = 1")
    fun getCouponsForShopFlow(shopId: String): Flow<List<CouponEntity>>

    @Query("SELECT * FROM coupons WHERE (shopId = :shopId OR shopId = 'global') AND isActive = 1")
    suspend fun getCouponsForShopSync(shopId: String): List<CouponEntity>

    @Query("SELECT * FROM coupons WHERE code = :code LIMIT 1")
    suspend fun getCouponByCode(code: String): CouponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupon(coupon: CouponEntity)

    @Query("DELETE FROM coupons WHERE id = :id")
    suspend fun deleteCoupon(id: String)
}

