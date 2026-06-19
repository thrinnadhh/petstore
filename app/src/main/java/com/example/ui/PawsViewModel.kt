package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import android.annotation.SuppressLint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface LocationResult {
    data class Serviceable(val city: CityEntity, val distanceKm: Float) : LocationResult
    data class NotServiceable(val city: CityEntity, val distanceKm: Float) : LocationResult
    data class Error(val message: String) : LocationResult
}

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
    object ChatList : Screen()
    data class ChatDetail(val shopId: String) : Screen()
    object MerchantDashboard : Screen()
    object MerchantOrders : Screen()
    object MerchantMenu : Screen()
    object MerchantShopSetup : Screen()
    object SuperAdmin : Screen()
    object SuperAdminUsers : Screen()
    object MerchantInventory : Screen()
    object Appointments : Screen()
    object TabletsIssued : Screen()
    object Vaccinations : Screen()
    object Favourites : Screen()
    object ReportsDashboard : Screen()
    object Orders : Screen()
    object FoodNutrition : Screen()
    object TreatsChews : Screen()
    object ToysEnrichment : Screen()
    object GroomingServices : Screen()
    object TravelApparel : Screen()
    object FurnitureSleep : Screen()
    object WasteManagement : Screen()

    // Doctor & Coupon Screens
    object MerchantDoctors : Screen()
    object MerchantCoupons : Screen()
    data class DoctorSlotPicker(
        val shopId: String,
        val doctorId: String,
        val serviceId: String,
        val price: Double
    ) : Screen()

    // Grooming Screens
    data class GroomingSlotPicker(
        val shopId: String,
        val serviceId: String,
        val variantName: String,
        val price: Double,
        val durationMinutes: Int,
        val petSizeCategory: String
    ) : Screen()
    data class GroomingBookingConfirmation(val bookingId: String) : Screen()
    object MyGroomingBookings : Screen()
    object MerchantGroomingServices : Screen()
    object MerchantGroomingSlots : Screen()
    object MerchantGroomingQueue : Screen()
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

    private val _selectedCityName = MutableStateFlow<String>("Hyderabad, Telangana")
    val selectedCityName: StateFlow<String> = _selectedCityName.asStateFlow()

    // Localization State
    private val _appLanguage = MutableStateFlow("en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    // Super Admin Financial Settings
    private val _platformCommission = MutableStateFlow(10.0)
    val platformCommission: StateFlow<Double> = _platformCommission.asStateFlow()

    private val _deliveryFeeTier = MutableStateFlow(30.0)
    val deliveryFeeTier: StateFlow<Double> = _deliveryFeeTier.asStateFlow()

    fun setPlatformCommission(commission: Double) {
        _platformCommission.value = commission
    }

    fun setDeliveryFeeTier(tier: Double) {
        _deliveryFeeTier.value = tier
    }

    fun setAppLanguage(lang: String) {
        _appLanguage.value = lang
        L10n.currentLanguage = lang
        val prefs = getApplication<Application>().getSharedPreferences("paws_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("app_language", lang).apply()
    }

    // General Feed States
    val cities = repository.activeCitiesFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val categories = repository.allCategoriesFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allOrders = repository.allOrdersFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val allOrderItems = repository.allOrderItemsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val petProblems = repository.allProblemsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    val shops = _selectedCityId.flatMapLatest { cityId ->
        repository.getShopsForCity(cityId)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val wishlists = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getWishlistForConsumerFlow(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Pending shops flow for Super Admin approvals
    val pendingShops = repository.allShopsFlow.map { list ->
        list.filter { it.status == "pending" }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Pending captains flow for Super Admin approvals
    val pendingCaptains = repository.pendingCaptainsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val currentCaptain = _currentUser.flatMapLatest { user ->
        if (user != null && user.role == "captain") repository.getCaptainByUserIdFlow(user.id)
        else flowOf(null)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)


    // Group RFQ Bidding State Flows
    private val _currentRfqSessionId = MutableStateFlow<String?>(null)
    val currentRfqSessionId: StateFlow<String?> = _currentRfqSessionId.asStateFlow()

    val activeRfqSession: StateFlow<GroupRfqSessionEntity?> = _currentRfqSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(null) else repository.getGroupRfqSessionByIdFlow(id)
        }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val activeRfqMemberItems: StateFlow<List<GroupRfqMemberItemEntity>> = _currentRfqSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getRfqMemberItemsForSession(id)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeRfqQuotations: StateFlow<List<MerchantQuotationEntity>> = _currentRfqSessionId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else repository.getQuotationsForSession(id)
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allRfqSessionsInCity: StateFlow<List<GroupRfqSessionEntity>> = _selectedCityId
        .flatMapLatest { cityId ->
            repository.getGroupRfqSessionsForCity(cityId)
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

    private val _searchTab = MutableStateFlow("All") // "All" | "Shops" | "Products"
    val searchTab: StateFlow<String> = _searchTab.asStateFlow()

    private val _selectedCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCategoryIds: StateFlow<Set<String>> = _selectedCategoryIds.asStateFlow()

    private val _sortType = MutableStateFlow("Popular 🏆") // "Popular 🏆" | "Top Rated" | "New" | "A-Z"
    val sortType: StateFlow<String> = _sortType.asStateFlow()

    // Swiggy One Premium Membership state
    private val _isSwiggyOneSubscriber = MutableStateFlow(false)
    val isSwiggyOneSubscriber: StateFlow<Boolean> = _isSwiggyOneSubscriber.asStateFlow()

    fun subscribeToSwiggyOne(subscribe: Boolean) {
        _isSwiggyOneSubscriber.value = subscribe
    }

    // Active Checkout Coupon Code State
    private val _appliedCoupon = MutableStateFlow<String?>(null)
    val appliedCoupon: StateFlow<String?> = _appliedCoupon.asStateFlow()

    fun applyCouponCode(code: String?) {
        _appliedCoupon.value = code
    }

    private val _filterOpenNow = MutableStateFlow(false)
    val filterOpenNow: StateFlow<Boolean> = _filterOpenNow.asStateFlow()

    private val _filterDelivery = MutableStateFlow(false)
    val filterDelivery: StateFlow<Boolean> = _filterDelivery.asStateFlow()

    private val _filterRating = MutableStateFlow(false)
    val filterRating: StateFlow<Boolean> = _filterRating.asStateFlow()

    private val _selectedFilterBrand = MutableStateFlow("All")
    val selectedFilterBrand: StateFlow<String> = _selectedFilterBrand.asStateFlow()

    private val _selectedFilterLifeStage = MutableStateFlow("All")
    val selectedFilterLifeStage: StateFlow<String> = _selectedFilterLifeStage.asStateFlow()

    private val _selectedFilterTag = MutableStateFlow("All")
    val selectedFilterTag: StateFlow<String> = _selectedFilterTag.asStateFlow()

    // Flow of all active products
    val allProducts = repository.allProductsFlow.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Combined filtered products flow
    val filteredProducts = combine(
        allProducts,
        _searchQuery,
        _selectedCategoryIds,
        _selectedFilterBrand,
        _selectedFilterLifeStage,
        _selectedFilterTag
    ) { flows: Array<Any?> ->
        val products = flows[0] as List<ProductEntity>
        val query = flows[1] as String
        val catIds = flows[2] as Set<String>
        val brand = flows[3] as String
        val lifeStage = flows[4] as String
        val tag = flows[5] as String

        products.filter { p ->
            val matchesQuery = query.isEmpty() || p.name.contains(query, ignoreCase = true) || p.description.contains(query, ignoreCase = true)
            val matchesCategory = catIds.isEmpty() || p.categoryId in catIds
            val matchesBrand = brand == "All" || p.brand.equals(brand, ignoreCase = true)
            val matchesLifeStage = lifeStage == "All" || p.lifeStage.equals(lifeStage, ignoreCase = true)
            val matchesTag = tag == "All" || p.tags.any { it.equals(tag, ignoreCase = true) }
            matchesQuery && matchesCategory && matchesBrand && matchesLifeStage && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Persistent bottom navigation tab tracker ("explore", "wishlist", "chat", "profile")
    private val _currentTab = MutableStateFlow("explore")
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    fun setTab(tab: String) {
        _currentTab.value = tab
    }

    // Banners State Flows
    val allBanners = repository.allBannersFlow.map { banners ->
        banners.filter { banner ->
            banner.title.trim().isNotEmpty() &&
            banner.description.trim().isNotEmpty() &&
            banner.imageUrl.trim().isNotEmpty()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val targetedBanners = combine(allBanners, _selectedCityId) { bannersList, cityId ->
        bannersList.filter { banner ->
            banner.isActive && (banner.targetCityIds.contains(cityId) || banner.targetCityIds.contains("all"))
        }.take(10)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Product Wishlist
    val wishlistProducts = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getWishlistProductsForConsumerFlow(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // All products (for favourites screen and global searches)
    val products = repository.allProductsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Chats State Flows
    private val _activeChatShopId = MutableStateFlow<String?>(null)
    val activeChatShopId: StateFlow<String?> = _activeChatShopId.asStateFlow()

    val activeChatMessages = _activeChatShopId.flatMapLatest { shopId ->
        if (shopId != null) repository.getMessagesForConversationFlow(shopId)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeConversations = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getMessagesForUserFlow(user.id)
        else flowOf(emptyList())
    }.map { messages ->
        messages.groupBy { it.shopId }.map { (shopId, msgList) ->
            msgList.maxByOrNull { it.timestamp }!!
        }.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Merchant Profile / Dashboard States
    private val _merchantShop = MutableStateFlow<ShopEntity?>(null)
    val merchantShop: StateFlow<ShopEntity?> = _merchantShop.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeAppointments = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getAppointmentsForConsumerFlow(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val merchantAppointments = _merchantShop.flatMapLatest { shop ->
        if (shop != null) repository.getAppointmentsForShopFlow(shop.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val merchantServices = _merchantShop.flatMapLatest { shop ->
        if (shop != null) repository.getServicesForShopFlow(shop.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeReminders = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getRemindersForConsumerFlow(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activePets = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getPetsForOwnerFlow(user.id)
        else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    // Active Tracking States
    private val _activeOrder = MutableStateFlow<OrderEntity?>(null)
    val activeOrder: StateFlow<OrderEntity?> = _activeOrder.asStateFlow()

    val activeOrderCaptain = _activeOrder.flatMapLatest { order ->
        if (order != null && !order.captainId.isNullOrEmpty()) {
            flow<CaptainEntity?> {
                emit(repository.getCaptainById(order.captainId))
            }
        } else {
            flowOf<CaptainEntity?>(null)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val _activeOrderItems = MutableStateFlow<List<OrderItemEntity>>(emptyList())
    val activeOrderItems: StateFlow<List<OrderItemEntity>> = _activeOrderItems.asStateFlow()

    // PowerSync Sync State Flow
    val powerSyncState = PowerSyncManager.syncState
    
    fun triggerManualPowerSync() {
        viewModelScope.launch {
            PowerSyncManager.triggerSync()
        }
    }

    // Firebase FCM registration token stream
    val fcmToken = FirebaseIntegrationManager.fcmToken

    // Product Specifications flows for admin controls
    val allProductSpecs = repository.getAllProductSpecsFlow().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Initialize global language settings
        val prefs = application.getSharedPreferences("paws_settings", Context.MODE_PRIVATE)
        val loadedLanguage = prefs.getString("app_language", "en") ?: "en"
        _appLanguage.value = loadedLanguage
        L10n.currentLanguage = loadedLanguage

        // Initialize global cloud services and analytics
        SupabaseManager.init(application)
        AnalyticsManager.init(application)
        NotificationManager.init(application)
        FirebaseIntegrationManager.init(application)

        viewModelScope.launch {
            // Seed Database
            repository.seedDatabaseIfEmpty()
            // Simulating a Splash Screen delay of 2 seconds
            delay(2000)
            // Auto-login as the demo consumer profile so the customer page is shown directly.
            // The Super Admin Portal is still accessible via Profile -> Super Admin Controls.
            val profile = repository.getProfile("consumer_arjun")
            if (profile != null) {
                _currentUser.value = profile
                _selectedCityId.value = profile.cityId
                val cityObj = repository.getAllCitiesSync().firstOrNull { it.id == profile.cityId }
                _selectedCityName.value = if (cityObj != null) "${cityObj.name}, ${cityObj.state}" else "Hyderabad, Telangana"
                _currentScreen.value = Screen.Home
            } else {
                _currentScreen.value = Screen.Auth
            }
        }
    }

    // Navigation Methods
    fun navigateTo(screen: Screen) {
        backstack.add(_currentScreen.value)
        _currentScreen.value = screen
        
        // Dynamic event track via PostHog
        AnalyticsManager.trackEvent("screen_viewed", mapOf("screen_name" to screen.javaClass.simpleName))
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

    fun updateShopServices(shopId: String, grooming: Boolean, vet: Boolean) {
        viewModelScope.launch {
            val shop = repository.getShopById(shopId) ?: return@launch
            val updated = shop.copy(groomingEnabled = grooming, vetClinicEnabled = vet)
            repository.insertShop(updated)
            _merchantShop.value = updated
        }
    }

    fun updateShopEnabledStatus(shopId: String, enabled: Boolean) {
        viewModelScope.launch {
            val shop = repository.getShopById(shopId) ?: return@launch
            val updated = shop.copy(shopEnabled = enabled)
            repository.insertShop(updated)
            _merchantShop.value = updated
        }
    }

    fun formatPhoneNumber(phone: String): String {
        val clean = phone.replace(Regex("[^0-9]"), "")
        return if (clean.length == 10) {
            "79$clean"
        } else if (clean.length == 12 && clean.startsWith("79")) {
            clean
        } else if (clean.length > 10) {
            "79" + clean.takeLast(10)
        } else {
            "79" + clean.padStart(10, '0').takeLast(10)
        }
    }

    // Auth Actions
    fun loginWithPhone(
        phone: String,
        pin: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val formattedPhone = formatPhoneNumber(phone)
            
            // ⚠️  SECURITY WARNING: REMOVE BEFORE PRODUCTION DEPLOYMENT ⚠️
            // These hardcoded phone numbers grant role-based access with ZERO authentication.
            // Replace with real Firebase Phone Auth OTP or Supabase OTP verification.
            // In production: roles must be assigned server-side and never trusted from local DB.
            if (ProductionConfig.IS_DEMO_MODE && (phone == "9876543210" || phone == "8765432109" || phone == "9999999999" || phone == "7777777777")) {
                val isMerchant = (phone == "8765432109")
                val isAdmin = (phone == "9999999999")
                val isCaptain = (phone == "7777777777")
                val userId = if (isAdmin) "admin_super" else if (isMerchant) "merchant_hyd_1" else if (isCaptain) "captain_ramesh" else "consumer_arjun"
                var defaultProfile = repository.getProfile(userId)
                if (defaultProfile == null) {
                    defaultProfile = ProfileEntity(
                        id = userId,
                        fullName = if (isAdmin) "Super Admin" else if (isMerchant) "Suresh Kumar" else if (isCaptain) "Ramesh Kumar" else "Arjun",
                        phone = formattedPhone,
                        cityId = "hyd",
                        avatarUrl = if (isAdmin) 
                            "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&auto=format&fit=crop"
                            else if (isMerchant) 
                            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop"
                            else if (isCaptain)
                            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100&auto=format&fit=crop"
                            else "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop",
                        role = if (isAdmin) "superadmin" else if (isMerchant) "merchant" else if (isCaptain) "captain" else "consumer",
                        password = if (isAdmin) "0000" else if (isMerchant) "5678" else if (isCaptain) "9999" else "1234",
                        address = "Villa 42, Road No 5, Banjara Hills, Hyderabad"
                    )
                    repository.insertProfile(defaultProfile)
                    
                    if (isCaptain) {
                        // Also insert matching Captain record in SQLite
                        val defaultCaptain = CaptainEntity(
                            id = "capt_default_1",
                            userId = userId,
                            fullName = "Ramesh Kumar",
                            phone = formattedPhone,
                            vehicleNumber = "TS-09-EA-9999",
                            panCard = "ABCDE1234F",
                            bankDetails = "SBIN0001234 - 10020030045",
                            aadharNumber = "123456789012",
                            panCardUrl = "https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?w=400",
                            aadharCardUrl = "https://images.unsplash.com/photo-1589758438368-0ad531db3366?w=400",
                            licenseUrl = "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=400",
                            selfieUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                            status = "approved",
                            isActive = true
                        )
                        repository.insertCaptain(defaultCaptain)
                    }
                }
            }
            
            val profile = MonitoringManager.measureQuery("getProfileByPhone") {
                repository.getProfileByPhone(formattedPhone)
            }
            if (profile == null) {
                onError("Account not found! Please register first as a Customer or Shop.")
                return@launch
            }

            if (profile.password != null && profile.password != pin) {
                onError("Incorrect 4-digit PIN password!")
                return@launch
            }
            
            MonitoringManager.logAuthEvent(formattedPhone, "login_success")
            _currentUser.value = profile
            _selectedCityId.value = profile.cityId
            val cityObj = repository.getAllCitiesSync().firstOrNull { it.id == profile.cityId }
            _selectedCityName.value = if (cityObj != null) "${cityObj.name}, ${cityObj.state}" else "Hyderabad, Telangana"
            
            // PostHog analytics integration
            AnalyticsManager.identifyUser(profile.id, mapOf("name" to profile.fullName, "role" to profile.role, "phone" to profile.phone))
            AnalyticsManager.trackEvent("user_login", mapOf("user_id" to profile.id, "role" to profile.role))

            if (profile.role == "merchant") {
                val shop = repository.getShopByOwnerId(profile.id)
                _merchantShop.value = shop
                if (shop == null) {
                    _currentScreen.value = Screen.MerchantShopSetup
                } else {
                    _currentScreen.value = Screen.MerchantDashboard
                }
            } else if (profile.role == "superadmin" || profile.role == "admin") {
                _currentScreen.value = Screen.SuperAdmin
            } else {
                _currentScreen.value = Screen.LocationSelect
            }
            onSuccess()
        }
    }

    fun registerWithPhone(
        fullName: String,
        phone: String,
        role: String, // "consumer" or "merchant"
        petName: String = "",
        petBreed: String = "",
        petAge: String = "",
        petWeight: String = "",
        avatarUrl: String = "",
        email: String = "",
        password: String = "",
        cityId: String = "hyd",
        address: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val formattedPhone = formatPhoneNumber(phone)
            
            // Check if phone number is already registered
            val existing = MonitoringManager.measureQuery("getProfileByPhone") {
                repository.getProfileByPhone(formattedPhone)
            }
            if (existing != null) {
                onError("Phone number is already registered! Please log in instead.")
                return@launch
            }
            
            val userId = UUID.randomUUID().toString()
            val finalAvatar = if (role == "merchant") 
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop"
            else 
                avatarUrl.ifBlank { "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=150" }
            
            val profile = ProfileEntity(
                id = userId,
                fullName = fullName.trim(),
                phone = formattedPhone,
                cityId = cityId,
                avatarUrl = finalAvatar,
                role = role,
                petName = if (role == "consumer") petName.trim() else "",
                email = email.trim().lowercase().ifBlank { null },
                password = password.ifBlank { null },
                address = address.trim()
            )
            
            MonitoringManager.measureQuery("insertProfile") {
                repository.insertProfile(profile)
            }

            // Insert matching PetEntity for the customer's pet
            if (role == "consumer" && petName.trim().isNotBlank()) {
                val pet = PetEntity(
                    id = "pet_" + UUID.randomUUID().toString().take(8),
                    ownerId = userId,
                    name = petName.trim(),
                    breed = petBreed.trim().ifBlank { "Golden Retriever" },
                    ageText = petAge.trim().ifBlank { "2 years" },
                    weight = petWeight.trim().ifBlank { "24 kg" },
                    avatarUrl = finalAvatar, // Same selfie with dog
                    allergies = "No wheat grains",
                    vaccineRecord = "Rabies vaccine (2025-08-20)",
                    dewormingDate = "2026-05-15",
                    vaccineDueDate = "2026-08-20"
                )
                repository.insertPet(pet)
            }
            
            MonitoringManager.logAuthEvent(formattedPhone, "register_success")
            _currentUser.value = profile
            _selectedCityId.value = profile.cityId
            _selectedCityName.value = "Hyderabad, Telangana"
            
            // PostHog registration analytics integration
            AnalyticsManager.identifyUser(profile.id, mapOf("name" to profile.fullName, "role" to profile.role, "phone" to profile.phone))
            AnalyticsManager.trackEvent("user_registration", mapOf("user_id" to profile.id, "role" to profile.role))

            if (role == "merchant") {
                _merchantShop.value = null
                _currentScreen.value = Screen.MerchantShopSetup
            } else {
                _currentScreen.value = Screen.LocationSelect
            }
            onSuccess()
        }
    }

    fun loginWithEmailOrPhoneAndPassword(
        identifier: String,
        passwordText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanIdentifier = identifier.trim()
            if (cleanIdentifier.isEmpty() || passwordText.isEmpty()) {
                onError("Please fill in both identifier and password!")
                return@launch
            }

            // Find profile by email or phone
            val profile = if (cleanIdentifier.contains("@")) {
                repository.getProfileByEmail(cleanIdentifier.lowercase())
            } else {
                val formattedPhone = formatPhoneNumber(cleanIdentifier)
                repository.getProfileByPhone(formattedPhone)
            }

            // Hardcoded fallback for Super Admin to ensure it always works
            if (profile == null && cleanIdentifier.lowercase() == "trinadhbandapalli@gmail.com" && passwordText == "thrinnadhh@Paws") {
                val superAdmin = ProfileEntity(
                    id = "admin_super",
                    fullName = "Super Admin",
                    phone = "79999999999",
                    cityId = "hyd",
                    avatarUrl = "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&auto=format&fit=crop",
                    role = "superadmin",
                    email = "trinadhbandapalli@gmail.com",
                    password = "thrinnadhh@Paws"
                )
                repository.insertProfile(superAdmin)
                
                _currentUser.value = superAdmin
                _selectedCityId.value = "hyd"
                _selectedCityName.value = "Hyderabad, Telangana"
                _currentScreen.value = Screen.SuperAdmin
                onSuccess()
                return@launch
            }

            if (profile == null) {
                onError("Account not found! Please check your credentials.")
                return@launch
            }

            // Verify password
            val expectedPassword = if (profile.id == "admin_super") {
                "thrinnadhh@Paws"
            } else {
                profile.password ?: ""
            }

            if (passwordText != expectedPassword) {
                onError("Incorrect password! Please try again or use Phone OTP login.")
                return@launch
            }

            // Login success
            _currentUser.value = profile
            _selectedCityId.value = profile.cityId
            val cityObj = repository.getAllCitiesSync().firstOrNull { it.id == profile.cityId }
            _selectedCityName.value = if (cityObj != null) "${cityObj.name}, ${cityObj.state}" else "Hyderabad, Telangana"

            AnalyticsManager.identifyUser(profile.id, mapOf("name" to profile.fullName, "role" to profile.role, "phone" to profile.phone))
            AnalyticsManager.trackEvent("user_login", mapOf("user_id" to profile.id, "role" to profile.role))

            if (profile.role == "superadmin" || profile.role == "admin") {
                _currentScreen.value = Screen.SuperAdmin
            } else if (profile.role == "merchant") {
                val shop = repository.getShopByOwnerId(profile.id)
                _merchantShop.value = shop
                if (shop == null) {
                    _currentScreen.value = Screen.MerchantShopSetup
                } else {
                    _currentScreen.value = Screen.MerchantDashboard
                }
            } else {
                _currentScreen.value = Screen.LocationSelect
            }
            onSuccess()
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

    // GPS Location Auto-Detection with Rollout Check
    suspend fun detectLocationAndCheckService(lat: Double, lng: Double): LocationResult {
        val allCities = repository.getAllCitiesSync()
        if (allCities.isEmpty()) {
            return LocationResult.Error("No cities configured in the database.")
        }

        // Try to reverse geocode using Android's built-in Geocoder
        var geocodedCityName: String? = null
        try {
            val geocoder = Geocoder(getApplication(), Locale.getDefault())
            val addresses = withContext(Dispatchers.IO) {
                // geocoder.getFromLocation can block, so we run on IO dispatcher
                geocoder.getFromLocation(lat, lng, 1)
            }
            val address = addresses?.firstOrNull()
            if (address != null) {
                // locality is typically the city name, subAdminArea as fallback
                geocodedCityName = address.locality ?: address.subAdminArea
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check if the geocoded city name matches any city in our database (case-insensitive)
        var matchedCity: CityEntity? = null
        if (!geocodedCityName.isNullOrBlank()) {
            matchedCity = allCities.firstOrNull { city ->
                city.name.equals(geocodedCityName, ignoreCase = true) ||
                geocodedCityName.contains(city.name, ignoreCase = true) ||
                city.name.contains(geocodedCityName, ignoreCase = true)
            }
        }

        if (matchedCity != null) {
            val distance = calculateDistanceInKm(lat, lng, matchedCity.lat, matchedCity.lng)
            if (!matchedCity.isActive) {
                return LocationResult.NotServiceable(matchedCity, distance)
            }
            // If active and they are actually in/near it, return Serviceable
            // (e.g., within a reasonable boundary like 100km of the matched center)
            if (distance <= 100.0f) {
                return LocationResult.Serviceable(matchedCity, distance)
            }
        }
        
        // Fallback to closest city by distance calculation
        var closestCity: CityEntity? = null
        var minDistance = Float.MAX_VALUE
        
        for (city in allCities) {
            val distance = calculateDistanceInKm(lat, lng, city.lat, city.lng)
            if (distance < minDistance) {
                minDistance = distance
                closestCity = city
            }
        }
        
        val city = closestCity ?: return LocationResult.Error("Unable to find closest city.")
        
        // We set 50 km as serviceable radius limit
        if (minDistance > 50.0f) {
            return LocationResult.NotServiceable(city, minDistance)
        }
        
        if (!city.isActive) {
            return LocationResult.NotServiceable(city, minDistance)
        }
        
        return LocationResult.Serviceable(city, minDistance)
    }

    private fun calculateDistanceInKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        try {
            android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
            return results[0] / 1000f
        } catch (e: Exception) {
            // Mathematical fallback (Haversine approximation)
            val earthRadius = 6371.0 // in km
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLng / 2) * Math.sin(dLng / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            return (earthRadius * c).toFloat()
        }
    }

    @SuppressLint("MissingPermission")
    fun detectLocation(context: Context, onResult: (LocationResult) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModelScope.launch {
                        val result = detectLocationAndCheckService(location.latitude, location.longitude)
                        onResult(result)
                    }
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        viewModelScope.launch {
                            if (lastLoc != null) {
                                val result = detectLocationAndCheckService(lastLoc.latitude, lastLoc.longitude)
                                onResult(result)
                            } else {
                                onResult(LocationResult.Error("GPS signal unavailable. Please try GPS simulation."))
                            }
                        }
                    }.addOnFailureListener {
                        onResult(LocationResult.Error("Failed to retrieve GPS location."))
                    }
                }
            }
            .addOnFailureListener {
                onResult(LocationResult.Error("Failed to request GPS location."))
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
    fun toggleSelectedCategory(categoryId: String) {
        val current = _selectedCategoryIds.value
        _selectedCategoryIds.value = if (categoryId in current) {
            current - categoryId
        } else {
            current + categoryId
        }
    }
    fun clearSelectedCategories() {
        _selectedCategoryIds.value = emptySet()
    }
    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun setSearchTab(tab: String) { _searchTab.value = tab }
    fun setFilterBrand(brand: String) { _selectedFilterBrand.value = brand }
    fun setFilterLifeStage(stage: String) { _selectedFilterLifeStage.value = stage }
    fun setFilterTag(tag: String) { _selectedFilterTag.value = tag }
    fun resetAllFilters() {
        _selectedFilterBrand.value = "All"
        _selectedFilterLifeStage.value = "All"
        _selectedFilterTag.value = "All"
        _selectedCategoryIds.value = emptySet()
        _searchQuery.value = ""
    }

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

            val deliveryFee = if (deliveryType == "delivery") deliveryFeeTier.value else 0.0
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

            // PostHog order tracking analytics
            AnalyticsManager.trackEvent("order_placed", mapOf(
                "order_id" to orderId,
                "amount" to totalAmount,
                "type" to deliveryType
            ))

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
                NotificationManager.fireInstantNotification(
                    getApplication(),
                    "Order Accepted 🐾",
                    "Your hyperlocal order has been accepted by the shop!"
                )
            }
            delay(12000)
            currentOrd = repository.getOrderById(orderId)
            if (currentOrd?.status == "accepted") {
                repository.updateOrderStatus(orderId, "preparing")
                _activeOrder.value = repository.getOrderById(orderId)
                NotificationManager.fireInstantNotification(
                    getApplication(),
                    "Order Preparing 📦",
                    "The merchant boutique is packaging your pet products."
                )
            }
            delay(15000)
            currentOrd = repository.getOrderById(orderId)
            if (currentOrd?.status == "preparing") {
                repository.updateOrderStatus(orderId, "out_for_delivery")
                _activeOrder.value = repository.getOrderById(orderId)
                NotificationManager.fireInstantNotification(
                    getApplication(),
                    "Out for Delivery 🚀",
                    "The Swiggy Paws rider has picked up your supplies and is on the way!"
                )
            }
            delay(15000)
            currentOrd = repository.getOrderById(orderId)
            if (currentOrd?.status == "out_for_delivery") {
                repository.updateOrderStatus(orderId, "delivered")
                _activeOrder.value = repository.getOrderById(orderId)
                NotificationManager.fireInstantNotification(
                    getApplication(),
                    "Order Delivered 🎉",
                    "Your pet supplies have been safely dropped off. Enjoy!"
                )
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
                phone = formatPhoneNumber(phone),
                email = email,
                photos = listOf("https://images.unsplash.com/photo-1541599540903-216a46ca1da0?w=600&auto=format&fit=crop&q=80"), // Placeholder
                isOpen = true,
                opensAt = opensAt,
                closesAt = closesAt,
                rating = 5.0,
                totalReviews = 0,
                deliveryAvailable = deliveryAvailable,
                isVerified = true,
                isActive = true,
                status = "pending"
            )
            MonitoringManager.measureQuery("insertShop") {
                repository.insertShop(newShop)
            }
            _merchantShop.value = newShop
            SupabaseManager.insertShopToCloud(newShop)
            
            // Seed 3 basic items for the merchant's store so they have full visual coverage
            val items = listOf(
                ProductEntity(UUID.randomUUID().toString(), shopId, "cat_food", "Healthy Puppy Starter Pack", "Protein rich initial feeding kit", 499.0, 599.0, photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=200"), inStock = true, brand = "Pedigree", lifeStage = "Puppy", stockCount = 10),
                ProductEntity(UUID.randomUUID().toString(), shopId, "cat_groom", "Lavender Refreshing Wash", "Gentle shampoo soothing sensitive skin", 350.0, 450.0, photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=200"), inStock = true, brand = "Generic", lifeStage = "Adult", stockCount = 5),
                ProductEntity(UUID.randomUUID().toString(), shopId, "cat_access", "Ultra Strong Nylon Leash", "Heavy duty durable daily walker harness", 299.0, 399.0, photos = listOf("https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=200"), inStock = true, brand = "Generic", lifeStage = "Adult", stockCount = 12)
            )
            MonitoringManager.measureQuery("seedMerchantProducts") {
                items.forEach { repository.insertProduct(it) }
            }
            items.forEach { SupabaseManager.insertProductToCloud(it) }

            // Seed default services for the merchant's shop so consultation slots and grooming packages are active by default
            val defaultServices = mutableListOf<ServiceEntity>()
            if (newShop.vetClinicEnabled) {
                defaultServices.add(ServiceEntity(id = "service_${shopId}_vet_1", shopId = shopId, name = "Emergency Surgery Consultation", price = 1200.0, category = "Vet Doctor Clinic"))
                defaultServices.add(ServiceEntity(id = "service_${shopId}_vet_2", shopId = shopId, name = "General OPD Consultation", price = 600.0, category = "Vet Doctor Clinic"))
                defaultServices.add(ServiceEntity(id = "service_${shopId}_vet_3", shopId = shopId, name = "In-house Lab Diagnostics Checkup", price = 1500.0, category = "Vet Doctor Clinic"))
            }
            if (newShop.groomingEnabled) {
                defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_1", shopId = shopId, name = "Teddy Bear Coat Styling", price = 999.0, category = "Grooming"))
                defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_2", shopId = shopId, name = "Kennel Summer Short Cut", price = 799.0, category = "Grooming"))
                defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_3", shopId = shopId, name = "Majestic Lion Pom Styling", price = 1499.0, category = "Grooming"))
                defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_4", shopId = shopId, name = "Oatmeal Soothing Bath", price = 499.0, category = "Bathing"))
                defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_5", shopId = shopId, name = "Anti-Tick & Flea Medicated Wash", price = 699.0, category = "Bathing"))
                defaultServices.add(ServiceEntity(id = "service_${shopId}_groom_6", shopId = shopId, name = "Premium Foam Aroma Spa Bath", price = 899.0, category = "Bathing"))
            }
            if (defaultServices.isNotEmpty()) {
                MonitoringManager.measureQuery("seedMerchantServices") {
                    defaultServices.forEach { repository.insertService(it) }
                }
                defaultServices.forEach { SupabaseManager.insertServiceToCloud(it) }
            }

            // Trigger external Zapier/Make webhooks to onboard the merchant to Google Sheets and post Slack alert
            SupabaseManager.triggerZapierMerchantOnboardingWebhook(
                shopName = newShop.name,
                ownerName = user.fullName,
                phone = newShop.phone,
                city = cityName
            )

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
                isActive = true,
                brand = "Generic",
                lifeStage = "Adult",
                stockCount = 10
            )
            repository.insertProduct(newProd)
        }
    }

    fun deleteMerchantProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProductById(productId)
        }
    }

    fun updateProductStock(productId: String, newStock: Int) {
        viewModelScope.launch {
            repository.updateProductStock(productId, newStock)
            val prod = repository.getProductById(productId)
            if (prod != null) {
                repository.insertProduct(prod.copy(stockCount = newStock, inStock = newStock > 0))
            }
            // Trigger refresh of merchant products by updating merchant shop reference flow
            _merchantShop.value = _merchantShop.value
        }
    }

    fun approveShop(shopId: String) {
        viewModelScope.launch {
            repository.updateShopApprovalStatus(shopId, "active")
            // Fetch the updated shop from db to sync ViewModels
            val shop = repository.getShopById(shopId)
            if (_merchantShop.value?.id == shopId) {
                _merchantShop.value = shop
            }
        }
    }

    fun declineShop(shopId: String) {
        viewModelScope.launch {
            repository.updateShopApprovalStatus(shopId, "declined")
            val shop = repository.getShopById(shopId)
            if (_merchantShop.value?.id == shopId) {
                _merchantShop.value = shop
            }
        }
    }

    fun approveCaptain(captainId: String) {
        viewModelScope.launch {
            repository.updateCaptainStatus(captainId, "approved", true)
        }
    }

    fun declineCaptain(captainId: String) {
        viewModelScope.launch {
            repository.updateCaptainStatus(captainId, "rejected", false)
        }
    }

    fun toggleCaptainOnlineStatus(captainId: String, isOnline: Boolean) {
        viewModelScope.launch {
            val captain = repository.getCaptainById(captainId) ?: return@launch
            repository.updateCaptainStatus(captainId, captain.status, isOnline)
        }
    }

    fun acceptDeliveryJob(orderId: String, captainId: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, "preparing", captainId)
            if (_activeOrder.value?.id == orderId) {
                _activeOrder.value = repository.getOrderById(orderId)
            }
        }
    }

    fun completeDeliveryJob(orderId: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, "delivered")
            if (_activeOrder.value?.id == orderId) {
                _activeOrder.value = repository.getOrderById(orderId)
            }
        }
    }

    fun registerCaptain(
        fullName: String,
        phone: String,
        vehicleNumber: String,
        panCard: String,
        bankDetails: String,
        aadharNumber: String,
        panCardUrl: String,
        aadharCardUrl: String,
        licenseUrl: String,
        selfieUrl: String,
        cityId: String = "hyd",
        address: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val formattedPhone = formatPhoneNumber(phone)
            
            // Check if phone number is already registered
            val existing = repository.getProfileByPhone(formattedPhone)
            if (existing != null) {
                onError("Phone number is already registered! Please log in instead.")
                return@launch
            }
            
            val userId = UUID.randomUUID().toString()
            val profile = ProfileEntity(
                id = userId,
                fullName = fullName.trim(),
                phone = formattedPhone,
                cityId = cityId,
                avatarUrl = selfieUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200" }, // Use Captain selfie as profile pic!
                role = "captain",
                address = address.trim()
            )
            
            repository.insertProfile(profile)
            
            val captain = CaptainEntity(
                id = "capt_" + UUID.randomUUID().toString().substring(0, 8),
                userId = userId,
                fullName = fullName.trim(),
                phone = formattedPhone,
                vehicleNumber = vehicleNumber.trim(),
                panCard = panCard.trim(),
                bankDetails = bankDetails.trim(),
                aadharNumber = aadharNumber.trim(),
                panCardUrl = panCardUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?w=400" },
                aadharCardUrl = aadharCardUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1589758438368-0ad531db3366?w=400" },
                licenseUrl = licenseUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=400" },
                selfieUrl = selfieUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200" },
                status = "pending",
                isActive = false
            )
            
            repository.insertCaptain(captain)
            
            _currentUser.value = profile
            _selectedCityId.value = profile.cityId
            _selectedCityName.value = "Hyderabad, Telangana"
            
            _currentScreen.value = Screen.LocationSelect // go to city selector
            onSuccess()
        }
    }

    // Product Wishlist Toggle
    fun toggleProductWishlist(productId: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val currentList = wishlistProducts.value
            val existing = currentList.find { it.productId == productId }
            if (existing != null) {
                repository.deleteWishlistProduct(user.id, productId)
            } else {
                repository.insertWishlistProduct(
                    WishlistProductEntity(
                        id = UUID.randomUUID().toString(),
                        consumerId = user.id,
                        productId = productId
                    )
                )
            }
        }
    }

    // Chats Management
    fun selectActiveChat(shopId: String) {
        _activeChatShopId.value = shopId
        _currentUser.value?.let { user ->
            viewModelScope.launch {
                repository.markMessagesAsRead(shopId, user.id)
            }
        }
    }

    fun sendChatMessage(shopId: String, text: String, senderName: String) {
        val user = _currentUser.value ?: return
        if (text.trim().isEmpty()) return
        viewModelScope.launch {
            val messageId = UUID.randomUUID().toString()
            val chatMsg = ChatMessageEntity(
                id = messageId,
                senderId = user.id,
                recipientId = shopId,
                shopId = shopId,
                message = text.trim(),
                senderName = senderName,
                timestamp = System.currentTimeMillis(),
                isRead = true
            )
            repository.insertChatMessage(chatMsg)
            
            // Trigger automated bot reply
            triggerAutomatedReply(shopId, text)
        }
    }

    private fun triggerAutomatedReply(shopId: String, customerMessage: String) {
        viewModelScope.launch {
            delay(1000) // 1-second delay for realistic feel
            
            val shop = repository.getShopById(shopId) ?: return@launch
            val replyMessage = when {
                customerMessage.contains("stock", ignoreCase = true) || customerMessage.contains("available", ignoreCase = true) -> {
                    "Hi! Yes, our catalog stock is synchronized in real-time. You can adjust quantities and place orders directly. Anything else you are looking for?"
                }
                customerMessage.contains("groom", ignoreCase = true) || customerMessage.contains("spa", ignoreCase = true) || customerMessage.contains("service", ignoreCase = true) -> {
                    "Hello! We offer premium dog grooming and organic oatmeal bath services. Slots can be booked by calling us at ${shop.phone} or visiting road/street details listed in the app!"
                }
                customerMessage.contains("discount", ignoreCase = true) || customerMessage.contains("offer", ignoreCase = true) || customerMessage.contains("deal", ignoreCase = true) -> {
                    "Woof! Yes, please check the featured banner promotions on the Explorer dashboard for our latest city discounts!"
                }
                else -> {
                    "Thanks for messaging ${shop.name}! 🐾 We are open today until ${shop.closesAt}. Let us know if you need any products delivered to your door!"
                }
            }
            
            repository.insertChatMessage(
                ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    senderId = shopId,
                    recipientId = _currentUser.value?.id ?: "consumer_arjun",
                    shopId = shopId,
                    message = replyMessage,
                    senderName = shop.name,
                    timestamp = System.currentTimeMillis(),
                    isRead = false
                )
            )
        }
    }

    // Super Admin Banner Controls
    fun createBanner(
        title: String,
        description: String,
        imageUrl: String,
        targetCityIds: List<String>,
        targetShopIds: List<String>
    ) {
        viewModelScope.launch {
            val banner = BannerEntity(
                id = UUID.randomUUID().toString(),
                imageUrl = imageUrl.trim(),
                title = title.trim(),
                description = description.trim(),
                targetCityIds = targetCityIds,
                targetShopIds = targetShopIds,
                isActive = true,
                createdAt = System.currentTimeMillis()
            )
            repository.insertBanner(banner)
        }
    }

    fun deleteBanner(bannerId: String) {
        viewModelScope.launch {
            repository.deleteBanner(bannerId)
        }
    }

    fun getAppointmentsForShopFlow(shopId: String): Flow<List<AppointmentEntity>> {
        return repository.getAppointmentsForShopFlow(shopId)
    }

    // Appointments Booking Actions
    fun bookAppointment(
        shopId: String,
        serviceId: String,
        serviceName: String,
        price: Double,
        date: String,
        time: String,
        petName: String
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val appt = AppointmentEntity(
                id = "appt_" + UUID.randomUUID().toString().substring(0, 8),
                consumerId = user.id,
                shopId = shopId,
                serviceId = serviceId,
                serviceName = serviceName,
                price = price,
                appointmentDate = date,
                appointmentTime = time,
                petName = petName.trim().ifEmpty { "Buddy" },
                status = "pending",
                createdAt = System.currentTimeMillis()
            )
            repository.insertAppointment(appt)
        }
    }

    // Merchant Service Management Actions
    fun addMerchantService(name: String, price: Double, category: String, isCustom: Boolean = false) {
        val shop = _merchantShop.value ?: return
        viewModelScope.launch {
            val service = ServiceEntity(
                id = "serv_" + UUID.randomUUID().toString().substring(0, 8),
                shopId = shop.id,
                name = name.trim(),
                price = price,
                category = category,
                isCustom = isCustom,
                createdAt = System.currentTimeMillis()
            )
            repository.insertService(service)
        }
    }

    fun deleteMerchantService(serviceId: String) {
        viewModelScope.launch {
            repository.deleteService(serviceId)
        }
    }

    fun getServicesFlow(shopId: String): Flow<List<ServiceEntity>> {
        return repository.getServicesForShopFlow(shopId)
    }

    fun updateAppointmentStatus(appointmentId: String, status: String) {
        viewModelScope.launch {
            repository.updateAppointmentStatus(appointmentId, status)
        }
    }

    fun rescheduleAppointment(appointment: AppointmentEntity, newDate: String, newTime: String) {
        viewModelScope.launch {
            val updated = appointment.copy(
                appointmentDate = newDate,
                appointmentTime = newTime,
                status = "pending"
            )
            repository.insertAppointment(updated)
        }
    }

    // Date Reminder Actions
    fun createReminder(title: String, petName: String, dateString: String, notes: String, type: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val reminder = ReminderEntity(
                id = "rem_" + UUID.randomUUID().toString().substring(0, 8),
                consumerId = user.id,
                title = title.trim(),
                petName = petName.trim().ifEmpty { "Buddy" },
                dateString = dateString.trim(),
                notes = notes.trim(),
                isCompleted = false,
                type = type,
                createdAt = System.currentTimeMillis()
            )
            repository.insertReminder(reminder)
        }
    }

    fun toggleReminderCompletion(reminderId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateReminderCompletion(reminderId, isCompleted)
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            repository.deleteReminder(reminderId)
        }
    }

    // Product Specs Administrative Functions
    fun getSpecsForProduct(productId: String): Flow<List<ProductSpecEntity>> {
        return repository.getSpecsForProductFlow(productId)
    }

    fun addOrUpdateProductSpec(
        productId: String,
        weightText: String,
        petCategory: String,
        imageUrls: List<String>,
        desc1: String,
        desc2: String,
        desc3: String,
        desc4: String
    ) {
        viewModelScope.launch {
            val specId = "spec_" + UUID.randomUUID().toString().substring(0, 8)
            val spec = ProductSpecEntity(
                id = specId,
                productId = productId,
                weightText = weightText,
                petCategory = petCategory,
                imageUrls = imageUrls,
                description1 = desc1,
                description2 = desc2,
                description3 = desc3,
                description4 = desc4
            )
            repository.insertProductSpec(spec)
        }
    }

    fun deleteProductSpec(specId: String) {
        viewModelScope.launch {
            repository.deleteProductSpec(specId)
        }
    }

    // Dynamic Catalog Administrative Functions
    fun createCategory(id: String, name: String, iconUrl: String) {
        viewModelScope.launch {
            val category = CategoryEntity(id = id, name = name, iconUrl = iconUrl)
            repository.insertCategory(category)
        }
    }

    fun createProduct(
        id: String,
        shopId: String,
        categoryId: String,
        name: String,
        description: String,
        price: Double,
        mrp: Double,
        photos: List<String>,
        brand: String,
        lifeStage: String,
        stockCount: Int
    ) {
        viewModelScope.launch {
            val product = ProductEntity(
                id = id,
                shopId = shopId,
                categoryId = categoryId,
                name = name,
                description = description,
                price = price,
                mrp = mrp,
                photos = photos,
                inStock = stockCount > 0,
                isActive = true,
                tags = listOf(name.lowercase(), brand.lowercase()),
                brand = brand,
                lifeStage = lifeStage,
                stockCount = stockCount
            )
            repository.insertProduct(product)
        }
    }

    // Pet Profile CRUD Operations
    fun addOrUpdatePet(
        id: String?,
        name: String,
        breed: String,
        ageText: String,
        weight: String,
        avatarUrl: String,
        allergies: String,
        vaccineRecord: String,
        dewormingDate: String,
        vaccineDueDate: String
    ) {
        viewModelScope.launch {
            val owner = _currentUser.value ?: return@launch
            val petId = id ?: ("pet_" + UUID.randomUUID().toString().substring(0, 8))
            val pet = PetEntity(
                id = petId,
                ownerId = owner.id,
                name = name,
                breed = breed,
                ageText = ageText,
                weight = weight,
                avatarUrl = avatarUrl.ifBlank { "https://images.unsplash.com/photo-1543466835-00a7907e9de1?w=400" }, // Default cute dog avatar
                allergies = allergies,
                vaccineRecord = vaccineRecord,
                dewormingDate = dewormingDate,
                vaccineDueDate = vaccineDueDate
            )
            repository.insertPet(pet)
        }
    }

    fun deletePet(id: String) {
        viewModelScope.launch {
            repository.deletePet(id)
        }
    }

    // Dynamic Remedies & Targeted Pet Problem Recommendations
    fun addPetProblem(title: String, description: String, solution: String, howToUse: String, emoji: String, productIds: List<String>) {
        viewModelScope.launch {
            val problem = ProblemEntity(
                id = "prob_" + UUID.randomUUID().toString().substring(0, 8),
                title = title.trim(),
                description = description.trim(),
                solution = solution.trim(),
                howToUse = howToUse.trim(),
                emoji = emoji.trim().ifEmpty { "🩺" },
                productIds = productIds,
                createdAt = System.currentTimeMillis()
            )
            repository.insertProblem(problem)
        }
    }

    fun deletePetProblem(id: String) {
        viewModelScope.launch {
            repository.deleteProblemById(id)
        }
    }

    // Collaborative B2C Group RFQ & Bidding Auction Logic
    fun createGroupRfqSession(cityId: String) {
        val user = _currentUser.value ?: return
        val sessionId = "RFQ-" + UUID.randomUUID().toString().substring(0, 8).uppercase()
        viewModelScope.launch {
            val session = GroupRfqSessionEntity(
                id = sessionId,
                hostId = user.id,
                cityId = cityId,
                status = "open",
                biddingExpiresAt = System.currentTimeMillis() + 10 * 60 * 1000 // 10 minute bidding window
            )
            repository.insertGroupRfqSession(session)
            _currentRfqSessionId.value = sessionId
        }
    }

    fun joinGroupRfqSession(sessionId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val session = repository.getGroupRfqSessionById(sessionId.trim().uppercase())
            if (session != null) {
                _currentRfqSessionId.value = session.id
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun leaveGroupRfqSession() {
        _currentRfqSessionId.value = null
    }

    fun addRfqMemberItem(productId: String, quantity: Int, address: String, lat: Double, lng: Double) {
        val user = _currentUser.value ?: return
        val sessionId = _currentRfqSessionId.value ?: return
        viewModelScope.launch {
            val item = GroupRfqMemberItemEntity(
                id = "item_" + UUID.randomUUID().toString().substring(0, 8),
                sessionId = sessionId,
                memberId = user.id,
                memberName = user.fullName,
                productId = productId,
                quantity = quantity,
                deliveryAddress = address,
                lat = lat,
                lng = lng
            )
            repository.insertRfqMemberItem(item)
        }
    }

    fun removeRfqMemberItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteRfqMemberItem(itemId)
        }
    }

    fun lockRfqCart() {
        val sessionId = _currentRfqSessionId.value ?: return
        viewModelScope.launch {
            repository.updateGroupRfqSessionStatus(sessionId, "bidding")
            
            if (ProductionConfig.IS_DEMO_MODE) {
                // For verification & demo purposes, wait 1.5 seconds and then auto-seed competitive bids
                // from nearby merchants so the user immediately sees quotes populating without a second device!
                delay(1500)
                val session = repository.getGroupRfqSessionById(sessionId) ?: return@launch
                val memberItems = repository.getRfqMemberItemsForSessionSync(sessionId)
                if (memberItems.isEmpty()) return@launch
                
                var totalSubtotal = 0.0
                for (item in memberItems) {
                    val prod = repository.getProductById(item.productId)
                    if (prod != null) {
                        totalSubtotal += prod.price * item.quantity
                    }
                }
                
                val cityShops = repository.getShopsForCitySync(session.cityId)
                val shop1 = cityShops.firstOrNull() ?: ShopEntity("shop_bid_1", "admin_super", session.cityId, "Super Paws Megastore", "Discount pet food distributor", "123 Central Mall", "Downtown", phone = "9999999999", email = "superpaws@paws.com", rating = 4.8)
                val shop2 = cityShops.getOrNull(1) ?: ShopEntity("shop_bid_2", "admin_super", session.cityId, "Happy Tails Boutique", "Premium organic dog supplies", "456 Park Avenue", "Greenwood", phone = "8888888888", email = "happytails@paws.com", rating = 4.7)
                
                // Insert 12% discount quotation
                val discount1 = 12.0
                val quote1 = MerchantQuotationEntity(
                    id = "quote_" + UUID.randomUUID().toString().substring(0, 8),
                    sessionId = sessionId,
                    shopId = shop1.id,
                    shopName = shop1.name,
                    discountPercentage = discount1,
                    quotedPrice = totalSubtotal * (1 - discount1 / 100.0)
                )
                repository.insertMerchantQuotation(quote1)
                
                delay(1500) // Stagger quotation submission for interactive premium effect
                
                // Insert winning 20% discount quotation!
                val discount2 = 20.0
                val quote2 = MerchantQuotationEntity(
                    id = "quote_" + UUID.randomUUID().toString().substring(0, 8),
                    sessionId = sessionId,
                    shopId = shop2.id,
                    shopName = shop2.name,
                    discountPercentage = discount2,
                    quotedPrice = totalSubtotal * (1 - discount2 / 100.0)
                )
                repository.insertMerchantQuotation(quote2)
            }
        }
    }

    fun submitMerchantQuotation(sessionId: String, shopId: String, shopName: String, discountPercent: Double) {
        viewModelScope.launch {
            val memberItems = repository.getRfqMemberItemsForSessionSync(sessionId)
            var totalSubtotal = 0.0
            for (item in memberItems) {
                val prod = repository.getProductById(item.productId)
                if (prod != null) {
                    totalSubtotal += prod.price * item.quantity
                }
            }
            val quote = MerchantQuotationEntity(
                id = "quote_" + UUID.randomUUID().toString().substring(0, 8),
                sessionId = sessionId,
                shopId = shopId,
                shopName = shopName,
                discountPercentage = discountPercent,
                quotedPrice = totalSubtotal * (1 - discountPercent / 100.0)
            )
            repository.insertMerchantQuotation(quote)
        }
    }

    fun acceptMerchantQuotation(sessionId: String, quotationId: String) {
        viewModelScope.launch {
            repository.acceptGroupRfqQuotation(sessionId, quotationId)
        }
    }

    fun checkoutRfqSession(quotation: MerchantQuotationEntity) {
        val sessionId = _currentRfqSessionId.value ?: return
        viewModelScope.launch {
            val memberItems = repository.getRfqMemberItemsForSessionSync(sessionId)
            if (memberItems.isEmpty()) return@launch
            
            // Group the bulk items by member to create separate orders dispatched to individual homes
            val itemsByMember = memberItems.groupBy { it.memberId }
            
            for ((memberId, items) in itemsByMember) {
                val firstItem = items.first()
                val address = firstItem.deliveryAddress
                
                var subtotal = 0.0
                val orderItemsToInsert = mutableListOf<OrderItemEntity>()
                val orderId = "ord_rfq_" + UUID.randomUUID().toString().substring(0, 8).uppercase()
                
                for (item in items) {
                    val prod = repository.getProductById(item.productId)
                    if (prod != null) {
                        // Apply the accepted discount percentage
                        val discountedPrice = prod.price * (1 - quotation.discountPercentage / 100.0)
                        subtotal += discountedPrice * item.quantity
                        
                        orderItemsToInsert.add(
                            OrderItemEntity(
                                id = "item_" + UUID.randomUUID().toString().substring(0, 8),
                                orderId = orderId,
                                productId = item.productId,
                                quantity = item.quantity,
                                unitPrice = discountedPrice,
                                subtotal = discountedPrice * item.quantity
                            )
                        )
                    }
                }
                
                // Add standard individual delivery charges & platform split charges
                val deliveryFee = deliveryFeeTier.value
                val platformFee = subtotal * (platformCommission.value / 100.0)
                val totalAmount = subtotal + deliveryFee + platformFee
                
                val order = OrderEntity(
                    id = orderId,
                    consumerId = memberId,
                    shopId = quotation.shopId,
                    type = "delivery",
                    status = "accepted", // Accepted instantly by the winning merchant
                    totalAmount = totalAmount,
                    deliveryAddress = address,
                    notes = "Collaborative group order filled by ${quotation.shopName} (Discount: ${quotation.discountPercentage}%)",
                    placedAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                repository.insertOrder(order)
                repository.insertOrderItems(orderItemsToInsert)
            }
            
            // Mark session as complete and clear selection
            repository.updateGroupRfqSessionStatus(sessionId, "completed")
            _currentRfqSessionId.value = null
        }
    }

    fun payMemberShare(sessionId: String, memberId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.markMemberItemsAsPaid(sessionId, memberId)
            onResult(true)
        }
    }

    // --- Public repository accessors for Composables ---

    fun getPetsForOwnerFlow(ownerId: String): kotlinx.coroutines.flow.Flow<List<PetEntity>> =
        repository.getPetsForOwnerFlow(ownerId)

    fun getProfileById(profileId: String, onResult: (ProfileEntity?) -> Unit) {
        viewModelScope.launch {
            val profile = repository.getProfile(profileId)
            onResult(profile)
        }
    }

    // --- Grooming ViewModel Methods ---

    // Flows
    fun getActiveGroomingServicesForShopFlow(shopId: String): Flow<List<GroomingServiceEntity>> =
        repository.getActiveGroomingServicesForShopFlow(shopId)

    fun getAllGroomingServicesForShopFlow(shopId: String): Flow<List<GroomingServiceEntity>> =
        repository.getAllGroomingServicesForShopFlow(shopId)

    fun getGroomingSlotsForShopAndDateFlow(shopId: String, date: String): Flow<List<GroomingSlotEntity>> =
        repository.getGroomingSlotsForShopAndDateFlow(shopId, date)

    fun getGroomingSlotsForDateRangeFlow(shopId: String, startDate: String, endDate: String): kotlinx.coroutines.flow.Flow<List<GroomingSlotEntity>> =
        repository.getGroomingSlotsForDateRangeFlow(shopId, startDate, endDate)

    // Booking flows for consumer & merchant
    val myGroomingBookings: Flow<List<GroomingBookingEntity>> = _currentUser.flatMapLatest { user ->
        if (user != null) repository.getGroomingBookingsForConsumerFlow(user.id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }

    val merchantGroomingBookings: Flow<List<GroomingBookingEntity>> = _merchantShop.flatMapLatest { shop ->
        if (shop != null) repository.getGroomingBookingsForShopFlow(shop.id)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }

    // Actions
    fun getOrGenerateSlotsForDate(shopId: String, date: String, onResult: (List<GroomingSlotEntity>) -> Unit) {
        viewModelScope.launch {
            val slots = repository.getOrGenerateSlotsForDate(shopId, date)
            onResult(slots)
        }
    }

    fun bulkEditSlotCapacity(
        shopId: String,
        startDate: String,
        endDate: String,
        daysOfWeek: List<Int>,
        newCapacity: Int,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.bulkEditSlotCapacity(shopId, startDate, endDate, daysOfWeek, newCapacity)
            onResult(true)
        }
    }

    fun toggleSlotBlocked(slot: GroomingSlotEntity) {
        viewModelScope.launch {
            val updated = slot.copy(isBlocked = !slot.isBlocked)
            repository.insertGroomingSlot(updated)
        }
    }

    fun bookGroomingSlot(
        shopId: String,
        serviceId: String,
        slotId: String,
        petId: String,
        petSizeCategory: String,
        specialInstructions: String?,
        totalPrice: Double,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = _currentUser.value ?: return onError("User not logged in.")
        viewModelScope.launch {
            try {
                val bookingId = "gr_bk_" + java.util.UUID.randomUUID().toString().substring(0, 8)
                val booking = GroomingBookingEntity(
                    id = bookingId,
                    consumerId = user.id,
                    shopId = shopId,
                    serviceId = serviceId,
                    slotId = slotId,
                    petId = petId,
                    petSizeCategory = petSizeCategory,
                    status = "pending",
                    specialInstructions = specialInstructions?.trim()?.takeIf { it.isNotEmpty() },
                    totalPrice = totalPrice,
                    bookedAt = System.currentTimeMillis()
                )
                repository.bookGroomingSlot(booking)
                onSuccess(bookingId)
            } catch (e: Exception) {
                onError(e.message ?: "Booking failed due to slot capacity or network issue.")
            }
        }
    }

    fun cancelGroomingBooking(bookingId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.cancelGroomingBooking(bookingId)
            onResult(true)
        }
    }

    fun updateGroomingBookingStatus(bookingId: String, status: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.updateGroomingBookingStatus(bookingId, status)
            onResult(true)
        }
    }

    fun getGroomingBookingById(bookingId: String, onResult: (GroomingBookingEntity?) -> Unit) {
        viewModelScope.launch {
            val booking = repository.getGroomingBookingById(bookingId)
            onResult(booking)
        }
    }

    fun getGroomingServiceById(serviceId: String, onResult: (GroomingServiceEntity?) -> Unit) {
        viewModelScope.launch {
            val service = repository.getGroomingServiceById(serviceId)
            onResult(service)
        }
    }

    fun saveGroomingService(
        serviceType: String,
        variantName: String,
        description: String,
        petSizeCategory: String,
        price: Double,
        durationMinutes: Int,
        imageUrls: List<String>,
        isActive: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        val shop = _merchantShop.value ?: return
        viewModelScope.launch {
            val id = "gs_" + shop.id + "_" + serviceType.replace("_", "") + "_" + variantName.replace(" ", "").lowercase() + "_" + petSizeCategory
            val service = GroomingServiceEntity(
                id = id,
                shopId = shop.id,
                serviceType = serviceType,
                variantName = variantName,
                description = description,
                petSizeCategory = petSizeCategory,
                price = price,
                durationMinutes = durationMinutes,
                imageUrls = imageUrls,
                isActive = isActive,
                createdAt = System.currentTimeMillis()
            )
            repository.insertGroomingService(service)
            onResult(true)
        }
    }

    fun deleteGroomingService(serviceId: String) {
        viewModelScope.launch {
            repository.deleteGroomingService(serviceId)
        }
    }

    fun updateGroomingService(service: GroomingServiceEntity) {
        viewModelScope.launch {
            repository.insertGroomingService(service)
        }
    }

    // --- Doctor & Hospital Management ---
    fun getDoctorsForShopFlow(shopId: String): Flow<List<DoctorEntity>> =
        repository.getDoctorsForShopFlow(shopId)

    fun getDoctorById(id: String, onResult: (DoctorEntity?) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getDoctorById(id))
        }
    }

    fun getDoctorSlotsFlow(shopId: String, doctorId: String, date: String): Flow<List<DoctorSlotEntity>> =
        repository.getDoctorSlotsFlow(shopId, doctorId, date)

    fun getOrGenerateDoctorSlotsForDate(shopId: String, doctorId: String, date: String, onResult: (List<DoctorSlotEntity>) -> Unit) {
        viewModelScope.launch {
            val slots = repository.getOrGenerateDoctorSlotsForDate(shopId, doctorId, date)
            onResult(slots)
        }
    }

    fun toggleDoctorSlotBlocked(slot: DoctorSlotEntity) {
        viewModelScope.launch {
            repository.toggleDoctorSlotBlocked(slot)
        }
    }

    fun updateDoctorSlotCapacity(slotId: String, capacity: Int) {
        viewModelScope.launch {
            repository.updateDoctorSlotCapacity(slotId, capacity)
        }
    }

    fun saveDoctor(
        id: String?,
        shopId: String,
        name: String,
        photoUrl: String,
        qualification: String,
        specialization: String,
        workingDays: List<String>,
        activeSlots: List<String>,
        isAvailable: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val docId = id ?: ("doc_" + UUID.randomUUID().toString().substring(0, 8))
            val doctor = DoctorEntity(
                id = docId,
                shopId = shopId,
                name = name.trim(),
                photoUrl = photoUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400" },
                qualification = qualification.trim(),
                specialization = specialization.trim(),
                workingDays = workingDays,
                activeSlots = activeSlots,
                isAvailable = isAvailable
            )
            repository.insertDoctor(doctor)
            onResult(true)
        }
    }

    fun deleteDoctor(doctorId: String) {
        viewModelScope.launch {
            repository.deleteDoctor(doctorId)
        }
    }

    fun bookDoctorAppointment(
        shopId: String,
        serviceId: String,
        serviceName: String,
        price: Double,
        date: String,
        time: String,
        petName: String,
        doctorId: String?,
        slotId: String?,
        concern: String = "",
        priority: String = "Normal",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _currentUser.value ?: return onError("User not logged in.")
        viewModelScope.launch {
            try {
                val appt = AppointmentEntity(
                    id = "appt_" + UUID.randomUUID().toString().substring(0, 8),
                    consumerId = user.id,
                    shopId = shopId,
                    serviceId = serviceId,
                    serviceName = serviceName,
                    price = price,
                    appointmentDate = date,
                    appointmentTime = time,
                    petName = petName.trim().ifEmpty { "Buddy" },
                    status = "pending",
                    doctorId = doctorId,
                    createdAt = System.currentTimeMillis(),
                    concern = concern,
                    priority = priority
                )
                if (slotId != null) {
                    repository.bookDoctorAppointment(appt, slotId)
                } else {
                    repository.insertAppointment(appt)
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Booking failed.")
            }
        }
    }

    // --- Rescheduling State Machine & Refunds ---
    fun proposeReschedule(appointment: AppointmentEntity, newDate: String, newTime: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val updated = appointment.copy(
                rescheduleDate = newDate,
                rescheduleTime = newTime,
                status = "reschedule_pending"
            )
            repository.insertAppointment(updated)
            onResult(true)
        }
    }

    fun acceptReschedule(appointment: AppointmentEntity, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val updated = appointment.copy(
                appointmentDate = appointment.rescheduleDate ?: appointment.appointmentDate,
                appointmentTime = appointment.rescheduleTime ?: appointment.appointmentTime,
                rescheduleDate = null,
                rescheduleTime = null,
                status = "confirmed"
            )
            repository.insertAppointment(updated)
            onResult(true)
        }
    }

    fun declineReschedule(appointment: AppointmentEntity, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val updated = appointment.copy(
                rescheduleDate = null,
                rescheduleTime = null,
                status = "cancelled"
            )
            repository.insertAppointment(updated)
            onResult(true)
        }
    }

    fun cancelAppointmentWithRefund(appointment: AppointmentEntity, slotId: String?, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.cancelDoctorAppointment(appointment.id, slotId)
            onResult(true)
        }
    }

    // Grooming Rescheduling Actions
    fun proposeGroomingReschedule(booking: GroomingBookingEntity, newDate: String, newTime: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val updated = booking.copy(
                rescheduleDate = newDate,
                rescheduleTime = newTime,
                status = "reschedule_pending"
            )
            repository.insertGroomingBooking(updated)
            onResult(true)
        }
    }

    fun acceptGroomingReschedule(booking: GroomingBookingEntity, newSlotId: String, newDate: String, newTime: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            database.pawsDao().decrementSlotBookedCount(booking.slotId)
            database.pawsDao().incrementSlotBookedCount(newSlotId)
            val updated = booking.copy(
                slotId = newSlotId,
                rescheduleDate = null,
                rescheduleTime = null,
                status = "confirmed"
            )
            repository.insertGroomingBooking(updated)
            onResult(true)
        }
    }

    fun declineGroomingReschedule(booking: GroomingBookingEntity, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val updated = booking.copy(
                rescheduleDate = null,
                rescheduleTime = null,
                status = "cancelled"
            )
            repository.insertGroomingBooking(updated)
            database.pawsDao().decrementSlotBookedCount(booking.slotId)
            onResult(true)
        }
    }

    // --- Coupon Management ---
    fun getCouponsForShopFlow(shopId: String): Flow<List<CouponEntity>> =
        repository.getCouponsForShopFlow(shopId)

    fun saveCoupon(
        code: String,
        discountPercentage: Double,
        maxDiscount: Double,
        minOrderAmount: Double,
        isActive: Boolean,
        onResult: (Boolean) -> Unit = {}
    ) {
        val shop = _merchantShop.value ?: return
        viewModelScope.launch {
            val coupon = CouponEntity(
                id = "coupon_" + UUID.randomUUID().toString().substring(0, 8),
                shopId = shop.id,
                code = code.trim().uppercase(),
                discountPercentage = discountPercentage,
                maxDiscount = maxDiscount,
                minOrderAmount = minOrderAmount,
                isActive = isActive
            )
            repository.insertCoupon(coupon)
            onResult(true)
        }
    }

    fun deleteCoupon(couponId: String) {
        viewModelScope.launch {
            repository.deleteCoupon(couponId)
        }
    }

    // --- Product free sample attachment ---
    fun attachSampleToProduct(productId: String, sampleProductId: String?, sampleDesc: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val prod = repository.getProductById(productId)
            if (prod != null) {
                repository.insertProduct(
                    prod.copy(
                        sampleAttachedProductId = sampleProductId,
                        sampleDescription = sampleDesc
                    )
                )
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun updateProductDetails(
        productId: String,
        stockCount: Int,
        price: Double,
        sampleProductId: String?,
        sampleDesc: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val prod = repository.getProductById(productId)
            if (prod != null) {
                repository.insertProduct(
                    prod.copy(
                        stockCount = stockCount,
                        price = price,
                        inStock = stockCount > 0,
                        sampleAttachedProductId = sampleProductId,
                        sampleDescription = sampleDesc
                    )
                )
                // Trigger refresh
                _merchantShop.value = _merchantShop.value
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun togglePetProblemActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            val problem = repository.getProblemById(id)
            if (problem != null) {
                repository.insertProblem(problem.copy(isActive = isActive))
            }
        }
    }

    // --- Super Admin Actions ---
    fun approveVetLicense(shopId: String) {
        viewModelScope.launch {
            val shop = repository.getShopById(shopId)
            if (shop != null) {
                repository.insertShop(shop.copy(isVetVerified = true))
                if (_merchantShop.value?.id == shopId) {
                    _merchantShop.value = repository.getShopById(shopId)
                }
            }
        }
    }

    fun updateMerchantShopServices(shopId: String, grooming: Boolean, vet: Boolean, shopEnabled: Boolean) {
        viewModelScope.launch {
            val shop = repository.getShopById(shopId) ?: return@launch
            val updated = shop.copy(groomingEnabled = grooming, vetClinicEnabled = vet, shopEnabled = shopEnabled)
            repository.insertShop(updated)
            _merchantShop.value = updated
        }
    }
}


