package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed class Screen {
    object Splash : Screen()
    object Onboarding : Screen()
    object Auth : Screen()
    object LocationSelect : Screen()
    object Home : Screen()
    data class ShopDetail(val shopId: String) : Screen()
    object Cart : Screen()
    data class OrderTracking(val orderId: String) : Screen()
    object Search : Screen()
    object SavedShops : Screen()
    object UserProfile : Screen()
    object MerchantDashboard : Screen()
    object MerchantOrders : Screen()
    object MerchantMenu : Screen()
    object MerchantShopSetup : Screen()
}

class PawsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = PawsRepository(database.pawsDao())

    // Navigation State
    private val backstack = mutableListOf<Screen>()
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Splash)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Auth State
    private val _currentUser = MutableStateFlow<ProfileEntity?>(null)
    val currentUser: StateFlow<ProfileEntity?> = _currentUser.asStateFlow()

    // Active Selection State
    private val _selectedCityId = MutableStateFlow<String>("hyd")
    val selectedCityId: StateFlow<String> = _selectedCityId.asStateFlow()

    private val _selectedCityName = MutableStateFlow<String>("Hyderabad")
    val selectedCityName: StateFlow<String> = _selectedCityName.asStateFlow()

    // General Feed States
    val cities = repository.activeCitiesFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val categories = repository.allCategoriesFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val shops = _selectedCityId.flatMapLatest { cityId ->
        repository.getShopsForCity(cityId)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val wishlists = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getWishlistForConsumerFlow(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Cart State
    private val _cartItems = MutableStateFlow<Map<String, Int>>(emptyMap()) // Product ID -> Quantity
    val cartItems: StateFlow<Map<String, Int>> = _cartItems.asStateFlow()

    private val _cartShopId = MutableStateFlow<String?>(null)
    val cartShopId: StateFlow<String?> = _cartShopId.asStateFlow()

    // Trigger Warning if adding from a different shop
    private val _showCartWarning = MutableStateFlow<ShopConflict?>(null)
    val showCartWarning: StateFlow<ShopConflict?> = _showCartWarning.asStateFlow()

    data class ShopConflict(val pendingProduct: ProductEntity, val currentShopName: String)

    // Search and Filter States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchTab = MutableStateFlow("Shops") // "Shops" | "Products"
    val searchTab: StateFlow<String> = _searchTab.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _sortType = MutableStateFlow("Nearest") // "Nearest" | "Top Rated" | "New"
    val sortType: StateFlow<String> = _sortType.asStateFlow()

    private val _filterOpenNow = MutableStateFlow(false)
    val filterOpenNow: StateFlow<Boolean> = _filterOpenNow.asStateFlow()

    private val _filterDelivery = MutableStateFlow(false)
    val filterDelivery: StateFlow<Boolean> = _filterDelivery.asStateFlow()

    private val _filterRating = MutableStateFlow(false)
    val filterRating: StateFlow<Boolean> = _filterRating.asStateFlow()

    // Active Tracking States
    private val _activeOrder = MutableStateFlow<OrderEntity?>(null)
    val activeOrder: StateFlow<OrderEntity?> = _activeOrder.asStateFlow()

    private val _activeOrderItems = MutableStateFlow<List<OrderItemEntity>>(emptyList())
    val activeOrderItems: StateFlow<List<OrderItemEntity>> = _activeOrderItems.asStateFlow()

    // Merchant Profile / Dashboard States
    private val _merchantShop = MutableStateFlow<ShopEntity?>(null)
    val merchantShop: StateFlow<ShopEntity?> = _merchantShop.asStateFlow()

    init {
        viewModelScope.launch {
            // Seed Database
            repository.seedDatabaseIfEmpty()
            // Simulating a Splash Screen delay of 2 seconds
            delay(2000)
            _currentScreen.value = Screen.Onboarding
        }
    }

    // Navigation Methods
    fun navigateTo(screen: Screen) {
        backstack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateBack() {
        if (backstack.isNotEmpty()) {
            _currentScreen.value = backstack.removeAt(backstack.size - 1)
        } else {
            // Fallback: If nothing, go to Home or Dashboard depending on role
            val user = _currentUser.value
            _currentScreen.value = if (user?.role == "merchant") Screen.MerchantDashboard else Screen.Home
        }
    }

    fun clearHistoryAndNavigate(screen: Screen) {
        backstack.clear()
        _currentScreen.value = screen
    }

    suspend fun getShopById(id: String): ShopEntity? = repository.getShopById(id)

    // Auth Actions
    fun loginWithPhone(phone: String, isMerchant: Boolean) {
        viewModelScope.launch {
            val formattedPhone = phone.trim()
            val userId = if (isMerchant) "merchant_hyd_1" else "consumer_arjun"
            val defaultName = if (isMerchant) "Suresh Kumar" else "Arjun"
            val defaultRole = if (isMerchant) "merchant" else "consumer"
            val avatarUrl = if (isMerchant) 
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop"
                else "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop"

            // Check if profile exists, otherwise create
            var profile = repository.getProfile(userId)
            if (profile == null) {
                profile = ProfileEntity(
                    id = userId,
                    fullName = defaultName,
                    phone = formattedPhone,
                    cityId = "hyd",
                    avatarUrl = avatarUrl,
                    role = defaultRole
                )
                repository.insertProfile(profile)
            }

            _currentUser.value = profile
            _selectedCityId.value = profile.cityId
            _selectedCityName.value = if (profile.cityId == "hyd") "Hyderabad" else if (profile.cityId == "blr") "Bengaluru" else "Chennai"

            if (isMerchant) {
                // Load merchant shop
                val shop = repository.getShopByOwnerId(userId)
                _merchantShop.value = shop
                if (shop == null) {
                    _currentScreen.value = Screen.MerchantShopSetup
                } else {
                    _currentScreen.value = Screen.MerchantDashboard
                }
            } else {
                _currentScreen.value = Screen.LocationSelect
            }
        }
    }

    fun selectCity(cityId: String, cityName: String) {
        viewModelScope.launch {
            _selectedCityId.value = cityId
            _selectedCityName.value = cityName
            _currentUser.value?.let { user ->
                repository.updateProfileCity(user.id, cityId)
                _currentUser.value = user.copy(cityId = cityId)
            }
            navigateTo(Screen.Home)
        }
    }

    fun logout() {
        _currentUser.value = null
        _cartItems.value = emptyMap()
        _cartShopId.value = null
        _merchantShop.value = null
        clearHistoryAndNavigate(Screen.Auth)
    }

    // Feed Filter Toggles
    fun setSortType(type: String) { _sortType.value = type }
    fun toggleFilterOpenNow() { _filterOpenNow.value = !_filterOpenNow.value }
    fun toggleFilterDelivery() { _filterDelivery.value = !_filterDelivery.value }
    fun toggleFilterRating() { _filterRating.value = !_filterRating.value }
    fun setSelectedCategory(categoryId: String?) { _selectedCategoryId.value = categoryId }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun setSearchTab(tab: String) { _searchTab.value = tab }

    // Cart Actions
    fun addToCart(product: ProductEntity, shop: ShopEntity) {
        val currentShopId = _cartShopId.value
        if (currentShopId != null && currentShopId != shop.id) {
            // Trigger warning dialog
            _showCartWarning.value = ShopConflict(product, _merchantShop.value?.name ?: shop.name)
            return
        }

        _cartShopId.value = shop.id
        val currentQt = _cartItems.value[product.id] ?: 0
        val updatedMap = _cartItems.value.toMutableMap()
        updatedMap[product.id] = currentQt + 1
        _cartItems.value = updatedMap
    }

    fun removeFromCart(productId: String) {
        val currentQt = _cartItems.value[productId] ?: return
        val updatedMap = _cartItems.value.toMutableMap()
        if (currentQt <= 1) {
            updatedMap.remove(productId)
        } else {
            updatedMap[productId] = currentQt - 1
        }
        _cartItems.value = updatedMap

        if (updatedMap.isEmpty()) {
            _cartShopId.value = null
        }
    }

    fun resolveCartConflict(clearCartAndAdd: Boolean) {
        val conflict = _showCartWarning.value
        _showCartWarning.value = null
        if (clearCartAndAdd && conflict != null) {
            _cartItems.value = emptyMap()
            _cartShopId.value = conflict.pendingProduct.shopId
            viewModelScope.launch {
                val shopObj = repository.getShopById(conflict.pendingProduct.shopId)
                if (shopObj != null) {
                    addToCart(conflict.pendingProduct, shopObj)
                }
            }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyMap()
        _cartShopId.value = null
    }

    // Placing Orders
    fun placeOrder(address: String, notes: String, deliveryType: String) {
        val user = _currentUser.value ?: return
        val shopId = _cartShopId.value ?: return
        val items = _cartItems.value

        viewModelScope.launch {
            val orderId = "order_" + UUID.randomUUID().toString().substring(0, 8)
            
            // Calculate totals
            var subtotal = 0.0
            val lineItems = mutableListOf<OrderItemEntity>()
            items.forEach { (prodId, qty) ->
                val prod = repository.getProductById(prodId)
                if (prod != null) {
                    val lineCost = prod.price * qty
                    subtotal += lineCost
                    lineItems.add(
                        OrderItemEntity(
                            id = UUID.randomUUID().toString(),
                            orderId = orderId,
                            productId = prodId,
                            quantity = qty,
                            unitPrice = prod.price,
                            subtotal = lineCost
                        )
                    )
                }
            }

            val deliveryFee = if (deliveryType == "delivery") 30.0 else 0.0
            val totalAmount = subtotal + deliveryFee

            val newOrder = OrderEntity(
                id = orderId,
                consumerId = user.id,
                shopId = shopId,
                type = deliveryType,
                status = "pending",
                totalAmount = totalAmount,
                deliveryAddress = address,
                notes = notes,
                placedAt = System.currentTimeMillis()
            )

            // Insert to DB
            repository.insertOrder(newOrder)
            repository.insertOrderItems(lineItems)

            // Clear Cart
            clearCart()

            // Navigate to tracking
            _activeOrder.value = newOrder
            _activeOrderItems.value = lineItems
            _currentScreen.value = Screen.OrderTracking(orderId)

            // Kick-off automatic tracker progress simulation
            launchOrderSimulation(orderId)
        }
    }

    private fun launchOrderSimulation(orderId: String) {
        viewModelScope.launch {
            // Check status at intervals and simulate rider progress
            delay(10000)
            var currentOrd = repository.getOrderById(orderId)
            if (currentOrd?.status == "pending") {
                repository.updateOrderStatus(orderId, "accepted")
                _activeOrder.value = repository.getOrderById(orderId)
            }
            delay(12000)
            currentOrd = repository.getOrderById(orderId)
            if (currentOrd?.status == "accepted") {
                repository.updateOrderStatus(orderId, "preparing")
                _activeOrder.value = repository.getOrderById(orderId)
            }
            delay(15000)
            currentOrd = repository.getOrderById(orderId)
            if (currentOrd?.status == "preparing") {
                repository.updateOrderStatus(orderId, "out_for_delivery")
                _activeOrder.value = repository.getOrderById(orderId)
            }
            delay(15000)
            currentOrd = repository.getOrderById(orderId)
            if (currentOrd?.status == "out_for_delivery") {
                repository.updateOrderStatus(orderId, "delivered")
                _activeOrder.value = repository.getOrderById(orderId)
            }
        }
    }

    fun refreshActiveOrder(orderId: String) {
        viewModelScope.launch {
            _activeOrder.value = repository.getOrderById(orderId)
            _activeOrderItems.value = repository.getOrderItemsForOrder(orderId)
        }
    }

    fun submitReview(shopId: String, rating: Int, comment: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val reviewId = "rev_" + UUID.randomUUID().toString().substring(0, 8)
            val newReview = ReviewEntity(
                id = reviewId,
                shopId = shopId,
                consumerId = user.id,
                rating = rating,
                comment = comment,
                createdAt = System.currentTimeMillis()
            )
            repository.insertReview(newReview)

            // Recalculate average shop rating & reviews count
            repository.getReviewsForShop(shopId).firstOrNull()?.let { list ->
                val allReviews = list + newReview
                val avg = allReviews.map { it.rating }.average()
                repository.updateShopRating(shopId, avg, allReviews.size)
            }
        }
    }

    // Wishlist (Save Shop) Toggle
    fun toggleWishlist(shopId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val list = wishlists.value
            val isSaved = list.any { it.shopId == shopId }
            if (isSaved) {
                repository.deleteWishlist(user.id, shopId)
            } else {
                repository.insertWishlist(
                    WishlistEntity(
                        id = UUID.randomUUID().toString(),
                        consumerId = user.id,
                        shopId = shopId
                    )
                )
            }
        }
    }

    // Get specific Flow for Shop detail from DB
    fun getProductsFlow(shopId: String) = repository.getProductsForShop(shopId)
    fun getReviewsFlow(shopId: String) = repository.getReviewsForShop(shopId)

    // Merchant Wizards and Setup Actions
    fun submitMerchantShopSetup(
        name: String, phone: String, email: String,
        cityName: String, address: String, locality: String,
        deliveryAvailable: Boolean, opensAt: String, closesAt: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val shopId = "shop_merchant_" + UUID.randomUUID().toString().substring(0, 8)
            val newShop = ShopEntity(
                id = shopId,
                ownerId = user.id,
                cityId = "hyd", // default
                name = name,
                description = "Grooming & accessories curated with care",
                address = address,
                locality = locality,
                phone = phone,
                email = email,
                photos = listOf("https://images.unsplash.com/photo-1541599540903-216a46ca1da0?w=600&auto=format&fit=crop&q=80"), // Placeholder
                isOpen = true,
                opensAt = opensAt,
                closesAt = closesAt,
                rating = 5.0,
                totalReviews = 0,
                deliveryAvailable = deliveryAvailable,
                isVerified = true,
                isActive = true
            )
            repository.insertShop(newShop)
            _merchantShop.value = newShop
            
            // Seed 3 basic items for the merchant's store so they have full visual coverage
            val items = listOf(
                ProductEntity(UUID.randomUUID().toString(), shopId, "cat_food", "Healthy Puppy Starter Pack", "Protein rich initial feeding kit", 499.0, 599.0, photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=200"), inStock = true),
                ProductEntity(UUID.randomUUID().toString(), shopId, "cat_groom", "Lavender Refreshing Wash", "Gentle shampoo soothing sensitive skin", 350.0, 450.0, photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=200"), inStock = true),
                ProductEntity(UUID.randomUUID().toString(), shopId, "cat_access", "Ultra Strong Nylon Leash", "Heavy duty durable daily walker harness", 299.0, 399.0, photos = listOf("https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=200"), inStock = true)
            )
            items.forEach { repository.insertProduct(it) }

            _currentScreen.value = Screen.MerchantDashboard
        }
    }

    // Merchant Management Actions
    fun getMerchantOrdersFlow(): Flow<List<OrderEntity>> {
        return _merchantShop.flatMapLatest { shop ->
            if (shop != null) repository.getOrdersForShop(shop.id)
            else flowOf(emptyList())
        }
    }

    fun getMerchantProductsFlow(): Flow<List<ProductEntity>> {
        return _merchantShop.flatMapLatest { shop ->
            if (shop != null) repository.getAllProductsForShopUnfiltered(shop.id)
            else flowOf(emptyList())
        }
    }

    fun updateMerchantShopOpenStatus(isOpen: Boolean) {
        val shop = _merchantShop.value ?: return
        viewModelScope.launch {
            repository.updateShopStatus(shop.id, isOpen)
            _merchantShop.value = shop.copy(isOpen = isOpen)
        }
    }

    fun updateMerchantOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            // If viewing active tracking, sync too
            if (_activeOrder.value?.id == orderId) {
                _activeOrder.value = repository.getOrderById(orderId)
            }
        }
    }

    fun addMerchantProduct(name: String, categoryId: String, description: String, price: Double, mrp: Double) {
        val shop = _merchantShop.value ?: return
        viewModelScope.launch {
            val newProd = ProductEntity(
                id = "p_mer_" + UUID.randomUUID().toString().substring(0, 8),
                shopId = shop.id,
                categoryId = categoryId,
                name = name,
                description = description,
                price = price,
                mrp = mrp,
                photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400"),
                inStock = true,
                isActive = true
            )
            repository.insertProduct(newProd)
        }
    }

    fun deleteMerchantProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProductById(productId)
        }
    }
}
