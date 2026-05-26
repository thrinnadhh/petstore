package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.R
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PawsApp(viewModel: PawsViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedCityName by viewModel.selectedCityName.collectAsState()
    val cartConflict by viewModel.showCartWarning.collectAsState()
    val showDialogContext = LocalContext.current

    // Register back button handler
    BackHandler(enabled = currentScreen !is Screen.Splash && currentScreen !is Screen.Onboarding) {
        viewModel.navigateBack()
    }

    // Main Scaffold with clean status and system bars handling
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            // Display swiggy-style cart footer bar on supported consumer views
            if (currentScreen is Screen.Home || currentScreen is Screen.ShopDetail || currentScreen is Screen.Search || currentScreen is Screen.SavedShops) {
                val cartItems by viewModel.cartItems.collectAsState()
                val cartShopId by viewModel.cartShopId.collectAsState()
                val shopsList by viewModel.shops.collectAsState()

                if (cartItems.isNotEmpty() && cartShopId != null) {
                    val activeShop = shopsList.find { it.id == cartShopId }
                    if (activeShop != null) {
                        FloatingCartBar(
                            itemCount = cartItems.values.sum(),
                            shopName = activeShop.name,
                            onViewCart = { viewModel.navigateTo(Screen.Cart) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    is Screen.Splash -> SplashScreen()
                    is Screen.Onboarding -> OnboardingScreen(onFinish = { viewModel.navigateTo(Screen.Auth) })
                    is Screen.Auth -> AuthScreen(
                        onLoginSubmit = { phone, isMerchant -> viewModel.loginWithPhone(phone, isMerchant) }
                    )
                    is Screen.LocationSelect -> LocationSelectScreen(
                        viewModel = viewModel,
                        onCityPicked = { id, name -> viewModel.selectCity(id, name) }
                    )
                    is Screen.Home -> HomeScreen(viewModel = viewModel)
                    is Screen.ShopDetail -> ShopDetailScreen(viewModel = viewModel, shopId = screen.shopId)
                    is Screen.Cart -> CartScreen(viewModel = viewModel)
                    is Screen.OrderTracking -> OrderTrackingScreen(viewModel = viewModel, orderId = screen.orderId)
                    is Screen.Search -> SearchScreen(viewModel = viewModel)
                    is Screen.SavedShops -> SavedShopsScreen(viewModel = viewModel)
                    is Screen.UserProfile -> UserProfileScreen(viewModel = viewModel)
                    is Screen.MerchantDashboard -> MerchantDashboardScreen(viewModel = viewModel)
                    is Screen.MerchantShopSetup -> MerchantShopSetupScreen(viewModel = viewModel)
                    else -> Box(modifier = Modifier.fillMaxSize())
                }
            }

            // Universal Swiggy Cart Reset Warning Dialog
            cartConflict?.let { conflict ->
                Dialog(onDismissRequest = { viewModel.resolveCartConflict(clearCartAndAdd = false) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart Conflict",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Replace Cart Items?",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Your cart contains items from another shop. Would you like to clear your current cart and add items from ${conflict.pendingProduct.name}?",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.resolveCartConflict(clearCartAndAdd = false) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = { viewModel.resolveCartConflict(clearCartAndAdd = true) },
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Clear & Add", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashSpinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1F22)), // Brand Dark Slate Background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(R.drawable.paws_logo_1779795154399),
                contentDescription = "PawsNearMe Icon",
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(32.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "PawsNearMe",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "Hyperlocal Dog Shop Discovery",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = Color(0xFFFF9E00),
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}

// ==========================================
// SCREEN 2: ONBOARDING CAROUSEL
// ==========================================
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val slides = listOf(
        OnboardSlide(
            title = "Hyperlocal Dog Food & Treats",
            desc = "Find nutrition kibbles, grain-free salmon snacks and accessories from verified dog stores right in your block.",
            illustration = "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=600&auto=format&fit=crop&q=80"
        ),
        OnboardSlide(
            title = "Premium Styling Spas",
            desc = "Connect with top dog salons in your area. Schedule professional bathes, coat therapy, and veteran wellness checkups.",
            illustration = "https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=600&auto=format&fit=crop&q=80"
        ),
        OnboardSlide(
            title = "Fast Home Delivery",
            desc = "Order products from local merchants. Track order states in real-time or pick up instantly yourself.",
            illustration = "https://images.unsplash.com/photo-1541599540903-216a46ca1da0?w=600&auto=format&fit=crop&q=80"
        )
    )

    val currentSlide = slides[currentPage]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onFinish) {
                Text("Skip", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(currentSlide.illustration)
                        .crossfade(true)
                        .error(R.drawable.paws_logo_1779795154399)
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(240.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = currentSlide.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = currentSlide.desc,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Indicators & Buttons
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                slides.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (i == currentPage) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            Button(
                onClick = {
                    if (currentPage < slides.size - 1) {
                        currentPage++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (currentPage == slides.size - 1) "Get Started" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

data class OnboardSlide(val title: String, val desc: String, val illustration: String)

// ==========================================
// SCREEN 3: AUTHENTICATION SCREEN
// ==========================================
@Composable
fun AuthScreen(onLoginSubmit: (String, Boolean) -> Unit) {
    var phoneNumber by remember { mutableStateOf("") }
    var mockOtp by remember { mutableStateOf("") }
    var isVerifiedRole by remember { mutableStateOf(false) } // False = Consumer, True = Merchant Shop Owner
    var isOtpSent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(R.drawable.paws_logo_1779795154399),
            contentDescription = "PawsNearMe Brand",
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(24.dp))
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Welcome to PawsNearMe",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Enter your phone number to find certified local dog stores",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        // Field: Phone
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { if (it.length <= 10) phoneNumber = it },
            label = { Text("Phone Number") },
            placeholder = { Text("e.g. 9876543210") },
            prefix = { Text("+91 ") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Call, null) }
        )

        if (isOtpSent) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = mockOtp,
                onValueChange = { if (it.length <= 4) mockOtp = it },
                label = { Text("Enter 4-Digit OTP") },
                placeholder = { Text("e.g. 1234") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Check, null) }
            )
            Text(
                "Use any mock OTP (e.g. 1234) to proceed",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 4.dp, start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Role select card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        if (isVerifiedRole) "Merchant Dashboard Account" else "Consumer Dog Owner Account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (isVerifiedRole) "Access inventory & fulfill customer orders" else "Browse pet shops, checkout products, track delivery",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Switch(
                    checked = isVerifiedRole,
                    onCheckedChange = { isVerifiedRole = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (phoneNumber.length < 10) {
                    Toast.makeText(context, "Please enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!isOtpSent) {
                    isOtpSent = true
                    Toast.makeText(context, "Mock OTP sent successfully to +91 $phoneNumber", Toast.LENGTH_SHORT).show()
                } else {
                    if (mockOtp.isEmpty()) {
                        Toast.makeText(context, "Please enter mock OTP", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onLoginSubmit(phoneNumber, isVerifiedRole)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                if (!isOtpSent) "Send OTP" else "Verify & Enter",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ==========================================
// SCREEN 4: LOCATION / CITY PICKER
// ==========================================
@Composable
fun LocationSelectScreen(viewModel: PawsViewModel, onCityPicked: (String, String) -> Unit) {
    val citiesList by viewModel.cities.collectAsState()
    var isDetecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Location",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Select Your City",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "PawsNearMe is hyperlocal and delivers exclusively within these active cities",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        // GPS Auto-detect Button
        OutlinedButton(
            onClick = {
                isDetecting = true
                scope.launch {
                    delay(1500)
                    isDetecting = false
                    Toast.makeText(context, "GPS auto-detected: Hyderabad!", Toast.LENGTH_SHORT).show()
                    onCityPicked("hyd", "Hyderabad")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isDetecting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto-detecting with GPS...")
                } else {
                    Icon(Icons.Default.LocationOn, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Auto Detect Location (GPS)")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                "OR PICK MANUALLY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(24.dp))

        // Cities List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(citiesList) { city ->
                Card(
                    onClick = { onCityPicked(city.id, city.name) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(city.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(city.state, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 5: CONSUMER HOME / DISCUSSION DISCOVERY
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PawsViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val shopsList by viewModel.shops.collectAsState()
    val categoryList by viewModel.categories.collectAsState()
    val selectedCityName by viewModel.selectedCityName.collectAsState()

    // Query state links
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryId.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val filterOpenNow by viewModel.filterOpenNow.collectAsState()
    val filterDelivery by viewModel.filterDelivery.collectAsState()
    val filterRating by viewModel.filterRating.collectAsState()

    var showCityPickerSheet by remember { mutableStateOf(false) }

    // Multi-criteria client side filter + sort orchestration
    val processedShops = remember(shopsList, searchQuery, selectedCategory, sortType, filterOpenNow, filterDelivery, filterRating) {
        var result = shopsList.filter { shop ->
            val matchesQuery = shop.name.contains(searchQuery, ignoreCase = true) || 
                               shop.locality.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || true // We can filter detailed categories in product page
            matchesQuery && matchesCategory
        }

        if (filterOpenNow) result = result.filter { it.isOpen }
        if (filterDelivery) result = result.filter { it.deliveryAvailable }
        if (filterRating) result = result.filter { it.rating >= 4.5 }

        when (sortType) {
            "Top Rated" -> result.sortedByDescending { it.rating }
            "New" -> result.sortedByDescending { it.createdAt }
            else -> result // Nearest
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 90.dp)
    ) {
        // App Header Row
        item {
            HomeHeader(
                cityName = selectedCityName,
                userName = currentUser?.fullName ?: "Pet Owner",
                avatarUrl = currentUser?.avatarUrl ?: "",
                onCityClick = { showCityPickerSheet = true },
                onProfileClick = { viewModel.navigateTo(Screen.UserProfile) }
            )
        }

        // Search action
        item {
            SearchBarPlaceholder(
                query = searchQuery,
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onTapSearch = { viewModel.navigateTo(Screen.Search) }
            )
        }

        // Horizontally Scrolling Featured Banner
        item {
            FeaturedBannerCarousel(shops = shopsList.filter { it.isFeatured }, onShopClick = {
                viewModel.navigateTo(Screen.ShopDetail(it.id))
            })
        }

        // Horizontal Categories List
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    "Discover by Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, bottom = 12.dp, top = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        CategoryChip(
                            name = "All",
                            iconUrl = "",
                            isSelected = selectedCategory == null,
                            onClick = { viewModel.setSelectedCategory(null) }
                        )
                    }
                    items(categoryList) { cat ->
                        CategoryChip(
                            name = cat.name,
                            iconUrl = cat.iconUrl,
                            isSelected = selectedCategory == cat.id,
                            onClick = { viewModel.setSelectedCategory(cat.id) }
                        )
                    }
                }
            }
        }

        // Horizontal Quick Selection Filters
        item {
            FilterSortStrip(
                activeSort = sortType,
                filterOpenNow = filterOpenNow,
                filterDelivery = filterDelivery,
                filterRating = filterRating,
                onSelectSort = { viewModel.setSortType(it) },
                onToggleOpen = { viewModel.toggleFilterOpenNow() },
                onToggleDelivery = { viewModel.toggleFilterDelivery() },
                onToggleRating = { viewModel.toggleFilterRating() }
            )
        }

        // Feed list
        if (processedShops.isEmpty()) {
            item {
                EmptyShopsState()
            }
        } else {
            item {
                Text(
                    "Showing ${processedShops.size} Dog Stores Near You",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            items(processedShops) { shop ->
                ShopItemCard(
                    shop = shop,
                    onClick = { viewModel.navigateTo(Screen.ShopDetail(shop.id)) }
                )
            }
        }
    }

    // Modal Sheet mock for City Selection
    if (showCityPickerSheet) {
        Dialog(onDismissRequest = { showCityPickerSheet = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Switch Current City", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    viewModel.cities.value.forEach { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectCity(city.id, city.name)
                                    showCityPickerSheet = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(city.name, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    OutlinedButton(
                        onClick = { showCityPickerSheet = false },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

// Sub Component: Header
@Composable
fun HomeHeader(
    cityName: String,
    userName: String,
    avatarUrl: String,
    onCityClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onCityClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = cityName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "Welcome back, $userName!",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Notification Mock
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Profile Avatar
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
                        .crossfade(true)
                        .error(R.drawable.paws_logo_1779795154399)
                        .build()
                ),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { onProfileClick() },
                contentScale = ContentScale.Crop
            )
        }
    }
}

// Search bar placeholder
@Composable
fun SearchBarPlaceholder(query: String, onQueryChange: (String) -> Unit, onTapSearch: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search dog foods, grooming salons...") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable { onTapSearch() },
        enabled = true,
        leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
        ),
        singleLine = true
    )
}

// Banners
@Composable
fun FeaturedBannerCarousel(shops: List<ShopEntity>, onShopClick: (ShopEntity) -> Unit) {
    if (shops.isEmpty()) return

    Column {
        Text(
            "Promo Offers & Deals",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(shops) { shop ->
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .height(150.dp)
                        .clickable { onShopClick(shop) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(shop.photos.firstOrNull())
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFF9E00), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("20% OFF ALL FOODS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            Column {
                                Text(
                                    shop.name,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Verified Delivery • ${shop.locality}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Category design
@Composable
fun CategoryChip(name: String, iconUrl: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary 
                             else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary 
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconUrl.isNotEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(iconUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// Fast filter bar
@Composable
fun FilterSortStrip(
    activeSort: String,
    filterOpenNow: Boolean,
    filterDelivery: Boolean,
    filterRating: Boolean,
    onSelectSort: (String) -> Unit,
    onToggleOpen: () -> Unit,
    onToggleDelivery: () -> Unit,
    onToggleRating: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterBadge(label = "Sort: $activeSort", isActive = true, onClick = {
                val next = if (activeSort == "Nearest") "Top Rated" else if (activeSort == "Top Rated") "New" else "Nearest"
                onSelectSort(next)
            })
        }
        item {
            FilterBadge(label = "Open Now", isActive = filterOpenNow, onClick = onToggleOpen)
        }
        item {
            FilterBadge(label = "Home Delivery Available", isActive = filterDelivery, onClick = onToggleDelivery)
        }
        item {
            FilterBadge(label = "⭐ Top Rated (4.5+)", isActive = filterRating, onClick = onToggleRating)
        }
    }
}

@Composable
fun FilterBadge(label: String, isActive: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = isActive,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) }
    )
}

// Shop Card representation
@Composable
fun ShopItemCard(shop: ShopEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(shop.photos.firstOrNull())
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Open status tag overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(
                            if (shop.isOpen) Color(0xFF4CAF50) else Color(0xFFF44336),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (shop.isOpen) "OPEN NOW" else "CLOSED",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = shop.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            shop.rating.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFB17E00)
                        )
                    }
                }

                Text(
                    shop.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(shop.locality, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }

                    if (shop.deliveryAvailable) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = Color(0xFF4DDFD2),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delivery Free", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2EC4B6))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyShopsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No Dog Stores Found",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Text(
            "Try switching your city selection or clearing filters to view available stores.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ==========================================
// SCREEN 6: SHOP CUSTOM DETAIL & PRODUCT CATALOG
// ==========================================
@Composable
fun ShopDetailScreen(viewModel: PawsViewModel, shopId: String) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("Menu") } // "Menu" | "Reviews" | "Info"
    var shopState = remember { mutableStateOf<ShopEntity?>(null) }
    var productsList by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    var reviewsList by remember { mutableStateOf<List<ReviewEntity>>(emptyList()) }
    val wishlists by viewModel.wishlists.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val scope = rememberCoroutineScope()

    // Load data from VM reactively
    LaunchedEffect(shopId) {
        val s = viewModel.getShopById(shopId)
        shopState.value = s
        viewModel.getProductsFlow(shopId).collect { productsList = it }
    }
    LaunchedEffect(shopId) {
        viewModel.getReviewsFlow(shopId).collect { reviewsList = it }
    }

    val shop = shopState.value ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Hero Photo Container with back actions
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(shop.photos.firstOrNull()),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )

                // Top Actions Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }

                    // Wishlist Heart Icon Action
                    val isFavorite = wishlists.any { it.shopId == shop.id }
                    IconButton(
                        onClick = { viewModel.toggleWishlist(shop.id) },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save Wishlist",
                            tint = if (isFavorite) Color.Red else Color.White
                        )
                    }
                }
            }
        }

        // Shop metadata Card
        item {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(shop.name, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(
                            shop.description,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFFFFB300).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            shop.rating.toString(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9E7000)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Call / Directions CTAs Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${shop.phone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Call Shop", color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val uri = Uri.parse("google.navigation:q=${shop.lat},${shop.lng}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            if (intent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Redirecting to Maps info...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Directions", color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
            }
        }

        // Segment Tab Layout
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    .padding(4.dp)
            ) {
                listOf("Menu", "Reviews (${reviewsList.size})", "Store Info").forEach { tab ->
                    val isSelected = activeTab == tab || (tab.startsWith("Reviews") && activeTab == "Reviews")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable {
                                activeTab = if (tab.startsWith("Reviews")) "Reviews" else tab
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // VIEW RENDERING: BY TAB
        when (activeTab) {
            "Menu" -> {
                if (productsList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No catalog items listed yet", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    items(productsList) { product ->
                        val qty = cartItems[product.id] ?: 0
                        ProductCatalogRow(
                            product = product,
                            quantity = qty,
                            onAdd = { viewModel.addToCart(product, shop) },
                            onRemove = { viewModel.removeFromCart(product.id) }
                        )
                    }
                }
            }

            "Reviews" -> {
                item {
                    ReviewsTabScreen(
                        reviews = reviewsList,
                        shopId = shop.id,
                        onSubmitReview = { rating, comment -> viewModel.submitReview(shop.id, rating, comment) }
                    )
                }
            }

            "Store Info" -> {
                item {
                    StoreInfoTabScreen(shop = shop)
                }
            }
        }
    }
}

// Sub Component: Product row listing
@Composable
fun ProductCatalogRow(
    product: ProductEntity,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.photos.firstOrNull())
                        .crossfade(true)
                        .error(R.drawable.paws_logo_1779795154399)
                        .build()
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    product.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹${product.price}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    if (product.mrp > product.price) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "₹${product.mrp}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Swiggy add button logic
            if (quantity > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(32.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                        Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Text(
                        quantity.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        fontSize = 13.sp
                    )
                    IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onAdd,
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Text("ADD", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// Floating bar
@Composable
fun FloatingCartBar(itemCount: Int, shopName: String, onViewCart: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF44336)), // Vibrant red swiggy pill
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable { onViewCart() }
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "$itemCount Items added",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    shopName,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View Cart", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.ShoppingCart, null, tint = Color.White)
            }
        }
    }
}

// Sub Component: Reviews Content Tab
@Composable
fun ReviewsTabScreen(
    reviews: List<ReviewEntity>,
    shopId: String,
    onSubmitReview: (Int, String) -> Unit
) {
    var showReviewDialog by remember { mutableStateOf(false) }
    var ratingChosen by remember { mutableStateOf(5) }
    var commentText by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Customer Reviews", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Button(
                onClick = { showReviewDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add Review")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (reviews.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No reviews yet. Be the first to share your dog's experience!", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            reviews.forEach { review ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(Color.Gray.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (review.consumerId.contains("arjun")) "Arjun" else "Doggie Lover",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Row {
                                repeat(5) { i ->
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (i < review.rating) Color(0xFFFFB300) else Color.LightGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            review.comment,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    if (showReviewDialog) {
        Dialog(onDismissRequest = { showReviewDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Write a Review", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("How was your dog's experience?", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(5) { i ->
                            val starIndex = i + 1
                            IconButton(onClick = { ratingChosen = starIndex }) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (starIndex <= ratingChosen) Color(0xFFFFB300) else Color.LightGray,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it },
                        label = { Text("What did your pup love/hate?") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showReviewDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Dismiss")
                        }
                        Button(
                            onClick = {
                                if (commentText.isNotEmpty()) {
                                    onSubmitReview(ratingChosen, commentText)
                                    commentText = ""
                                    showReviewDialog = false
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Submit Review", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// Sub Component: Info Content Tab
@Composable
fun StoreInfoTabScreen(shop: ShopEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Operational Info", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Address: ${shop.address}", fontSize = 12.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Phone Support: ${shop.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Text("Merchant Email: ${shop.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Standard Operating Timings", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Monday - Sunday", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Text("${shop.opensAt} - ${shop.closesAt}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ==========================================
// SCREEN 7: CART / CHECKOUT SCREEN (CONSUMER)
// ==========================================
@Composable
fun CartScreen(viewModel: PawsViewModel) {
    val cartItems by viewModel.cartItems.collectAsState()
    val cartShopId by viewModel.cartShopId.collectAsState()
    val shopsList by viewModel.shops.collectAsState()
    val context = LocalContext.current

    var deliveryType by remember { mutableStateOf("delivery") } // "delivery" | "pickup"
    var addressInput by remember { mutableStateOf("Villa 42, Road No 5, Banjara Hills, Hyderabad") }
    var noteInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val currentShop = remember(cartShopId, shopsList) {
        shopsList.find { it.id == cartShopId }
    }

    if (cartItems.isEmpty() || currentShop == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Cart is Empty", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("Add delicious foods & grooming spa vouchers to proceed with placing orders.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.navigateTo(Screen.Home) }) {
                Text("Discover pet shops")
            }
        }
        return
    }

    // Load actual product objects in order to map checkout values
    var computedSubtotal = 0.0
    val listOfProductsWithQty = mutableListOf<Pair<ProductEntity, Int>>()
    val allProdsFlow = viewModel.getProductsFlow(currentShop.id)
    var catalogProducts by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }

    LaunchedEffect(currentShop.id) {
        allProdsFlow.collect { catalogProducts = it }
    }

    cartItems.forEach { (id, qty) ->
        val p = catalogProducts.find { it.id == id }
        if (p != null) {
            computedSubtotal += (p.price * qty)
            listOfProductsWithQty.add(p to qty)
        }
    }

    val deliveryCost = if (deliveryType == "delivery") 30.0 else 0.0
    val grandTotal = computedSubtotal + deliveryCost

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Cart Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Secure Checkout", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(currentShop.name, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                }
            }

            // Products in selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Selected items", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    listOfProductsWithQty.forEach { (prod, qty) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("₹${prod.price} x $qty", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "₹${prod.price * qty}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                // +/- steppers inside checkout
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(32.dp))
                                        .padding(horizontal = 2.dp, vertical = 1.dp)
                                ) {
                                    IconButton(
                                        onClick = { viewModel.removeFromCart(prod.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    IconButton(
                                        onClick = { viewModel.addToCart(prod, currentShop) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Delivery switcher
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    val pickupAllowed = true
                    val delAllowed = currentShop.deliveryAvailable

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (deliveryType == "delivery") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { if (delAllowed) deliveryType = "delivery" }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Home Delivery (₹30)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (deliveryType == "delivery") Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (deliveryType == "pickup") MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { deliveryType = "pickup" }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Self Pickup (Free)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (deliveryType == "pickup") Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Address Inputs
            if (deliveryType == "delivery") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Delivery Address", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Shop Notes
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add checkout notes for the shop", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = { Text("e.g. Please leave pack at door, dog of nervous temperament") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Bill Breakdown Check receipt
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Invoice Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Item Subtotal", fontSize = 13.sp)
                        Text("₹$computedSubtotal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Partner Fee", fontSize = 13.sp)
                        Text("₹$deliveryCost", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Grand Total to Pay", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("₹$grandTotal", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    }
                }
            }
        }

        // Checkout Button Footer row
        Button(
            onClick = {
                viewModel.placeOrder(
                    address = if (deliveryType == "delivery") addressInput else "Pickup from merchant clinic",
                    notes = noteInput,
                    deliveryType = deliveryType
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Place Dog Food Order • ₹$grandTotal", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        }
    }
}

// ==========================================
// SCREEN 8: REAL-TIME TIMELINE ORDER TRACKING
// ==========================================
@Composable
fun OrderTrackingScreen(viewModel: PawsViewModel, orderId: String) {
    val activeOrder by viewModel.activeOrder.collectAsState()
    val activeOrderItems by viewModel.activeOrderItems.collectAsState()
    val shopsList by viewModel.shops.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(orderId) {
        viewModel.refreshActiveOrder(orderId)
    }

    val order = activeOrder ?: return
    val shop = shopsList.find { it.id == order.shopId } ?: return

    val statusLevels = listOf("pending", "accepted", "preparing", "out_for_delivery", "delivered")
    val currentIndex = statusLevels.indexOf(order.status)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // Tracker Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F22)) // Solid dark themed
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Order #${order.id}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text(shop.name, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Grand Total: ₹${order.totalAmount}", color = Color(0xFFFF9E00), fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        Text(order.type.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Status Timeline Vertical Tracker
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Delivery Status Tracking", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(20.dp))

                val trackingSteps = listOf(
                    TimelineStep("Order Placed", "Your order has been registered by PawsNearMe", "pending"),
                    TimelineStep("Order Accepted", "The merchant clinic has confirmed and verified stock", "accepted"),
                    TimelineStep("Preparing Delivery", "Dog items are packed carefully under clean standards", "preparing"),
                    TimelineStep("Out for Delivery", "Delivering directly via secure local dog service", "out_for_delivery"),
                    TimelineStep("Delivered successfully", "Package received by dog parent", "delivered")
                )

                trackingSteps.forEachIndexed { i, step ->
                    val isPast = i < currentIndex
                    val isActive = i == currentIndex
                    val isFuture = i > currentIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Drawing indicators
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(36.dp)
                        ) {
                            val dotColor = when {
                                isActive -> Color(0xFFE65100)
                                isPast -> Color(0xFF4CAF50)
                                else -> Color.LightGray
                            }

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(dotColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPast) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                }
                            }

                            if (i < trackingSteps.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(35.dp)
                                        .background(if (isPast) Color(0xFF4CAF50) else Color.LightGray)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                step.title,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (isFuture) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                step.description,
                                fontSize = 11.sp,
                                color = if (isFuture) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Support Actions Footer
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${shop.phone}"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Call, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Call Helpline")
            }

            Button(
                onClick = { viewModel.navigateTo(Screen.Home) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("All Shops", color = MaterialTheme.colorScheme.onBackground)
            }
        }

        // If status delivered, render inline quick rating
        if (order.status == "delivered") {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Add Dog Shop Review", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    ReviewsTabScreen(
                        reviews = emptyList(),
                        shopId = shop.id,
                        onSubmitReview = { rating, comment -> 
                            viewModel.submitReview(shop.id, rating, comment)
                            Toast.makeText(context, "Thank you for the review!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

data class TimelineStep(val title: String, val description: String, val status: String)

// ==========================================
// SCREEN 9: DEBOUNCED SEARCH SCREEN
// ==========================================
@Composable
fun SearchScreen(viewModel: PawsViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val shopsList by viewModel.shops.collectAsState()
    val activeTab by viewModel.searchTab.collectAsState() // "Shops" | "Products"
    val context = LocalContext.current

    var searchInput by remember { mutableStateOf(searchQuery) }

    // Simulating debounce
    LaunchedEffect(searchInput) {
        delay(300)
        viewModel.updateSearchQuery(searchInput)
    }

    val matchedShops = remember(searchQuery, shopsList) {
        shopsList.filter { it.name.contains(searchQuery, ignoreCase = true) || it.locality.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                placeholder = { Text("Search specific pet shops or dog tags...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Tabs: search in shops or products
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            listOf("Shops", "Products").forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.setSearchTab(tab) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column {
                        Text(
                            tab,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            fontSize = 14.sp
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .height(2.dp)
                                    .width(40.dp)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (searchInput.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(56.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Type keyword to search shops in Hyderabad", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
                if (activeTab == "Shops") {
                    if (matchedShops.isEmpty()) {
                        item {
                            Text("No match found", modifier = Modifier.padding(20.dp), color = Color.Gray)
                        }
                    } else {
                        items(matchedShops) { shop ->
                            ShopItemCard(shop = shop, onClick = { viewModel.navigateTo(Screen.ShopDetail(shop.id)) })
                        }
                    }
                } else {
                    // Match products across active city shops
                    // We gather products for shops and display standard previews
                    item {
                        Text("Searching products across all local stores...", modifier = Modifier.padding(horizontal = 20.dp), fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 10: SAVED SHOPS (WISHLIST)
// ==========================================
@Composable
fun SavedShopsScreen(viewModel: PawsViewModel) {
    val wishlists by viewModel.wishlists.collectAsState()
    val shopsList by viewModel.shops.collectAsState()

    val favShops = remember(wishlists, shopsList) {
        shopsList.filter { shop -> wishlists.any { it.shopId == shop.id } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Saved Wishlist (Dog Salons & Shops)", fontWeight = FontWeight.Black, fontSize = 18.sp)
        }

        if (favShops.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Favorite, null, modifier = Modifier.size(72.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(12.dp))
                Text("You haven't saved any dog stores yet", fontWeight = FontWeight.Bold)
                Text("Tap the heart icon on any detail shop page to pin it here for quick access", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn {
                items(favShops) { shop ->
                    ShopItemCard(shop = shop, onClick = { viewModel.navigateTo(Screen.ShopDetail(shop.id)) })
                }
            }
        }
    }
}

// ==========================================
// SCREEN 11: USER PROFILE VIEW
// ==========================================
@Composable
fun UserProfileScreen(viewModel: PawsViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Toolbar
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = rememberAsyncImagePainter(currentUser?.avatarUrl),
            contentDescription = null,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(currentUser?.fullName ?: "Arjun", fontWeight = FontWeight.Black, fontSize = 24.sp)
        Text("+91 ${currentUser?.phone ?: "9876543210"}", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        // Actions List
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ProfileOptionRow(icon = Icons.Default.Favorite, title = "Saved Wishlist", subtitle = "Your marked favorite dog stores", onClick = { viewModel.navigateTo(Screen.SavedShops) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.LocationOn, title = "Change City Location", subtitle = "Current city Hyderabad", onClick = { viewModel.navigateTo(Screen.LocationSelect) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.Person, title = "Fictional Role: Merchant Toggle", subtitle = "Switch context to Suresh (Pet Shop Owner)", onClick = {
                    viewModel.loginWithPhone("9999988888", true)
                })
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            border = BorderStroke(1.5.dp, Color.Red),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Log out of PawsNearMe", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProfileOptionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Icon(Icons.Default.ArrowForward, null, tint = Color.LightGray)
    }
}

// ==========================================
// SCREEN 12: MERCHANT DASHBOARD & STATUS SCREEN
// ==========================================
@Composable
fun MerchantDashboardScreen(viewModel: PawsViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val shopState by viewModel.merchantShop.collectAsState()
    val orders by viewModel.getMerchantOrdersFlow().collectAsState(initial = emptyList())
    val products by viewModel.getMerchantProductsFlow().collectAsState(initial = emptyList())
    val context = LocalContext.current

    val completedOrders = remember(orders) { orders.filter { it.status == "delivered" } }
    val totalRevenue = remember(completedOrders) { completedOrders.sumOf { it.totalAmount } }

    var isAddingProduct by remember { mutableStateOf(false) }
    var prodName by remember { mutableStateOf("") }
    var prodDesc by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }

    val shop = shopState
    if (shop == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Awaiting shop creation...")
            Button(onClick = { viewModel.navigateTo(Screen.MerchantShopSetup) }) {
                Text("Go Setup Shop")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Merchant Custom Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(shop.name, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text("Role: Merchant Suresh Manager", fontSize = 11.sp, color = Color.Gray)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.logout() }) {
                        Text("Exit", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Stats cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active Orders", fontSize = 11.sp, color = Color.Gray)
                        Text(orders.filter { it.status != "delivered" && it.status != "rejected" }.size.toString(), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                }
                Card(modifier = Modifier.weight(1.2f)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Completed Rev", fontSize = 11.sp, color = Color.Gray)
                        Text("₹${totalRevenue.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                    }
                }
            }

            // Shop Open Switch Toggle
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (shop.isOpen) Color(0xFF4CAF50).copy(alpha = 0.08f) 
                                     else Color(0xFFF44336).copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (shop.isOpen) "Shop Status: OPEN" else "Shop Status: CLOSED",
                            fontWeight = FontWeight.Bold,
                            color = if (shop.isOpen) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Text("Controls display in consumer application city listings", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = shop.isOpen,
                        onCheckedChange = { viewModel.updateMerchantShopOpenStatus(it) }
                    )
                }
            }

            // Incoming Orders List Section
            Text(
                "Incoming Direct Orders Grid",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp)
            )

            if (orders.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text("No customer requests yet.", modifier = Modifier.padding(16.dp), color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                orders.forEach { order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Order #${order.id}", fontWeight = FontWeight.Bold)
                                    Text("Status: ${order.status.uppercase()}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("₹${order.totalAmount}", fontWeight = FontWeight.Black)
                            }
                            Text("Delivery To: ${order.deliveryAddress}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                            if (order.notes.isNotEmpty()) {
                                Text("Customer Note: \"${order.notes}\"", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ACCEPTANCE ACTIONS
                            when (order.status) {
                                "pending" -> {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.updateMerchantOrderStatus(order.id, "accepted") },
                                            modifier = Modifier.weight(1.5f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                        ) {
                                            Text("Accept Order", color = Color.White)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.updateMerchantOrderStatus(order.id, "rejected") },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reject", color = Color.Red)
                                        }
                                    }
                                }
                                "accepted" -> {
                                    Button(
                                        onClick = { viewModel.updateMerchantOrderStatus(order.id, "preparing") },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Mark: Preparing Pack")
                                    }
                                }
                                "preparing" -> {
                                    Button(
                                        onClick = { viewModel.updateMerchantOrderStatus(order.id, "out_for_delivery") },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Mark: Dispatch Out for Delivery")
                                    }
                                }
                                "out_for_delivery" -> {
                                    Button(
                                        onClick = { viewModel.updateMerchantOrderStatus(order.id, "delivered") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                    ) {
                                        Text("Mark: Delivered", color = Color.White)
                                    }
                                }
                                "delivered" -> {
                                    Text("✓ Delivered Successfully", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                "rejected" -> {
                                    Text("✗ Order Declined", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Products list + Add product capability
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manage Active Catalog (${products.size})", fontWeight = FontWeight.Bold)
                TextButton(onClick = { isAddingProduct = true }) {
                    Text("+ Add Item")
                }
            }

            products.forEach { prod ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(prod.name, fontWeight = FontWeight.Bold)
                            Text("Price: ₹${prod.price}", fontSize = 12.sp, color = Color.Gray)
                        }
                        IconButton(onClick = { viewModel.deleteMerchantProduct(prod.id) }) {
                            Icon(Icons.Default.Delete, null, tint = Color.Red)
                        }
                    }
                }
            }
        }
    }

    if (isAddingProduct) {
        Dialog(onDismissRequest = { isAddingProduct = false }) {
            Card(modifier = Modifier.padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Store Product", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(value = prodName, onValueChange = { prodName = it }, label = { Text("Name") })
                    OutlinedTextField(value = prodDesc, onValueChange = { prodDesc = it }, label = { Text("Description") })
                    OutlinedTextField(
                        value = prodPrice, onValueChange = { prodPrice = it },
                        label = { Text("Price (Rs)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row {
                        OutlinedButton(onClick = { isAddingProduct = false }) { Text("Dismiss") }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(onClick = {
                            if (prodName.isNotEmpty() && prodPrice.isNotEmpty()) {
                                viewModel.addMerchantProduct(
                                    prodName, "cat_food", prodDesc, prodPrice.toDoubleOrNull() ?: 100.0,
                                    (prodPrice.toDoubleOrNull() ?: 100.0) * 1.2
                                )
                                prodName = ""
                                prodDesc = ""
                                prodPrice = ""
                                isAddingProduct = false
                            }
                        }) { Text("Confirm Add") }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 13: MERCHANT SHOP SETUP WIZARD
// ==========================================
@Composable
fun MerchantShopSetupScreen(viewModel: PawsViewModel) {
    var shopName by remember { mutableStateOf("") }
    var shopPhone by remember { mutableStateOf("") }
    var shopEmail by remember { mutableStateOf("") }
    var shopAddress by remember { mutableStateOf("") }
    var shopLocality by remember { mutableStateOf("") }
    var shopDeliveryAvailable by remember { mutableStateOf(true) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Home, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Register Your Dog Store", fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text("Complete wizard steps to list in Hyderabad city indices", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = shopPhone, onValueChange = { shopPhone = it }, label = { Text("Contact Phone") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = shopEmail, onValueChange = { shopEmail = it }, label = { Text("Contact Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = shopAddress, onValueChange = { shopAddress = it }, label = { Text("Full Address") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = shopLocality, onValueChange = { shopLocality = it }, label = { Text("Locality (e.g. Banjara Hills)") }, modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Support Home Delivery?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Customer ordering support available", fontSize = 11.sp, color = Color.Gray)
                }
                Switch(checked = shopDeliveryAvailable, onCheckedChange = { shopDeliveryAvailable = it })
            }
        }

        Button(
            onClick = {
                if (shopName.isNotEmpty() && shopPhone.isNotEmpty() && shopAddress.isNotEmpty()) {
                    viewModel.submitMerchantShopSetup(
                        name = shopName, phone = shopPhone, email = shopEmail,
                        cityName = "Hyderabad", address = shopAddress, locality = shopLocality,
                        deliveryAvailable = shopDeliveryAvailable, opensAt = "09:00", closesAt = "21:00"
                    )
                } else {
                    Toast.makeText(context, "Please write essential details", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Register Store & Open", fontWeight = FontWeight.Bold)
        }
    }
}
