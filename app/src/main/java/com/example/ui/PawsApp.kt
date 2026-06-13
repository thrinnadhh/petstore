package com.example.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.R
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

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
            val mainConsumerScreens = listOf(
                Screen.Home, Screen.Search, Screen.Favourites, Screen.Orders, Screen.UserProfile, Screen.SavedShops
            )
            val isMainConsumerScreen = currentScreen in mainConsumerScreens ||
                currentScreen is Screen.ShopDetail

            // Premium 5-tab bottom navigation bar for consumer screens
            if (isMainConsumerScreen && currentUser?.role == "consumer") {
                Column {
                    // Floating cart bar if items in cart
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
                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(elevation = 12.dp, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    ) {
                        data class NavItem(val label: String, val icon: ImageVector, val screen: Screen)
                        val navItems = listOf(
                            NavItem("Home", Icons.Default.Home, Screen.Home),
                            NavItem("Search", Icons.Default.Search, Screen.Search),
                            NavItem("Favourites", Icons.Default.Favorite, Screen.Favourites),
                            NavItem("Orders", Icons.Default.ShoppingCart, Screen.Orders),
                            NavItem("Profile", Icons.Default.Person, Screen.UserProfile)
                        )
                        navItems.forEach { item ->
                            val selected = when (item.screen) {
                                Screen.Home -> currentScreen is Screen.Home || currentScreen is Screen.ShopDetail
                                Screen.Search -> currentScreen is Screen.Search
                                Screen.Favourites -> currentScreen is Screen.Favourites || currentScreen is Screen.SavedShops
                                Screen.Orders -> currentScreen is Screen.Orders || currentScreen is Screen.Cart
                                Screen.UserProfile -> currentScreen is Screen.UserProfile || currentScreen is Screen.Appointments ||
                                    currentScreen is Screen.TabletsIssued || currentScreen is Screen.Vaccinations ||
                                    currentScreen is Screen.ReportsDashboard
                                else -> false
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) viewModel.clearHistoryAndNavigate(item.screen)
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFFC8019),
                                    selectedTextColor = Color(0xFFFC8019),
                                    indicatorColor = Color(0xFFFC8019).copy(alpha = 0.12f),
                                    unselectedIconColor = Color(0xFF8C8C8C),
                                    unselectedTextColor = Color(0xFF8C8C8C)
                                )
                            )
                        }
                    }
                }
            } else if (!isMainConsumerScreen ||
                (currentUser?.role != "consumer" &&
                    (currentScreen is Screen.Home || currentScreen is Screen.ShopDetail ||
                     currentScreen is Screen.Search || currentScreen is Screen.SavedShops))) {
                // Show cart bar for non-consumer contexts (e.g., browsing as guest)
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
                    is Screen.Auth -> AuthScreen(viewModel = viewModel)
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
                    is Screen.SuperAdmin -> SuperAdminScreen(viewModel = viewModel)
                    is Screen.SuperAdminUsers -> SuperAdminUsersScreen(viewModel = viewModel)
                    is Screen.MerchantInventory -> MerchantInventoryScreen(viewModel = viewModel)
                    is Screen.ChatList -> ChatListScreen(viewModel = viewModel)
                    is Screen.ChatDetail -> ChatDetailScreen(viewModel = viewModel, shopId = screen.shopId)
                    is Screen.MerchantOrders -> MerchantOrdersScreen(viewModel = viewModel)
                    is Screen.MerchantMenu -> MerchantMenuScreen(viewModel = viewModel)
                    is Screen.Appointments -> AppointmentsScreen(viewModel = viewModel)
                    is Screen.TabletsIssued -> TabletsIssuedScreen(viewModel = viewModel)
                    is Screen.Vaccinations -> VaccinationsScreen(viewModel = viewModel)
                    is Screen.Favourites -> FavouritesScreen(viewModel = viewModel)
                    is Screen.ReportsDashboard -> ReportsDashboardScreen(viewModel = viewModel)
                    is Screen.Orders -> OrdersScreen(viewModel = viewModel)
                    is Screen.FoodNutrition -> CatalogScreen(categoryId = "cat_food", viewModel = viewModel)
                    is Screen.TreatsChews -> CatalogScreen(categoryId = "cat_treats", viewModel = viewModel)
                    is Screen.ToysEnrichment -> CatalogScreen(categoryId = "cat_toys", viewModel = viewModel)
                    is Screen.GroomingServices -> GroomingServicesScreen(viewModel = viewModel)
                    is Screen.TravelApparel -> CatalogScreen(categoryId = "cat_travel", viewModel = viewModel)
                    is Screen.FurnitureSleep -> CatalogScreen(categoryId = "cat_furniture", viewModel = viewModel)
                    is Screen.WasteManagement -> CatalogScreen(categoryId = "cat_waste", viewModel = viewModel)
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
                contentDescription = "Swiggy Paws Icon",
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(32.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = L10n.getString("app_name"),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = L10n.getString("splash_tagline"),
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
            title = L10n.getString("onboarding_title_1"),
            desc = L10n.getString("onboarding_desc_1"),
            illustration = "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=600&auto=format&fit=crop&q=80"
        ),
        OnboardSlide(
            title = L10n.getString("onboarding_title_2"),
            desc = L10n.getString("onboarding_desc_2"),
            illustration = "https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=600&auto=format&fit=crop&q=80"
        ),
        OnboardSlide(
            title = L10n.getString("onboarding_title_3"),
            desc = L10n.getString("onboarding_desc_3"),
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
                Text(L10n.getString("skip"), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
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
                    text = if (currentPage == slides.size - 1) L10n.getString("get_started") else L10n.getString("next"),
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
@OptIn(ExperimentalMaterial3Api::class)
fun AuthScreen(viewModel: PawsViewModel) {
    var activeTab by remember { mutableStateOf("login") } // "login" | "register"
    
    // Login fields
    var loginPhone by remember { mutableStateOf("") }
    var loginOtp by remember { mutableStateOf("") }
    var isLoginOtpSent by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    
    // New Email/Phone & Password fields
    var loginEmailOrPhone by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isOtpLoginOption by remember { mutableStateOf(false) }
    
    // Register fields
    var regName by remember { mutableStateOf("") }
    var regPhone by remember { mutableStateOf("") }
    var regEmail by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("consumer") } // "consumer" | "merchant" | "captain"
    var regOtp by remember { mutableStateOf("") }
    var isRegOtpSent by remember { mutableStateOf(false) }
    var regError by remember { mutableStateOf<String?>(null) }
    var regPetName by remember { mutableStateOf("") }
    var regPetBreed by remember { mutableStateOf("") }
    var regPetAge by remember { mutableStateOf("") }
    var regPetWeight by remember { mutableStateOf("") }
    var regSelfieUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=150") }
    var regVehicleNumber by remember { mutableStateOf("") }
    var regPanCard by remember { mutableStateOf("") }
    var regBankDetails by remember { mutableStateOf("") }
    var regAadharNumber by remember { mutableStateOf("") }
    var regPanCardUrl by remember { mutableStateOf("") }
    var regAadharCardUrl by remember { mutableStateOf("") }
    var regLicenseUrl by remember { mutableStateOf("") }
    var regSelfieUrlForCaptain by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val panCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            regPanCardUrl = uri.toString()
        }
    }

    val aadharCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            regAadharCardUrl = uri.toString()
        }
    }

    val licenseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            regLicenseUrl = uri.toString()
        }
    }

    val selfieLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            regSelfieUrlForCaptain = uri.toString()
        }
    }

    val cameraPanCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = java.io.File(context.cacheDir, "pancard_${System.currentTimeMillis()}.jpg")
            try {
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                regPanCardUrl = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraAadharCardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = java.io.File(context.cacheDir, "aadhar_${System.currentTimeMillis()}.jpg")
            try {
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                regAadharCardUrl = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraLicenseLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = java.io.File(context.cacheDir, "license_${System.currentTimeMillis()}.jpg")
            try {
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                regLicenseUrl = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraSelfieLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = java.io.File(context.cacheDir, "selfie_${System.currentTimeMillis()}.jpg")
            try {
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                regSelfieUrlForCaptain = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val consumerSelfieLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            regSelfieUrl = uri.toString()
        }
    }

    val cameraConsumerSelfieLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = java.io.File(context.cacheDir, "consumer_selfie_${System.currentTimeMillis()}.jpg")
            try {
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                regSelfieUrl = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Image(
            painter = rememberAsyncImagePainter(R.drawable.paws_logo_1779795154399),
            contentDescription = "Swiggy Paws Brand",
            modifier = Modifier
                .size(110.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Swiggy Paws 🐾",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "Unified Pet Hub: Dogs, Cats, Birds, Hamsters nestled in comfort",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Pill slider tab switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeTab == "login") MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { activeTab = "login" }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    L10n.getString("login"),
                    fontWeight = FontWeight.Bold,
                    color = if (activeTab == "login") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (activeTab == "register") MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable { activeTab = "register" }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    L10n.getString("register"),
                    fontWeight = FontWeight.Bold,
                    color = if (activeTab == "register") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        if (activeTab == "login") {
            // LOGIN TAB VIEW
            if (!isOtpLoginOption) {
                // Email / Phone & Password Login
                OutlinedTextField(
                    value = loginEmailOrPhone,
                    onValueChange = { loginEmailOrPhone = it },
                    label = { Text(L10n.getString("email_or_phone")) },
                    placeholder = { Text("e.g. trinadhbandapalli@gmail.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Email, null) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                var passwordVisibility by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = loginPassword,
                    onValueChange = { loginPassword = it },
                    label = { Text(L10n.getString("password")) },
                    placeholder = { Text("Enter your password") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (passwordVisibility) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisibility = !passwordVisibility }) {
                            Icon(if (passwordVisibility) Icons.Default.Info else Icons.Default.Lock, contentDescription = "Toggle password visibility")
                        }
                    }
                )

                if (loginError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        loginError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start).padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (loginEmailOrPhone.trim().isEmpty() || loginPassword.isEmpty()) {
                            Toast.makeText(context, "Please fill in all credentials", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.loginWithEmailOrPhoneAndPassword(
                            identifier = loginEmailOrPhone,
                            passwordText = loginPassword,
                            onSuccess = {
                                Toast.makeText(context, "Logged in successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                loginError = error
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        L10n.getString("login_btn"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        isOtpLoginOption = true
                        loginError = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(L10n.getString("forgot_password"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                // Phone Number & OTP Login
                OutlinedTextField(
                    value = loginPhone,
                    onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) loginPhone = it },
                    label = { Text(L10n.getString("phone_number")) },
                    placeholder = { Text("e.g. 9876543210") },
                    prefix = { Text("+91 ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Call, null) }
                )

                if (loginError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        loginError!!,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start).padding(horizontal = 4.dp)
                    )
                }

                if (isLoginOtpSent) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = loginOtp,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) loginOtp = it },
                        label = { Text(L10n.getString("enter_mock_otp")) },
                        placeholder = { Text("e.g. 1234") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Check, null) }
                    )
                    Text(
                        L10n.getString("mock_otp_hint"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(top = 4.dp, start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (loginPhone.length < 10) {
                            Toast.makeText(context, "Please enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!isLoginOtpSent) {
                            isLoginOtpSent = true
                            loginError = null
                            Toast.makeText(context, "Mock OTP sent successfully to +91 $loginPhone", Toast.LENGTH_SHORT).show()
                        } else {
                            if (loginOtp.isEmpty()) {
                                Toast.makeText(context, "Please enter mock OTP", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.loginWithPhone(
                                phone = loginPhone,
                                onSuccess = {
                                    Toast.makeText(context, "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    loginError = error
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        if (!isLoginOtpSent) L10n.getString("send_otp") else L10n.getString("verify_and_login"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = {
                        isOtpLoginOption = false
                        loginError = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(L10n.getString("back_to_password_login"), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        } else {
            // REGISTER TAB VIEW
            OutlinedTextField(
                value = regName,
                onValueChange = { regName = it },
                label = { Text(L10n.getString("full_name")) },
                placeholder = { Text("e.g. Arjun Kumar") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = regPhone,
                onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) regPhone = it },
                label = { Text("Phone Number") },
                placeholder = { Text("e.g. 9876543210") },
                prefix = { Text("+91 ") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Call, null) }
            )

            if (regError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    regError!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start).padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "I want to register as:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start).padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Selection cards for role: customer (consumer), shop (merchant), and captain (delivery)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Customer selection card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { regRole = "consumer" }
                        .border(
                            width = 2.dp,
                            color = if (regRole == "consumer") MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (regRole == "consumer") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🐶🐱🐹", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Customer", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        Text("Buy products", fontSize = 8.sp, color = Color.Gray, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }

                // Shop selection card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { regRole = "merchant" }
                        .border(
                            width = 2.dp,
                            color = if (regRole == "merchant") MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (regRole == "merchant") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏪🦜🏥", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Pet Shop", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        Text("Sell items", fontSize = 8.sp, color = Color.Gray, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }

                // Captain selection card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { regRole = "captain" }
                        .border(
                            width = 2.dp,
                            color = if (regRole == "captain") MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (regRole == "captain") MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🛵📦💨", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Captain", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                        Text("Deliver food", fontSize = 8.sp, color = Color.Gray, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }

            if (regRole == "consumer") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = regEmail,
                    onValueChange = { regEmail = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("e.g. arjun@gmail.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Email, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = regPassword,
                    onValueChange = { regPassword = it },
                    label = { Text("Set Password") },
                    placeholder = { Text("Minimum 6 characters") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Lock, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = regPetName,
                    onValueChange = { regPetName = it },
                    label = { Text("Pet Name 🐶") },
                    placeholder = { Text("e.g. Buddy, Max, Rocky") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Favorite, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = regPetBreed,
                    onValueChange = { regPetBreed = it },
                    label = { Text("Pet Breed 🐕") },
                    placeholder = { Text("e.g. Golden Retriever, Beagle") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Star, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = regPetAge,
                    onValueChange = { regPetAge = it },
                    label = { Text("Pet Age 🎂") },
                    placeholder = { Text("e.g. 2 years, 6 months") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.DateRange, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = regPetWeight,
                    onValueChange = { regPetWeight = it },
                    label = { Text("Pet Weight ⚖️") },
                    placeholder = { Text("e.g. 24 kg, 12.5 kg") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Info, null) }
                )

                Spacer(modifier = Modifier.height(16.dp))
                DocumentAttachmentCard(
                    title = "Profile Photo (Selfie with Dog) 🤳",
                    currentValue = regSelfieUrl,
                    onValueChange = { regSelfieUrl = it },
                    onSelectFileClick = { consumerSelfieLauncher.launch("image/*") },
                    onTakePhotoClick = { cameraConsumerSelfieLauncher.launch() },
                    placeholderUrl = "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=150"
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Or choose a preset avatar from list:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    val selfies = listOf(
                        "https://images.unsplash.com/photo-1548199973-03cce0bbc87b?w=150",
                        "https://images.unsplash.com/photo-1534361960057-19889db9621e?w=150",
                        "https://images.unsplash.com/photo-1587300003388-59208cc962cb?w=150",
                        "https://images.unsplash.com/photo-1552053831-71594a27632d?w=150"
                    )
                    items(selfies) { url ->
                        val isSelected = regSelfieUrl == url
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 3.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { regSelfieUrl = url }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(url),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            if (regRole == "captain") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = regVehicleNumber,
                    onValueChange = { regVehicleNumber = it },
                    label = { Text("Vehicle Registration Number 🛵") },
                    placeholder = { Text("e.g. TS-09-EA-1234") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Info, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = regPanCard,
                    onValueChange = { regPanCard = it },
                    label = { Text("PAN Card Number 💳") },
                    placeholder = { Text("e.g. ABCDE1234F") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = regAadharNumber,
                    onValueChange = { if (it.length <= 12 && it.all { char -> char.isDigit() }) regAadharNumber = it },
                    label = { Text("Aadhar Card Number 🆔") },
                    placeholder = { Text("e.g. 123456789012") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null) }
                )

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = regBankDetails,
                    onValueChange = { regBankDetails = it },
                    label = { Text("Bank Details (A/C & IFSC) 🏦") },
                    placeholder = { Text("e.g. HDFC0001234 - A/C 50100123456789") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.ShoppingCart, null) }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Visual Document Verification 📸",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                DocumentAttachmentCard(
                    title = "PAN Card Photo 💳",
                    currentValue = regPanCardUrl,
                    onValueChange = { regPanCardUrl = it },
                    onSelectFileClick = { panCardLauncher.launch("image/*") },
                    onTakePhotoClick = { cameraPanCardLauncher.launch() },
                    placeholderUrl = "https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?w=400"
                )

                DocumentAttachmentCard(
                    title = "Aadhar Card Photo 🆔",
                    currentValue = regAadharCardUrl,
                    onValueChange = { regAadharCardUrl = it },
                    onSelectFileClick = { aadharCardLauncher.launch("image/*") },
                    onTakePhotoClick = { cameraAadharCardLauncher.launch() },
                    placeholderUrl = "https://images.unsplash.com/photo-1589758438368-0ad531db3366?w=400"
                )

                DocumentAttachmentCard(
                    title = "Driving License Photo 🛵",
                    currentValue = regLicenseUrl,
                    onValueChange = { regLicenseUrl = it },
                    onSelectFileClick = { licenseLauncher.launch("image/*") },
                    onTakePhotoClick = { cameraLicenseLauncher.launch() },
                    placeholderUrl = "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=400"
                )

                DocumentAttachmentCard(
                    title = "Live Selfie (Profile Pic) 🤳",
                    currentValue = regSelfieUrlForCaptain,
                    onValueChange = { regSelfieUrlForCaptain = it },
                    onSelectFileClick = { selfieLauncher.launch("image/*") },
                    onTakePhotoClick = { cameraSelfieLauncher.launch() },
                    placeholderUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200"
                )
            }

            if (isRegOtpSent) {
                Spacer(modifier = Modifier.height(20.dp))
                OutlinedTextField(
                    value = regOtp,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) regOtp = it },
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

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (regName.trim().isEmpty()) {
                        Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (regPhone.length < 10) {
                        Toast.makeText(context, "Please enter a valid 10-digit number", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (regRole == "consumer") {
                        if (regEmail.trim().isEmpty() || !regEmail.endsWith("@gmail.com", ignoreCase = true)) {
                            Toast.makeText(context, "Please enter a valid Gmail address (ending with @gmail.com)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (regPassword.length < 6) {
                            Toast.makeText(context, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (regPetName.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter your pet's name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }
                    if (regRole == "captain") {
                        if (regVehicleNumber.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter vehicle registration number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (regPanCard.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter PAN card", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (regAadharNumber.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter Aadhar number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (regBankDetails.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter bank details", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                    }
                    if (!isRegOtpSent) {
                        isRegOtpSent = true
                        regError = null
                        Toast.makeText(context, "Mock OTP sent successfully to +91 $regPhone", Toast.LENGTH_SHORT).show()
                    } else {
                        if (regOtp.isEmpty()) {
                            Toast.makeText(context, "Please enter mock OTP", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (regRole == "captain") {
                            viewModel.registerCaptain(
                                fullName = regName,
                                phone = regPhone,
                                vehicleNumber = regVehicleNumber,
                                panCard = regPanCard,
                                bankDetails = regBankDetails,
                                aadharNumber = regAadharNumber,
                                panCardUrl = regPanCardUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?w=400" },
                                aadharCardUrl = regAadharCardUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1589758438368-0ad531db3366?w=400" },
                                licenseUrl = regLicenseUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=400" },
                                selfieUrl = regSelfieUrlForCaptain.trim().ifEmpty { "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200" },
                                onSuccess = {
                                    Toast.makeText(context, "Captain registered successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    regError = error
                                }
                            )
                        } else {
                            viewModel.registerWithPhone(
                                fullName = regName,
                                phone = regPhone,
                                role = regRole,
                                petName = regPetName,
                                petBreed = regPetBreed,
                                petAge = regPetAge,
                                petWeight = regPetWeight,
                                avatarUrl = regSelfieUrl,
                                email = regEmail,
                                password = regPassword,
                                onSuccess = {
                                    Toast.makeText(context, "Registered successfully!", Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    regError = error
                                }
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    if (!isRegOtpSent) "Send OTP" else "Verify & Register",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun DocumentAttachmentCard(
    title: String,
    currentValue: String,
    onValueChange: (String) -> Unit,
    onSelectFileClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    placeholderUrl: String
) {
    var showUrlInput by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Status indicator
                val isAttached = currentValue.isNotEmpty()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isAttached) Color(0xFF2DB37A) else Color(0xFFE53935))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAttached) "Attached" else "Pending",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAttached) Color(0xFF2DB37A) else Color(0xFFE53935)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Visual Preview Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (currentValue.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(currentValue),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "No attachment provided",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Custom attachment action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSelectFileClick,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery 📁", fontSize = 12.sp)
                }

                Button(
                    onClick = onTakePhotoClick,
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Camera 📸", fontSize = 12.sp)
                }

                IconButton(
                    onClick = { showUrlInput = !showUrlInput },
                    modifier = Modifier
                        .background(
                            color = if (showUrlInput) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "URL Link",
                        tint = if (showUrlInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (showUrlInput) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = onValueChange,
                    label = { Text("Or paste image URL 🔗") },
                    placeholder = { Text("e.g. https://images.unsplash.com/photo-...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                )
            }
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

    var showSimulationDialog by remember { mutableStateOf(false) }
    var showComingSoonDialog by remember { mutableStateOf(false) }
    var unserviceableCityName by remember { mutableStateOf("") }

    fun handleLocationResult(result: LocationResult) {
        when (result) {
            is LocationResult.Serviceable -> {
                Toast.makeText(
                    context,
                    "Auto-detected: ${result.city.name}, ${result.city.state} (within ${String.format("%.1f", result.distanceKm)} km)",
                    Toast.LENGTH_LONG
                ).show()
                onCityPicked(result.city.id, "${result.city.name}, ${result.city.state}")
            }
            is LocationResult.NotServiceable -> {
                unserviceableCityName = "${result.city.name}, ${result.city.state}"
                showComingSoonDialog = true
            }
            is LocationResult.Error -> {
                Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isDetecting = true
            viewModel.detectLocation(context) { result ->
                isDetecting = false
                handleLocationResult(result)
            }
        } else {
            Toast.makeText(context, "Location permission denied. Showing simulator options.", Toast.LENGTH_LONG).show()
            showSimulationDialog = true
        }
    }

    // Coming Soon Dialog (Rollout Control warning)
    if (showComingSoonDialog) {
        AlertDialog(
            onDismissRequest = { showComingSoonDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Coming Soon! 🐾",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "We currently don't service $unserviceableCityName yet.",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To ensure quality care, Swiggy Paws is rolling out phase-by-phase. Currently, we only service Hyderabad, Bengaluru, and Chennai.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showComingSoonDialog = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    // Location Simulation Dialog
    if (showSimulationDialog) {
        AlertDialog(
            onDismissRequest = { showSimulationDialog = false },
            title = {
                Text(
                    "Select Location Source",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Test real GPS or simulate mock coordinates for rollout checks:",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // Device GPS Option
                    Button(
                        onClick = {
                            showSimulationDialog = false
                            isDetecting = true
                            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.ACCESS_FINE_LOCATION
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            
                            if (hasPermission) {
                                viewModel.detectLocation(context) { result ->
                                    isDetecting = false
                                    handleLocationResult(result)
                                }
                            } else {
                                isDetecting = false
                                permissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Real Device GPS", color = Color.White)
                        }
                    }
                    
                    Text(
                        "Presets (Active Service Areas):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                showSimulationDialog = false
                                isDetecting = true
                                scope.launch {
                                    delay(1000)
                                    isDetecting = false
                                    val result = viewModel.detectLocationAndCheckService(17.3850, 78.4867) // Hyd
                                    handleLocationResult(result)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Hyderabad", fontSize = 11.sp)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                showSimulationDialog = false
                                isDetecting = true
                                scope.launch {
                                    delay(1000)
                                    isDetecting = false
                                    val result = viewModel.detectLocationAndCheckService(12.9716, 77.5946) // Blr
                                    handleLocationResult(result)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Bengaluru", fontSize = 11.sp)
                        }
                    }
                    
                    OutlinedButton(
                        onClick = {
                            showSimulationDialog = false
                            isDetecting = true
                            scope.launch {
                                delay(1000)
                                isDetecting = false
                                val result = viewModel.detectLocationAndCheckService(13.0827, 80.2707) // Chennai
                                handleLocationResult(result)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Chennai (Active)")
                    }
                    
                    Text(
                        "Rollout Controlled (Coming Soon / Disabled):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                showSimulationDialog = false
                                isDetecting = true
                                scope.launch {
                                    delay(1000)
                                    isDetecting = false
                                    val result = viewModel.detectLocationAndCheckService(28.6139, 77.2090) // Delhi
                                    handleLocationResult(result)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Delhi", fontSize = 11.sp)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                showSimulationDialog = false
                                isDetecting = true
                                scope.launch {
                                    delay(1000)
                                    isDetecting = false
                                    val result = viewModel.detectLocationAndCheckService(19.0760, 72.8777) // Mumbai
                                    handleLocationResult(result)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Text("Mumbai", fontSize = 11.sp)
                        }
                    }
                    
                    OutlinedButton(
                        onClick = {
                            showSimulationDialog = false
                            isDetecting = true
                            scope.launch {
                                delay(1000)
                                isDetecting = false
                                val result = viewModel.detectLocationAndCheckService(51.5074, -0.1278) // London
                                handleLocationResult(result)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                    ) {
                        Text("Simulate London (Out of Range)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSimulationDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

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
            L10n.getString("select_city_title"),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            L10n.getString("select_city_desc"),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        // GPS Auto-detect Button
        OutlinedButton(
            onClick = {
                showSimulationDialog = true
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
                    onClick = { onCityPicked(city.id, "${city.name}, ${city.state}") },
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
                                Text("${city.name}, ${city.state}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

data class PetCareGuide(
    val id: String,
    val title: String,
    val summary: String,
    val content: String,
    val emoji: String,
    val productIds: List<String>,
    val gradientColors: List<Color>
)

// ==========================================
// SCREEN 4.5: DELIVERY CAPTAIN DASHBOARD
// ==========================================
@Composable
fun CaptainDashboardScreen(viewModel: PawsViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentCaptain by viewModel.currentCaptain.collectAsState()
    val allOrders by viewModel.allOrders.collectAsState()
    val shopsList by viewModel.shops.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FC))
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Dashboard Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Swiggy Paws Captain 🛵",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFC8019) // Swiggy Orange
                )
                Text(
                    text = "Welcome back, ${currentUser?.fullName ?: "Captain"}!",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            IconButton(onClick = { viewModel.logout() }) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (currentCaptain == null) {
            // Seeding loading state or just circular progress
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFC8019))
                }
            }
        } else {
            val captain = currentCaptain!!

            when (captain.status) {
                "pending" -> {
                    // Pending onboarding state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4EB)), // Light orange
                        border = BorderStroke(1.5.dp, Color(0xFFFC8019))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "⏳ Application Under Review",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFC8019)
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(
                                text = "Your onboarding verification is in progress. The Swiggy onboarding team is currently validating your vehicle & tax records. This process usually completes within 24 hours.",
                                fontSize = 13.sp,
                                color = Color(0xFF4A4A4A),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0xFFFC8019).copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Submitted Details:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.align(Alignment.Start),
                                color = Color(0xFF333333)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            DetailRow(label = "Vehicle Registration No", value = captain.vehicleNumber)
                            DetailRow(label = "PAN Card Number", value = captain.panCard.takeLast(4).padStart(captain.panCard.length, '*'))
                            DetailRow(label = "Aadhar Card Number", value = "XXXX-XXXX-" + captain.aadharNumber.takeLast(4))
                            DetailRow(label = "Bank Details", value = captain.bankDetails.takeLast(4).padStart(captain.bankDetails.length, '*'))
                        }
                    }
                }
                "rejected" -> {
                    // Rejected onboarding state
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2F2)), // Light Red
                        border = BorderStroke(1.5.dp, Color.Red)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "❌ Onboarding Rejected",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(
                                text = "Unfortunately, your application was declined because the documents submitted could not be verified. Please contact super admin or re-register with accurate details.",
                                fontSize = 13.sp,
                                color = Color(0xFF4A4A4A),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                "approved" -> {
                    // Active Captain Delivery Portal
                    val isShiftOnline = captain.isActive

                    // Shift Switch Status
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isShiftOnline) Color(0xFF2DB37A) else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isShiftOnline) "Online • Ready for Deliveries" else "Offline • Shift Paused",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isShiftOnline) Color(0xFF2DB37A) else Color.Gray
                                )
                            }
                            Switch(
                                checked = isShiftOnline,
                                onCheckedChange = { isOnline ->
                                    viewModel.toggleCaptainOnlineStatus(captain.id, isOnline)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF2DB37A)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Compute dynamic metrics
                    val completedOrders = allOrders.filter { it.captainId == captain.id && it.status == "delivered" }
                    val tripsCount = completedOrders.size
                    val todayEarnings = tripsCount * 45.0 // ₹45 per delivery payout

                    // Delivery stats dashboard grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Today's Earnings", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("₹${String.format("%.2f", todayEarnings)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Trips Completed", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$tripsCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Rating", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("5.0 ⭐", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Active orders or radar scanner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isShiftOnline) {
                                val activeJob = allOrders.firstOrNull { it.captainId == captain.id && (it.status == "preparing" || it.status == "out_for_delivery") }
                                val availableJob = allOrders.firstOrNull { it.status == "accepted" && it.captainId.isNullOrEmpty() }

                                if (activeJob != null) {
                                    val activeShop = shopsList.find { it.id == activeJob.shopId }
                                    val activeShopName = activeShop?.name ?: "Local Pet Shop"

                                    Text(
                                        text = "📦 Active Delivery In Progress",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFC8019)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Order Ref: #${activeJob.id.take(8).uppercase()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("📍 Pickup: $activeShopName", fontSize = 12.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("🏁 Deliver to: ${activeJob.deliveryAddress}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("💵 Delivery Earning: ₹45.00", fontSize = 13.sp, color = Color(0xFF2DB37A), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { 
                                            viewModel.completeDeliveryJob(activeJob.id)
                                            Toast.makeText(context, "Order delivered successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DB37A))
                                    ) {
                                        Text("MARK AS DELIVERED ✓", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else if (availableJob != null) {
                                    val availableShop = shopsList.find { it.id == availableJob.shopId }
                                    val availableShopName = availableShop?.name ?: "Local Pet Supplies"

                                    Text(
                                        text = "🚨 Active Delivery Request Found!",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFC8019)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4EB)),
                                        border = BorderStroke(1.dp, Color(0xFFFC8019).copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text("Order Ref: #${availableJob.id.take(8).uppercase()}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("📍 Store: $availableShopName", fontSize = 12.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("🏁 Delivery Address: ${availableJob.deliveryAddress}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("Est. Time: 25 mins", fontSize = 12.sp, color = Color.Gray)
                                                Text("Payout: ₹45.00", fontSize = 13.sp, color = Color(0xFF2DB37A), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { 
                                            viewModel.acceptDeliveryJob(availableJob.id, captain.id)
                                            Toast.makeText(context, "Delivery job accepted! Navigate to store.", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019))
                                    ) {
                                        Text("ACCEPT DELIVERY JOB 🛵", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text(
                                        text = "Searching for Delivery Jobs...",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFC8019)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Ready to deliver pet food orders & accessories in Hyderabad!",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    CircularProgressIndicator(color = Color(0xFFFC8019))
                                }
                            } else {
                                Text(
                                    text = "Shift is Offline",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Go online to start receiving food delivery orders from pet stores.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Profile & Vehicle details card
                    Text("Verified Profile Details", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            DetailRow(label = "Delivery Partner ID", value = captain.id)
                            DetailRow(label = "Verified Vehicle No", value = captain.vehicleNumber)
                            DetailRow(label = "Registered PAN No", value = captain.panCard.takeLast(4).padStart(captain.panCard.length, '*'))
                            DetailRow(label = "Registered Aadhar", value = "XXXX-XXXX-" + captain.aadharNumber.takeLast(4))
                            DetailRow(label = "Payment Bank Info", value = captain.bankDetails.takeLast(4).padStart(captain.bankDetails.length, '*'))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
    }
}

// ==========================================
// SCREEN 5: CONSUMER HOME / DISCUSSION DISCOVERY
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PawsViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()

    if (currentUser?.role == "captain") {
        CaptainDashboardScreen(viewModel = viewModel)
        return
    }

    val shopsList by viewModel.shops.collectAsState()
    val categoryList by viewModel.categories.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val petProblems by viewModel.petProblems.collectAsState()
    var selectedProblemId by remember { mutableStateOf<String?>(null) }
    var activeRemedyProductDetail by remember { mutableStateOf<ProductEntity?>(null) }
    var selectedGuide by remember { mutableStateOf<PetCareGuide?>(null) }
    // remediesExpanded hoisted
    var remediesExpanded by remember { mutableStateOf(false) }
    var auctionExpanded by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val petCareGuides = remember {
        listOf(
            PetCareGuide(
                id = "guide_coat",
                title = "Silky Smooth Coat Guide",
                summary = "Secrets to prevent shedding, dry skin, and dandruff for a radiant hair coat.",
                content = "A healthy coat starts from the inside out and is maintained with premium topical care:\n\n• **Regular Grooming**: Bathe your dog every 2-4 weeks with medicated oat-meal or tea-tree oil shampoos. This calms active skin inflammation, moisturizes the epidermal barrier, and washes away dandruff flakes.\n\n• **Omega Fatty Acids**: Salmon oil and wild-caught sea fish are packed with DHA and EPA, which stimulate hair root follicles, reduce shedding by up to 70%, and restore natural oils to dry fur.\n\n• **Brush Daily**: Regular brushing distributes natural skin oils along the hair shaft, removes dead undercoat fur, and prevents painful mats.",
                emoji = "🐕💈",
                productIds = listOf("p_shampoo_itch", "p_shampoo_dandruff", "p_blr_1"),
                gradientColors = listOf(Color(0xFF6B1A24), Color(0xFF9E2A2B))
            ),
            PetCareGuide(
                id = "guide_immunity",
                title = "Immunity & Joint Vitality",
                summary = "Vitamins, glucosamine, and supplement diets recommended by veterinarians.",
                content = "Provide your pet with structured nutrients to boost bone density and build robust immunity:\n\n• **Glucosamine & MSM**: Crucial for senior dogs and active breeds (like Retrievers or Shepherds) to rebuild joint cartilage, prevent arthritis, and maintain peak mobility.\n\n• **Immune Minerals**: Zinc, Vitamin E, and organic Selenium act as robust antioxidants, protecting cells from disease and ensuring optimal liver/kidney functions.\n\n• **Deworming Integrity**: Keep internal parasites at bay by scheduling standard deworming pills every 3 months.",
                emoji = "🍗💊",
                productIds = listOf("p_hyd_4", "p_hyd_1"),
                gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
            ),
            PetCareGuide(
                id = "guide_digestion",
                title = "Gourmet Taste & Digestion",
                summary = "How to select wet food, grain-free kibble, and protein preferences.",
                content = "Maximize your dog's interest in their meals while optimizing gastrointestinal digestion:\n\n• **Understand Preferences**: Dogs are natural carnivores who love high-moisture foods. Mixing dry kibbles with high-quality wet food (like mackerel or salmon cuts) improves hydration and appetite.\n\n• **Grain-Free Advantages**: Grain-sensitive pets benefit highly from sweet potato or tapioca carb sources instead of cheap wheat fillers, reducing flatulence and skin allergies.\n\n• **Safe Treats**: Offer protein-rich, dehydrated single-ingredient treats (like chicken jerky or cod skins) rather than processed flour biscuits.",
                emoji = "🥩😋",
                productIds = listOf("p_blr_1", "p_hyd_1"),
                gradientColors = listOf(Color(0xFF0F291B), Color(0xFF1E5E3A))
            )
        )
    }
    val selectedCityName by viewModel.selectedCityName.collectAsState()
    val selectedCityId by viewModel.selectedCityId.collectAsState()
    val dynamicBanners by viewModel.targetedBanners.collectAsState()
    val context = LocalContext.current
    val syncState by viewModel.powerSyncState.collectAsState()
    val isSwiggyOne by viewModel.isSwiggyOneSubscriber.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLanguage by viewModel.appLanguage.collectAsState()

    // Query state links
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryIds by viewModel.selectedCategoryIds.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val filterOpenNow by viewModel.filterOpenNow.collectAsState()
    val filterDelivery by viewModel.filterDelivery.collectAsState()
    val filterRating by viewModel.filterRating.collectAsState()

    val allOrders by viewModel.allOrders.collectAsState()
    val allOrderItems by viewModel.allOrderItems.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()

    var showCityPickerSheet by remember { mutableStateOf(false) }

    // Multi-criteria client side filter + sort orchestration
    val processedShops = remember(shopsList, searchQuery, selectedCategoryIds, sortType, filterOpenNow, filterDelivery, filterRating, allOrders, allOrderItems, allProducts) {
        var result = shopsList.filter { shop ->
            val matchesQuery = shop.name.contains(searchQuery, ignoreCase = true) || 
                               shop.locality.contains(searchQuery, ignoreCase = true)
            val matchesCategory = if (selectedCategoryIds.isEmpty()) {
                true
            } else {
                selectedCategoryIds.any { catId ->
                    if (catId == "cat_groom") {
                        shop.name.contains("groom", ignoreCase = true) || 
                        shop.name.contains("styling", ignoreCase = true) || 
                        shop.description.contains("groom", ignoreCase = true) || 
                        shop.description.contains("styling", ignoreCase = true)
                    } else {
                        allProducts.any { it.shopId == shop.id && it.categoryId == catId }
                    }
                }
            }
            matchesQuery && matchesCategory
        }

        if (filterOpenNow) result = result.filter { it.isOpen }
        if (filterDelivery) result = result.filter { it.deliveryAvailable }
        if (filterRating) result = result.filter { it.rating >= 4.5 }

        // Placement Scoring logic helper function
        fun getShopPlacementScore(shop: ShopEntity): Double {
            val totalOrders = allOrders.count { it.shopId == shop.id }
            val deliveredOrderIds = allOrders.filter { it.shopId == shop.id && it.status == "delivered" }.map { it.id }.toSet()
            val totalDeliveredProducts = allOrderItems.filter { it.orderId in deliveredOrderIds }.sumOf { it.quantity }
            return (totalOrders * 1.0) + (totalDeliveredProducts * 2.0) + (shop.rating * 10.0)
        }

        when (sortType) {
            "Popular 🏆" -> result.sortedByDescending { getShopPlacementScore(it) }
            "Top Rated" -> result.sortedByDescending { it.rating }
            "New" -> result.sortedByDescending { it.createdAt }
            "A-Z" -> result.sortedBy { it.name }
            else -> result.sortedByDescending { getShopPlacementScore(it) } // Default descending score placement
        }
    }

    val mockShops = remember {
        listOf(
            ShopEntity(
                id = "mock_posh_paws",
                ownerId = "system",
                cityId = "hyd",
                name = "The Posh Paws",
                description = "Luxury Accessories & Food",
                address = "Road No 2, Banjara Hills, Hyderabad",
                locality = "Banjara Hills",
                phone = "9876543210",
                email = "posh@paws.com",
                photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?auto=format&fit=crop&q=80&w=600"),
                isOpen = true,
                opensAt = "08:00",
                closesAt = "22:00",
                rating = 4.8,
                totalReviews = 42,
                deliveryAvailable = true,
                isVerified = true,
                isActive = true,
                isFeatured = true,
                groomingEnabled = false,
                vetClinicEnabled = false
            ),
            ShopEntity(
                id = "mock_healthy_hounds",
                ownerId = "system",
                cityId = "hyd",
                name = "Healthy Hounds Pantry",
                description = "Organic & Raw Diet Specialist",
                address = "Phase 2, Jubilee Hills, Hyderabad",
                locality = "Jubilee Hills",
                phone = "9876543211",
                email = "healthy@paws.com",
                photos = listOf("https://images.unsplash.com/photo-1583337130417-3346a1be7dee?auto=format&fit=crop&q=80&w=600"),
                isOpen = true,
                opensAt = "09:00",
                closesAt = "21:00",
                rating = 4.5,
                totalReviews = 88,
                deliveryAvailable = true,
                isVerified = true,
                isActive = true,
                isFeatured = true,
                groomingEnabled = false,
                vetClinicEnabled = false
            ),
            ShopEntity(
                id = "mock_city_hospital",
                ownerId = "system",
                cityId = "hyd",
                name = "City Pet Hospital",
                description = "24/7 Emergency & Surgery",
                address = "Metro Station Road, Madhapur, Hyderabad",
                locality = "Madhapur",
                phone = "9876543212",
                email = "cityhospital@paws.com",
                photos = listOf("https://images.unsplash.com/photo-1597633425046-08f5110420b5?auto=format&fit=crop&q=80&w=600"),
                isOpen = true,
                opensAt = "07:00",
                closesAt = "23:00",
                rating = 4.9,
                totalReviews = 19,
                deliveryAvailable = true,
                isVerified = true,
                isActive = true,
                isFeatured = false,
                groomingEnabled = false,
                vetClinicEnabled = true
            ),
            ShopEntity(
                id = "mock_fluffy_friends",
                ownerId = "system",
                cityId = "hyd",
                name = "Fluffy Friends",
                description = "Full Grooming & Spa",
                address = "Hitech City, Hyderabad",
                locality = "Hitech City",
                phone = "9876543213",
                email = "fluffy@paws.com",
                photos = emptyList(),
                isOpen = true,
                opensAt = "09:00",
                closesAt = "20:00",
                rating = 4.8,
                totalReviews = 56,
                deliveryAvailable = true,
                isVerified = true,
                isActive = true,
                isFeatured = false,
                groomingEnabled = true,
                vetClinicEnabled = false
            ),
            ShopEntity(
                id = "mock_paw_spa",
                ownerId = "system",
                cityId = "hyd",
                name = "Paw Spa",
                description = "Bath & Nail Clipping",
                address = "Kondapur, Hyderabad",
                locality = "Kondapur",
                phone = "9876543214",
                email = "pawspa@paws.com",
                photos = emptyList(),
                isOpen = true,
                opensAt = "10:00",
                closesAt = "19:00",
                rating = 4.6,
                totalReviews = 27,
                deliveryAvailable = true,
                isVerified = true,
                isActive = true,
                isFeatured = false,
                groomingEnabled = true,
                vetClinicEnabled = false
            )
        )
    }

    val displayShops = remember(processedShops, selectedCityId, mockShops, searchQuery, selectedCategoryIds, filterOpenNow, filterDelivery, filterRating) {
        if (selectedCityId == "hyd" && processedShops.size < 4) {
            val matchedMocks = mockShops.filter { shop ->
                val matchesQuery = searchQuery.isEmpty() ||
                                   shop.name.contains(searchQuery, ignoreCase = true) || 
                                   shop.locality.contains(searchQuery, ignoreCase = true) ||
                                   shop.description.contains(searchQuery, ignoreCase = true)
                val matchesCategory = if (selectedCategoryIds.isEmpty()) {
                    true
                } else {
                    selectedCategoryIds.any { catId ->
                        if (catId == "cat_groom") {
                            shop.groomingEnabled
                        } else if (catId == "cat_vet") {
                            shop.vetClinicEnabled
                        } else {
                            true
                        }
                    }
                }
                val matchesOpen = !filterOpenNow || shop.isOpen
                val matchesDelivery = !filterDelivery || shop.deliveryAvailable
                val matchesRating = !filterRating || shop.rating >= 4.5
                
                matchesQuery && matchesCategory && matchesOpen && matchesDelivery && matchesRating
            }
            
            val existingIds = processedShops.map { it.id }.toSet()
            val uniqueMocks = matchedMocks.filter { it.id !in existingIds }
            processedShops + uniqueMocks
        } else {
            processedShops
        }
    }

    val baseIndexForShops = 8
    val remediesIndex = baseIndexForShops + if (displayShops.isEmpty()) 1 else displayShops.size
    val auctionIndex = remediesIndex + 1

    val premiumShops = remember(displayShops) {
        displayShops.filter { !it.vetClinicEnabled && !it.groomingEnabled && (it.isFeatured || it.rating >= 4.5) }
    }
    val hospitalShops = remember(displayShops) {
        displayShops.filter { it.vetClinicEnabled }
    }
    val groomingShops = remember(displayShops) {
        displayShops.filter { it.groomingEnabled }
    }

        // ── MAIN FEED CONTENT ──────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
        // App Header Row
        item {
            HomeHeader(
                cityName = selectedCityName,
                userName = currentUser?.fullName ?: "Pet Owner",
                avatarUrl = currentUser?.avatarUrl ?: "",
                petName = currentUser?.petName ?: "Buddy",
                onCityClick = { showCityPickerSheet = true },
                onProfileClick = { viewModel.navigateTo(Screen.UserProfile) },
                onChatClick = { viewModel.navigateTo(Screen.ChatList) },
                onLanguageClick = { showLanguageDialog = true },
                syncState = syncState,
                onSyncClick = { viewModel.triggerManualPowerSync() }
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

        // ── QUICK ACCESS SHORTCUTS ROW ──────────────────────────────────────────────
        item {
            val shortcuts = listOf(
                Triple(Icons.Default.Favorite, "Favourites", { viewModel.navigateTo(Screen.SavedShops) }),
                Triple(Icons.Default.ShoppingCart, "Orders", { viewModel.navigateTo(Screen.Cart) }),
                Triple(Icons.Default.Star, "New Arrivals", {
                    viewModel.updateSearchQuery("New Arrivals")
                    viewModel.navigateTo(Screen.Search)
                })
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                shortcuts.forEach { (icon, label, action) ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { action() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF004AC6).copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(24.dp),
                                tint = Color(0xFF004AC6)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF004AC6),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Horizontally Scrolling Dynamic Promotional Banners Carousel
        if (dynamicBanners.isNotEmpty()) {
            item {
                DynamicPromoBannerCarousel(
                    banners = dynamicBanners,
                    onBannerClick = { banner ->
                        val targetShopId = banner.targetShopIds.firstOrNull()
                        if (targetShopId != null && targetShopId != "all") {
                            viewModel.navigateTo(Screen.ShopDetail(targetShopId))
                        } else {
                            NotificationManager.fireInstantNotification(context, banner.title, banner.description)
                        }
                    }
                )
            }
        } else {
            // Fallback to featured shops carousel if no custom banners are active
            item {
                FeaturedBannerCarousel(shops = shopsList.filter { it.isFeatured }, onShopClick = {
                    viewModel.navigateTo(Screen.ShopDetail(it.id))
                })
            }
        }

        // ── CATEGORIES ROW ──────────────────────────────────────────────────────────
        if (categoryList.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = "What's on your pet's mind? 🐾",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 20.dp, bottom = 4.dp, top = 8.dp)
                    )
                    Text(
                        text = "Choose from premium foods, toys, grooming services & more",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoryList) { category ->
                            val isSelected = selectedCategoryIds.contains(category.id)
                            val categoryImageUrl = when (category.id) {
                                "cat_food" -> "https://lh3.googleusercontent.com/aida/AP1WRLtm0J5MuRuW4w4olkkflcjkSX-9bxCk23-GrLzJRiqZ3_Zhhy2q5eirMvjXv9zBuFs-CX_wO-6hy7L6dSiuCztDtcMr-ivqfjh1miBnYaJCBqeENN0uVuip23wlVEFdtlPGQnFLRX3DX5fIQbX-zxuI7suRgPsZfrgEES97W4eShI8nPsgFqE4D5gfrllobU8d8bK_gIhaDhfBFjG_xFI4evQH34o8Zj-nfArMBARfijbV2pPvCBxZhVNc"
                                "cat_treats" -> "https://lh3.googleusercontent.com/aida/AP1WRLu2j9XUv_Re8vRJWJIq3otI7IoX2NgLk7u6dz-Q8YcKZM56ZhlEEG2hhVjR_8a8TLk1hk-4Tl9pMOlZyJGEzTBYJn_bkhNE9uLeijZ6EWFbm_jLi4gI6TBl8Gw26ZsyNvJfA59F0JWB29hKlzMIleF_-IFt7EarCxyfY7nXTlogfRnnfkUY63uBoXugR7m67gBg3tiS5d5irmOdJvPRSbNWfAdde8rkOop9HKp5a_RfrM4tCAPcR1ICVDY"
                                "cat_toys" -> "https://lh3.googleusercontent.com/aida/AP1WRLtxvZTVCFJV_VWo3wS_Re5Qq0invY-1glo_OjI0J4hnMPR5MsjoVIKEEB49TttNQJp72uxI1VSi0wNPgKqXEtHAdcpBZuG27EMpH_1RfaFojtTrIRgPUv35DFFVg-9ecM4jajxDGRfXVSmtgvzxmHysSJYFIWRJ2SMSFm1lNrv4u-Ghvt7H5oCHglK5OaRJ6K3T0Op6_cCx801FfzvuWPMT2d8gdn5EVH4KwwsSeKMDpOeSeMGimOg2jHY"
                                "cat_groom" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuCLDcsiQzTJ35jcCpCNHSC0CPGtsB--0Xdb-LVHpAoteDtktABgPSTQMMPGcfAgwvMEa22Twz_PWoxMANUVHDlfmcOgn53ytuQl7eHMq2kD2oBJX8mNowGEJjxAIHOdSyARgHYwDg6TFxoXYoYnVogC8c3QqEQxzKXQHBhPxhv1VK3mWc1o8kwr-eyteIwsACN_yi3C9LZwRdXcVVbk_7sQFr6t-JFQsx7yaIuZTVNVZeEEPbhBBDvdW00lu99huqxwo4ClJpdhVnY"
                                "cat_travel" -> "https://lh3.googleusercontent.com/aida/AP1WRLtGMKTRDD97W5R-veADg2j3lfveNZ2oerY0hevdFnQgDaTJbt99ZIlpABlOxwf1kGlPaGHbVZr7PUJCnX_EqmzDgRXFImh97MiasRQu_kNuciIDGp_S8g_B8MuPXvZLKO5kwtSueMsWt0dFZ7G13zv6VLEpf_WPg54CPQsq_GGFnu6K70UjqfxgalRLTUqFmz49cSXfKPKZstWqH44WXlNCUPSDxaZcoGPJPblVe195H6OdCKuPMTGxRTo"
                                "cat_furniture" -> "https://lh3.googleusercontent.com/aida/AP1WRLuM8GS_upn2HJga7jQOuDSmL5SqawQMTfZHP4nqQkhuqR7hiXqhvYa3W67xUXLK1TVCgShE7PifD6yxJiutvXETsQMmAT2cZfHUtxsAP6dr9aGy97dPGFz7JDrOFOKmBD2i1JaL5BAEkvwVyg1jfIfdMw9AolveynTuW2IBc26GAI91P9GHwtN0zPO-C-NrS4O2qOhN2XkYQpMSLFxGCOkqkdCUbWS9S21KyEinwpCw0eSDPZrcLq1iHXA"
                                "cat_waste" -> "https://lh3.googleusercontent.com/aida-public/AB6AXuA8-OnbYbH6ervRc4iDKjRxKLt6mO6wKvK8uA3YF7QqP3s6MzG7DILE7cEzhjoG1QhhOujkvk6kROOkrlX_HL2AqoacPYkIXR9PWO8eOCuNrkd24m2rUzV3v_SsO_Tt-eng-sTQpDJE-rHj2Ksx8Qw8uGaUZB-6jpIsSfhmFTkAVrxBXvue6givMDI98jjybom420pH3sbIUeml2Io6RygcKD0Xk279U3oRRXPXcZSjpIgZMptmDBLqWFDLWZce7mlSIJJ-aZXYgOs"
                                else -> category.iconUrl
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clickable {
                                        when (category.id) {
                                            "cat_food" -> viewModel.navigateTo(Screen.FoodNutrition)
                                            "cat_treats" -> viewModel.navigateTo(Screen.TreatsChews)
                                            "cat_toys" -> viewModel.navigateTo(Screen.ToysEnrichment)
                                            "cat_groom" -> viewModel.navigateTo(Screen.GroomingServices)
                                            "cat_travel" -> viewModel.navigateTo(Screen.TravelApparel)
                                            "cat_furniture" -> viewModel.navigateTo(Screen.FurnitureSleep)
                                            "cat_waste" -> viewModel.navigateTo(Screen.WasteManagement)
                                            else -> viewModel.toggleSelectedCategory(category.id)
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFD3E4FE),
                                            shape = CircleShape
                                        )
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(categoryImageUrl),
                                        contentDescription = category.name,
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = category.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSelected) Color(0xFFFC8019) else Color(0xFF434655),
                                    maxLines = 2,
                                    minLines = 2,
                                    textAlign = TextAlign.Center,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    }

                    // Horizontal sub-filters underneath
                    val subFilters = listOf("All", "Dry Food", "Wet Food", "Puppy", "Adult", "Senior")
                    var selectedSubFilter by remember { mutableStateOf("All") }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                    ) {
                        items(subFilters) { filter ->
                            val isSelected = selectedSubFilter == filter
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) Color(0xFF004AC6) else Color(0xFFE5EEFF),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedSubFilter = filter }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF004AC6)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Horizontally Scrolling Pet Care Guides Section
        item {
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Text(
                    "Pet Care & Nutrition Guides 📚",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, bottom = 4.dp, top = 8.dp)
                )
                Text(
                    "Expert tips and curated product recommendations",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(petCareGuides) { guide ->
                        Card(
                            modifier = Modifier
                                .width(280.dp)
                                .height(130.dp)
                                .clickable { selectedGuide = guide },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(guide.gradientColors)
                                    )
                                    .padding(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            guide.emoji,
                                            fontSize = 24.sp
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "READ GUIDE ➔",
                                                fontSize = 8.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            guide.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            guide.summary,
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Swiggy One pass removed per user request

        // ── ACTIVE FILTERS & SORTING ROW ──────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter badge / Reset Button
                if (selectedCategoryIds.isNotEmpty() || filterOpenNow || filterDelivery || filterRating) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFC8019), RoundedCornerShape(8.dp))
                            .clickable { viewModel.resetAllFilters() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Clear All ✕",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Open Now Pill
                FilterPill(
                    label = "Open Now",
                    active = filterOpenNow,
                    onClick = { viewModel.toggleFilterOpenNow() }
                )
                
                // Delivery Pill
                FilterPill(
                    label = "Free Delivery",
                    active = filterDelivery,
                    onClick = { viewModel.toggleFilterDelivery() }
                )
                
                // Rating Pill
                FilterPill(
                    label = "Top Rated (4.5+)",
                    active = filterRating,
                    onClick = { viewModel.toggleFilterRating() }
                )
            }
        }

        // Feed list
        if (displayShops.isEmpty()) {
            item {
                EmptyShopsState()
            }
        } else {
            // Row 1: Premium Pet Shops Nearby
            if (premiumShops.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = L10n.getString("premium_shops_nearby") + " 🏆",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, bottom = 4.dp, top = 8.dp)
                        )
                        Text(
                            text = "Handpicked elite stores with exceptional ratings near you",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(premiumShops) { shop ->
                                val totalOrders = allOrders.count { it.shopId == shop.id }
                                val deliveredOrderIds = allOrders.filter { it.shopId == shop.id && it.status == "delivered" }.map { it.id }.toSet()
                                val totalDeliveredProducts = allOrderItems.filter { it.orderId in deliveredOrderIds }.sumOf { it.quantity }
                                
                                HorizontalShopCard(
                                    shop = shop,
                                    totalOrders = totalOrders,
                                    totalDeliveredProducts = totalDeliveredProducts,
                                    onClick = { viewModel.navigateTo(Screen.ShopDetail(shop.id)) }
                                )
                            }
                        }
                    }
                }
            }

            // Row 2: Hospitals Nearby
            if (hospitalShops.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = L10n.getString("hospitals_nearby") + " 🏥",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, bottom = 4.dp, top = 8.dp)
                        )
                        Text(
                            text = "24/7 veterinary assistance and critical medical care",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(hospitalShops) { shop ->
                                val totalOrders = allOrders.count { it.shopId == shop.id }
                                val deliveredOrderIds = allOrders.filter { it.shopId == shop.id && it.status == "delivered" }.map { it.id }.toSet()
                                val totalDeliveredProducts = allOrderItems.filter { it.orderId in deliveredOrderIds }.sumOf { it.quantity }
                                
                                HorizontalShopCard(
                                    shop = shop,
                                    totalOrders = totalOrders,
                                    totalDeliveredProducts = totalDeliveredProducts,
                                    onClick = { viewModel.navigateTo(Screen.ShopDetail(shop.id)) }
                                )
                            }
                        }
                    }
                }
            }

            // Row 3: Grooming Nearby
            if (groomingShops.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            text = L10n.getString("grooming_nearby") + " ✂️",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 20.dp, bottom = 4.dp, top = 8.dp)
                        )
                        Text(
                            text = "Premium styling, bath, and pampering centers for your pets",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(groomingShops) { shop ->
                                val totalOrders = allOrders.count { it.shopId == shop.id }
                                val deliveredOrderIds = allOrders.filter { it.shopId == shop.id && it.status == "delivered" }.map { it.id }.toSet()
                                val totalDeliveredProducts = allOrderItems.filter { it.orderId in deliveredOrderIds }.sumOf { it.quantity }
                                
                                HorizontalShopCard(
                                    shop = shop,
                                    totalOrders = totalOrders,
                                    totalDeliveredProducts = totalDeliveredProducts,
                                    onClick = { viewModel.navigateTo(Screen.ShopDetail(shop.id)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── TARGETED PET REMEDIES: COLLAPSIBLE SECTION ─────────────────────────────
        item {
            // remediesExpanded hoisted
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section Header Row — tap to expand/collapse
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { remediesExpanded = !remediesExpanded }
                        .background(
                            color = if (remediesExpanded) Color(0xFFFC8019).copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🩺", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Targeted Pet Remedies",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (petProblems.isNotEmpty()) "${petProblems.size} concerns listed"
                                else "Common pet health concerns",
                                fontSize = 10.sp,
                                color = Color(0xFFFC8019),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Icon(
                        imageVector = if (remediesExpanded) Icons.Default.KeyboardArrowUp
                                      else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (remediesExpanded) "Collapse" else "Expand",
                        tint = Color(0xFFFC8019),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Collapsible content
                androidx.compose.animation.AnimatedVisibility(
                    visible = remediesExpanded,
                    enter = androidx.compose.animation.expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    ) + androidx.compose.animation.fadeOut()
                ) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        Text(
                            "Targeted Pet Remedies & Concerns 🩺",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFC8019),
                            modifier = Modifier.padding(start = 20.dp, bottom = 4.dp, top = 4.dp)
                        )
                        Text(
                            "Select a pet health concern to see vet-recommended solutions and direct products",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                        )

                        if (petProblems.isNotEmpty()) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(petProblems) { problem ->
                                    val isSelected = selectedProblemId == problem.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(30.dp))
                                            .background(
                                                if (isSelected) Color(0xFFFC8019)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) Color(0xFFFC8019) else Color.Gray.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(30.dp)
                                            )
                                            .clickable {
                                                selectedProblemId = if (isSelected) null else problem.id
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = problem.emoji.ifEmpty { "🩺" }, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = problem.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }

                            val selectedProblem = petProblems.find { it.id == selectedProblemId }
                            if (selectedProblem != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFFC8019).copy(alpha = 0.05f)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFFC8019).copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = selectedProblem.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color(0xFFFC8019)
                                            )
                                            IconButton(
                                                onClick = { selectedProblemId = null },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Close",
                                                    tint = Color.Gray,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = selectedProblem.description,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "CARE SOLUTION / TREATMENT 🧼",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFC8019),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = selectedProblem.solution.ifEmpty { "Follow expert care guidelines regularly." },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "WAY TO USE PRODUCTS 🧴",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFC8019),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = selectedProblem.howToUse.ifEmpty { "Apply as indicated on the product catalog instructions." },
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            lineHeight = 16.sp
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "RECOMMENDED PRODUCTS 📦",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFC8019),
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))

                                        val recommendedProductIds = selectedProblem.productIds
                                        val recommendedProducts = allProducts.filter { it.id in recommendedProductIds }

                                        if (recommendedProducts.isEmpty()) {
                                            Text(
                                                text = "No recommended products matching in this city.",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        } else {
                                            recommendedProducts.forEach { product ->
                                                val qty = cartItems[product.id] ?: 0
                                                val shop = shopsList.find { it.id == product.shopId }
                                                if (shop != null) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 6.dp)
                                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                                            .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                                            .padding(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = product.name,
                                                                fontWeight = FontWeight.SemiBold,
                                                                fontSize = 12.sp,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "₹${product.price}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFFC8019)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        if (qty > 0) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier
                                                                    .background(Color(0xFFFC8019), RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = "-",
                                                                    color = Color.White,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier
                                                                        .clickable { viewModel.removeFromCart(product.id) }
                                                                        .padding(horizontal = 6.dp)
                                                                )
                                                                Text(
                                                                    text = qty.toString(),
                                                                    color = Color.White,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                                )
                                                                Text(
                                                                    text = "+",
                                                                    color = Color.White,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    modifier = Modifier
                                                                        .clickable { viewModel.addToCart(product, shop) }
                                                                        .padding(horizontal = 6.dp)
                                                                )
                                                            }
                                                        } else {
                                                            Button(
                                                                onClick = { viewModel.addToCart(product, shop) },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019)),
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                modifier = Modifier.height(26.dp),
                                                                shape = RoundedCornerShape(4.dp)
                                                            ) {
                                                                Text("ADD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No targeted care remedies seeded at this time.",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    } // end AnimatedVisibility content Column
                } // end AnimatedVisibility
            } // end outer Column
        } // end item
        // ── COLLABORATIVE GROUP AUCTION WIDGET: COLLAPSIBLE ────────────────────────
        item {
            // auctionExpanded hoisted
            val currentRfqSessionIdForHeader by viewModel.currentRfqSessionId.collectAsState()
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { auctionExpanded = !auctionExpanded }
                        .background(
                            color = if (auctionExpanded) Color(0xFFFC8019).copy(alpha = 0.08f)
                            else MaterialTheme.colorScheme.surface
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👥", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Group Supply Auction",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (currentRfqSessionIdForHeader != null) "Active session! 🔴"
                                else "Combine orders for bulk discounts",
                                fontSize = 10.sp,
                                color = if (currentRfqSessionIdForHeader != null) Color.Red else Color(0xFFFC8019),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Icon(
                        imageVector = if (auctionExpanded) Icons.Default.KeyboardArrowUp
                                      else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (auctionExpanded) "Collapse" else "Expand",
                        tint = Color(0xFFFC8019),
                        modifier = Modifier.size(26.dp)
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = auctionExpanded,
                    enter = androidx.compose.animation.expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    ) + androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.shrinkVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                    ) + androidx.compose.animation.fadeOut()
                ) {
                    // Group Auction content
                    val currentRfqSessionId by viewModel.currentRfqSessionId.collectAsState()
                    val activeSession by viewModel.activeRfqSession.collectAsState()
                    val memberItems by viewModel.activeRfqMemberItems.collectAsState()
                    val quotations by viewModel.activeRfqQuotations.collectAsState()

                    var showJoinDialog by remember { mutableStateOf(false) }
                    var rfqInputId by remember { mutableStateOf("") }

                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    if (currentRfqSessionId == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFC8019).copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Color(0xFFFC8019).copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👥", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Group Supply Auction & Home Delivery",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFFC8019)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Combine orders with friends to get bulk discount quotations from local shopkeepers! Each friend gets delivered to their own home.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { viewModel.createGroupRfqSession(viewModel.selectedCityId.value) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019)),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Start Auction", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                            
                            OutlinedButton(
                                onClick = { showJoinDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFC8019)),
                                modifier = Modifier.weight(1f).height(38.dp)
                            ) {
                                Text("Join Session", fontWeight = FontWeight.Bold, color = Color(0xFFFC8019), fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                if (showJoinDialog) {
                    Dialog(onDismissRequest = { showJoinDialog = false }) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text("Join Group Auction", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = rfqInputId,
                                    onValueChange = { rfqInputId = it },
                                    label = { Text("Session ID (e.g. RFQ-XXXX)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { showJoinDialog = false }) {
                                        Text("Cancel", color = Color.Gray)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = {
                                            viewModel.joinGroupRfqSession(rfqInputId) { success ->
                                                if (success) {
                                                    showJoinDialog = false
                                                    rfqInputId = ""
                                                } else {
                                                    Toast.makeText(context, "Invalid Session ID!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019))
                                    ) {
                                        Text("Join", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Currently in a Group RFQ Session!
                val session = activeSession
                if (session != null) {
                    val isHost = session.hostId == currentUser?.id
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, Color(0xFFFC8019))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Active Group Auction 👥",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFFFC8019)
                                    )
                                    Text(
                                        text = "ID: ${session.id}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when (session.status) {
                                                "open" -> Color(0xFF4CAF50)
                                                "bidding" -> Color(0xFFFFB300)
                                                "accepted" -> Color(0xFFFC8019)
                                                else -> Color.Gray
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = session.status.uppercase(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            if (session.status == "bidding") {
                                val uniqueMerchants = quotations.map { it.shopName }.distinct()
                                BiddingRadarWidget(nearbyShopNames = uniqueMerchants)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Members & items list
                            Text(
                                text = "CART MEMBERS & DISPATCH ADDRESSES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            if (memberItems.isEmpty()) {
                                Text(
                                    text = "No products added yet. Friends can join using the session ID above.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                memberItems.forEach { item ->
                                    val prod = allProducts.find { it.id == item.productId }
                                    if (prod != null) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${item.memberName} ➔ ${prod.name} (x${item.quantity})",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "📍 Address: ${item.deliveryAddress}",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            if (item.memberId == currentUser?.id && session.status == "open") {
                                                IconButton(
                                                    onClick = { viewModel.removeRfqMemberItem(item.id) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Remove",
                                                        tint = Color.Red,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Collaborative Add Item block if status is open
                            if (session.status == "open") {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "ADD YOUR PRODUCT TO GROUP CART",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                        
                                        var selectedProdId by remember { mutableStateOf("") }
                                        var inputAddress by remember { mutableStateOf("") }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        // Product drop-down simulation / select
                                        val selectableProducts = allProducts.take(5)
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(selectableProducts) { prod ->
                                                val isSelected = selectedProdId == prod.id
                                                Card(
                                                    modifier = Modifier
                                                        .clickable { selectedProdId = prod.id }
                                                        .border(
                                                            width = 1.dp,
                                                            color = if (isSelected) Color(0xFFFC8019) else Color.Transparent,
                                                            shape = RoundedCornerShape(8.dp)
                                                        ),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSelected) Color(0xFFFC8019).copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                                                    )
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Text(prod.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        Text("₹${prod.price}", fontSize = 9.sp, color = Color(0xFFFC8019))
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        OutlinedTextField(
                                            value = inputAddress,
                                            onValueChange = { inputAddress = it },
                                            label = { Text("Your Home Delivery Address") },
                                            placeholder = { Text("e.g. 101 Park Avenue, Flat 2B") },
                                            modifier = Modifier.fillMaxWidth().height(50.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Button(
                                            onClick = {
                                                if (selectedProdId.isNotEmpty() && inputAddress.isNotEmpty()) {
                                                    viewModel.addRfqMemberItem(
                                                        productId = selectedProdId,
                                                        quantity = 1,
                                                        address = inputAddress,
                                                        lat = 17.385,
                                                        lng = 78.486
                                                    )
                                                    selectedProdId = ""
                                                    inputAddress = ""
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019))
                                        ) {
                                            Text("Add My Item ✅", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                            
                            // Host Control / Bidding Actions
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (session.status == "open") {
                                if (isHost) {
                                    Button(
                                        onClick = { viewModel.lockRfqCart() },
                                        modifier = Modifier.fillMaxWidth().height(44.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019))
                                    ) {
                                        Text("Lock Cart & Request Quotes 🔒", fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                } else {
                                    Text(
                                        text = "Waiting for Host to lock cart and request quotes...",
                                        fontSize = 11.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = Color.Gray,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            } else if (session.status == "bidding") {
                                Text(
                                    text = "REVERSE AUCTION ACTIVE BIDS ⚖️",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (quotations.isEmpty()) {
                                    Text(
                                        text = "Waiting for merchants to submit bids... (Autoseeding in 2s)",
                                        fontSize = 11.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = Color.Gray
                                    )
                                } else {
                                    quotations.forEach { bid ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(bid.shopName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color(0xFF4CAF50), RoundedCornerShape(2.dp))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("-${bid.discountPercentage.toInt()}%", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                Text("Total Quote: ₹${bid.quotedPrice.toInt()}", fontSize = 11.sp, color = Color(0xFFFC8019), fontWeight = FontWeight.Bold)
                                            }
                                            
                                            if (isHost) {
                                                Button(
                                                    onClick = { viewModel.acceptMerchantQuotation(session.id, bid.id) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.height(28.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Accept", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else if (session.status == "accepted") {
                                val acceptedBid = quotations.find { it.id == session.chosenQuotationId }
                                if (acceptedBid != null) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "QUOTATION ACCEPTED! 🎉",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFC8019)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${acceptedBid.shopName} won the auction with a flat ${acceptedBid.discountPercentage}% discount on all products!",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            lineHeight = 15.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             horizontalArrangement = Arrangement.SpaceBetween,
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             Text(
                                                 text = "SPLIT BILL SUMMARY (DISPATCH TO HOMES)",
                                                 fontSize = 9.sp,
                                                 fontWeight = FontWeight.Bold,
                                                 color = Color.Gray
                                             )
                                             if (isHost && !memberItems.all { it.hasPaid }) {
                                                 TextButton(
                                                     onClick = {
                                                         memberItems.forEach { item ->
                                                             if (!item.hasPaid) {
                                                                 viewModel.payMemberShare(session.id, item.memberId)
                                                             }
                                                         }
                                                     },
                                                     contentPadding = PaddingValues(0.dp),
                                                     modifier = Modifier.height(24.dp)
                                                 ) {
                                                     Text("Simulate All Paid 🧪", fontSize = 10.sp, color = Color(0xFFFC8019), fontWeight = FontWeight.Bold)
                                                 }
                                             }
                                         }
                                         Spacer(modifier = Modifier.height(6.dp))
                                         
                                         val itemsByMember = memberItems.groupBy { it.memberId }
                                         itemsByMember.forEach { (memberId, items) ->
                                             val hasPaid = items.all { it.hasPaid }
                                             var subtotal = 0.0
                                             items.forEach { item ->
                                                 val prod = allProducts.find { it.id == item.productId }
                                                 if (prod != null) {
                                                     val discounted = prod.price * (1 - acceptedBid.discountPercentage / 100.0)
                                                     subtotal += discounted * item.quantity
                                                 }
                                             }
                                             val delFee = 30.0
                                             val platFee = 10.0
                                             val total = subtotal + delFee + platFee
                                             
                                             Row(
                                                 modifier = Modifier
                                                     .fillMaxWidth()
                                                     .padding(vertical = 4.dp)
                                                     .background(
                                                         if (hasPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                                         shape = RoundedCornerShape(8.dp)
                                                     )
                                                     .padding(10.dp),
                                                 verticalAlignment = Alignment.CenterVertically,
                                                 horizontalArrangement = Arrangement.SpaceBetween
                                             ) {
                                                 Column(modifier = Modifier.weight(1f)) {
                                                     Text(
                                                         text = items.first().memberName,
                                                         fontWeight = FontWeight.Bold,
                                                         fontSize = 12.sp,
                                                         color = if (hasPaid) Color(0xFF2E7D32) else Color(0xFFE65100)
                                                     )
                                                     Text(
                                                         text = "Supplies: ₹${subtotal.toInt()} | Delivery: ₹${delFee.toInt()} | Platform: ₹${platFee.toInt()}",
                                                         fontSize = 10.sp,
                                                         color = Color.DarkGray
                                                     )
                                                     Text(
                                                         text = "Total Share: ₹${total.toInt()}",
                                                         fontSize = 11.sp,
                                                         fontWeight = FontWeight.Bold,
                                                         color = MaterialTheme.colorScheme.onSurface
                                                     )
                                                 }
                                                 
                                                 if (hasPaid) {
                                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                                         Icon(
                                                             imageVector = Icons.Default.CheckCircle,
                                                             contentDescription = "Paid",
                                                             tint = Color(0xFF2E7D32),
                                                             modifier = Modifier.size(16.dp)
                                                         )
                                                         Spacer(modifier = Modifier.width(4.dp))
                                                         Text("Paid", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                     }
                                                 } else {
                                                     if (memberId == currentUser?.id) {
                                                         Button(
                                                             onClick = {
                                                                 PaymentManager.startRazorpayCheckout(
                                                                     context = context,
                                                                     amountInRupees = total,
                                                                     orderId = "rfq_pay_${session.id}_${memberId}",
                                                                     email = currentUser?.email ?: "trinadhbandapalli@gmail.com",
                                                                     phone = currentUser?.phone ?: "9999999999",
                                                                     onSuccess = { paymentId ->
                                                                         viewModel.payMemberShare(session.id, memberId) { success ->
                                                                             if (success) {
                                                                                 Toast.makeText(context, "Payment successful! Your share is paid.", Toast.LENGTH_SHORT).show()
                                                                             }
                                                                         }
                                                                     },
                                                                     onFailure = { err ->
                                                                         Toast.makeText(context, "Payment failed: $err", Toast.LENGTH_SHORT).show()
                                                                     }
                                                                 )
                                                             },
                                                             shape = RoundedCornerShape(6.dp),
                                                             colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019)),
                                                             contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                             modifier = Modifier.height(28.dp)
                                                         ) {
                                                             Text("Pay ₹${total.toInt()} 💳", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                         }
                                                     } else {
                                                         Row(verticalAlignment = Alignment.CenterVertically) {
                                                             Icon(
                                                                 imageVector = Icons.Default.Info,
                                                                 contentDescription = "Pending",
                                                                 tint = Color(0xFFE65100),
                                                                 modifier = Modifier.size(14.dp)
                                                             )
                                                             Spacer(modifier = Modifier.width(4.dp))
                                                             Text("Pending ⏳", color = Color(0xFFE65100), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                                         }
                                                     }
                                                 }
                                             }
                                         }
                                         
                                         Spacer(modifier = Modifier.height(16.dp))
                                         
                                         if (isHost) {
                                             val allPaid = memberItems.all { it.hasPaid }
                                             Button(
                                                 onClick = { viewModel.checkoutRfqSession(acceptedBid) },
                                                 enabled = allPaid,
                                                 modifier = Modifier.fillMaxWidth().height(44.dp),
                                                 shape = RoundedCornerShape(8.dp),
                                                 colors = ButtonDefaults.buttonColors(
                                                     containerColor = if (allPaid) Color(0xFFFC8019) else Color.Gray,
                                                     disabledContainerColor = Color.LightGray
                                                 )
                                             ) {
                                                 Text(
                                                     text = if (allPaid) "Complete & Dispatch Group Orders 🚀" else "Waiting for All Payments ⏳",
                                                     fontWeight = FontWeight.Bold,
                                                     color = Color.White
                                                 )
                                             }
                                         } else {
                                             Text(
                                                 text = "Waiting for Host to complete checkout...",
                                                 fontSize = 11.sp,
                                                 fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                 color = Color.Gray,
                                                 modifier = Modifier.align(Alignment.CenterHorizontally)
                                             )
                                         }
                                    }
                                }
                            }
                            TextButton(
                                onClick = { viewModel.leaveGroupRfqSession() },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Leave Session", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            } // end if currentRfqSessionId == null / else (inside Column)
                } // end AnimatedVisibility content Column
                } // end AnimatedVisibility
            } // end outer auction Column
        } // end outer auction item

    }

    // Remedy Product Details & Store Availability Dialog
    if (activeRemedyProductDetail != null) {
        val product = activeRemedyProductDetail!!
        val productShop = shopsList.find { it.id == product.shopId }
        val shopName = productShop?.name ?: "Paws Boutique Store"
        val shopLocality = productShop?.locality ?: "City Wide"
        
        Dialog(onDismissRequest = { activeRemedyProductDetail = null }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFC8019).copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Gray.copy(alpha = 0.05f))
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(product.photos.firstOrNull() ?: ""),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    
                    Text(
                        text = "Brand: ${product.brand} | Life Stage: ${product.lifeStage}",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = product.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Shop Availability & Direct Navigation
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFC8019).copy(alpha = 0.03f)),
                        border = BorderStroke(1.dp, Color(0xFFFC8019).copy(alpha = 0.1f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("STORE AVAILABILITY 🏠", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$shopName - $shopLocality",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = {
                                    activeRemedyProductDetail = null
                                    viewModel.navigateTo(Screen.ShopDetail(product.shopId))
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019)),
                                modifier = Modifier.fillMaxWidth().height(42.dp)
                            ) {
                                Text("VIEW IN SHOP ➔", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(onClick = { activeRemedyProductDetail = null }) {
                        Text("Go Back", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
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

    // Selected Care Guide Detailed Overlay Dialog
    selectedGuide?.let { guide ->
        var activeProductDetail by remember { mutableStateOf<ProductEntity?>(null) }
        
        Dialog(onDismissRequest = { 
            selectedGuide = null
            activeProductDetail = null
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(guide.emoji, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = guide.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { 
                            selectedGuide = null
                            activeProductDetail = null
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Detailed Content Area (Scrollable)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = guide.content,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Curated Products section
                        Text(
                            "Curated Products For Hair & Health 🛍️",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Tap on any product to see availability and purchase details:",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        // Filter matching products from database
                        val matchingProducts = allProducts.filter { it.id in guide.productIds }
                        
                        if (matchingProducts.isEmpty()) {
                            Text("No matching products in store right now.", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                items(matchingProducts) { product ->
                                    val isCurProductActive = activeProductDetail?.id == product.id
                                    Card(
                                        modifier = Modifier
                                            .width(180.dp)
                                            .clickable { activeProductDetail = product }
                                            .border(
                                                width = 2.dp,
                                                color = if (isCurProductActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurProductActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) 
                                                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Image(
                                                painter = rememberAsyncImagePainter(product.photos.firstOrNull() ?: ""),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(80.dp)
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = product.name,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "₹${product.price.toInt()}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Active Product Details Drawer Pane inside Dialog
                        activeProductDetail?.let { product ->
                            val productShop = shopsList.find { it.id == product.shopId }
                            val shopName = productShop?.name ?: "Paws Boutique Store"
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)), RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Brand: ${product.brand} | Stage: ${product.lifeStage}",
                                        fontSize = 10.5.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        product.description,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("STORE AVAILABILITY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                            Text(
                                                shopName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        Button(
                                            onClick = {
                                                selectedGuide = null
                                                activeProductDetail = null
                                                viewModel.navigateTo(Screen.ShopDetail(product.shopId))
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(14.dp), tint = Color.White)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Go to Shop 🏪", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { viewModel.setAppLanguage(it) },
            onDismissRequest = { showLanguageDialog = false }
        )
    }
}

@Composable
fun FilterPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (active) Color(0xFFFC8019).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (active) Color(0xFFFC8019) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = if (active) Color(0xFFFC8019) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
            )
            if (active) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "✓",
                    color = Color(0xFFFC8019),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun HomeHeader(
    cityName: String,
    userName: String,
    avatarUrl: String,
    petName: String = "Buddy",
    onCityClick: () -> Unit,
    onProfileClick: () -> Unit,
    onChatClick: () -> Unit,
    onLanguageClick: () -> Unit,
    syncState: PowerSyncManager.SyncState,
    onSyncClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Row 1: App Branding & Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Logo / Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("🐾", fontSize = 22.sp)
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color(0xFFFC8019), fontWeight = FontWeight.Bold)) {
                            append("Swiggy ")
                        }
                        withStyle(style = SpanStyle(color = Color(0xFF004AC6), fontWeight = FontWeight.Black)) {
                            append("Paws")
                        }
                    },
                    fontSize = 20.sp
                )
            }

            // Minimized Controls Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chats button
                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF004AC6).copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Chats",
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF004AC6)
                    )
                }

                // Notification button
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Gray.copy(alpha = 0.05f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        modifier = Modifier.size(15.dp),
                        tint = Color.DarkGray
                    )
                }

                // Language button
                IconButton(
                    onClick = onLanguageClick,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Gray.copy(alpha = 0.05f), CircleShape)
                ) {
                    Text("🌐", fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Minimized circular Profile Avatar
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color(0xFFFC8019), CircleShape)
                        .clickable { onProfileClick() }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUrl)
                                .crossfade(true)
                                .error(R.drawable.paws_logo_1779795154399)
                                .build()
                        ),
                        contentDescription = "Profile",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2: Location selector & Sync Status (compact & premium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location Selector Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xFFFC8019).copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    .border(0.5.dp, Color(0xFFFC8019).copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .clickable { onCityClick() }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFFC8019),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = cityName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF282C3F)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF282C3F),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Sync status (small & clean)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onSyncClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = when (syncState) {
                                is PowerSyncManager.SyncState.Connected -> Color(0xFF4CAF50)
                                is PowerSyncManager.SyncState.Syncing -> Color(0xFFFFC107)
                                is PowerSyncManager.SyncState.Paused -> Color(0xFF9E9E9E)
                                is PowerSyncManager.SyncState.Offline -> Color(0xFFF44336)
                            },
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (syncState) {
                        is PowerSyncManager.SyncState.Connected -> "Sync: Connected"
                        is PowerSyncManager.SyncState.Syncing -> "Syncing..."
                        is PowerSyncManager.SyncState.Paused -> "Paused"
                        is PowerSyncManager.SyncState.Offline -> "Offline"
                    },
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 3: Slim Welcome Greeting Banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .scale(pulseScale)
                .fillMaxWidth()
                .border(1.dp, Color(0xFFFC8019).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("🐾", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${if (petName.isBlank()) "Buddy" else petName} is dreaming of premium treats! 🦴",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD35400)
            )
        }
    }
}

@Composable
fun BiddingRadarWidget(nearbyShopNames: List<String>) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarTransition")
    
    // Wave 1 Scale and Alpha
    val wave1Scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Scale"
    )
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1Alpha"
    )

    // Wave 2 Scale and Alpha (Delayed by 1000ms equivalent)
    val wave2Scale by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Scale"
    )
    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2Alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            // Draw concentric background circles
            drawCircle(color = Color(0xFFFC8019).copy(alpha = 0.08f), radius = size.minDimension / 2)
            drawCircle(color = Color(0xFFFC8019).copy(alpha = 0.15f), radius = size.minDimension / 3)
            drawCircle(color = Color(0xFFFC8019).copy(alpha = 0.25f), radius = size.minDimension / 4)
            
            // Draw Wave 1
            drawCircle(
                color = Color(0xFFFC8019).copy(alpha = wave1Alpha),
                radius = (size.minDimension / 2) * wave1Scale,
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Draw Wave 2
            drawCircle(
                color = Color(0xFFFC8019).copy(alpha = wave2Alpha * 0.7f),
                radius = (size.minDimension / 2) * wave2Scale * 0.7f,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        // Central Pulse Pin
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFFC8019), CircleShape)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🎯", fontSize = 20.sp)
        }
        
        // Orbiting Shop Names
        val shopsToDisplay = if (nearbyShopNames.isEmpty()) listOf("Paws Store", "Pet Nutrition Hub", "Dog Care Center") else nearbyShopNames
        shopsToDisplay.take(3).forEachIndexed { index, name ->
            val angle = (index * 120 + 30) * (Math.PI / 180f)
            val radius = 55.dp
            val xOffset = (Math.cos(angle) * radius.value).dp
            val yOffset = (Math.sin(angle) * radius.value).dp
            
            Box(
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
                    .background(Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "🏬 $name",
                    fontSize = 8.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


// Search bar placeholder
@Composable
fun SearchBarPlaceholder(query: String, onQueryChange: (String) -> Unit, onTapSearch: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(L10n.getString("search_placeholder")) },
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

// Dynamic Promotional Banners Carousel
@Composable
fun DynamicPromoBannerCarousel(banners: List<BannerEntity>, onBannerClick: (BannerEntity) -> Unit) {
    if (banners.isEmpty()) return

    Column {
        Text(
            "Featured Promotions 🌟",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 12.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(banners) { banner ->
                Card(
                    modifier = Modifier
                        .width(300.dp)
                        .height(160.dp)
                        .clickable { onBannerClick(banner) },
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(banner.imageUrl)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = banner.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                banner.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                banner.description,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemedyChip(
    title: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .height(44.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFC8019) 
                             else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFFFC8019) 
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(emoji.ifEmpty { "🩺" }, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun RemedyProductCard(
    product: ProductEntity,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .wrapContentHeight()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray.copy(alpha = 0.05f))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.photos.firstOrNull())
                            .crossfade(true)
                            .error(R.drawable.paws_logo_1779795154399)
                            .build()
                    ),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            
            Text(
                text = product.description,
                fontSize = 10.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
                textAlign = TextAlign.Start
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹${product.price}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFC8019),
                        fontSize = 13.sp
                    )
                    if (product.mrp > product.price) {
                        Text(
                            text = "₹${product.mrp}",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }
                
                if (quantity > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFFFC8019), RoundedCornerShape(16.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onRemove() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Text(
                            text = quantity.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onAdd() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("ADD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
                val next = if (activeSort == "Popular 🏆") "Top Rated" 
                           else if (activeSort == "Top Rated") "New" 
                           else if (activeSort == "New") "A-Z" 
                           else "Popular 🏆"
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

// Shop Card representation — Premium redesign (Phase 9)
@Composable
fun ShopItemCard(
    shop: ShopEntity,
    totalOrders: Int = 0,
    totalDeliveredProducts: Int = 0,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // ── Hero Image with overlaid badges ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
            ) {
                // Shop cover photo
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(shop.photos.firstOrNull())
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = shop.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient scrim at the bottom (for text legibility)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                                startY = 60f
                            )
                        )
                )

                // ── Top-right: Open/Closed pill ─────────────────────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(
                            color = if (shop.isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (shop.isOpen) "● OPEN" else "✕ CLOSED",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                // ── Bottom-left: Star rating pill overlaid on image ─────────
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 10.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐", fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = shop.rating.toString(),
                        color = Color(0xFFFFD54F),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }

                // ── Bottom-right: Free delivery chip (if applicable) ─────────
                if (shop.deliveryAvailable) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 10.dp)
                            .background(Color(0xFF00BCD4).copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("FREE DELIVERY", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // ── Info section ─────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                // Shop name + orders badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = shop.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // Orders pill
                    if (totalOrders > 0) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFC8019).copy(alpha = 0.10f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "$totalOrders orders",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFC8019)
                            )
                        }
                    }
                }

                // Description
                Text(
                    text = shop.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp, bottom = 10.dp)
                )

                // Footer: Locality + delivery count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFFC8019).copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = shop.locality,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                        )
                    }
                    if (totalDeliveredProducts > 0) {
                        Text(
                            text = "📦 $totalDeliveredProducts items delivered",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalShopCard(
    shop: ShopEntity,
    totalOrders: Int = 0,
    totalDeliveredProducts: Int = 0,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .padding(end = 12.dp, top = 4.dp, bottom = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // COVER IMAGE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                if (shop.photos.isNotEmpty() && shop.photos.first().isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(shop.photos.first())
                                .crossfade(true)
                                .build()
                        ),
                        contentDescription = shop.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF004AC6).copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (shop.vetClinicEnabled) Icons.Default.Info else Icons.Default.Home,
                            contentDescription = null,
                            tint = Color(0xFF004AC6).copy(alpha = 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f)),
                                startY = 40f
                            )
                        )
                )

                // Open status (Top Start)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(
                            color = if (shop.isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (shop.isOpen) "● OPEN" else "✕ CLOSED",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Rating (Top End)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⭐", fontSize = 9.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = shop.rating.toString(),
                        color = Color(0xFF282C3F),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }

                // Category overlay tags (Bottom Start)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (shop.vetClinicEnabled) {
                        // Hospital Tag (Orange)
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFC8019).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Hospital", color = Color(0xFFFC8019), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        // Medicine Tag (Green)
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Medicine", color = Color(0xFF2E7D32), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (shop.groomingEnabled) {
                        // Grooming Tag (Orange)
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFC8019).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Grooming", color = Color(0xFFFC8019), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Shop Tag (Orange)
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFC8019).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Shop", color = Color(0xFFFC8019), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        // If it is Healthy Hounds Pantry, also add Medicine Tag
                        if (shop.name.contains("Pantry", ignoreCase = true) || shop.description.contains("Diet", ignoreCase = true)) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Medicine", color = Color(0xFF2E7D32), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // INFO
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = shop.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = shop.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📍 " + shop.locality,
                        fontSize = 10.sp,
                        color = Color(0xFFFC8019),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (totalOrders > 0) {
                        Text(
                            text = "$totalOrders orders",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFC8019)
                        )
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
    val selectedCatIds by viewModel.selectedCategoryIds.collectAsState()
    val firstSelectedCatId = remember(selectedCatIds) { selectedCatIds.firstOrNull() }
    var activeTab by remember(firstSelectedCatId) { 
        mutableStateOf(if (firstSelectedCatId == "cat_groom") "Grooming & Vet Slots" else "Menu") 
    }
    var shopState = remember { mutableStateOf<ShopEntity?>(null) }
    var productsList by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    var selectedConcernTag by remember { mutableStateOf<String?>(null) }
    val categoryList by viewModel.categories.collectAsState()
    var selectedCategoryId by remember(firstSelectedCatId) {
        mutableStateOf(if (firstSelectedCatId == "cat_groom") null else firstSelectedCatId)
    }
    var reviewsList by remember { mutableStateOf<List<ReviewEntity>>(emptyList()) }
    var servicesList by remember { mutableStateOf<List<ServiceEntity>>(emptyList()) }
    val wishlists by viewModel.wishlists.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val scope = rememberCoroutineScope()

    val filteredProductsList = remember(productsList, selectedCategoryId, selectedConcernTag) {
        var result = productsList
        if (selectedCategoryId != null) {
            result = result.filter { it.categoryId == selectedCategoryId }
        }
        if (selectedConcernTag != null) {
            result = result.filter { product ->
                product.name.contains(selectedConcernTag!!, ignoreCase = true) ||
                product.description.contains(selectedConcernTag!!, ignoreCase = true) ||
                product.tags.any { it.contains(selectedConcernTag!!, ignoreCase = true) }
            }
        }
        result
    }

    val filteredServicesList = remember(servicesList, shopState.value?.groomingEnabled, shopState.value?.vetClinicEnabled) {
        val s = shopState.value
        if (s == null) emptyList()
        else {
            servicesList.filter { service ->
                val isGrooming = service.category.contains("groom", ignoreCase = true) || service.category.contains("bath", ignoreCase = true)
                val isVet = service.category.contains("vet", ignoreCase = true) || service.category.contains("clinic", ignoreCase = true) || service.category.contains("doctor", ignoreCase = true)
                
                if (isGrooming && !s.groomingEnabled) false
                else if (isVet && !s.vetClinicEnabled) false
                else true
            }
        }
    }

    // Load data from VM reactively
    LaunchedEffect(shopId) {
        val s = viewModel.getShopById(shopId)
        shopState.value = s
        viewModel.getProductsFlow(shopId).collect { productsList = it }
    }
    LaunchedEffect(shopId) {
        viewModel.getReviewsFlow(shopId).collect { reviewsList = it }
    }
    LaunchedEffect(shopId) {
        viewModel.getServicesFlow(shopId).collect { servicesList = it }
    }

    val shop = shopState.value ?: return

    if (shop.id == "mock_posh_paws") {
        PoshPawsShopDetailScreen(
            shop = shop,
            viewModel = viewModel,
            productsList = productsList,
            filteredProductsList = filteredProductsList,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { selectedCategoryId = it },
            wishlists = wishlists,
            cartItems = cartItems
        )
        return
    }

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

                // Call / Chat / Directions CTAs Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${shop.phone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call Shop", color = MaterialTheme.colorScheme.onBackground, fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            viewModel.selectActiveChat(shop.id)
                            viewModel.navigateTo(Screen.ChatDetail(shop.id))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chat Shop", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, maxLines = 1)
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
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Directions", color = MaterialTheme.colorScheme.onBackground, fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
            }
        }

        // Segment Tab Layout
        item {
            val tabs = remember(shop.groomingEnabled, shop.vetClinicEnabled, reviewsList.size) {
                val list = mutableListOf("Menu")
                if (shop.groomingEnabled || shop.vetClinicEnabled) {
                    list.add("Grooming & Vet Slots")
                }
                list.add("Reviews (${reviewsList.size})")
                list.add("Store Info")
                list
            }
            
            LaunchedEffect(tabs) {
                if (activeTab !in tabs && !activeTab.startsWith("Reviews")) {
                    activeTab = "Menu"
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    .padding(4.dp)
            ) {
                tabs.forEach { tab ->
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
                            fontSize = 11.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // VIEW RENDERING: BY TAB
        when (activeTab) {
            "Menu" -> {
                // Horizontal Category filter strip
                item {
                    Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                        Text(
                            text = "Product Categories:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                val isSelected = selectedCategoryId == null
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedCategoryId = null }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "All Categories 🏷️",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            items(categoryList) { cat ->
                                if (cat.id != "cat_groom") {
                                    val isSelected = selectedCategoryId == cat.id
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { selectedCategoryId = cat.id }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = cat.name,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Horizontal concern tag filter strip for shampoos & medicated care
                item {
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(
                            text = "Filter by Concern (Medicated Shampoos & Care):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                Pair(null, "All Items 🏷️"),
                                Pair("itching", "Itching 🧼"),
                                Pair("ticks", "Ticks & Fleas 🕷️"),
                                Pair("dandruff", "Dandruff Care ❄️"),
                                Pair("fungal", "Fungal Infections 🍄")
                            ).forEach { (tagKey, tagLabel) ->
                                item {
                                    val isSelected = selectedConcernTag == tagKey
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { selectedConcernTag = tagKey }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tagLabel,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (filteredProductsList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No catalog items match this concern filter.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(filteredProductsList) { product ->
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

            "Grooming & Vet Slots" -> {

                if (filteredServicesList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No services active at this time.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    items(filteredServicesList) { service ->
                        ServiceCatalogRow(
                            service = service,
                            viewModel = viewModel,
                            shopId = shop.id
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
            Text(L10n.getString("my_cart"), fontWeight = FontWeight.Black, fontSize = 20.sp)
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

    val isSwiggyOne by viewModel.isSwiggyOneSubscriber.collectAsState()
    val appliedCoupon by viewModel.appliedCoupon.collectAsState()

    // Swiggy One delivery fee waiver
    val deliveryCost = if (deliveryType == "delivery") {
        if (isSwiggyOne) 0.0 else 30.0
    } else {
        0.0
    }

    // Coupon discount calculation
    val couponDiscount = when (appliedCoupon) {
        "PAWSSWIGGY50" -> minOf(computedSubtotal * 0.5, 100.0)
        "FREEPET" -> if (deliveryType == "delivery" && !isSwiggyOne) 30.0 else 0.0
        else -> 0.0
    }

    val grandTotal = maxOf(computedSubtotal + deliveryCost - couponDiscount, 0.0)

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
                    Text(L10n.getString("my_cart"), fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                        Text(L10n.getString("delivery_address"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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

            // Swiggy-like Coupon / Offers Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎟️ ", fontSize = 18.sp)
                            Text("Swiggy Coupons & Offers", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        if (appliedCoupon != null) {
                            TextButton(onClick = { viewModel.applyCouponCode(null) }) {
                                Text("Remove", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (appliedCoupon == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Coupon 1
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFC8019).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFFFC8019).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .clickable { 
                                        viewModel.applyCouponCode("PAWSSWIGGY50") 
                                        Toast.makeText(context, "Coupon PAWSSWIGGY50 Applied Successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("PAWSSWIGGY50", fontWeight = FontWeight.Black, color = Color(0xFFFC8019), fontSize = 13.sp)
                                    Text("Get 50% discount up to ₹100 on pet foods & items.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("APPLY", fontWeight = FontWeight.Bold, color = Color(0xFFFC8019), fontSize = 12.sp)
                            }
                            
                            // Coupon 2
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF60B246).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF60B246).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .clickable { 
                                        viewModel.applyCouponCode("FREEPET")
                                        Toast.makeText(context, "Coupon FREEPET Applied Successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("FREEPET", fontWeight = FontWeight.Black, color = Color(0xFF3F8F27), fontSize = 13.sp)
                                    Text("Get FREE delivery on your order (Saves ₹30).", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("APPLY", fontWeight = FontWeight.Bold, color = Color(0xFF3F8F27), fontSize = 12.sp)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF60B246).copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFF60B246), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎉 ", fontSize = 16.sp)
                            Column {
                                Text("Code '$appliedCoupon' Applied", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF3F8F27))
                                Text("You saved ₹${couponDiscount.toInt()} on this order!", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
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
                    Text(L10n.getString("bill_details"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(L10n.getString("item_total"), fontSize = 13.sp)
                        Text("₹$computedSubtotal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(L10n.getString("delivery_fee"), fontSize = 13.sp)
                        if (deliveryType == "delivery") {
                            if (isSwiggyOne) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("₹30", fontSize = 13.sp, style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough), color = Color.Gray)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("FREE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F8F27))
                                }
                            } else {
                                Text("₹30.0", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("FREE (Pickup)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F8F27))
                        }
                    }

                    if (couponDiscount > 0.0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Coupon Discount ($appliedCoupon)", fontSize = 13.sp, color = Color(0xFF3F8F27))
                            Text("-₹$couponDiscount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F8F27))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.getString("to_pay"), fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("₹$grandTotal", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    }
                }
            }
        }

        // Checkout Button Footer row
        Button(
            onClick = {
                val currentUserVal = viewModel.currentUser.value
                val userPhone = currentUserVal?.phone ?: "9876543210"
                val userEmail = (currentUserVal?.fullName ?: "arjun").replace(" ", "").lowercase() + "@example.com"
                
                PaymentManager.startRazorpayCheckout(
                    context = context,
                    amountInRupees = grandTotal,
                    orderId = "order_chk_" + java.util.UUID.randomUUID().toString().take(6),
                    email = userEmail,
                    phone = userPhone,
                    onSuccess = { paymentId ->
                        viewModel.placeOrder(
                            address = if (deliveryType == "delivery") addressInput else "Pickup from merchant clinic",
                            notes = noteInput,
                            deliveryType = deliveryType
                        )
                        Toast.makeText(context, "Payment Successful! ID: $paymentId", Toast.LENGTH_LONG).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(context, "Payment Failed: $error", Toast.LENGTH_LONG).show()
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(L10n.getString("proceed_checkout") + " • ₹$grandTotal", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
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
    val activeOrderCaptain by viewModel.activeOrderCaptain.collectAsState()
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

        // ── SWIGGY PAWS RIDER DETAILS PANEL ──────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Swiggy Delivery Partner", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                if (!order.captainId.isNullOrEmpty()) {
                    val captainName = activeOrderCaptain?.fullName ?: "Ramesh Kumar"
                    val captainPhone = activeOrderCaptain?.phone ?: "+91 9876543210"
                    val vehicleNo = activeOrderCaptain?.vehicleNumber ?: "TS-09-EA-9999"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFC8019).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🛵", fontSize = 24.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(captainName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⭐ 4.9 ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDB7C00))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("• $vehicleNo", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Normal)
                                }
                            }
                        }
                        
                        IconButton(
                            onClick = { Toast.makeText(context, "Calling $captainName ($captainPhone)...", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(Icons.Default.Phone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            "❄️ Carrying your premium fresh pet food & kibbles in a temperature-controlled thermal container.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = Color(0xFFFC8019)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Allocating Delivery Partner...", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Rider will be assigned once your pet supplies are packaged.", fontSize = 11.sp, color = Color.Gray)
                        }
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
                    TimelineStep(L10n.getString("status_pending"), L10n.getString("desc_pending"), "pending"),
                    TimelineStep(L10n.getString("status_accepted"), L10n.getString("desc_accepted"), "accepted"),
                    TimelineStep(L10n.getString("status_preparing"), L10n.getString("desc_preparing"), "preparing"),
                    TimelineStep(L10n.getString("status_out_for_delivery"), L10n.getString("desc_out_for_delivery"), "out_for_delivery"),
                    TimelineStep(L10n.getString("status_delivered"), L10n.getString("desc_delivered"), "delivered")
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
    val allProducts by viewModel.allProducts.collectAsState()
    val categoryList by viewModel.categories.collectAsState()
    val activeTab by viewModel.searchTab.collectAsState() // "All" | "Shops" | "Products"
    val context = LocalContext.current
    val selectedCityId by viewModel.selectedCityId.collectAsState()

    var searchInput by remember { mutableStateOf(searchQuery) }

    // Dynamic Filter State
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedPetCategory by remember { mutableStateOf<String?>(null) }
    var selectedConcernTag by remember { mutableStateOf<String?>(null) }
    var selectedPriceRange by remember { mutableStateOf<String>("All Prices") } // "All Prices" | "Under ₹500" | "₹500 - ₹1000" | "₹1000 - ₹2000" | "Above ₹2000"
    var inStockOnly by remember { mutableStateOf(false) }
    var selectedBrand by remember { mutableStateOf<String>("All Brands") }
    var showFilters by remember { mutableStateOf(false) }

    // Simulating debounce for search input
    LaunchedEffect(searchInput) {
        delay(300)
        viewModel.updateSearchQuery(searchInput)
    }

    val mockShops = remember {
        listOf(
            ShopEntity(
                id = "mock_posh_paws",
                ownerId = "system",
                cityId = "hyd",
                name = "The Posh Paws",
                description = "Luxury Accessories & Food",
                address = "Road No 2, Banjara Hills, Hyderabad",
                locality = "Banjara Hills",
                phone = "9876543210",
                email = "posh@paws.com",
                photos = listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?auto=format&fit=crop&q=80&w=600"),
                isOpen = true,
                opensAt = "08:00",
                closesAt = "22:00",
                rating = 4.8,
                totalReviews = 42,
                deliveryAvailable = true,
                isVerified = true,
                isActive = true,
                isFeatured = true,
                groomingEnabled = false,
                vetClinicEnabled = false
            ),
            ShopEntity(
                id = "mock_healthy_hounds",
                ownerId = "system",
                cityId = "hyd",
                name = "Healthy Hounds Pantry",
                description = "Organic & Raw Diet Specialist",
                address = "Phase 2, Jubilee Hills, Hyderabad",
                locality = "Jubilee Hills",
                phone = "9876543211",
                email = "healthy@paws.com",
                photos = listOf("https://images.unsplash.com/photo-1583337130417-3346a1be7dee?auto=format&fit=crop&q=80&w=600"),
                isOpen = true,
                opensAt = "09:00",
                closesAt = "21:00",
                rating = 4.5,
                totalReviews = 88,
                deliveryAvailable = true,
                isVerified = true,
                isActive = true,
                isFeatured = true,
                groomingEnabled = false,
                vetClinicEnabled = false
            ),
            ShopEntity(
                id = "mock_city_hospital",
                ownerId = "system",
                cityId = "hyd",
                name = "City Pet Hospital",
                description = "24/7 Emergency & Surgery",
                address = "Metro Station Road, Madhapur, Hyderabad",
                locality = "Madhapur",
                phone = "9876543212",
                email = "cityhospital@paws.com",
                photos = listOf("https://images.unsplash.com/photo-1597633425046-08f5110420b5?auto=format&fit=crop&q=80&w=600"),
                isOpen = true,
                opensAt = "07:00",
                closesAt = "23:00",
                rating = 4.9,
                totalReviews = 19,
                deliveryAvailable = true,
                isVerified = true,
                isActive = true,
                isFeatured = false,
                groomingEnabled = false,
                vetClinicEnabled = true
            ),
            ShopEntity(
                id = "mock_fluffy_friends",
                ownerId = "system",
                cityId = "hyd",
                name = "Fluffy Friends",
                description = "Full Grooming & Spa",
                address = "Hitech City, Hyderabad",
                locality = "Hitech City",
                phone = "9876543213",
                email = "fluffy@paws.com",
                photos = emptyList(),
                isOpen = true,
                opensAt = "09:00",
                closesAt = "20:00",
                rating = 4.8,
                totalReviews = 56,
                deliveryAvailable = true,
                isVerified = true,
                isActive = true,
                isFeatured = false,
                groomingEnabled = true,
                vetClinicEnabled = false
            ),
            ShopEntity(
                id = "mock_paw_spa",
                ownerId = "system",
                cityId = "hyd",
                name = "Paw Spa",
                description = "Bath & Nail Clipping",
                address = "Kondapur, Hyderabad",
                locality = "Kondapur",
                phone = "9876543214",
                email = "pawspa@paws.com",
                photos = emptyList(),
                isOpen = true,
                opensAt = "10:00",
                closesAt = "19:00",
                rating = 4.6,
                totalReviews = 27,
                deliveryAvailable = true,
                isVerified = true,
                isActive = true,
                isFeatured = false,
                groomingEnabled = true,
                vetClinicEnabled = false
            )
        )
    }

    val displayShopsForSearch = remember(shopsList, selectedCityId, mockShops) {
        if (selectedCityId == "hyd") {
            val existingIds = shopsList.map { it.id }.toSet()
            val uniqueMocks = mockShops.filter { it.id !in existingIds }
            shopsList + uniqueMocks
        } else {
            shopsList
        }
    }

    // Filtered matched shops
    val matchedShops = remember(searchQuery, displayShopsForSearch) {
        displayShopsForSearch.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.locality.contains(searchQuery, ignoreCase = true) ||
            it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    // Dynamic available brands list based on products in the city
    val availableBrands = remember(allProducts) {
        listOf("All Brands") + allProducts.map { it.brand }.distinct().filter { it.isNotBlank() }
    }

    // Filtered matched products across the database
    val matchedProducts = remember(searchQuery, allProducts, selectedCategoryId, selectedPetCategory, selectedConcernTag, selectedPriceRange, inStockOnly, selectedBrand) {
        allProducts.filter { product ->
            val matchesQuery = searchQuery.isEmpty() || 
                               product.name.contains(searchQuery, ignoreCase = true) || 
                               product.brand.contains(searchQuery, ignoreCase = true) ||
                               product.description.contains(searchQuery, ignoreCase = true) ||
                               product.tags.any { it.contains(searchQuery, ignoreCase = true) }
                               
            val matchesCategory = selectedCategoryId == null || product.categoryId == selectedCategoryId
            
            val matchesPetCategory = selectedPetCategory == null || 
                                     product.name.contains(selectedPetCategory!!, ignoreCase = true) ||
                                     product.description.contains(selectedPetCategory!!, ignoreCase = true) ||
                                     product.tags.any { it.contains(selectedPetCategory!!, ignoreCase = true) } ||
                                     product.lifeStage.contains(selectedPetCategory!!, ignoreCase = true)
                                     
            val matchesConcern = selectedConcernTag == null || 
                                 product.name.contains(selectedConcernTag!!, ignoreCase = true) ||
                                 product.description.contains(selectedConcernTag!!, ignoreCase = true) ||
                                 product.tags.any { it.contains(selectedConcernTag!!, ignoreCase = true) }

            val matchesPrice = when (selectedPriceRange) {
                "Under ₹500" -> product.price < 500.0
                "₹500 - ₹1000" -> product.price >= 500.0 && product.price <= 1000.0
                "₹1000 - ₹2000" -> product.price >= 1000.0 && product.price <= 2000.0
                "Above ₹2000" -> product.price > 2000.0
                else -> true
            }
            
            val matchesStock = !inStockOnly || product.inStock
            val matchesBrand = selectedBrand == "All Brands" || product.brand.equals(selectedBrand, ignoreCase = true)
            
            matchesQuery && matchesCategory && matchesPetCategory && matchesConcern && matchesPrice && matchesStock && matchesBrand
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                placeholder = { Text("Search specific pet shops or products...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = { searchInput = "" }) {
                            Icon(Icons.Default.Clear, "Clear Search")
                        }
                    } else {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp))
                    }
                }
            )
        }

        // Tabs: search in shops or products
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            listOf("All", "Shops", "Products").forEach { tab ->
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

        Spacer(modifier = Modifier.height(8.dp))

        // Filter trigger toggle row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (activeTab == "All") "Matched Results (${matchedShops.size + matchedProducts.size})"
                       else if (activeTab == "Shops") "Matched Stores (${matchedShops.size})"
                       else "Matched Products (${matchedProducts.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable { showFilters = !showFilters }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Toggle Filters",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showFilters) "Hide Filters" else "Refine Search Filters",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Expandable Filter Section
        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Search Filter Constraints 🎯", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Reset All",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                selectedCategoryId = null
                                selectedPetCategory = null
                                selectedConcernTag = null
                                selectedPriceRange = "All Prices"
                                inStockOnly = false
                                selectedBrand = "All Brands"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Category filter
                    Text("Product Category:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChipHelper(
                                name = "All Categories",
                                isSelected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null }
                            )
                        }
                        items(categoryList) { cat ->
                            FilterChipHelper(
                                name = cat.name,
                                isSelected = selectedCategoryId == cat.id,
                                onClick = { selectedCategoryId = cat.id }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Pet Category filter
                    Text("Target Animal Species:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf("All Pets", "Dog", "Cat", "Cattle", "Kitten", "Puppy", "Hamster", "Rabbit", "Bird")) { pet ->
                            val isSelected = (pet == "All Pets" && selectedPetCategory == null) || (selectedPetCategory == pet.lowercase())
                            FilterChipHelper(
                                name = pet,
                                isSelected = isSelected,
                                onClick = { selectedPetCategory = if (pet == "All Pets") null else pet.lowercase() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Shampoo Care Concerns filter
                    Text("Shampoo Care Concerns:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val concerns = listOf(
                            Pair("All Concerns", null),
                            Pair("Itching 🧼", "itching"),
                            Pair("Ticks & Fleas 🕷️", "ticks"),
                            Pair("Dandruff Care ❄️", "dandruff"),
                            Pair("Fungal Infections 🍄", "fungal")
                        )
                        items(concerns) { (label, key) ->
                            val isSelected = (key == null && selectedConcernTag == null) || (selectedConcernTag == key)
                            FilterChipHelper(
                                name = label,
                                isSelected = isSelected,
                                onClick = { selectedConcernTag = key }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Price Filter + Brand Filter Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Price Range limit:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var priceExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { priceExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(selectedPriceRange, fontSize = 10.sp)
                                }
                                DropdownMenu(
                                    expanded = priceExpanded,
                                    onDismissRequest = { priceExpanded = false }
                                ) {
                                    listOf("All Prices", "Under ₹500", "₹500 - ₹1000", "₹1000 - ₹2000", "Above ₹2000").forEach { price ->
                                        DropdownMenuItem(
                                            text = { Text(price, fontSize = 11.sp) },
                                            onClick = {
                                                selectedPriceRange = price
                                                priceExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Specific Brand:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            var brandExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { brandExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(selectedBrand, fontSize = 10.sp)
                                }
                                DropdownMenu(
                                    expanded = brandExpanded,
                                    onDismissRequest = { brandExpanded = false }
                                ) {
                                    availableBrands.forEach { brand ->
                                        DropdownMenuItem(
                                            text = { Text(brand, fontSize = 11.sp) },
                                            onClick = {
                                                selectedBrand = brand
                                                brandExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Availability checkbox
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = inStockOnly,
                            onCheckedChange = { inStockOnly = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Show In-Stock Only", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (searchInput.trim().isEmpty() && !showFilters) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(56.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Type keyword to search shops & pet products in active city", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 90.dp)) {
                if (activeTab == "All") {
                    if (matchedShops.isEmpty() && matchedProducts.isEmpty()) {
                        item {
                            Text("No matching pet stores or products found.", modifier = Modifier.padding(20.dp), color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        if (matchedShops.isNotEmpty()) {
                            item {
                                Text(
                                    "MATCHED STORES 🏪",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(matchedShops) { shop ->
                                SearchShopRow(shop = shop, viewModel = viewModel)
                            }
                        }
                        if (matchedProducts.isNotEmpty()) {
                            item {
                                Text(
                                    "MATCHED PRODUCTS 📦",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(matchedProducts) { product ->
                                val productShop = displayShopsForSearch.find { it.id == product.shopId }
                                val shopName = productShop?.name ?: "Local Shop"
                                SearchProductRow(
                                    product = product,
                                    viewModel = viewModel,
                                    shopName = shopName,
                                    onAdd = { 
                                        viewModel.addToCart(product, productShop ?: displayShopsForSearch.firstOrNull() ?: return@SearchProductRow)
                                        Toast.makeText(context, "Added ${product.name} to Cart!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                } else if (activeTab == "Shops") {
                    if (matchedShops.isEmpty()) {
                        item {
                            Text("No matching pet stores found.", modifier = Modifier.padding(20.dp), color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        items(matchedShops) { shop ->
                            SearchShopRow(shop = shop, viewModel = viewModel)
                        }
                    }
                } else {
                    if (matchedProducts.isEmpty()) {
                        item {
                            Text("No matching pet products found.", modifier = Modifier.padding(20.dp), color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        items(matchedProducts) { product ->
                            val productShop = displayShopsForSearch.find { it.id == product.shopId }
                            val shopName = productShop?.name ?: "Local Shop"
                            SearchProductRow(
                                product = product,
                                viewModel = viewModel,
                                shopName = shopName,
                                onAdd = { 
                                    viewModel.addToCart(product, productShop ?: displayShopsForSearch.firstOrNull() ?: return@SearchProductRow)
                                    Toast.makeText(context, "Added ${product.name} to Cart!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChipHelper(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary 
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SearchShopRow(
    shop: ShopEntity,
    viewModel: PawsViewModel
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { viewModel.navigateTo(Screen.ShopDetail(shop.id)) },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(shop.photos.firstOrNull()),
                contentDescription = null,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = shop.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFC8019).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SHOP",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFC8019)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(shop.locality, fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⭐ ${shop.rating}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (shop.isOpen) Color(0xFF2E7D32).copy(alpha = 0.1f) else Color(0xFFC62828).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (shop.isOpen) "OPEN" else "CLOSED",
                            color = if (shop.isOpen) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SearchProductRow(
    product: ProductEntity,
    viewModel: PawsViewModel,
    shopName: String,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { viewModel.navigateTo(Screen.ShopDetail(product.shopId)) },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(product.photos.firstOrNull()),
                contentDescription = null,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Brand: ${product.brand} • Store: $shopName", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${product.price}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("₹${product.mrp}", fontSize = 11.sp, color = Color.Gray, style = LocalTextStyle.current.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
    var showCareCalendar by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val currentLanguage by viewModel.appLanguage.collectAsState()
    val selectedCityName by viewModel.selectedCityName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .verticalScroll(rememberScrollState())
    ) {
        // ── Premium Header ──────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))))
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Column {
                Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFFFFD700).copy(alpha = 0.15f)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
                        Text("Premium \u2022 Max", color = Color(0xFFFFD700), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = rememberAsyncImagePainter(currentUser?.avatarUrl ?: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100&auto=format&fit=crop"),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp).clip(CircleShape).border(2.dp, Color(0xFFFC8019), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(currentUser?.fullName ?: "Arjun Kumar", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text(currentUser?.email ?: "arjun@gmail.com", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("\uD83D\uDCCD $selectedCityName", color = Color(0xFFFC8019), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                // Dog Stats Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("\uD83D\uDC15", fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Max", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Golden Retriever \u2022 3 Yrs", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
                        }
                        Divider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.White.copy(alpha = 0.2f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("32 kg", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Weight", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
                        }
                        Divider(modifier = Modifier.height(40.dp).width(1.dp), color = Color.White.copy(alpha = 0.2f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("High", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Activity", color = Color.White.copy(alpha = 0.55f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Pet Health Section ─────────────────────────────────────
        Text("Pet Health", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C8C8C), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
            Column {
                ProfileOptionRow(icon = Icons.Default.Favorite, title = "Health Hub", subtitle = "Vaccination status & medical records", onClick = { viewModel.navigateTo(Screen.ReportsDashboard) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.DateRange, title = "Appointments", subtitle = "Manage pet checkups & upcoming visits", onClick = { viewModel.navigateTo(Screen.Appointments) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.CheckCircle, title = "Medication Log", subtitle = "Track daily pills & chewable doses", onClick = { viewModel.navigateTo(Screen.TabletsIssued) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.Star, title = "Health & Vaccinations", subtitle = "Full vaccination history & health records", onClick = { viewModel.navigateTo(Screen.Vaccinations) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Shopping Section ───────────────────────────────────────
        Text("Shopping", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C8C8C), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
            Column {
                ProfileOptionRow(icon = Icons.Default.Favorite, title = "Favourites", subtitle = "Saved products & shops", onClick = { viewModel.navigateTo(Screen.Favourites) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.ShoppingCart, title = "My Orders", subtitle = "Track current & past orders", onClick = { viewModel.navigateTo(Screen.Orders) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.Lock, title = "Payment Methods", subtitle = "UPI, Cards & wallet options", onClick = { Toast.makeText(context, "Coming soon! \uD83D\uDCB3", Toast.LENGTH_SHORT).show() })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.Star, title = "Subscription Details", subtitle = "PawsApp Premium plan", onClick = { Toast.makeText(context, "Manage subscription coming soon!", Toast.LENGTH_SHORT).show() })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Account Section ────────────────────────────────────────
        Text("Account", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C8C8C), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
            Column {
                ProfileOptionRow(icon = Icons.Default.DateRange, title = L10n.getString("care_calendar"), subtitle = "Vaccine schedules & birthdays", onClick = { showCareCalendar = true })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.LocationOn, title = L10n.getString("change_city"), subtitle = "Current: $selectedCityName", onClick = { viewModel.navigateTo(Screen.LocationSelect) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.Info, title = L10n.getString("select_language"), subtitle = L10n.getString("language_subtitle"), onClick = { showLanguageDialog = true })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.Info, title = "Support", subtitle = "Help center & FAQs", onClick = { Toast.makeText(context, "Support center coming soon! \uD83E\uDD1D", Toast.LENGTH_SHORT).show() })
                if (currentUser?.role == "superadmin" || currentUser?.role == "admin") {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ProfileOptionRow(icon = Icons.Default.Settings, title = L10n.getString("super_admin_controls"), subtitle = "Approve pet stores and push banners", onClick = { viewModel.navigateTo(Screen.SuperAdmin) })
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Logout ─────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { viewModel.logout() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEDED)),
            border = BorderStroke(1.dp, Color(0xFFFF4444).copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(Icons.Default.ExitToApp, null, tint = Color(0xFFFF4444), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(L10n.getString("logout_btn"), color = Color(0xFFFF4444), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showCareCalendar) { CareCalendarSheet(viewModel = viewModel, onDismiss = { showCareCalendar = false }) }
    if (showLanguageDialog) { LanguageSelectionDialog(currentLanguage = currentLanguage, onLanguageSelected = { viewModel.setAppLanguage(it) }, onDismissRequest = { showLanguageDialog = false }) }
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
    val syncState by viewModel.powerSyncState.collectAsState()

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Role: Merchant Suresh Manager", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = when (syncState) {
                                        is PowerSyncManager.SyncState.Connected -> Color(0xFF4CAF50)
                                        is PowerSyncManager.SyncState.Syncing -> Color(0xFFFFC107)
                                        is PowerSyncManager.SyncState.Paused -> Color(0xFF9E9E9E)
                                        is PowerSyncManager.SyncState.Offline -> Color(0xFFF44336)
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { viewModel.triggerManualPowerSync() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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

            // Inventory Manager Shortcut
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clickable { viewModel.navigateTo(Screen.MerchantInventory) },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF004AC6))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("📦 Inventory & Pricing Manager", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        Text("Stock levels • Pricing • Campaigns", fontSize = 11.sp, color = Color.White.copy(alpha = 0.75f))
                    }
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }


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

            // Grooming and Vet Services Channel Toggles Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Services Channel Availability 🛠️", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Grooming Facility", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Toggle on/off grooming slots and services", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = shop.groomingEnabled,
                            onCheckedChange = { viewModel.updateShopServices(shop.id, grooming = it, vet = shop.vetClinicEnabled) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Vet Doctor Clinic", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Toggle on/off vet clinic doctor availability", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = shop.vetClinicEnabled,
                            onCheckedChange = { viewModel.updateShopServices(shop.id, grooming = shop.groomingEnabled, vet = it) }
                        )
                    }
                }
            }

            // ── GROUP LEADS AUCTION (REVERSE AUCTION) ───────────────────────────────────
            Text(
                "Group Leads Auction 🎯",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp)
            )
            
            val context = LocalContext.current
            val activeSessions by viewModel.allRfqSessionsInCity.collectAsState()
            val sessionsOpenForBids = activeSessions.filter { it.status == "bidding" }
            
            if (sessionsOpenForBids.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No active bulk group order leads in your city at this time.", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                sessionsOpenForBids.forEach { session ->
                    var bidDiscountInput by remember { mutableStateOf("") }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Lead ID: ${session.id}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFC8019), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "REVERSE AUCTION",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "A group of pet parents nearby has consolidated their high-volume dog supply needs. Submit your best discount quotation to win the entire bulk order!",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 15.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = bidDiscountInput,
                                    onValueChange = { bidDiscountInput = it },
                                    label = { Text("Discount (%)") },
                                    placeholder = { Text("e.g. 15") },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                    ),
                                    singleLine = true
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Button(
                                    onClick = {
                                        val percent = bidDiscountInput.toDoubleOrNull() ?: 0.0
                                        if (percent > 0) {
                                            viewModel.submitMerchantQuotation(
                                                sessionId = session.id,
                                                shopId = shop.id,
                                                shopName = shop.name,
                                                discountPercent = percent
                                            )
                                            bidDiscountInput = ""
                                            Toast.makeText(context, "Quotation Bid Submitted Successfully! 🎯", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019))
                                ) {
                                    Text("Submit Bid", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
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

// ============================================================================
// NEW DYNAMIC INTEGRATION COMPOSABLES AND SCREENS
// ============================================================================

@Composable
fun ServiceCatalogRow(service: ServiceEntity, viewModel: PawsViewModel, shopId: String) {
    val context = LocalContext.current
    var showBookingDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(service.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(service.category, color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("₹${service.price}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
            }
            Button(
                onClick = { showBookingDialog = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Book Slot")
            }
        }
    }

    if (showBookingDialog) {
        val currentUserVal by viewModel.currentUser.collectAsState()
        var petName by remember { mutableStateOf(currentUserVal?.petName ?: "") }
        
        // Generate next 7 days dynamically
        val datesList = remember {
            val list = mutableListOf<String>()
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val cal = java.util.Calendar.getInstance()
            for (i in 0 until 7) {
                list.add(format.format(cal.time))
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            list
        }
        var selectedDate by remember { mutableStateOf(datesList.firstOrNull() ?: "2026-05-29") }
        
        val timeSlots = listOf(
            "09:00 AM", "10:30 AM", "12:00 PM", "01:30 PM", "03:00 PM", "04:30 PM", "06:00 PM"
        )
        var selectedTime by remember { mutableStateOf("09:00 AM") }
        
        val shopAppointments by remember(shopId) {
            viewModel.getAppointmentsForShopFlow(shopId)
        }.collectAsState(initial = emptyList())
        
        Dialog(onDismissRequest = { showBookingDialog = false }) {
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
                    Text("Book Appointment 📅", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = petName,
                        onValueChange = { petName = it },
                        label = { Text("Pet Name") },
                        placeholder = { Text("e.g. Buddy") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Select Date:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        items(datesList) { date ->
                            val isSelected = selectedDate == date
                            val friendly = try {
                                val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                                val outputFormat = java.text.SimpleDateFormat("EEE dd", java.util.Locale.US)
                                outputFormat.format(inputFormat.parse(date))
                            } catch (e: Exception) {
                                date
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .clickable { selectedDate = date }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    friendly,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Select Time Slot:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
                    )

                    // Render timing chips. If booked, paint black and disable.
                    timeSlots.chunked(3).forEach { rowSlots ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSlots.forEach { slot ->
                                val isBooked = shopAppointments.any { appt ->
                                    appt.appointmentDate == selectedDate && 
                                    appt.appointmentTime.equals(slot, ignoreCase = true) && 
                                    appt.status != "cancelled"
                                }
                                val isSelected = selectedTime == slot
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isBooked) Color.Black
                                            else if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable(enabled = !isBooked) {
                                            selectedTime = slot
                                        }
                                        .border(
                                            width = 1.dp,
                                            color = if (isBooked) Color.Black else if (isSelected) Color.Transparent else Color.LightGray.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isBooked) "$slot\n(Booked 🔒)" else slot,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isBooked) Color.Gray 
                                               else if (isSelected) Color.White 
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showBookingDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (petName.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter pet name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val currentUserVal = viewModel.currentUser.value
                                val userPhone = currentUserVal?.phone ?: "9876543210"
                                val userEmail = (currentUserVal?.fullName ?: "arjun").replace(" ", "").lowercase() + "@example.com"
                                
                                PaymentManager.startRazorpayCheckout(
                                    context = context,
                                    amountInRupees = service.price,
                                    orderId = "appt_chk_" + java.util.UUID.randomUUID().toString().take(6),
                                    email = userEmail,
                                    phone = userPhone,
                                    onSuccess = { paymentId ->
                                        viewModel.bookAppointment(
                                            shopId = shopId,
                                            serviceId = service.id,
                                            serviceName = service.name,
                                            price = service.price,
                                            date = selectedDate,
                                            time = selectedTime,
                                            petName = petName
                                        )
                                        showBookingDialog = false
                                        Toast.makeText(context, "Appointment Booked! Payment ID: $paymentId", Toast.LENGTH_LONG).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(context, "Payment Failed: $error", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Pay & Book")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CareCalendarSheet(viewModel: PawsViewModel, onDismiss: () -> Unit) {
    val reminders by viewModel.activeReminders.collectAsState()
    val pets by viewModel.activePets.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf("Passport") } // "Passport" | "Calendar"
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showAddPetDialog by remember { mutableStateOf(false) }
    var editingPet by remember { mutableStateOf<PetEntity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pet Hub 🐾", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null)
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Custom Pill Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeTab == "Passport") MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { activeTab = "Passport" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Health Passport",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (activeTab == "Passport") MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeTab == "Calendar") MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { activeTab = "Calendar" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Smart Calendar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (activeTab == "Calendar") MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (activeTab == "Passport") {
                    // TAB 1: UNIFIED PET HEALTH PASSPORT
                    if (pets.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Info, null, modifier = Modifier.size(56.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Pet Passport configured.", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Create a digital health card for vaccines and deworming records.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
                            Button(
                                onClick = { 
                                    editingPet = null
                                    showAddPetDialog = true 
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Create Health Passport")
                            }
                        }
                    } else {
                        val activePet = pets.first()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Premium Velvet-Maroon Passport Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(2.dp, Color(0xFFFFD700)), RoundedCornerShape(20.dp)), // Premium Gold Border
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF6B1A24)) // Velvet-Maroon Color
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("PET HEALTH PASSPORT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), letterSpacing = 1.sp)
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("v7.0 Room", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(
                                            painter = rememberAsyncImagePainter(activePet.avatarUrl),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(65.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, Color(0xFFFFD700), CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(activePet.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                                            Text(activePet.breed, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                                            Text("Age: ${activePet.ageText}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                            Text("Weight: ${activePet.weight.ifBlank { "24 kg" }}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.15f)))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Compliance Status Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Deworming Badge
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                                .padding(10.dp)
                                        ) {
                                            Column {
                                                Text("Deworming", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                                                Text("Last: ${activePet.dewormingDate}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), CircleShape))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Up-to-Date", fontSize = 8.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        
                                        // Vaccines Badge
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                                .padding(10.dp)
                                        ) {
                                            Column {
                                                Text("Rabies Vaccine", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                                                Text("Due: ${activePet.vaccineDueDate}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                                    val isOverdue = false // Can add dates check
                                                    Box(modifier = Modifier.size(6.dp).background(if (isOverdue) Color.Red else Color(0xFFFFB300), CircleShape))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isOverdue) "Overdue" else "Due Soon", fontSize = 8.sp, color = if (isOverdue) Color.Red else Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Health Specifications List
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Passport Credentials:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("⚠️", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Allergies & Sensitivities", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                            Text(activePet.allergies.ifBlank { "No known allergies." }, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("💉", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Vaccination History Log", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                            Text(activePet.vaccineRecord.ifBlank { "No records logged." }, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Smart Care Checklist Alert Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("☀️ Smart Care: Summer Tick Alert!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Banjara Hills is experiencing high humidity. Protect Buddy from tick infestations:", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Apply spot-on tick pipette spray", fontSize = 11.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Inspect ears/paws after daily walk", fontSize = 11.sp)
                                    }
                                    
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    // One click booking link
                                    Button(
                                        onClick = {
                                            viewModel.navigateTo(Screen.ShopDetail("shop_hyd_2")) // Open Puppy Love Groomers
                                            onDismiss()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Book Tick-Cooling Bath at Puppy Love 🏪", fontSize = 10.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Edit & Delete actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Delete Profile",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        viewModel.deletePet(activePet.id)
                                        Toast.makeText(context, "Pet passport removed.", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                Text(
                                    "Edit Passport Details 📝",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        editingPet = activePet
                                        showAddPetDialog = true
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // TAB 2: SMART CARE CALENDAR & REMINDERS LIST
                    Button(
                        onClick = { showAddReminderDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Care Reminder")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (reminders.isEmpty()) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("No reminders set yet! Keep your pet healthy by adding schedules.", color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(reminders) { reminder ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = reminder.isCompleted,
                                                onCheckedChange = { checked ->
                                                    viewModel.toggleReminderCompletion(reminder.id, checked)
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    reminder.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    style = androidx.compose.ui.text.TextStyle(
                                                        textDecoration = if (reminder.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                                    )
                                                )
                                                Text("Pet: ${reminder.petName} • Date: ${reminder.dateString}", fontSize = 11.sp, color = Color.Gray)
                                                if (reminder.notes.isNotEmpty()) {
                                                    Text(reminder.notes, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                                                }
                                            }
                                            IconButton(onClick = { viewModel.deleteReminder(reminder.id) }) {
                                                Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        
                                        // One-Click Local vet clinic or grooming booking link depending on type
                                        val isVaccine = reminder.title.contains("vaccine", ignoreCase = true) || reminder.title.contains("shot", ignoreCase = true) || reminder.title.contains("rabies", ignoreCase = true)
                                        val isGrooming = reminder.title.contains("groom", ignoreCase = true) || reminder.title.contains("bath", ignoreCase = true) || reminder.title.contains("spa", ignoreCase = true)
                                        
                                        if (isVaccine || isGrooming) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = {
                                                    val targetShopId = if (isVaccine) "shop_hyd_1" else "shop_hyd_2"
                                                    viewModel.navigateTo(Screen.ShopDetail(targetShopId))
                                                    onDismiss()
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isVaccine) Color(0xFF2DB37A) else MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(14.dp), tint = Color.White)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (isVaccine) "Book Vaccination at Royal Canine Hub 🏪" else "Book Grooming at Puppy Love 🏪",
                                                    fontSize = 10.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reminders Form Dialog
    if (showAddReminderDialog) {
        var title by remember { mutableStateOf("") }
        var petName by remember { mutableStateOf(pets.firstOrNull()?.name ?: "Buddy") }
        var dateStr by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("general") } // general | doctor | vaccination | grooming
        
        Dialog(onDismissRequest = { showAddReminderDialog = false }) {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Add Care Schedule 🔔", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Reminder Title (e.g. Rabies Vaccine)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = petName, onValueChange = { petName = it }, label = { Text("Pet Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = dateStr, onValueChange = { dateStr = it }, label = { Text("Date (e.g. 2026-06-01)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Special Notes") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showAddReminderDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (title.trim().isEmpty() || petName.trim().isEmpty() || dateStr.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter all fields", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val reminderType = when {
                                    title.contains("vacc", ignoreCase = true) || title.contains("shot", ignoreCase = true) -> "vaccination"
                                    title.contains("groom", ignoreCase = true) || title.contains("bath", ignoreCase = true) -> "grooming"
                                    else -> "general"
                                }
                                viewModel.createReminder(title, petName, dateStr, notes, reminderType)
                                NotificationManager.fireInstantNotification(context, "Reminder Created", "Alert scheduled for $petName on $dateStr!")
                                showAddReminderDialog = false
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Schedule")
                        }
                    }
                }
            }
        }
    }

    // Health Passport Form Dialog
    if (showAddPetDialog) {
        val existingPet = editingPet
        var name by remember { mutableStateOf(existingPet?.name ?: "") }
        var breed by remember { mutableStateOf(existingPet?.breed ?: "") }
        var ageText by remember { mutableStateOf(existingPet?.ageText ?: "") }
        var weight by remember { mutableStateOf(existingPet?.weight ?: "") }
        var avatarUrl by remember { mutableStateOf(existingPet?.avatarUrl ?: "") }
        var allergies by remember { mutableStateOf(existingPet?.allergies ?: "") }
        var vaccineRecord by remember { mutableStateOf(existingPet?.vaccineRecord ?: "") }
        var dewormingDate by remember { mutableStateOf(existingPet?.dewormingDate ?: "") }
        var vaccineDueDate by remember { mutableStateOf(existingPet?.vaccineDueDate ?: "") }

        Dialog(onDismissRequest = { showAddPetDialog = false }) {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (existingPet != null) "Edit Health Passport 🐾" else "Create Health Passport 🐾",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Pet Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = breed, onValueChange = { breed = it }, label = { Text("Breed (e.g. Golden Retriever)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = ageText, onValueChange = { ageText = it }, label = { Text("Age (e.g. 2 years)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (e.g. 24 kg)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = avatarUrl, onValueChange = { avatarUrl = it }, label = { Text("Avatar Image URL") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = allergies, onValueChange = { allergies = it }, label = { Text("Allergies (Grain-sensitive...)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = vaccineRecord, onValueChange = { vaccineRecord = it }, label = { Text("Vaccine Log (Dewormed...)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = dewormingDate, onValueChange = { dewormingDate = it }, label = { Text("Last Deworming Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = vaccineDueDate, onValueChange = { vaccineDueDate = it }, label = { Text("Next Vaccine Due Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showAddPetDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (name.trim().isEmpty() || breed.trim().isEmpty()) {
                                    Toast.makeText(context, "Please write pet name & breed", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.addOrUpdatePet(
                                    id = existingPet?.id,
                                    name = name,
                                    breed = breed,
                                    ageText = ageText,
                                    weight = weight,
                                    avatarUrl = avatarUrl,
                                    allergies = allergies,
                                    vaccineRecord = vaccineRecord,
                                    dewormingDate = dewormingDate,
                                    vaccineDueDate = vaccineDueDate
                                )
                                Toast.makeText(context, "Health Passport updated successfully!", Toast.LENGTH_SHORT).show()
                                showAddPetDialog = false
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Save Passport")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuperAdminScreen(viewModel: PawsViewModel) {
    val pendingShops by viewModel.pendingShops.collectAsState(initial = emptyList())
    val pendingCaptains by viewModel.pendingCaptains.collectAsState(initial = emptyList())
    val allBanners by viewModel.allBanners.collectAsState()
    val categoryList by viewModel.categories.collectAsState()
    val shopsList by viewModel.shops.collectAsState()
    val petProblems by viewModel.petProblems.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showAddBannerDialog by remember { mutableStateOf(false) }
    var showAddRemedyDialog by remember { mutableStateOf(false) }

    var adminUploadedProductImageUrl by remember { mutableStateOf("") }

    val productGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            adminUploadedProductImageUrl = uri.toString()
        }
    }

    val productCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = java.io.File(context.cacheDir, "admin_prod_${System.currentTimeMillis()}.jpg")
            try {
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                adminUploadedProductImageUrl = Uri.fromFile(file).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val presetProductImages = remember {
        listOf(
            Pair("Grooming Shampoo 🧼", "https://images.unsplash.com/photo-1583947215259-38e31be8751f?w=400"),
            Pair("Puppy Kibble 🥩", "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400"),
            Pair("Fungal Shampoo 🧴", "https://images.unsplash.com/photo-1516733725897-1aa73b87c8e8?w=400"),
            Pair("Flea Powder 🚿", "https://images.unsplash.com/photo-1608248597481-496100c80836?w=400"),
            Pair("Dental Chew 🦴", "https://images.unsplash.com/photo-1544438380-ae37b90c425f?w=400"),
            Pair("Chew Toy 🧸", "https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=400")
        )
    }

    var selectedTab by remember { mutableStateOf("shops") }
    var isSidePanelOpen by remember { mutableStateOf(true) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column Side Panel
        if (isSidePanelOpen) {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF282C3F)) // Swiggy Charcoal
                    .padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Swiggy Paws 🛡️",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFC8019) // Swiggy Orange
                        )
                        Text(
                            text = "Super Admin Portal",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                    }
                    IconButton(onClick = { isSidePanelOpen = false }) {
                        Icon(
                            imageVector = Icons.Default.Menu, // "three lines" hamburger menu button
                            contentDescription = "Collapse Sidebar",
                            tint = Color.White
                        )
                    }
                }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val navItems = listOf(
                    Triple("shops", "🏪 Shop Approvals", pendingShops.size),
                    Triple("captains", "🛵 Captain Approvals", pendingCaptains.size),
                    Triple("users", "👥 User Management", 0),
                    Triple("banners", "🖼️ Promo Banners", allBanners.size),
                    Triple("catalog", "🛠️ Catalog Controls", 0),
                    Triple("specs", "📦 Specs & Assets", 0),
                    Triple("remedies", "🩺 Remedy Guides", petProblems.size)
                )

                navItems.forEach { (tabId, label, badgeCount) ->
                    val isSelected = selectedTab == tabId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (tabId == "users") viewModel.navigateTo(Screen.SuperAdminUsers)
                                else selectedTab = tabId
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFFC8019) else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color.LightGray,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (badgeCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) Color.White else Color(0xFFFC8019),
                                            CircleShape
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badgeCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFFC8019) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.logout() },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🚪 Exit Portal", color = Color.Red, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        }

        if (isSidePanelOpen) {
            VerticalDivider(color = Color.LightGray.copy(alpha = 0.3f))
        }

        // Right Content Viewport Panel
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFF7F9FC))
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (!isSidePanelOpen) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { isSidePanelOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Menu, // "three lines" hamburger menu button
                            contentDescription = "Expand Sidebar",
                            tint = Color(0xFFFC8019)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Swiggy Paws Super Admin 🛡️",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFC8019)
                    )
                }
            }
            if (selectedTab == "shops") {
                // Section 1: Pending Shop Registrations
        Text("Pending Shop Approvals", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        
        if (pendingShops.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No pending pet store registrations.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            pendingShops.forEach { shop ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Phone: ${shop.phone} • Locality: ${shop.locality}", fontSize = 12.sp, color = Color.Gray)
                        Text(shop.description, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.declineShop(shop.id) },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color.Red),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Text("Reject")
                            }
                            Button(
                                onClick = { viewModel.approveShop(shop.id) },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DB37A))
                            ) {
                                Text("Approve Store", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        }

        if (selectedTab == "captains") {
            // Section 1.5: Pending Captain Approvals
        Text("Pending Captain Approvals 🛵", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        if (pendingCaptains.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No pending delivery captain applications.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            pendingCaptains.forEach { captain ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = rememberAsyncImagePainter(captain.selfieUrl.ifEmpty { "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200" }),
                                contentDescription = "Selfie",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFFFC8019), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(captain.fullName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Phone: ${captain.phone}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Documents Submitted:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Vehicle Reg No: ${captain.vehicleNumber}", fontSize = 12.sp)
                        Text("• PAN Card No: ${"+".repeat(captain.panCard.length - 4) + captain.panCard.takeLast(4)}", fontSize = 12.sp)
                        Text("• Aadhar Card No: XXXX-XXXX-${captain.aadharNumber.takeLast(4)}", fontSize = 12.sp)
                        Text("• Bank Details: ${"+".repeat((captain.bankDetails.length - 4).coerceAtLeast(0)) + captain.bankDetails.takeLast(4)}", fontSize = 12.sp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Document Images:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // PAN Card Card
                            Card(
                                modifier = Modifier.size(width = 140.dp, height = 100.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(captain.panCardUrl.ifEmpty { "https://images.unsplash.com/photo-1554415707-6e8cfc93fe23?w=400" }),
                                        contentDescription = "PAN Card Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            "PAN Card 💳",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Aadhar Card Card
                            Card(
                                modifier = Modifier.size(width = 140.dp, height = 100.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(captain.aadharCardUrl.ifEmpty { "https://images.unsplash.com/photo-1589758438368-0ad531db3366?w=400" }),
                                        contentDescription = "Aadhar Card Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            "Aadhar Card 🆔",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Driving License Card
                            Card(
                                modifier = Modifier.size(width = 140.dp, height = 100.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(captain.licenseUrl.ifEmpty { "https://images.unsplash.com/photo-1590283603385-17ffb3a7f29f?w=400" }),
                                        contentDescription = "License Image",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            "Driving License 🛵",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.declineCaptain(captain.id) },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color.Red),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Text("Reject")
                            }
                            Button(
                                onClick = { viewModel.approveCaptain(captain.id) },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DB37A))
                            ) {
                                Text("Approve Captain", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
        }

        if (selectedTab == "banners") {
            // Section 2: Banner management
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Promotional Banners", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { showAddBannerDialog = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Banner")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (allBanners.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No banners uploaded yet.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            allBanners.forEach { banner ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = rememberAsyncImagePainter(banner.imageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(banner.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(banner.description, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { viewModel.deleteBanner(banner.id) }) {
                            Icon(Icons.Default.Delete, "Delete Banner", tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
        }

        if (selectedTab == "catalog") {
            Text("Administrative Catalog Controls 🛠️", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Create custom categories and products dynamically into SQLite database.", fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))

        var showCategoryForm by remember { mutableStateOf(false) }
        var showProductForm by remember { mutableStateOf(false) }

        // Category Creation Form Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showCategoryForm = !showCategoryForm },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create New Category", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = if (showCategoryForm) Icons.Default.Info else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showCategoryForm) {
                    var catId by remember { mutableStateOf("") }
                    var catName by remember { mutableStateOf("") }
                    var catIconUrl by remember { mutableStateOf("") }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = catId,
                        onValueChange = { catId = it },
                        label = { Text("Category ID (e.g. 'cat_supplements')") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Name (e.g. 'Supplements & Care')") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = catIconUrl,
                        onValueChange = { catIconUrl = it },
                        label = { Text("Icon Image URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (catId.trim().isEmpty() || catName.trim().isEmpty()) {
                                Toast.makeText(context, "ID and Name cannot be empty!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.createCategory(
                                id = catId.trim(),
                                name = catName.trim(),
                                iconUrl = catIconUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=100" }
                            )
                            Toast.makeText(context, "Category created successfully!", Toast.LENGTH_SHORT).show()
                            catId = ""
                            catName = ""
                            catIconUrl = ""
                            showCategoryForm = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Category")
                    }
                }
            }
        }

        // Product Creation Form Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { showProductForm = !showProductForm },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create New Product Name", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = if (showProductForm) Icons.Default.Info else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showProductForm) {
                    var prodId by remember { mutableStateOf("") }
                    var prodName by remember { mutableStateOf("") }
                    var prodBrand by remember { mutableStateOf("") }
                    var prodDesc by remember { mutableStateOf("") }
                    var prodPrice by remember { mutableStateOf("") }
                    var prodMrp by remember { mutableStateOf("") }
                    var prodStock by remember { mutableStateOf("10") }
                    var prodLifeStage by remember { mutableStateOf("Adult") }
                    var prodPhotoUrl by remember(adminUploadedProductImageUrl) { mutableStateOf(adminUploadedProductImageUrl) }
                    
                    var expandedCatDropdown by remember { mutableStateOf(false) }
                    var selectedCatIndex by remember { mutableStateOf(0) }
                    
                    var expandedShopDropdown by remember { mutableStateOf(false) }
                    var selectedShopIndex by remember { mutableStateOf(0) }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = prodId,
                        onValueChange = { prodId = it },
                        label = { Text("Product ID (e.g. 'p_pedigree_supplement')") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = { Text("Product Name (e.g. 'Pedigree Puppy DentaStix')") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prodBrand,
                        onValueChange = { prodBrand = it },
                        label = { Text("Brand Name (e.g. 'Pedigree', 'Royal Canin')") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prodDesc,
                        onValueChange = { prodDesc = it },
                        label = { Text("Product Description") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = prodPrice,
                            onValueChange = { prodPrice = it },
                            label = { Text("Price (₹)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = prodMrp,
                            onValueChange = { prodMrp = it },
                            label = { Text("MRP (₹)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = prodStock,
                            onValueChange = { prodStock = it },
                            label = { Text("Stock Count") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = prodLifeStage,
                            onValueChange = { prodLifeStage = it },
                            label = { Text("Life Stage (e.g. 'Puppy', 'Adult')") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = prodPhotoUrl,
                        onValueChange = { prodPhotoUrl = it },
                        label = { Text("Product Main Photo URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Product Image Attachment 📸", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Upload an image via gallery/camera, choose from pre-existing items, or paste a custom URL.", fontSize = 10.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { productGalleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFC8019).copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Share, null, tint = Color(0xFFFC8019), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery 📁", color = Color(0xFFFC8019), fontSize = 11.sp)
                        }
                        
                        OutlinedButton(
                            onClick = { productCameraLauncher.launch() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFC8019).copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color(0xFFFC8019), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera 📸", color = Color(0xFFFC8019), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Or select from Existing Premium Products:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetProductImages) { preset ->
                            val isSelected = prodPhotoUrl == preset.second
                            Card(
                                modifier = Modifier
                                    .width(80.dp)
                                    .clickable {
                                        adminUploadedProductImageUrl = preset.second
                                    },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFFFC8019) else Color.LightGray.copy(alpha = 0.5f))
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(preset.second),
                                            contentDescription = preset.first,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.4f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = preset.first.substringBefore(" "),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(2.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                        colors = CardDefaults.cardColors(containerColor = Color.Gray.copy(alpha = 0.05f))
                    ) {
                        if (prodPhotoUrl.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(prodPhotoUrl)
                                            .crossfade(true)
                                            .error(R.drawable.paws_logo_1779795154399)
                                            .build()
                                    ),
                                    contentDescription = "Product Image Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(topEnd = 8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (prodPhotoUrl.startsWith("content://") || prodPhotoUrl.startsWith("file://")) "Preview: Local Attachment ✅" else "Preview: Remote Link 🔗",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No Product Image Attached ❌", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dropdown for Category ID Selection
                    Text("Select Product Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val activeCategory = categoryList.getOrNull(selectedCatIndex)
                        OutlinedButton(
                            onClick = { expandedCatDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(activeCategory?.name ?: "Select Category")
                        }
                        DropdownMenu(
                            expanded = expandedCatDropdown,
                            onDismissRequest = { expandedCatDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categoryList.forEachIndexed { index, cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCatIndex = index
                                        expandedCatDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dropdown for Shop ID Selection
                    Text("Select Shop to Assign:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val activeShop = shopsList.getOrNull(selectedShopIndex)
                        OutlinedButton(
                            onClick = { expandedShopDropdown = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(activeShop?.name ?: "Select Shop")
                        }
                        DropdownMenu(
                            expanded = expandedShopDropdown,
                            onDismissRequest = { expandedShopDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            shopsList.forEachIndexed { index, shop ->
                                DropdownMenuItem(
                                    text = { Text("${shop.name} (${shop.locality})") },
                                    onClick = {
                                        selectedShopIndex = index
                                        expandedShopDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val priceVal = prodPrice.toDoubleOrNull() ?: 0.0
                            val mrpVal = prodMrp.toDoubleOrNull() ?: 0.0
                            val stockVal = prodStock.toIntOrNull() ?: 10
                            val shopObj = shopsList.getOrNull(selectedShopIndex)
                            val catObj = categoryList.getOrNull(selectedCatIndex)

                            if (prodId.trim().isEmpty() || prodName.trim().isEmpty() || shopObj == null || catObj == null) {
                                Toast.makeText(context, "Product ID, Product Name, Category and Shop must be selected!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.createProduct(
                                id = prodId.trim(),
                                shopId = shopObj.id,
                                categoryId = catObj.id,
                                name = prodName.trim(),
                                description = prodDesc.trim(),
                                price = priceVal,
                                mrp = mrpVal,
                                photos = listOf(prodPhotoUrl.trim().ifEmpty { "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400" }),
                                brand = prodBrand.trim().ifEmpty { "Generic" },
                                lifeStage = prodLifeStage.trim().ifEmpty { "Adult" },
                                stockCount = stockVal
                            )
                            Toast.makeText(context, "Product successfully created!", Toast.LENGTH_SHORT).show()
                            prodId = ""
                            prodName = ""
                            prodBrand = ""
                            prodDesc = ""
                            prodPrice = ""
                            prodMrp = ""
                            prodStock = "10"
                            prodLifeStage = "Adult"
                            prodPhotoUrl = ""
                            showProductForm = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Create Product")
                    }
                }
            }
        }
        }

        if (selectedTab == "specs") {
            // Section 3: Product Package Specifications & Asset Controls
            Text("Product Package Specifications & Asset Controls 🐾", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Configure weight-specific product packaging images and static description lists.", fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(12.dp))

        val allProducts by viewModel.allProducts.collectAsState()
        var adminSearchQuery by remember { mutableStateOf("") }
        var adminSelectedProductId by remember { mutableStateOf("p_pedigree_dry") }
        
        // Auto-select first available product once loaded, if current selection is invalid
        LaunchedEffect(allProducts) {
            if (allProducts.isNotEmpty() && (adminSelectedProductId.isEmpty() || allProducts.none { it.id == adminSelectedProductId })) {
                adminSelectedProductId = allProducts.first().id
            }
        }

        // Filtered products list matching admin search query
        val adminFilteredProducts = remember(allProducts, adminSearchQuery) {
            allProducts.filter { 
                it.name.contains(adminSearchQuery, ignoreCase = true) || 
                it.brand.contains(adminSearchQuery, ignoreCase = true) 
            }
        }

        // Product Selection Bar (Search & Select Autocomplete UX)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Search & Select Target Product:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                val focusManager = LocalFocusManager.current

                Box(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        OutlinedTextField(
                            value = adminSearchQuery,
                            onValueChange = { adminSearchQuery = it },
                            placeholder = { Text("Type name or brand (e.g. 'pedigree', 'whiskas')", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { 
                                if (adminSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { adminSearchQuery = "" }) {
                                        Icon(Icons.Default.Clear, "Clear Search")
                                    }
                                } else {
                                    Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) 
                                }
                            }
                        )
                        
                        // Show Currently Selected Product info card when not searching
                        if (adminSearchQuery.trim().isEmpty()) {
                            val currentSelectedProd = allProducts.find { it.id == adminSelectedProductId }
                            if (currentSelectedProd != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Active Product Selected:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        Text(currentSelectedProd.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Brand: ${currentSelectedProd.brand} • Base Price: ₹${currentSelectedProd.price}", fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Selected", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                    }
                                }
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = adminSearchQuery.trim().isNotEmpty(),
                        onDismissRequest = { adminSearchQuery = "" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        if (adminFilteredProducts.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No matching products found.", fontSize = 12.sp, color = Color.Gray) },
                                onClick = {}
                            )
                        } else {
                            adminFilteredProducts.take(6).forEach { product ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(product.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                            Text("Brand: ${product.brand} • Category: ${product.categoryId.replace("cat_", "").uppercase()}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        adminSelectedProductId = product.id
                                        adminSearchQuery = ""
                                        focusManager.clearFocus()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Specifications and Preview Area
        val selectedProduct = remember(allProducts, adminSelectedProductId) {
            allProducts.find { it.id == adminSelectedProductId }
        }

        if (selectedProduct != null) {
            val specs by remember(adminSelectedProductId) { viewModel.getSpecsForProduct(adminSelectedProductId) }.collectAsState(initial = emptyList())
            var activeSpecIndex by remember { mutableStateOf(0) }
            
            // Auto reset active spec index if specs size changes
            LaunchedEffect(specs) {
                activeSpecIndex = 0
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Active Specifications for: ${selectedProduct.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Brand: ${selectedProduct.brand} • Base Price: ₹${selectedProduct.price}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    if (specs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No package specifications configured for this product.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val autoCategory = when {
                                            selectedProduct.name.contains("cat", ignoreCase = true) || selectedProduct.brand.equals("Whiskas", ignoreCase = true) -> "cat"
                                            selectedProduct.name.contains("cattle", ignoreCase = true) || selectedProduct.brand.equals("Bovishield", ignoreCase = true) -> "cattle"
                                            selectedProduct.name.contains("rabbit", ignoreCase = true) || selectedProduct.brand.equals("Oxbow", ignoreCase = true) -> "rabbits"
                                            selectedProduct.name.contains("bird", ignoreCase = true) || selectedProduct.brand.equals("Wagner", ignoreCase = true) -> "birds"
                                            selectedProduct.name.contains("hamster", ignoreCase = true) || selectedProduct.brand.equals("Kaytee", ignoreCase = true) -> "hamster"
                                            selectedProduct.name.contains("puppy", ignoreCase = true) -> "puppy"
                                            selectedProduct.name.contains("kitten", ignoreCase = true) -> "kitten"
                                            else -> "dog"
                                        }
                                        viewModel.addOrUpdateProductSpec(
                                            productId = adminSelectedProductId,
                                            weightText = "3 kg",
                                            petCategory = autoCategory,
                                            imageUrls = listOf(
                                                "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400",
                                                "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=400",
                                                "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400",
                                                "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400"
                                            ),
                                            desc1 = "Premium protein sourced from quality ingredients supporting strong, lean muscles.",
                                            desc2 = "Rich in Omega-6 fatty acids and zinc for a noticeably radiant skin and healthy coat.",
                                            desc3 = "Fortified with dietary fiber to support digestion and maximum nutrient absorption.",
                                            desc4 = "Specially shaped crunchy kibble designed to help clean teeth and keep gums healthy."
                                        )
                                        viewModel.addOrUpdateProductSpec(
                                            productId = adminSelectedProductId,
                                            weightText = "10 kg",
                                            petCategory = autoCategory,
                                            imageUrls = listOf(
                                                "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=400",
                                                "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=400",
                                                "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400",
                                                "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400"
                                            ),
                                            desc1 = "Bulk active family size package providing long-lasting puppy energy and nutrition.",
                                            desc2 = "Optimized calcium and phosphorus ratio to support healthy bone structure and active joints.",
                                            desc3 = "Infused with organic prebiotics to promote a balanced gut flora in growing dogs.",
                                            desc4 = "Fortified with natural vitamin E and minerals to boost strong immune defenses."
                                        )
                                        Toast.makeText(context, "Seeded specifications successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Seed Sample Specs", fontSize = 11.sp)
                                }
                            }
                        }
                    } else {
                        // Weight selection row (Dynamic package selector)
                        Text("Select Package Weight Variation:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            specs.forEachIndexed { index, spec ->
                                val isSelected = index == activeSpecIndex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { activeSpecIndex = index }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = spec.weightText,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Selected spec presentation (Image & Descriptions)
                        val activeSpec = specs.getOrNull(activeSpecIndex)
                        if (activeSpec != null) {
                            // Horizontal scrolling Row of up to 4 package images with standard borders
                            Text("Package Images (Up to 4):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                            ) {
                                items(activeSpec.imageUrls) { imgUrl ->
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(SupabaseManager.resolveImageUrl(imgUrl)),
                                            contentDescription = "Package Image Variation",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Custom static description bullet points (up to 4)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Package Highlights:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = activeSpec.petCategory.uppercase(),
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val points = listOf(
                                        activeSpec.description1,
                                        activeSpec.description2,
                                        activeSpec.description3,
                                        activeSpec.description4
                                    ).filter { it.isNotBlank() }

                                    points.forEach { point ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text("•", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(point, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Delete active spec action
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "Remove Spec",
                                    color = Color.Red,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        viewModel.deleteProductSpec(activeSpec.id)
                                        Toast.makeText(context, "Specification removed successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sub-form to Add New Custom Package Specifications
        var showAddSpecEditor by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAddSpecEditor = !showAddSpecEditor },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create Custom Package Specification ➕", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = if (showAddSpecEditor) Icons.Default.Info else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (showAddSpecEditor) {
                    var weightText by remember { mutableStateOf("") }
                    var specPetCategory by remember { mutableStateOf("dog") }
                    var imageUrl1 by remember { mutableStateOf("") }
                    var imageUrl2 by remember { mutableStateOf("") }
                    var imageUrl3 by remember { mutableStateOf("") }
                    var imageUrl4 by remember { mutableStateOf("") }
                    var desc1 by remember { mutableStateOf("") }
                    var desc2 by remember { mutableStateOf("") }
                    var desc3 by remember { mutableStateOf("") }
                    var desc4 by remember { mutableStateOf("") }
                    var isUploadingSpec by remember { mutableStateOf(false) }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Weight Variation (e.g. '3 kg', '10 kg', '500 g')") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = imageUrl1,
                        onValueChange = { imageUrl1 = it },
                        label = { Text("Package Image 1 URL (Paste link or upload below)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = imageUrl2,
                        onValueChange = { imageUrl2 = it },
                        label = { Text("Package Image 2 URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = imageUrl3,
                        onValueChange = { imageUrl3 = it },
                        label = { Text("Package Image 3 URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = imageUrl4,
                        onValueChange = { imageUrl4 = it },
                        label = { Text("Package Image 4 URL (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    // Target Animal Category Selector
                    Text("Select Target Pet Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("dog", "cat", "cattle", "kitten", "puppy", "hamster", "rabbits", "birds").forEach { catKey ->
                            val isCatSelected = specPetCategory == catKey
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isCatSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .clickable { specPetCategory = catKey }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = catKey.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    color = if (isCatSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    // Supabase upload for spec packaging asset
                    OutlinedButton(
                        onClick = {
                            isUploadingSpec = true
                            scope.launch {
                                val mockBytes = ByteArray(10)
                                val publicCdnUrl = SupabaseManager.uploadProductImage("photos", "spec_" + System.currentTimeMillis() + ".jpg", mockBytes)
                                if (imageUrl1.isEmpty()) imageUrl1 = publicCdnUrl
                                else if (imageUrl2.isEmpty()) imageUrl2 = publicCdnUrl
                                else if (imageUrl3.isEmpty()) imageUrl3 = publicCdnUrl
                                else imageUrl4 = publicCdnUrl
                                isUploadingSpec = false
                                Toast.makeText(context, "Spec package uploaded to Supabase CDN!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isUploadingSpec
                    ) {
                        Text(if (isUploadingSpec) "Uploading packaging image..." else "Upload Packaging Image to CDN")
                    }

                    if (imageUrl1.isNotEmpty()) {
                        Text("Image 1 URL: $imageUrl1", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Custom Dynamic Descriptions (Up to 4):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = desc1, onValueChange = { desc1 = it }, label = { Text("Description 1") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = desc2, onValueChange = { desc2 = it }, label = { Text("Description 2") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = desc3, onValueChange = { desc3 = it }, label = { Text("Description 3") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(value = desc4, onValueChange = { desc4 = it }, label = { Text("Description 4") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp))

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val imgList = listOf(imageUrl1, imageUrl2, imageUrl3, imageUrl4).filter { it.trim().isNotEmpty() }
                            if (weightText.trim().isEmpty() || imgList.isEmpty()) {
                                Toast.makeText(context, "Please configure Weight and at least one package image!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.addOrUpdateProductSpec(
                                productId = adminSelectedProductId,
                                weightText = weightText,
                                petCategory = specPetCategory,
                                imageUrls = imgList,
                                desc1 = desc1,
                                desc2 = desc2,
                                desc3 = desc3,
                                desc4 = desc4
                            )
                            Toast.makeText(context, "New specification saved successfully!", Toast.LENGTH_SHORT).show()
                            weightText = ""
                            imageUrl1 = ""
                            imageUrl2 = ""
                            imageUrl3 = ""
                            imageUrl4 = ""
                            desc1 = ""
                            desc2 = ""
                            desc3 = ""
                            desc4 = ""
                            showAddSpecEditor = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Specification")
                    }
                }
            }
        }
        }

        if (selectedTab == "remedies") {
            // Remedies & Targeted Problems Management Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Targeted Remedy Guides 🩺", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Manage pet problems, care remedies, and recommended products shown to customer users.", fontSize = 11.sp, color = Color.Gray)
                }
                Button(
                    onClick = { showAddRemedyDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019))
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Guide", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var showRemedyForm by remember { mutableStateOf(false) }

            // Collapsible Remedy Creation Form Card (Exactly like Create Category)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showRemedyForm = !showRemedyForm },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Create New Remedy Guide", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Icon(
                            imageVector = if (showRemedyForm) Icons.Default.Info else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (showRemedyForm) {
                        var title by remember { mutableStateOf("") }
                        var emoji by remember { mutableStateOf("🩺") }
                        var description by remember { mutableStateOf("") }
                        var solution by remember { mutableStateOf("") }
                        var howToUse by remember { mutableStateOf("") }
                        var selectedProductIds by remember { mutableStateOf(emptySet<String>()) }
                        var productSearchQuery by remember { mutableStateOf("") }

                        val filteredProducts = remember(allProducts, productSearchQuery) {
                            allProducts.filter {
                                it.name.contains(productSearchQuery, ignoreCase = true) ||
                                it.brand.contains(productSearchQuery, ignoreCase = true)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Problem Title (e.g. 'Full Hair Growth')") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = emoji,
                            onValueChange = { emoji = it },
                            label = { Text("Emoji Icon (e.g. '🩺', '🦁')") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Care Description") },
                            placeholder = { Text("Describe care remedies & dietary tips...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = solution,
                            onValueChange = { solution = it },
                            label = { Text("Care Solution / Remedies") },
                            placeholder = { Text("Provide detailed care remedies & treatments...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = howToUse,
                            onValueChange = { howToUse = it },
                            label = { Text("Way to Use the Products") },
                            placeholder = { Text("Specify steps on how to use recommended products...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Select Recommended Products:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = productSearchQuery,
                            onValueChange = { productSearchQuery = it },
                            placeholder = { Text("Search products by name or brand...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(8.dp)
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(filteredProducts) { prod ->
                                    val isChecked = prod.id in selectedProductIds
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedProductIds = if (isChecked) {
                                                    selectedProductIds - prod.id
                                                } else {
                                                    selectedProductIds + prod.id
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                selectedProductIds = if (isChecked) {
                                                    selectedProductIds - prod.id
                                                } else {
                                                    selectedProductIds + prod.id
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Image(
                                            painter = rememberAsyncImagePainter(prod.photos.firstOrNull()),
                                            contentDescription = null,
                                            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(prod.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("₹${prod.price}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (title.trim().isEmpty() || description.trim().isEmpty() || solution.trim().isEmpty() || howToUse.trim().isEmpty()) {
                                    Toast.makeText(context, "Please fill in all details, solutions, and usage instructions!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.addPetProblem(
                                    title = title,
                                    description = description,
                                    solution = solution,
                                    howToUse = howToUse,
                                    emoji = emoji,
                                    productIds = selectedProductIds.toList()
                                )
                                Toast.makeText(context, "Remedy Guide created successfully!", Toast.LENGTH_SHORT).show()
                                title = ""
                                emoji = "🩺"
                                description = ""
                                solution = ""
                                howToUse = ""
                                selectedProductIds = emptySet()
                                showRemedyForm = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save Remedy Guide", color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (petProblems.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No remedy guides defined. Click Create Guide to add one.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                petProblems.forEach { problem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(problem.emoji.ifEmpty { "🩺" }, fontSize = 18.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = problem.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                                
                                IconButton(
                                    onClick = { 
                                        viewModel.deletePetProblem(problem.id)
                                        Toast.makeText(context, "Remedy Guide deleted successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Guide", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Description: ${problem.description}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Solution: ${problem.solution.ifEmpty { "N/A" }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Procedure: ${problem.howToUse.ifEmpty { "N/A" }}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Recommended Products:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Load related products in a horizontal row
                            val associatedProducts = allProducts.filter { it.id in problem.productIds }
                            if (associatedProducts.isEmpty()) {
                                Text("No products linked currently.", fontSize = 11.sp, color = Color.Gray)
                            } else {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(associatedProducts) { prod ->
                                        Card(
                                            modifier = Modifier.width(180.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(prod.photos.firstOrNull()),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(6.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(prod.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("₹${prod.price}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }

    if (showAddRemedyDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var solution by remember { mutableStateOf("") }
        var howToUse by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf("🩺") }
        var selectedProductIds by remember { mutableStateOf(emptySet<String>()) }
        var productSearchQuery by remember { mutableStateOf("") }

        val filteredProducts = remember(allProducts, productSearchQuery) {
            allProducts.filter {
                it.name.contains(productSearchQuery, ignoreCase = true) ||
                it.brand.contains(productSearchQuery, ignoreCase = true)
            }
        }
        
        Dialog(onDismissRequest = { showAddRemedyDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Create Remedy Guide 🩺", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Problem Title") },
                        placeholder = { Text("e.g. Full Hair Growth") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Emoji Icon") },
                        placeholder = { Text("e.g. 🦁") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Care Description") },
                        placeholder = { Text("Describe care remedies & dietary tips...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = solution,
                        onValueChange = { solution = it },
                        label = { Text("Care Solution / Remedies") },
                        placeholder = { Text("Provide detailed care remedies & treatments...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = howToUse,
                        onValueChange = { howToUse = it },
                        label = { Text("Way to Use the Products") },
                        placeholder = { Text("Specify steps on how to use recommended products...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Recommended Products:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = productSearchQuery,
                        onValueChange = { productSearchQuery = it },
                        placeholder = { Text("Search products by name or brand...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredProducts) { prod ->
                                val isChecked = prod.id in selectedProductIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedProductIds = if (isChecked) {
                                                selectedProductIds - prod.id
                                            } else {
                                                selectedProductIds + prod.id
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = {
                                            selectedProductIds = if (isChecked) {
                                                selectedProductIds - prod.id
                                            } else {
                                                selectedProductIds + prod.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Image(
                                        painter = rememberAsyncImagePainter(prod.photos.firstOrNull()),
                                        contentDescription = null,
                                        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(prod.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("₹${prod.price}", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddRemedyDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (title.trim().isEmpty() || description.trim().isEmpty() || solution.trim().isEmpty() || howToUse.trim().isEmpty()) {
                                    Toast.makeText(context, "Please fill in all details, solutions, and usage instructions!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.addPetProblem(
                                    title = title,
                                    description = description,
                                    solution = solution,
                                    howToUse = howToUse,
                                    emoji = emoji,
                                    productIds = selectedProductIds.toList()
                                )
                                Toast.makeText(context, "Remedy Guide created successfully!", Toast.LENGTH_SHORT).show()
                                showAddRemedyDialog = false
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC8019)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Create Guide", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showAddBannerDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var imageUrl by remember { mutableStateOf("") }
        var isUploading by remember { mutableStateOf(false) }

        val citiesList by viewModel.cities.collectAsState(initial = emptyList())
        val availableStates = remember(citiesList) { citiesList.map { it.state }.distinct() }
        var selectedStateFilter by remember { mutableStateOf("All States") }
        var selectedCityIds by remember { mutableStateOf(emptySet<String>()) }
        
        // Auto-select all cities by default when the dialog opens
        LaunchedEffect(citiesList) {
            if (citiesList.isNotEmpty() && selectedCityIds.isEmpty()) {
                selectedCityIds = citiesList.map { it.id }.toSet()
            }
        }
        
        // Filter cities in the selection dropdown by target State
        val filteredCitiesByState = remember(citiesList, selectedStateFilter) {
            if (selectedStateFilter == "All States") citiesList
            else citiesList.filter { it.state == selectedStateFilter }
        }

        Dialog(onDismissRequest = { showAddBannerDialog = false }) {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Create Promo Banner 🌟", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Banner Title") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Banner Description") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Supabase Storage CDN Mock Upload Button
                    OutlinedButton(
                        onClick = {
                            isUploading = true
                            scope.launch {
                                val mockBytes = ByteArray(10)
                                val publicCdnUrl = SupabaseManager.uploadProductImage("banners", "banner_" + System.currentTimeMillis() + ".jpg", mockBytes)
                                imageUrl = publicCdnUrl
                                isUploading = false
                                Toast.makeText(context, "Uploaded successfully to Supabase Storage CDN!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isUploading
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isUploading) "Uploading to Supabase CDN..." else "Upload Image to Supabase CDN")
                    }
                    
                    if (imageUrl.isNotEmpty()) {
                        Text("CDN URL: $imageUrl", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // State Filter Row
                    Text("Filter Cities by State:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val stateFilters = listOf("All States") + availableStates
                        stateFilters.forEach { stateName ->
                            val isSelected = selectedStateFilter == stateName
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedStateFilter = stateName }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stateName,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Select All Toggle Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCityIds = if (selectedCityIds.size == citiesList.size) {
                                    emptySet()
                                } else {
                                    citiesList.map { it.id }.toSet()
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedCityIds.size == citiesList.size && citiesList.isNotEmpty(),
                            onCheckedChange = { checked ->
                                selectedCityIds = if (checked) citiesList.map { it.id }.toSet() else emptySet()
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select All Cities (Default)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    // Dynamic Cities Checkbox List (Scrollable box)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 100.dp)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column {
                            filteredCitiesByState.forEach { city ->
                                val isCityChecked = selectedCityIds.contains(city.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCityIds = if (isCityChecked) {
                                                selectedCityIds - city.id
                                            } else {
                                                selectedCityIds + city.id
                                            }
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isCityChecked,
                                        onCheckedChange = { checked ->
                                            selectedCityIds = if (checked) {
                                                selectedCityIds + city.id
                                            } else {
                                                selectedCityIds - city.id
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(city.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(city.state, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { showAddBannerDialog = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (title.trim().isEmpty() || description.trim().isEmpty() || imageUrl.trim().isEmpty()) {
                                    Toast.makeText(context, "Please fill title, desc & upload/paste image", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedCityIds.isEmpty()) {
                                    Toast.makeText(context, "Please select at least one target city!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.createBanner(
                                    title = title,
                                    description = description,
                                    imageUrl = imageUrl,
                                    targetCityIds = if (selectedCityIds.size == citiesList.size) listOf("all") else selectedCityIds.toList(),
                                    targetShopIds = listOf("all")
                                )
                                showAddBannerDialog = false
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Save Banner")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListScreen(viewModel: PawsViewModel) {
    val shopsList by viewModel.shops.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Direct Messages 💬", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn {
            items(shopsList) { shop ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            viewModel.selectActiveChat(shop.id)
                            viewModel.navigateTo(Screen.ChatDetail(shop.id))
                        },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = rememberAsyncImagePainter(shop.photos.firstOrNull()),
                            contentDescription = null,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Click to view messages and ask about stock & groomers", fontSize = 11.sp, color = Color.Gray)
                        }
                        Icon(Icons.Default.ArrowForward, null, tint = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatDetailScreen(viewModel: PawsViewModel, shopId: String) {
    val messages by viewModel.activeChatMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val shopsList by viewModel.shops.collectAsState()
    
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    val currentShop = remember(shopId, shopsList) { shopsList.find { it.id == shopId } }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Image(
                painter = rememberAsyncImagePainter(currentShop?.photos?.firstOrNull()),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(currentShop?.name ?: "Pet Shop", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(if (currentShop?.isOpen == true) "Open now" else "Closed now", fontSize = 11.sp, color = if (currentShop?.isOpen == true) Color(0xFF2DB37A) else Color.Red)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == (currentUser?.id ?: "consumer_arjun")
                
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.widthIn(max = 260.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (!isMe) {
                                Text(msg.senderName, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(msg.message, color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Ask about stock, grooming, discounts...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 2
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (textInput.trim().isNotEmpty()) {
                        viewModel.sendChatMessage(
                            shopId = shopId,
                            text = textInput,
                            senderName = currentUser?.fullName ?: "Customer"
                        )
                        textInput = ""
                    }
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(44.dp)
            ) {
                Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun MerchantOrdersScreen(viewModel: PawsViewModel) {
    val orders by viewModel.getMerchantOrdersFlow().collectAsState(initial = emptyList())
    val context = LocalContext.current
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Merchant Orders 📦", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (orders.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No customer orders placed yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(orders) { order ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Order ID: #${order.id.takeLast(6).uppercase()}", fontWeight = FontWeight.Bold)
                                Text(
                                    order.status.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (order.status == "delivered") Color(0xFF2DB37A) else Color(0xFFFFB300)
                                )
                            }
                            Text("Address: ${order.deliveryAddress}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                            Text("Grand Total: ₹${order.totalAmount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (order.status == "pending") {
                                    Button(
                                        onClick = {
                                            viewModel.updateMerchantOrderStatus(order.id, "processing")
                                            NotificationManager.fireInstantNotification(context, "Order Processing", "We are packing your items!")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Accept")
                                    }
                                } else if (order.status == "processing") {
                                    Button(
                                        onClick = {
                                            viewModel.updateMerchantOrderStatus(order.id, "shipped")
                                            NotificationManager.fireInstantNotification(context, "Order Shipped", "Delivery partner is on the way!")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Ship Out")
                                    }
                                } else if (order.status == "shipped") {
                                    Button(
                                        onClick = {
                                            viewModel.updateMerchantOrderStatus(order.id, "delivered")
                                            NotificationManager.fireInstantNotification(context, "Order Delivered", "Woof! Thank you for ordering!")
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DB37A))
                                    ) {
                                        Text("Deliver")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantMenuScreen(viewModel: PawsViewModel) {
    val shopState by viewModel.merchantShop.collectAsState()
    val products by viewModel.getMerchantProductsFlow().collectAsState(initial = emptyList())
    val context = LocalContext.current
    
    var isAddingProduct by remember { mutableStateOf(false) }
    var prodName by remember { mutableStateOf("") }
    var prodDesc by remember { mutableStateOf("") }
    var prodPrice by remember { mutableStateOf("") }

    val shop = shopState ?: return

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Inventory 🏪", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Button(onClick = { isAddingProduct = true }) {
                Icon(Icons.Default.Add, null)
                Text("Add Item")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        if (products.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Your store catalog is empty.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(products) { prod ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Price: ₹${prod.price}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteMerchantProduct(prod.id) }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    if (isAddingProduct) {
        Dialog(onDismissRequest = { isAddingProduct = false }) {
            Card(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Add Store Product 🏪", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = prodName, onValueChange = { prodName = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = prodDesc, onValueChange = { prodDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = prodPrice, onValueChange = { prodPrice = it }, label = { Text("Price (₹)") }, modifier = Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { isAddingProduct = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val priceVal = prodPrice.toDoubleOrNull()
                                if (prodName.trim().isEmpty() || priceVal == null) {
                                    Toast.makeText(context, "Please write correct details", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.addMerchantProduct(
                                    name = prodName,
                                    categoryId = "dog_food",
                                    description = prodDesc,
                                    price = priceVal,
                                    mrp = priceVal * 1.2
                                )
                                prodName = ""
                                prodDesc = ""
                                prodPrice = ""
                                isAddingProduct = false
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Save Product")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
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
                Text(
                    text = L10n.getString("select_language_title"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                val options = listOf(
                    Triple("en", "English", "English"),
                    Triple("hi", "हिन्दी", "Hindi"),
                    Triple("te", "తెలుగు", "Telugu")
                )
                
                options.forEach { (code, nativeName, engName) ->
                    val isSelected = code == currentLanguage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable {
                                onLanguageSelected(code)
                                onDismissRequest()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = nativeName,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = engName,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismissRequest) {
                    Text(L10n.getString("cancel"))
                }
            }
        }
    }
}

// ==========================================
// SCREEN: APPOINTMENTS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(viewModel: PawsViewModel) {
    val appointments by viewModel.activeAppointments.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Upcoming, 1 = Past
    var showRescheduleDialog by remember { mutableStateOf<AppointmentEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Appointments",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Profile image
            val currentUser by viewModel.currentUser.collectAsState()
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.secondary, CircleShape)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        currentUser?.avatarUrl ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuCB2YwNOMe08wQT5HC7_1fP0roGDby2v9VT2elw1_xojx6a-WmXM5t2j2mr55QUX-FZedgBz1KrwrBwOZ9mT-EgkAxuilcmkSfWwrPRjYW71yxIAkgJQLF6SGTvnKh-cbUROw0hH6s-r4eeD90bGi3irqYlly003FRTAj-z5eRZKSXuaxvmZLfi2OPx91mELxEtpFORREtuAc7Aby49lMZ3_OFPPfdJuxFSxp9nPdp4utdJFz807gOXSmvkQd4ItX-mzPUxa6K8o-Q"
                    ),
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Subtitle
        Text(
            text = "Manage your pet's upcoming visits and view past history.",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp)
        )

        // Segmented Control (Tabs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .background(Color(0xFFE5EEFF), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            val upcomingSelected = selectedTab == 0
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (upcomingSelected) Color.White else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Upcoming",
                    fontWeight = FontWeight.Bold,
                    color = if (upcomingSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontSize = 14.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!upcomingSelected) Color.White else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Past",
                    fontWeight = FontWeight.Bold,
                    color = if (!upcomingSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filtered Appointments
        val filtered = remember(appointments, selectedTab) {
            appointments.filter { appt ->
                if (selectedTab == 0) {
                    appt.status == "pending" || appt.status == "confirmed"
                } else {
                    appt.status == "completed" || appt.status == "cancelled"
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(filtered) { appt ->
                AppointmentCard(
                    appointment = appt,
                    isUpcoming = selectedTab == 0,
                    onAddToCal = {
                        Toast.makeText(context, "Added to Calendar! 📅", Toast.LENGTH_SHORT).show()
                    },
                    onReschedule = {
                        showRescheduleDialog = appt
                    },
                    onCancel = {
                        viewModel.updateAppointmentStatus(appt.id, "cancelled")
                        Toast.makeText(context, "Appointment cancelled", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (selectedTab == 0) {
                item {
                    // Book New Visit Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clickable {
                                viewModel.navigateTo(Screen.Home)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE5EEFF)
                        ),
                        border = BorderStroke(1.5.dp, Color(0xFF2563EB).copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Book",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Book New Visit",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Schedule a checkup, grooming, or consultation.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                if (filtered.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No past appointments found", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    // Reschedule Dialog
    showRescheduleDialog?.let { appt ->
        var newDate by remember { mutableStateOf(appt.appointmentDate) }
        var newTime by remember { mutableStateOf(appt.appointmentTime) }

        AlertDialog(
            onDismissRequest = { showRescheduleDialog = null },
            title = { Text("Reschedule Appointment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select new date and time for ${appt.petName}'s appointment:")
                    
                    OutlinedTextField(
                        value = newDate,
                        onValueChange = { newDate = it },
                        label = { Text("New Date (e.g. YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newTime,
                        onValueChange = { newTime = it },
                        label = { Text("New Time (e.g. HH:MM AM/PM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rescheduleAppointment(appt, newDate, newTime)
                        showRescheduleDialog = null
                        Toast.makeText(context, "Rescheduled successfully!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescheduleDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AppointmentCard(
    appointment: AppointmentEntity,
    isUpcoming: Boolean,
    onAddToCal: () -> Unit,
    onReschedule: () -> Unit,
    onCancel: () -> Unit
) {
    val doctorName = if (appointment.shopId == "shop_hyd_1") "Dr. Sarah Jenkins" else "Dr. Michael Chen"
    val clinicName = if (appointment.shopId == "shop_hyd_1") "City Paws Veterinary Clinic" else "Downtown Animal Hospital"
    val dateParts = appointment.appointmentDate.split("-")
    val monthStr = if (dateParts.size >= 2) {
        when (dateParts[1]) {
            "01" -> "Jan"
            "02" -> "Feb"
            "03" -> "Mar"
            "04" -> "Apr"
            "05" -> "May"
            "06" -> "Jun"
            "07" -> "Jul"
            "08" -> "Aug"
            "09" -> "Sep"
            "10" -> "Oct"
            "11" -> "Nov"
            "12" -> "Dec"
            else -> "Oct"
        }
    } else "Oct"
    val dayStr = if (dateParts.size >= 3) dateParts[2] else "24"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.serviceName,
                        fontWeight = FontWeight.Bold,
                        color = if (appointment.shopId == "shop_hyd_1") Color(0xFF004AC6) else Color(0xFF855300),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = doctorName,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = clinicName,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFD3E4FE)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.width(60.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = monthStr.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = dayStr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF004AC6)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Time",
                    tint = Color(0xFF004AC6),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${appointment.appointmentTime} - ${if (appointment.serviceName.contains("Wellness")) "11:15 AM" else "02:30 PM"}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFF4FF), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val avatar = if (appointment.petName.contains("Bella")) {
                    "https://images.unsplash.com/photo-1552053831-71594a27632d?w=200"
                } else {
                    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=200"
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(avatar),
                        contentDescription = "Pet Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${appointment.petName} (${if (appointment.petName.contains("Bella")) "Dog" else "Cat"})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isUpcoming) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddToCal,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add to Cal", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onReschedule,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reschedule", fontSize = 12.sp, color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Cancel Appointment", fontSize = 12.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Status: ${appointment.status.uppercase()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (appointment.status == "completed") Color(0xFF006242) else Color.Red
                )
            }
        }
    }
}

// ==========================================
// SCREEN: TABLETS ISSUED (MEDICATION LOG)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletsIssuedScreen(viewModel: PawsViewModel) {
    UnifiedVaccinationsTabletsScreen(viewModel = viewModel, defaultTab = 0)
}

// ==========================================
// SCREEN: HEALTH & VACCINATIONS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationsScreen(viewModel: PawsViewModel) {
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF2563EB).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color.White, CircleShape)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            "https://images.unsplash.com/photo-1552053831-71594a27632d?w=200"
                        ),
                        contentDescription = "Buddy Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = petName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$breed • $age",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Up to date",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vaccination History",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("Track Buddy's immunizations and due dates.", fontSize = 11.sp, color = Color.Gray)
                }

                TextButton(
                    onClick = { showAddVaccDialog = true }
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Record", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp)
            ) {
                vaccinations.forEachIndexed { index, vacc ->
                    val notesParts = vacc.notes.split("|").map { it.trim() }
                    val adminStr = notesParts.getOrNull(0) ?: "Administered: N/A"
                    val dueStr = notesParts.getOrNull(1) ?: ""
                    val docName = notesParts.getOrNull(2) ?: "Dr. Sarah Jenkins"
                    val hospName = notesParts.getOrNull(3) ?: ""
                    val hasCert = notesParts.getOrNull(4) == "cert"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == 0) MaterialTheme.colorScheme.primary 
                                        else Color.LightGray
                                    )
                            )
                            if (index < vaccinations.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(140.dp)
                                        .background(Color(0xFFE5EEFF))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.3f)),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(vacc.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (dueStr.isNotEmpty()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF4FF)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = dueStr,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(adminStr, fontSize = 11.sp, color = Color.Gray)

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color(0xFFEFF4FF))
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(docName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            if (hospName.isNotEmpty()) {
                                                Text(hospName, fontSize = 10.sp, color = Color.Gray)
                                            }
                                        }
                                    }

                                    if (hasCert) {
                                        IconButton(
                                            onClick = {
                                                Toast.makeText(
                                                    context,
                                                    "Downloading vaccination certificate for ${vacc.title}... 📄",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = "Certificate",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Health Records & Documents",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text("Tap to view scanned documents or lab results.", fontSize = 11.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))

            val docs = listOf(
                Pair("Blood Report", "Oct 2023"),
                Pair("X-Ray Chest", "Jun 2023"),
                Pair("Vaccination Cert", "Mar 2023"),
                Pair("Prescription", "Oct 2023")
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in docs.indices step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (j in i..i+1) {
                            if (j < docs.size) {
                                val doc = docs[j]
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            Toast.makeText(
                                                context,
                                                "Opening document: ${doc.first} (${doc.second})... 💾",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFEFF4FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = doc.first,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = doc.second,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    if (showAddVaccDialog) {
        var name by remember { mutableStateOf("") }
        var dateAdministered by remember { mutableStateOf("") }
        var nextDue by remember { mutableStateOf("") }
        var docName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddVaccDialog = false },
            title = { Text("Add Vaccination Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Vaccine Name (e.g. Rabies)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dateAdministered,
                        onValueChange = { dateAdministered = it },
                        label = { Text("Date Administered (e.g. Oct 15, 2023)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nextDue,
                        onValueChange = { nextDue = it },
                        label = { Text("Next Due Date (e.g. Oct 15, 2024)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docName,
                        onValueChange = { docName = it },
                        label = { Text("Doctor Name (e.g. Dr. Sarah Jenkins)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotEmpty()) {
                            val notesText = "Administered: $dateAdministered | Due: $nextDue | $docName | City Vet Clinic | cert"
                            viewModel.createReminder(
                                title = name,
                                petName = "Buddy",
                                dateString = "2026-10-24",
                                notes = notesText,
                                type = "vaccination"
                            )
                            showAddVaccDialog = false
                            Toast.makeText(context, "Vaccination record added successfully!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVaccDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// SCREEN: FAVOURITES
// ==========================================
@Composable
fun FavouritesScreen(viewModel: PawsViewModel) {
    val wishlists by viewModel.wishlists.collectAsState()
    val wishlistProducts by viewModel.wishlistProducts.collectAsState()
    val shopsList by viewModel.shops.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val favShops = remember(wishlists, shopsList) {
        shopsList.filter { shop -> wishlists.any { it.shopId == shop.id } }
    }

    val allProductsFlow by viewModel.products.collectAsState()
    val favProducts = remember(wishlistProducts, allProductsFlow) {
        allProductsFlow.filter { prod -> wishlistProducts.any { it.productId == prod.id } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0B1C30))
            }
            Text(
                text = "Favourites",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1C30),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { /* Notifications */ }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF0B1C30))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Favourites",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0B1C30)
                    )
                    Text(
                        "Your saved shops & products",
                        fontSize = 13.sp,
                        color = Color(0xFF434655)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search saved items or shops...", color = Color(0xFF737686)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF737686)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        containerColor = Color.White,
                        focusedBorderColor = Color(0xFF004AC6),
                        unfocusedBorderColor = Color(0xFFC3C6D7).copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val pills = listOf(
                        "All" to "All Favourites",
                        "Products" to "Products (${favProducts.size})",
                        "Shops" to "Shops (${favShops.size})"
                    )
                    items(pills) { (key, label) ->
                        val isSelected = selectedFilter == key
                        val containerColor = if (isSelected) Color(0xFF004AC6) else Color.White
                        val contentColor = if (isSelected) Color.White else Color(0xFF434655)
                        val borderStroke = if (isSelected) null else BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.5f))
                        
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = containerColor,
                            contentColor = contentColor,
                            border = borderStroke,
                            modifier = Modifier
                                .clickable { selectedFilter = key }
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (selectedFilter == "All" || selectedFilter == "Products") {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Saved Products",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B1C30),
                        modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp)
                    )
                }

                if (favProducts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No saved products yet.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    val chunkedProds = favProducts.chunked(2)
                    items(chunkedProds) { rowProds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (product in rowProds) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SavedProductCard(
                                        product = product,
                                        onRemove = { viewModel.toggleProductWishlist(product.id) },
                                        onAddToCart = {
                                            viewModel.viewModelScope.launch {
                                                val shop = viewModel.getShopById(product.shopId)
                                                if (shop != null) {
                                                    viewModel.addToCart(product, shop)
                                                    Toast.makeText(context, "Added to cart! 🛒", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                            if (rowProds.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (selectedFilter == "All" || selectedFilter == "Shops") {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Saved Shops",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B1C30),
                        modifier = Modifier.padding(horizontal = 16.dp, bottom = 8.dp)
                    )
                }

                if (favShops.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No saved shops yet.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(favShops) { shop ->
                        SavedShopCard(
                            shop = shop,
                            onRemove = { viewModel.toggleWishlist(shop.id) },
                            onClick = { viewModel.navigateTo(Screen.ShopDetail(shop.id)) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN: ORDERS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(viewModel: PawsViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0B1C30))
            }
            Text(
                text = "Orders",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1C30),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { /* Notifications */ }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF0B1C30))
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                "Orders",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0B1C30)
            )
            Text(
                "Track, manage, and reorder your pet's favorites.",
                fontSize = 13.sp,
                color = Color(0xFF434655)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search orders, items, or shops...", color = Color(0xFF737686)) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF737686)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White,
                focusedBorderColor = Color(0xFF004AC6),
                unfocusedBorderColor = Color(0xFFC3C6D7).copy(alpha = 0.5f)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF004AC6),
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Color(0xFF004AC6),
                    height = 3.dp
                )
            }
        ) {
            val tabs = listOf("Active Orders (2)", "Past Orders", "Subscriptions")
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(vertical = 14.dp),
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (selectedTab == index) Color(0xFF004AC6) else Color(0xFF434655)
                    )
                }
            }
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        ActiveOrderCard(
                            shopName = "Paws & Claws Premium",
                            orderId = "#PC-8892",
                            time = "Placed Today, 10:42 AM",
                            status = "Out for Delivery",
                            deliveryInfo = "Arriving in approx. 15 mins",
                            itemsSummary = "1x Royal Canin Maxi Adult (15kg), 2x Dental Chew Sticks...",
                            price = "$84.50",
                            trackable = true,
                            onAction = {
                                Toast.makeText(context, "Tracking order PC-8892... 🛵", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    item {
                        ActiveOrderCard(
                            shopName = "VetCare Pharmacy",
                            orderId = "#VC-4410",
                            time = "Placed Yesterday, 4:15 PM",
                            status = "Processing",
                            deliveryInfo = "Expected delivery by tomorrow, 2 PM",
                            itemsSummary = "1x Flea & Tick Treatment (3 Pack), 1x Ear Cleaning Solution",
                            price = "$42.00",
                            trackable = false,
                            onAction = {
                                Toast.makeText(context, "Showing details for VC-4410...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            1 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        PastOrderCard(
                            shopName = "Urban Fetch Toys",
                            date = "Delivered • Oct 12",
                            itemsSummary = "2x Indestructible Chew Bone, 1x Squeaky Ball",
                            price = "$28.90",
                            onReorder = {
                                Toast.makeText(context, "Reordered Urban Fetch Toys items! 🛒", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    item {
                        PastOrderCard(
                            shopName = "Paws & Claws Premium",
                            date = "Delivered • Sep 28",
                            itemsSummary = "1x Royal Canin Maxi Adult (15kg)",
                            price = "$75.00",
                            onReorder = {
                                Toast.makeText(context, "Reordered Paws & Claws items! 🛒", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
            2 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No active subscriptions yet.", color = Color.Gray)
                }
            }
        }
    }
}

// ==========================================
// SCREEN: REPORTS DASHBOARD (HEALTH HUB)
// ==========================================
@Composable
fun ReportsDashboardScreen(viewModel: PawsViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .verticalScroll(rememberScrollState())
    ) {
        // Premium gradient header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.verticalGradient(colors = listOf(Color(0xFF0D7377), Color(0xFF14A085))))
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.navigateBack() }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Health Hub", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Overall health score
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🐾", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("92", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                            Text("Health Score", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("💉", fontSize = 16.sp)
                                Column {
                                    Text("Vaccinations", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                    Text("4/5 complete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📅", fontSize = 16.sp)
                                Column {
                                    Text("Next Checkup", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                                    Text("Jun 15, 2025", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions
        Text("Quick Actions", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C8C8C), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val quickActions = listOf(
                Triple("💉", "Vaccines", Screen.Vaccinations),
                Triple("📋", "Appointments", Screen.Appointments),
                Triple("💊", "Medication", Screen.TabletsIssued)
            )
            quickActions.forEach { (emoji, label, screen) ->
                Card(
                    modifier = Modifier.weight(1f).clickable { viewModel.navigateTo(screen) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vaccination Progress
        Text("Vaccination Progress", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C8C8C), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                val vaccines = listOf(
                    Triple("DHPPiL (Core)", true, "Mar 2025"),
                    Triple("Rabies", true, "Mar 2025"),
                    Triple("Leptospirosis", true, "Jan 2025"),
                    Triple("Bordetella", true, "Dec 2024"),
                    Triple("Canine Influenza", false, "Due Jun 2025")
                )
                vaccines.forEach { (name, done, date) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (done) Color(0xFFF0FFF4) else Color(0xFFFFF3E0), modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (done) Icons.Default.CheckCircle else Icons.Default.Refresh,
                                    null,
                                    tint = if (done) Color(0xFF4CAF50) else Color(0xFFFC8019),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(date, fontSize = 11.sp, color = Color.Gray)
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = if (done) Color(0xFFF0FFF4) else Color(0xFFFFF3E0)) {
                            Text(if (done) "Done" else "Pending", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (done) Color(0xFF4CAF50) else Color(0xFFFC8019), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    if (vaccines.indexOf(Triple(name, done, date)) < vaccines.size - 1) {
                        HorizontalDivider(color = Color(0xFFF5F5F5))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Monthly Activity Summary
        Text("Monthly Summary", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C8C8C), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                val stats = listOf(
                    Triple("🏃", "18 Walks", "This month"),
                    Triple("🍖", "Good Diet", "Compliance"),
                    Triple("💊", "28/30", "Med doses"),
                    Triple("🛁", "3 Baths", "Grooming")
                )
                stats.forEach { (emoji, value, label) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(label, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weight & Growth Trend
        Text("Max's Health Tip", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C8C8C), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💡", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Time for Canine Influenza Vaccine!", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Max's annual Canine Influenza vaccination is due next month. Schedule a vet visit soon to stay protected.", fontSize = 12.sp, color = Color(0xFF388E3C))
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.navigateTo(Screen.Appointments) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Book Appointment", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}


// ==========================================
// SCREEN: MERCHANT INVENTORY & PRICING MANAGER
// Based on Stitch design: Product & Pricing Manager - Shop Owner
// ==========================================
@Composable
fun MerchantInventoryScreen(viewModel: PawsViewModel) {
    val products by viewModel.getMerchantProductsFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0=In Stock, 1=Low Stock, 2=Out of Stock
    var showEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var editStockText by remember { mutableStateOf("") }
    var editPriceText by remember { mutableStateOf("") }

    // Color tokens matching Stitch design system
    val primary = Color(0xFF004AC6)
    val primaryContainer = Color(0xFF2563EB)
    val onPrimary = Color.White
    val surface = Color(0xFFF8F9FF)
    val surfaceContainer = Color(0xFFE5EEFF)
    val surfaceContainerLowest = Color.White
    val onSurface = Color(0xFF0B1C30)
    val onSurfaceVariant = Color(0xFF434655)
    val outlineVariant = Color(0xFFC3C6D7)
    val tertiaryContainer = Color(0xFF007D55)
    val secondaryContainer = Color(0xFFFEA619)
    val errorColor = Color(0xFFBA1A1A)
    val errorContainer = Color(0xFFFFDAD6)

    val inStockProducts = remember(products) { products.filter { it.stockCount > 5 } }
    val lowStockProducts = remember(products) { products.filter { it.stockCount in 1..5 } }
    val outOfStockProducts = remember(products) { products.filter { it.stockCount == 0 } }

    val displayedProducts = remember(products, searchQuery, selectedTab) {
        val base = when (selectedTab) {
            1 -> lowStockProducts
            2 -> outOfStockProducts
            else -> inStockProducts
        }
        if (searchQuery.isBlank()) base
        else base.filter { it.name.contains(searchQuery, ignoreCase = true) || it.brand.contains(searchQuery, ignoreCase = true) }
    }

    if (showEditDialog && editingProduct != null) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceContainerLowest),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Quick Edit", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = onSurface)
                    Text(editingProduct!!.name, fontSize = 14.sp, color = onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    OutlinedTextField(
                        value = editStockText,
                        onValueChange = { editStockText = it.filter(Char::isDigit) },
                        label = { Text("Stock Count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editPriceText,
                        onValueChange = { editPriceText = it },
                        label = { Text("Selling Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { showEditDialog = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                val stock = editStockText.toIntOrNull() ?: editingProduct!!.stockCount
                                scope.launch { viewModel.updateProductStock(editingProduct!!.id, stock) }
                                showEditDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = primary)
                        ) { Text("Save", color = onPrimary) }
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = surface,
        bottomBar = {
            NavigationBar(containerColor = surfaceContainerLowest, tonalElevation = 4.dp) {
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.MerchantDashboard) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Inventory") },
                    label = { Text("Inventory", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.MerchantOrders) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Orders") },
                    label = { Text("Orders", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.ChatList) },
                    icon = { Icon(Icons.Default.MailOutline, contentDescription = "Messages") },
                    label = { Text("Messages", fontSize = 10.sp) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top App Bar ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceContainerLowest)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.MerchantDashboard) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primary)
                    }
                    Column {
                        Text("Inventory & Pricing", fontWeight = FontWeight.Black, fontSize = 18.sp, color = primary)
                        Text("Manage stock, prices & campaigns", fontSize = 11.sp, color = onSurfaceVariant)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SO", color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                // ── Active Campaign Banner ───────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF004AC6), Color(0xFF2563EB))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                Text("ACTIVE CAMPAIGN", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Summer Pet Fest: 20% Off All Toys", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Ends in 3 days • Applies to ${products.size} products", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = {},
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Edit", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Search Bar ──────────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search products by name or brand...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = outlineVariant,
                        focusedBorderColor = primary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // ── Stock Status Tabs ───────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceContainerLowest, RoundedCornerShape(12.dp))
                        .border(1.dp, outlineVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        "In Stock (${inStockProducts.size})",
                        "Low Stock (${lowStockProducts.size})",
                        "Out of Stock (${outOfStockProducts.size})"
                    )
                    tabs.forEachIndexed { idx, label ->
                        val isSelected = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) primaryContainer else Color.Transparent)
                                .clickable { selectedTab = idx }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Product Cards Grid ──────────────────────────────────
                if (displayedProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = outlineVariant, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No products in this category", color = onSurfaceVariant, fontSize = 14.sp)
                        }
                    }
                } else {
                    displayedProducts.forEach { product ->
                        ProductInventoryCard(
                            product = product,
                            onEditClick = {
                                editingProduct = product
                                editStockText = product.stockCount.toString()
                                editPriceText = product.price.toString()
                                showEditDialog = true
                            },
                            primary = primary,
                            primaryContainer = primaryContainer,
                            surfaceContainerLowest = surfaceContainerLowest,
                            outlineVariant = outlineVariant,
                            tertiaryContainer = tertiaryContainer,
                            secondaryContainer = secondaryContainer,
                            errorColor = errorColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ProductInventoryCard(
    product: ProductEntity,
    onEditClick: () -> Unit,
    primary: Color,
    primaryContainer: Color,
    surfaceContainerLowest: Color,
    outlineVariant: Color,
    tertiaryContainer: Color,
    secondaryContainer: Color,
    errorColor: Color
) {
    val stockStatus = when {
        product.stockCount == 0 -> Triple("Out of Stock", errorColor, errorColor.copy(alpha = 0.1f))
        product.stockCount <= 5 -> Triple("Low: ${product.stockCount} left", secondaryContainer, secondaryContainer.copy(alpha = 0.1f))
        else -> Triple("${product.stockCount} in stock", tertiaryContainer, tertiaryContainer.copy(alpha = 0.1f))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Product image
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE5EEFF)),
                    contentAlignment = Alignment.Center
                ) {
                    if (product.photos.isNotEmpty()) {
                        androidx.compose.foundation.Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(LocalContext.current)
                                    .data(product.photos.first())
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = primary.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                    }
                    // Best seller badge
                    if (product.stockCount > 20) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .background(secondaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Best", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                // Product info
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0B1C30), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(product.brand, fontSize = 11.sp, color = Color(0xFF434655))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("₹${product.price.toInt()}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primary)
                        if (product.mrp > product.price) {
                            Text("₹${product.mrp.toInt()}", fontSize = 13.sp, color = Color(0xFF737686), style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                        }
                    }
                }
            }
            // Footer with stock + edit button
            HorizontalDivider(color = outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8F9FF))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(stockStatus.first.let { if (product.stockCount == 0) errorColor else if (product.stockCount <= 5) secondaryContainer else tertiaryContainer }, CircleShape))
                    Text(stockStatus.first, fontSize = 12.sp, color = stockStatus.second, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDBE1FF)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (product.stockCount == 0) "Restock" else "Quick Edit", fontSize = 12.sp, color = primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// ==========================================
// SCREEN: SUPER ADMIN USER MANAGEMENT
// Based on Stitch design: User Management - Super Admin
// ==========================================
@Composable
fun SuperAdminUsersScreen(viewModel: PawsViewModel) {
    val shops by viewModel.shops.collectAsState()
    val pendingShops by viewModel.pendingShops.collectAsState()
    val pendingCaptains by viewModel.pendingCaptains.collectAsState()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(0) } // 0=All, 1=Consumers, 2=Merchants, 3=Captains
    var selectedUser by remember { mutableStateOf<ShopEntity?>(null) }
    var showSuspendDialog by remember { mutableStateOf(false) }

    // Color tokens
    val primary = Color(0xFF004AC6)
    val primaryContainer = Color(0xFF2563EB)
    val onPrimary = Color.White
    val surface = Color(0xFFF8F9FF)
    val surfaceContainerLowest = Color.White
    val surfaceContainer = Color(0xFFE5EEFF)
    val surfaceContainerLow = Color(0xFFEFF4FF)
    val onSurface = Color(0xFF0B1C30)
    val onSurfaceVariant = Color(0xFF434655)
    val outlineVariant = Color(0xFFC3C6D7)
    val tertiaryContainer = Color(0xFF007D55)
    val secondaryContainer = Color(0xFFFEA619)
    val errorColor = Color(0xFFBA1A1A)

    // Build a combined user-like list from shops + captains for display
    val merchantUsers = remember(shops) { shops }
    val captainUsers = remember(pendingCaptains) { pendingCaptains }

    val displayedShops = remember(merchantUsers, searchQuery) {
        if (searchQuery.isBlank()) merchantUsers
        else merchantUsers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.address.contains(searchQuery, ignoreCase = true) }
    }

    if (showSuspendDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showSuspendDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = errorColor) },
            title = { Text("Suspend Account?", fontWeight = FontWeight.Bold) },
            text = { Text("This will temporarily disable ${selectedUser!!.name}'s access to the PawsApp platform. They will not be able to accept orders or serve customers.") },
            confirmButton = {
                Button(
                    onClick = { showSuspendDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor)
                ) { Text("Suspend", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSuspendDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        containerColor = surface,
        bottomBar = {
            NavigationBar(containerColor = surfaceContainerLowest, tonalElevation = 4.dp) {
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.SuperAdmin) },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Governance") },
                    label = { Text("Governance", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.SuperAdmin) },
                    icon = { Icon(Icons.Default.Info, contentDescription = "Analytics") },
                    label = { Text("Analytics", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Users") },
                    label = { Text("Users", fontSize = 10.sp) }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { viewModel.navigateTo(Screen.SuperAdmin) },
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Catalog") },
                    label = { Text("Catalog", fontSize = 10.sp) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Top App Bar ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surfaceContainerLowest)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.SuperAdmin) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = primary)
                    }
                    Column {
                        Text("PawsApp Central Governance", fontWeight = FontWeight.Black, fontSize = 16.sp, color = primary)
                        Text("User Management", fontSize = 11.sp, color = onSurfaceVariant)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SA", color = onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Page Header ─────────────────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("User Management", fontWeight = FontWeight.Black, fontSize = 22.sp, color = onSurface)
                    Text("Manage Consumers, Merchants, and Captains across the platform.", fontSize = 13.sp, color = onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Search + Bulk Actions bar ────────────────────────────
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, email, ID...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = onSurfaceVariant) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = outlineVariant,
                            focusedBorderColor = primary
                        )
                    )
                }

                // ── Role Filter Tabs ─────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceContainerLowest, RoundedCornerShape(12.dp))
                            .border(1.dp, outlineVariant, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("All Users", "Consumers", "Merchants", "Captains").forEachIndexed { idx, label ->
                            val isSelected = selectedRole == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) surfaceContainerLowest else Color.Transparent)
                                    .border(if (isSelected) 1.dp else 0.dp, if (isSelected) outlineVariant else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedRole = idx }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) primary else onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // ── Merchant Queue section header ─────────────────────────
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Merchant Approvals", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onSurface)
                        if (pendingShops.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(errorColor, CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("${pendingShops.size} pending", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Pending shops approval cards
                if (pendingShops.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = surfaceContainerLowest),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No pending shop approvals ✅", color = onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(pendingShops) { shop ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceContainerLowest),
                            border = BorderStroke(1.dp, outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(surfaceContainer, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Home, contentDescription = null, tint = primary, modifier = Modifier.size(24.dp))
                                    }
                                    Column {
                                        Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = onSurface)
                                        Text("${shop.locality} • ${shop.address}", fontSize = 11.sp, color = onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {},
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) { Text("Review", fontSize = 12.sp) }
                                    Button(
                                        onClick = { scope.launch { viewModel.approveShop(shop.id) } },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) { Text("Approve", fontSize = 12.sp, color = onPrimary) }
                                }
                            }
                        }
                    }
                }

                // ── Active Merchant Accounts ──────────────────────────────
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Active Merchants", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onSurface)
                        Text("${displayedShops.size} total", fontSize = 12.sp, color = onSurfaceVariant)
                    }
                }

                // User list table
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = surfaceContainerLowest),
                        border = BorderStroke(1.dp, outlineVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column {
                            // Table header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surfaceContainer)
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Merchant Info", fontSize = 11.sp, color = onSurfaceVariant, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                                Text("City", fontSize = 11.sp, color = onSurfaceVariant, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f))
                                Text("Status", fontSize = 11.sp, color = onSurfaceVariant, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.8f))
                            }
                            HorizontalDivider(color = outlineVariant.copy(alpha = 0.5f))

                            if (displayedShops.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text("No merchants found", color = onSurfaceVariant, fontSize = 13.sp)
                                }
                            } else {
                                displayedShops.forEach { shop ->
                                    val isSelected = selectedUser?.id == shop.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) primary.copy(alpha = 0.06f) else Color.Transparent)
                                            .clickable { selectedUser = if (isSelected) null else shop }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1.5f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(secondaryContainer.copy(alpha = 0.2f), CircleShape)
                                                    .border(1.dp, outlineVariant, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    shop.name.take(2).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF855300)
                                                )
                                            }
                                            Column {
                                                Text(shop.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(shop.address, fontSize = 11.sp, color = onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        Text(shop.locality, fontSize = 11.sp, color = onSurfaceVariant, modifier = Modifier.weight(0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Box(
                                            modifier = Modifier.weight(0.8f),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            val statusColor = if (shop.isActive) tertiaryContainer else errorColor
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier
                                                    .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                    .border(1.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Box(modifier = Modifier.size(6.dp).background(statusColor, CircleShape))
                                                Text(if (shop.isActive) "Active" else "Inactive", fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (shop.id != displayedShops.last().id) {
                                        HorizontalDivider(color = outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 14.dp))
                                    }
                                }
                            }

                            // Pagination footer
                            HorizontalDivider(color = outlineVariant.copy(alpha = 0.5f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Showing ${displayedShops.size} of ${displayedShops.size} merchants", fontSize = 11.sp, color = onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = {}, enabled = false, contentPadding = PaddingValues(all = 6.dp), modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Prev", modifier = Modifier.size(16.dp))
                                    }
                                    OutlinedButton(onClick = {}, contentPadding = PaddingValues(all = 6.dp), modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Next", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Selected User Detail Panel ────────────────────────────
                if (selectedUser != null) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Account Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceContainerLowest),
                            border = BorderStroke(1.dp, outlineVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column {
                                // Banner
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .background(
                                            Brush.horizontalGradient(listOf(Color(0xFF2563EB).copy(alpha = 0.3f), Color(0xFFE5EEFF)))
                                        )
                                ) {
                                    IconButton(onClick = {}, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(30.dp)) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .offset(y = (-28).dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(secondaryContainer.copy(alpha = 0.2f), CircleShape)
                                            .border(3.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(selectedUser!!.name.take(2).uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF855300))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(selectedUser!!.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = onSurface)
                                    Text(selectedUser!!.address, fontSize = 12.sp, color = onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 2)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .background(surfaceContainer, RoundedCornerShape(20.dp))
                                                .border(1.dp, outlineVariant, RoundedCornerShape(20.dp))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) { Text("Merchant", fontSize = 11.sp, color = onSurfaceVariant, fontWeight = FontWeight.Bold) }
                                        Box(
                                            modifier = Modifier
                                                .background(tertiaryContainer.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                                .border(1.dp, tertiaryContainer.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) { Text(if (selectedUser!!.isActive) "Active" else "Inactive", fontSize = 11.sp, color = tertiaryContainer, fontWeight = FontWeight.Bold) }
                                    }

                                    // Mini stats grid
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = surfaceContainerLow),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Home, contentDescription = null, tint = primary, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(if (selectedUser!!.isOpen) "Open" else "Closed", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = onSurface)
                                                Text("Shop Status", fontSize = 10.sp, color = onSurfaceVariant)
                                            }
                                        }
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(containerColor = surfaceContainerLow),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = secondaryContainer, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("${selectedUser!!.rating}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = onSurface)
                                                Text("Rating", fontSize = 10.sp, color = onSurfaceVariant)
                                            }
                                        }
                                    }

                                    // Account details
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 4.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Phone", color = onSurfaceVariant, fontSize = 13.sp)
                                            Text(selectedUser!!.phone.ifBlank { "—" }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = onSurface)
                                        }
                                        HorizontalDivider(color = outlineVariant.copy(alpha = 0.3f))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Locality", color = onSurfaceVariant, fontSize = 13.sp)
                                            Text(selectedUser!!.locality, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = onSurface)
                                        }
                                        HorizontalDivider(color = outlineVariant.copy(alpha = 0.3f))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Verified", color = onSurfaceVariant, fontSize = 13.sp)
                                            Text(if (selectedUser!!.isVerified) "✅ Yes" else "❌ No", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = onSurface)
                                        }
                                    }

                                    // Suspend toggle
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 4.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(errorColor.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                            .border(1.dp, errorColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Suspend Account", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = errorColor)
                                            Text("Temporarily disable platform access.", fontSize = 11.sp, color = onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = !selectedUser!!.isActive,
                                            onCheckedChange = { showSuspendDialog = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = errorColor,
                                                uncheckedThumbColor = Color.White,
                                                uncheckedTrackColor = Color(0xFFC3C6D7)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }

                // Captain queue section
                if (pendingCaptains.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Captain Approvals", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onSurface)
                            Box(modifier = Modifier.background(secondaryContainer, CircleShape).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text("${pendingCaptains.size} pending", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    items(pendingCaptains) { captain ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = surfaceContainerLowest),
                            border = BorderStroke(1.dp, outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier.size(44.dp).background(primary.copy(alpha = 0.1f), CircleShape).border(1.dp, outlineVariant, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = primary, modifier = Modifier.size(22.dp))
                                    }
                                    Column {
                                        Text(captain.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = onSurface)
                                        Text("🛵 ${captain.vehicleNumber}", fontSize = 11.sp, color = onSurfaceVariant)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {}, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), modifier = Modifier.height(34.dp)) {
                                        Text("Review", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { scope.launch { viewModel.approveCaptain(captain.id) } },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = primary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) { Text("Approve", fontSize = 12.sp, color = Color.White) }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

fun getMockProductsForCategory(categoryId: String): List<ProductEntity> {
    return when (categoryId) {
        "cat_food" -> listOf(
            ProductEntity(
                id = "p_rc_mini_adult",
                shopId = "shop_hyd_1",
                categoryId = "cat_food",
                name = "Mini Adult Dry Dog Food",
                description = "Complete feed for adult small breed dogs (weight between 1 and 10 kg) - Over 10 months old.",
                price = 299.0,
                mrp = 365.0,
                photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=300"),
                inStock = true,
                isActive = true,
                tags = listOf("Dry Food", "Dog", "18% OFF"),
                brand = "Royal Canin",
                lifeStage = "Adult",
                stockCount = 15
            ),
            ProductEntity(
                id = "p_pedigree_chicken_veg",
                shopId = "shop_hyd_1",
                categoryId = "cat_food",
                name = "Adult Dry Dog Food, Chicken & Veg",
                description = "Pedigree complete and balanced dog food for adult dogs, with the goodness of chicken and vegetables.",
                price = 650.0,
                mrp = 650.0,
                photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=300"),
                inStock = true,
                isActive = true,
                tags = listOf("Dry Food", "Dog", "BEST SELLER"),
                brand = "Pedigree",
                lifeStage = "Adult",
                stockCount = 20
            ),
            ProductEntity(
                id = "p_drools_chicken_egg",
                shopId = "shop_hyd_1",
                categoryId = "cat_food",
                name = "Chicken and Egg Puppy Dry Food",
                description = "Drools Chicken and Egg Puppy Dry Dog Food is loaded with essential nutrients to support growth.",
                price = 320.0,
                mrp = 320.0,
                photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=300"),
                inStock = true,
                isActive = true,
                tags = listOf("Dry Food", "Dog"),
                brand = "Drools",
                lifeStage = "Puppy",
                stockCount = 12
            ),
            ProductEntity(
                id = "p_orijen_grain_free",
                shopId = "shop_hyd_1",
                categoryId = "cat_food",
                name = "Original Grain-Free Dry Dog Food",
                description = "Orijen Original features grain-free kibble prepared with fresh whole chicken, turkey, and wild-caught fish.",
                price = 1899.0,
                mrp = 2110.0,
                photos = listOf("https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=300"),
                inStock = true,
                isActive = true,
                tags = listOf("Dry Food", "Dog", "10% OFF"),
                brand = "Orijen",
                lifeStage = "Adult",
                stockCount = 8
            ),
            ProductEntity(
                id = "p_whiskas_dry",
                shopId = "shop_hyd_1",
                categoryId = "cat_food",
                name = "Whiskas Premium Cat Kibble (Mackerel)",
                description = "Crunchy pockets filled with mackerel and premium sea protein, customized for adult cats.",
                price = 399.0,
                mrp = 490.0,
                photos = listOf("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=300"),
                inStock = true,
                isActive = true,
                tags = listOf("Dry Food", "Cat"),
                brand = "Royal Canin",
                lifeStage = "Adult",
                stockCount = 30
            ),
            ProductEntity(
                id = "p_rc_wet_cat",
                shopId = "shop_hyd_1",
                categoryId = "cat_food",
                name = "Royal Canin Kitten Wet Gravy Food",
                description = "Easy chewing chunks in delicious gravy sauce formulated for kittens.",
                price = 90.0,
                mrp = 100.0,
                photos = listOf("https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=300"),
                inStock = true,
                isActive = true,
                tags = listOf("Wet Food", "Cat"),
                brand = "Royal Canin",
                lifeStage = "Puppy",
                stockCount = 25
            ),
            ProductEntity(
                id = "p_oxbow_rabbit",
                shopId = "shop_hyd_1",
                categoryId = "cat_food",
                name = "Timothy Gold Premium Rabbit Feed",
                description = "Premium high-fiber Timothy hay pellets specifically designed for active adult rabbits.",
                price = 450.0,
                mrp = 550.0,
                photos = listOf("https://images.unsplash.com/photo-1585110396000-c9ffd4e4b308?w=300"),
                inStock = true,
                isActive = true,
                tags = listOf("Dry Food", "Small Pet"),
                brand = "Orijen",
                lifeStage = "Adult",
                stockCount = 18
            ),
            ProductEntity(
                id = "p_tetra_fish",
                shopId = "shop_hyd_1",
                categoryId = "cat_food",
                name = "TetraBits Complete Fish Food",
                description = "Nutritially complete slow-sinking granules designed for optimal digestion and health in aquarium fish.",
                price = 250.0,
                mrp = 300.0,
                photos = listOf("https://images.unsplash.com/photo-1522850959076-58d7c244737a?w=300"),
                inStock = true,
                isActive = true,
                tags = listOf("Dry Food", "Fish"),
                brand = "Drools",
                lifeStage = "All Ages",
                stockCount = 40
            )
        )
        "cat_treats" -> listOf(
            ProductEntity(
                id = "p_orijen_regional_red_biscuits",
                shopId = "shop_hyd_1",
                categoryId = "cat_treats",
                name = "Orijen High-Protein Regional Red Biscuits",
                description = "High-protein regional red baked dog biscuits made with beef, wild boar, and lamb.",
                price = 450.0,
                mrp = 550.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuBH7YeUXJ96VO2zzK5bp1QXpwRu7YNTyiRCVVT0-e83V1rLkfGijcJE3web3Ogc1WU6iBJZtXAkuAL8k4Bm9uGcleOi9M-fH1VMzQSxIJsqSZTrDxe9v97xyUhq_MgJnTbbElcO3LQ4DfUzcQiTyCCrEaJk0r6B9IadZLsmgBmpbgKrNTil1Zkbp3yEvYyj-DI6xS8aZBhoLIldS6WKkLXZGvRVnIiUnisirs3llTO8796kRpVGdOX0iORj5iTiV18g0n0m5RHqWMs"),
                inStock = true,
                isActive = true,
                tags = listOf("Biscuits", "Dog", "18% OFF", "Bestseller"),
                brand = "Orijen",
                lifeStage = "Adult",
                stockCount = 20
            ),
            ProductEntity(
                id = "p_pedigree_dentastix",
                shopId = "shop_hyd_1",
                categoryId = "cat_treats",
                name = "Pedigree Dentastix Daily Oral Care Chews",
                description = "Daily oral care chews clinically proven to reduce tartar build-up in dogs.",
                price = 180.0,
                mrp = 200.0,
                photos = listOf("https://lh3.googleusercontent.com/aida/AP1WRLu2j9XUv_Re8vRJWJIq3otI7IoX2NgLk7u6dz-Q8YcKZM56ZhlEEG2hhVjR_8a8TLk1hk-4Tl9pMOlZyJGEzTBYJn_bkhNE9uLeijZ6EWFbm_jLi4gI6TBl8Gw26ZsyNvJfA59F0JWB29hKlzMIleF_-IFt7EarCxyfY7nXTlogfRnnfkUY63uBoXugR7m67gBg3tiS5d5irmOdJvPRSbNWfAdde8rkOop9HKp5a_RfrM4tCAPcR1ICVDY"),
                inStock = true,
                isActive = true,
                tags = listOf("Dental", "Dog", "10% OFF", "Sale"),
                brand = "Pedigree",
                lifeStage = "Adult",
                stockCount = 35
            ),
            ProductEntity(
                id = "p_hills_mobility_treats",
                shopId = "shop_hyd_1",
                categoryId = "cat_treats",
                name = "Healthy Mobility Baked Dog Treats",
                description = "Healthy mobility dog treats formulated for active dogs supporting joint care.",
                price = 350.0,
                mrp = 400.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuBSBZQjs5eKzrCGdFsmvuYlxWl5wP38L9RoLUNkdYcLxeYcPQhw9xhzDCgy8eNSxczcabhKSFAxAwghevpBto4jhGekPFtFllV4AjoH8cRZ1qzH12MOi9keSmXEpKzBSlLuz-plmZZAAj5zem-3KwA2WqyvmB33AIXo_Ap7H8LrjKVYN0Y1sfXEBBZlDQ0ewf4kRJMQ6EZtA9uH2JSCUcoKtG4sb3hKuaxx5W9n1Vljae2g4qoyNYEGMCL7FXlHqffddxqNv1OW2QA"),
                inStock = true,
                isActive = true,
                tags = listOf("Natural", "Dog", "12% OFF"),
                brand = "Hill's Science Diet",
                lifeStage = "All Ages",
                stockCount = 18
            ),
            ProductEntity(
                id = "p_barking_heads_treats",
                shopId = "shop_hyd_1",
                categoryId = "cat_treats",
                name = "Meaty Treats Chicken & Herb",
                description = "Tasty grain-free dog treats made with 100% natural ingredients.",
                price = 299.0,
                mrp = 299.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuD6heK9zKhK9CQnrHlcVs0nsFrVcG4dVag5Q3qWWSQjpzX3FxCe2Ku5sDJKbVa0oAmNOIoQqDrtm59DRTjM_tSof16oJkLH09oeS3EZFm3jUl8no1S3gPGM281DYtdwtCfXDhoH29dOf4FQXco69KowvXGlq-rByr1aq9iyV5xEPjtdlrOY0w0Qbb5_Mj1ONvaA7FQ-AUDznmnUJRpr5zna1C6AGoCiQfxrMU05fLa5kgTefywfLpxkezwylHDK6inWnigkEG0dfYc"),
                inStock = true,
                isActive = true,
                tags = listOf("Natural", "Dog"),
                brand = "Barking Heads",
                lifeStage = "Adult",
                stockCount = 25
            )
        )
        "cat_toys" -> listOf(
            ProductEntity(
                id = "p_kong_classic",
                shopId = "shop_hyd_1",
                categoryId = "cat_toys",
                name = "Classic Rubber Toy",
                description = "Ultra-durable natural rubber dog toy for mental stimulation and chewing.",
                price = 899.0,
                mrp = 1100.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuAR1ny6pOdEO4AE9-38Cdg9tkkxG-q-nWRhlK8Zl8s8Vy266yKxwmpxGQc_z55xpYW-wnCeJbQhI2rSYWOq38XfQh2AoAHJ13neVHdUg-Ta8PdQZ2XgDVdpzSsx3XtguKLcIlxaxW3VGYCyzD3jL2grKMWrzrAjIndKP2wpWBuViPApnzk96o5EveidjmZysnCVN-1yNpqvw18LalZLXT3LMu8M2xkgZP0uI-Xz8ydKsCRXFnbx7DibVEQTO32OpEFfnWgznNQAe80"),
                inStock = true,
                isActive = true,
                tags = listOf("Chew", "Dog", "10% OFF"),
                brand = "Kong",
                lifeStage = "All Ages",
                stockCount = 30
            ),
            ProductEntity(
                id = "p_outward_hound_brick",
                shopId = "shop_hyd_1",
                categoryId = "cat_toys",
                name = "Nina Ottosson Dog Brick Puzzle",
                description = "Level 2 interactive dog puzzle toy designed for mental enrichment and play.",
                price = 2450.0,
                mrp = 2450.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuCrI6IU4jfFLEkB-dE1GVpX9pd6jA6Wa7f9R6OIPtc1qu2jmvi9HGA5LzjBIFoHL4clmQoNeQwtlY1jhObRMzj-J2dl4A52mC8V_k1YNpV74NWCd8fVrDhjrb5N9klLry1m4NpsDqi510mB1T0kAt_XmTFuCTOil7oJ-cOdYWKOU4J13n6M4I7t9ihiZNAb02D7c1tNCU2vsb4QabPWe1U910LpWPlBTqpMcwEjQ-QUsdE9qlQUOl2tyV_OuqCNtPMRTe4Y1Z8ytPo"),
                inStock = true,
                isActive = true,
                tags = listOf("Interactive", "Dog"),
                brand = "Outward Hound",
                lifeStage = "All Ages",
                stockCount = 10
            ),
            ProductEntity(
                id = "p_chuckit_ultra_ball",
                shopId = "shop_hyd_1",
                categoryId = "cat_toys",
                name = "Ultra Ball Dog Toy",
                description = "Chuckit! high-bounce durable rubber ball for fetch and active outdoor play.",
                price = 650.0,
                mrp = 650.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuA-uJ6kCnWNSEz9inTIeo-DaO7_QJZroCBWtIv_4GHZ5MTwoVLIwVb2opWaSfRjqBV4yDpPXb3AZb5ZltALZNQNPe4drmxoj-NE1nktRdf6LR-QrEPkRjqQpn4IpzldkE-CVj0ZYgtstN0hCelB0Pm2yMKO4HbFWuRnrBLfMRbltqmW8cLP13r61EFsfTqJgZSvQ0S05m0_JyiaQtHu-hRoHpyUtCioASRjY1xjE6ojR6QqvOaZMIo7aIOQHL8vqrYvBn_lzqrcw_k"),
                inStock = true,
                isActive = true,
                tags = listOf("Fetch", "Dog"),
                brand = "Chuckit!",
                lifeStage = "All Ages",
                stockCount = 24
            ),
            ProductEntity(
                id = "p_heavy_duty_rope",
                shopId = "shop_hyd_1",
                categoryId = "cat_toys",
                name = "Heavy Duty Tug Rope",
                description = "Premium thick braided multi-color cotton rope with double knots for tug-of-war.",
                price = 499.0,
                mrp = 499.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuA1PsRjnPY40VYykkQcgfyYWkek5kEYR_kn22iVdoNTrOHJYk-jSD0YxPW9zzxO3LpQTla9MdVgFI-MpWoTwMnB_w3Yx2Dh9U7zQd4I__cxx-B2z3vkmTfzzPwWIRaG_SJ6cgE6sQXaW-iHyIuDQ3BgfKJJXnXDZ-NLJgkWmOjaMtiiSSpLmMtdH_BV-Po1EAngH31x4CtiXI2H1DWdgVC6oUi5nP6fZyXvWIDSIZEh_C4pwCMvb4-S9_tAr35rrNhBvXxy0-HEUNI"),
                inStock = true,
                isActive = true,
                tags = listOf("Tug", "Dog", "Best Seller"),
                brand = "Pawsome",
                lifeStage = "Adult",
                stockCount = 15
            )
        )
        "cat_travel" -> listOf(
            ProductEntity(
                id = "p_denim_harness",
                shopId = "shop_hyd_1",
                categoryId = "cat_travel",
                name = "Denim Blue Comfort Harness & Leash Set",
                description = "Luxury styled woven denim blue harness with sturdy brass hardware and soft padded structure.",
                price = 48.0,
                mrp = 48.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuBqVfcoxARGwW9gaA7nx42d8FbDxWiCujm8_KKCckeRwBJyxAjoOZqQ3lJDuigUvz1EK56DrPoPLjbNoYvAUHEuLqv0aBLRKGD5bx6lX7Zwiwjd9OEv5XBHTgGBvS8Dq7O24CKxcUcTUEtbPhKyaDvXhZH26IXUsARdoOvP4JhO0hIz_2iR1GUHyJfueCMIa6ZYumUupFfnaMVzAfChSzE55_Qtxvk49WTaXUCQU0FpJv8gFkRfK850NMMnqbOufieHfe6TDzfpS0Q"),
                inStock = true,
                isActive = true,
                tags = listOf("Harness", "Dog", "Best Seller"),
                brand = "Luxury Pet Boutique",
                lifeStage = "All Ages",
                stockCount = 10
            ),
            ProductEntity(
                id = "p_tactical_harness",
                shopId = "shop_hyd_1",
                categoryId = "cat_travel",
                name = "K9 Tactical Adventure Harness - Olive",
                description = "Heavy-duty tactical vest with multiple accessory pockets and adjustment points for active dogs.",
                price = 65.0,
                mrp = 65.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuD-QHWp4QCk1VNmJdIzmfZ-85TGg8bbGE1NCCh4LsUdk7NkLsUfHuzv3XG0onZyoxqBSHd8MasEiQSG8moM6-Ose0FSf_m2Na6TgqJpDPuZJO18k4l_LWSfQ_33JR8Yi3UtxDZr0khnao8cXp8qB2ct-O161M5lFDRqU2oQPzN5yKT3YptErWZW0JnjsQQnnTzVds63EzQOBpEgEIpwquXR-UiLlqNd_l_NGDAi3h5_CEe0gf0s-vTyhS26_GkYbRLx4J6HCt_3PiE"),
                inStock = true,
                isActive = true,
                tags = listOf("Harness", "Dog"),
                brand = "Barking Heads Store",
                lifeStage = "Adult",
                stockCount = 12
            ),
            ProductEntity(
                id = "p_leather_lead",
                shopId = "shop_hyd_1",
                categoryId = "cat_travel",
                name = "Classic Rolled Leather City Lead",
                description = "Genuine rolled leather city lead with premium mahogany brown tint and solid brass hook.",
                price = 32.0,
                mrp = 32.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuBstmBxcbiDYtks9mq4NixOW-4D3L9VcAfquJWjfv1N0bCdho7p0KKgykp_qBcyTD0ENSGCyfIDfHQQH-gLSY8Kv-0v2MaS87Cs83aU9F4el_pyARk80ra1n2N_KxNkfpXizkatturIIYh8XSS28oOFnV4ePt4jX_kYYukkKH01CBX7aMlHG5MF_ddHTXG6wKRAVDyDvOfIh8ABrjnm0W_4SeeJcoKal0ugJu3MGoH5fSpdWNVoTPnc0aC4"),
                inStock = true,
                isActive = true,
                tags = listOf("Leash", "Dog"),
                brand = "LeatherCraft Pets",
                lifeStage = "All Ages",
                stockCount = 20
            ),
            ProductEntity(
                id = "p_airline_carrier",
                shopId = "shop_hyd_1",
                categoryId = "cat_travel",
                name = "Airline Approved Comfort Carrier",
                description = "Premium lightweight breathable mesh travel carrier with soft-sided frame.",
                price = 89.0,
                mrp = 110.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuC0ipHQmI817cbTajzccC0_zvXkOdt6EOuMCl0fb7FFbUUGuMiQw86twslM8WkqKkflWYagr5MNIOdxfCl4YbF_w03ZqK9wkbBh2W3v4lfLCt6l58UJQQoE_qcVNCn41DozEiDKPL43F6j4cybIp-dcVmVLjygtybnMIl1VUPRaWkEn2K5FquFRv7SRqwk7O0o48a7njmhUMbjH60v_gWsuDUjQ_UVzikekYi8R4qENil-hX7ZV4IwzYAKbCB4LOCYYtIpp-eRXdpA"),
                inStock = true,
                isActive = true,
                tags = listOf("Carrier", "Cat", "Sale"),
                brand = "JetSet Pets",
                lifeStage = "All Ages",
                stockCount = 8
            )
        )
        "cat_furniture" -> listOf(
            ProductEntity(
                id = "p_ortho_lounger",
                shopId = "shop_hyd_1",
                categoryId = "cat_furniture",
                name = "Cloud Nine Orthopedic Lounger",
                description = "Luxury orthopedic memory foam pet bed with removable cream sherpa top and grey base.",
                price = 129.99,
                mrp = 129.99,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuAyB22rbZoXa_TMwG1PEQTebwvY15P0IxP7v6TdhTCytfwZMgahnIef-ltbzHyLc_859V8B5Psd23oz7A9s8bSBmvfCPqBasyHBffdOkRPAxP9NvPH_pu4BzKT13fsIdR33FIlcOoXrfZ7cdUEMPQdN6rYxYtvXd9xBISfzj2aZ5-mVQ_WpQh7eb4G-GZwg-4swSJJf-HKfoZk7aXifTG1h6p-b0c3r3LMv3WzClpsDJGOEeVhCIqHh_FTY8RhpbCtjZMXZ41lMicM"),
                inStock = true,
                isActive = true,
                tags = listOf("Beds", "Dog", "Best Seller"),
                brand = "Pawsome Furniture",
                lifeStage = "All Ages",
                stockCount = 6
            ),
            ProductEntity(
                id = "p_modern_crate",
                shopId = "shop_hyd_1",
                categoryId = "cat_furniture",
                name = "Mid-Century Modern Crate",
                description = "Sophisticated indoor dog crate designed as furniture, with walnut wood top and black metal bars.",
                price = 245.0,
                mrp = 245.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuCXTaMdebKGAKtAnT0cEV4gXbtJVxJp-x0WhP9EZpn46KATHlUE_gQjtwHH121xm7uc_f1tso3uPRX7woaDW4HcgQ7wgyrhmR1iXytIx-b_lUu_diRbY1ae0huSkyCCUcWZEaFLEXWJm07kdOBAaANZ3K25fOARhpTLgiRWXP2IBObfzKtZQh0SMC2Y-4dWVbFzgjLB9wSnHBkyRhoCHI7L6GUr98L4eTfbMUcvGPziVUHRrxUsK5bvkOO6fPIyag_K45MK81ZZYe0"),
                inStock = true,
                isActive = true,
                tags = listOf("Crates", "Dog"),
                brand = "The Cozy Den",
                lifeStage = "All Ages",
                stockCount = 4
            ),
            ProductEntity(
                id = "p_cat_tree",
                shopId = "shop_hyd_1",
                categoryId = "cat_furniture",
                name = "Scandi Minimalist Cat Tree",
                description = "Birch wood architecturally clean 3-tier cat tree scratching post with felt pads.",
                price = 189.0,
                mrp = 220.0,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuDANZpzNndPE-rVda5l7w0EsRG9jdriO-kyH0RPe7HkiUuey3JzoINi3hZRsCiHBcRj05fSgtSah8ZTVwGxdhq4soYi2lJBTgbJLljinFCcZnJJGLVZSlo7CxiFhf-G2Ao7fYoYhfSGrurBcKYIDOCdiaw2Xmv2ryKNxUQ7X6YP3Fvdl62f_lbQZnKBsbXzYnfjZVvSTX-EB1UPCPURcdRqMSmqcjvdH9oX27krCGvl4bfZbkblpFvIql0dgeCSBobZ6IrEJ3aAtns"),
                inStock = true,
                isActive = true,
                tags = listOf("Trees", "Cat", "Sale"),
                brand = "Feline Fine Designs",
                lifeStage = "All Ages",
                stockCount = 7
            ),
            ProductEntity(
                id = "p_velvet_donut",
                shopId = "shop_hyd_1",
                categoryId = "cat_furniture",
                name = "Royal Velvet Donut Bed",
                description = "Upholstered round navy blue velvet pet donut lounger for royal sleep comfort.",
                price = 75.50,
                mrp = 75.50,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuCXnG9j1AnSA_q_kS_rzvrieaz-Tz0Bv5rvZ62mbtFF6j2ZfioOavFXmWUxEOtjCEiW0U823DiGseN9AS_14rdV8bBeCPBI2_EP75EOorHW0bZ8IileRfjvX3q3k-7rWTPhxldqgfldGzvHZZsOUk_UanXurbpU8_twp3rhl1wS7_NCb-ag2_UjTfjpKok60uuuqpmv0c04Fet2BvDCry4xFwL3w6pic3i29ZdaodjxuabsWI3oLrtAczVKQPvQJ0zq9esgLAf1_lg"),
                inStock = true,
                isActive = true,
                tags = listOf("Loungers", "Dog"),
                brand = "Pawsome Furniture",
                lifeStage = "All Ages",
                stockCount = 14
            )
        )
        "cat_waste" -> listOf(
            ProductEntity(
                id = "p_earth_rated_bags",
                shopId = "shop_hyd_1",
                categoryId = "cat_waste",
                name = "Earth Rated Lavender Scented Poop Bags",
                description = "Lavender scented degradable thick green dog poop bags with standard roll size.",
                price = 12.99,
                mrp = 12.99,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuAmWBhmnG8zVtMYSmDr9dsFp9M7d1fBUVpE8ZEjOJ4m8XFzJhctZztGQTo8qVeRZGlWTb2YIBkudkgUIAJ7SWTOCvdeMdQxT39gexF1mQQ9gDJDRUJE8LF1w5uFEvySdRCQyS7DHvbfBQDfIWBFPOkcWqL0ZT5HgH5ODLKS9plD86cz3v-9dm0TbdyMVHX7osl0yYnR4Sowdf6KX_VhjOWm21YceyrPq7cnjPvPtWy2bbZAPCUJqIa-7ZMO70KlqDuWcqXjkR8Kzew"),
                inStock = true,
                isActive = true,
                tags = listOf("Bags", "Dog", "Best Seller"),
                brand = "EcoPet Supplies",
                lifeStage = "All Ages",
                stockCount = 50
            ),
            ProductEntity(
                id = "p_tofu_litter",
                shopId = "shop_hyd_1",
                categoryId = "cat_waste",
                name = "Premium Clumping Tofu Litter - 10lbs",
                description = "Fast-clumping natural biodegradable eco-friendly clumping tofu cat litter.",
                price = 24.50,
                mrp = 24.50,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuBTzIR8bpIFcWjR2Lh0gfMechDQI_jhSsTBDnIospTf0-ab09IKr7cGiStYy9DiawVBEq16ADbH_RyF_2Dgbg8Dua9HyaH863mJWQ-AoKvyOK-DgauseXhTdeu4XRZO0ahfnsXsJMaB8RcwwIAG3nZLGoRW-SIr9TMH68eSe4FDMkav0vL2onMVJDclebkMZltVYYUdL78CqQU3fqyMlzChHafWTjiIYQ6kbDaRu6YPBWgTbSgdEoBMYQn1PKXW6s6K_iz0jluliHM"),
                inStock = true,
                isActive = true,
                tags = listOf("Litter", "Cat"),
                brand = "CleanPaws Hub",
                lifeStage = "All Ages",
                stockCount = 30
            ),
            ProductEntity(
                id = "p_moderna_litterbox",
                shopId = "shop_hyd_1",
                categoryId = "cat_waste",
                name = "Moderna Top-Entry Litter Box",
                description = "Spacious top entry litter box to prevent litter tracking and scattering.",
                price = 45.0,
                mrp = 52.99,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuAN4zDQO5H87AhGgUZyUTdyG2akkOm67ZzN0iMOCVTB-O91UY7BTd9BHkzq8G1oNps8NN8Pb9PTAUMAK30gQTZKiomZNjZiBzUqTdyJmtcgV6Fo04c1ZRBA3vi7vpG2jW9_VZQNRoLGAUMRI4z_gMFnQRGFsdUUtD62bO5VRLU3PdDiv1qeW_Lhq2C2hgkqsM4opEtKDytvmCt_YeTbUj97rzIpAJ-nXRThD9YHnIBMey3f1c-w7jGDcLO9XV-xGRtcRtN1K-MjZwQ"),
                inStock = true,
                isActive = true,
                tags = listOf("Litter Box", "Cat", "Sale 15%"),
                brand = "Feline Furnishings",
                lifeStage = "All Ages",
                stockCount = 5
            ),
            ProductEntity(
                id = "p_enzyme_spray",
                shopId = "shop_hyd_1",
                categoryId = "cat_waste",
                name = "Enzymatic Stain & Odour Eliminator Spray",
                description = "High-efficiency natural enzymatic cleaning spray targeting organic pet stains and odour.",
                price = 16.50,
                mrp = 16.50,
                photos = listOf("https://lh3.googleusercontent.com/aida-public/AB6AXuBE08T9Hz6NTH5n-IyVZv-EjObVVjdsoenmoIPbeSIWwdeUXNl2BJh_8fnHnGr6dDXdjotC4_S1_nNwuMPPwmLC05eWAmcPNDPOR8F_UjKrz_cDTuhM_zZ6P4cVNp0_um5Y8pKzjPEvZohN_X44gZPina_w1njKvhr5Vg7S52JzAPgsgEI2lSU_S9xK2ezNPTgOOkk0hEncpk-GRBCcO2a2q5gQj6mtpAH5CereunCfIK2uB55v7VuVX75G9_iexKID3qjcC0415XQ"),
                inStock = true,
                isActive = true,
                tags = listOf("Cleaning", "Dog"),
                brand = "FreshHome Pets",
                lifeStage = "All Ages",
                stockCount = 25
            )
        )
        else -> emptyList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(categoryId: String, viewModel: PawsViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val shops by viewModel.shops.collectAsState()
    
    // Dynamic settings based on categoryId
    val isDollar = categoryId == "cat_travel" || categoryId == "cat_furniture" || categoryId == "cat_waste"
    val currencySym = if (isDollar) "$" else "₹"
    val catalogTitle = when (categoryId) {
        "cat_food" -> "Food & Nutrition"
        "cat_treats" -> "Treats & Chews"
        "cat_toys" -> "Toys & Mental Enrichment"
        "cat_travel" -> "Travel, Leashes & Apparel"
        "cat_furniture" -> "Furniture & Sleep"
        "cat_waste" -> "Waste Management & Litter"
        else -> "Catalog"
    }
    val catalogSubtitle = when (categoryId) {
        "cat_food" -> "Complete nutritional care."
        "cat_treats" -> "Delicious chews."
        "cat_toys" -> "Keep them active."
        "cat_travel" -> "Premium gear for your next adventure."
        "cat_furniture" -> "Create the perfect resting spot."
        "cat_waste" -> "Keep it clean and fresh with premium supplies."
        else -> "Premium products for your pets."
    }
    val searchPlaceholder = when (categoryId) {
        "cat_food" -> "Search foods..."
        "cat_treats" -> "Search treats..."
        "cat_toys" -> "Search toys..."
        "cat_travel" -> "Search travel gear..."
        "cat_furniture" -> "Search furniture, beds, and more..."
        "cat_waste" -> "Search products..."
        else -> "Search catalog..."
    }
    val defaultChip = when (categoryId) {
        "cat_food" -> "Dry Food"
        "cat_treats" -> "All Treats"
        "cat_toys" -> "All Toys"
        "cat_travel" -> "All Gear"
        "cat_furniture" -> "All"
        else -> "All Supplies"
    }
    val defaultMaxPrice = if (isDollar) 500f else 5000f

    // Filters State
    var selectedCategoryChip by remember(categoryId) { mutableStateOf(defaultChip) }
    var showFiltersSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Bottom Sheet filter selections
    var selectedPetType by remember { mutableStateOf("Dog") }
    var selectedLifeStage by remember { mutableStateOf("Puppy") }
    val selectedFoodTypes = remember { mutableStateListOf<String>() }
    val selectedBrands = remember { mutableStateListOf<String>() }
    var priceRange by remember(categoryId) { mutableStateOf(0f..defaultMaxPrice) }
    
    // Available products list
    val mockProducts = remember(categoryId) { getMockProductsForCategory(categoryId) }

    // Filter Logic
    val filteredProducts = remember(
        selectedCategoryChip,
        selectedPetType,
        selectedLifeStage,
        selectedFoodTypes.toList(),
        selectedBrands.toList(),
        priceRange,
        searchQuery,
        categoryId
    ) {
        mockProducts.filter { product ->
            // Match category chip
            val matchesCategoryChip = if (selectedCategoryChip.startsWith("All")) true else {
                product.tags.contains(selectedCategoryChip) || product.name.contains(selectedCategoryChip, ignoreCase = true)
            }

            // Match search query
            val matchesSearch = if (searchQuery.isEmpty()) true else {
                product.name.contains(searchQuery, ignoreCase = true) || product.brand.contains(searchQuery, ignoreCase = true)
            }

            // Match Pet Type from bottom sheet
            val matchesPetType = when (selectedPetType) {
                "Dog" -> product.tags.contains("Dog")
                "Cat" -> product.tags.contains("Cat")
                "Small Pet" -> product.tags.contains("Small Pet")
                "Fish" -> product.tags.contains("Fish")
                else -> true
            }

            // Match Life Stage from bottom sheet
            val matchesLifeStage = when (selectedLifeStage) {
                "Puppy" -> product.lifeStage.equals("Puppy", ignoreCase = true)
                "Adult" -> product.lifeStage.equals("Adult", ignoreCase = true)
                "Senior" -> product.lifeStage.equals("Senior", ignoreCase = true)
                "All Ages" -> true
                else -> true
            }

            // Match Brand from bottom sheet
            val matchesBrand = if (selectedBrands.isEmpty()) true else {
                selectedBrands.contains(product.brand)
            }

            // Match Price Range from bottom sheet
            val matchesPrice = product.price >= priceRange.start && product.price <= priceRange.endInclusive

            matchesCategoryChip && matchesSearch && matchesPetType && matchesLifeStage && matchesBrand && matchesPrice
        }
    }

    // Custom Bottom Sheet Layout using a Box Overlay
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FF))
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF434655)
                    )
                }

                Text(
                    text = catalogTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF004AC6),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Search) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF434655)
                        )
                    }
                    IconButton(onClick = { viewModel.navigateTo(Screen.Cart) }) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Color(0xFF434655)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFC3C6D7).copy(alpha = 0.5f))

            // Main Scrollable Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Section Title and Subtitle
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            text = catalogTitle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B1C30)
                        )
                        Text(
                            text = catalogSubtitle,
                            fontSize = 12.sp,
                            color = Color(0xFF434655),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Search Bar Input
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(searchPlaceholder, fontSize = 13.sp, color = Color(0xFF737686)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF737686)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFEFF4FF),
                                unfocusedContainerColor = Color(0xFFEFF4FF),
                                focusedBorderColor = Color(0xFF004AC6),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }

                // Horizontal Image/Icon Chips
                item {
                    val chips = when (categoryId) {
                        "cat_food" -> listOf(
                            "Dry Food" to "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=100",
                            "Wet Food" to "https://images.unsplash.com/photo-1537151608828-ea2b117b6281?w=100",
                            "Raw" to "https://images.unsplash.com/photo-1576201836106-db1758fd1c97?w=100",
                            "Freeze-Dried" to "https://images.unsplash.com/photo-1541599540903-216a46ca1da0?w=100"
                        )
                        "cat_treats" -> listOf(
                            "All Treats" to "",
                            "Biscuits" to "https://lh3.googleusercontent.com/aida-public/AB6AXuDOps8AnyTzPPSpLxzJCtksHUhj9JO8eOziN2D8_mGGPm2WJ_hKMFmVRP-KRm_MweqPRrBNv8M_n7ucAvYYPcZ3yf8HyPC9KUh5kX-1RiCcrnv7IdBVZ0wZDlAaFEDIT-PzJbo1pHaJTBw2SFEa4VkSlkb6xdBi-xKZ77nbIulP9jtkwHht8cQNZbd4hwB0Cq1i7b_NJT3v-T74y8sMorNvtFJu1CfxBuOsulUL80EufSwX36ZfDJkyLdKrSw9ZUvhXyPOkKWhUrnQ",
                            "Dental" to "https://lh3.googleusercontent.com/aida/AP1WRLu2j9XUv_Re8vRJWJIq3otI7IoX2NgLk7u6dz-Q8YcKZM56ZhlEEG2hhVjR_8a8TLk1hk-4Tl9pMOlZyJGEzTBYJn_bkhNE9uLeijZ6EWFbm_jLi4gI6TBl8Gw26ZsyNvJfA59F0JWB29hKlzMIleF_-IFt7EarCxyfY7nXTlogfRnnfkUY63uBoXugR7m67gBg3tiS5d5irmOdJvPRSbNWfAdde8rkOop9HKp5a_RfrM4tCAPcR1ICVDY",
                            "Jerky" to "https://lh3.googleusercontent.com/aida-public/AB6AXuD1LsCqZae6WHZ_DyMPocb2L4zhbz8oNQSN2_TR8LsSedLCx22EDma5pJFMX1zgQGpQiYNgLXF2SUAF91QxXNXHdXiY0Qr6fYFUxwxWR8MXXTDIoQNe-f62c8iddyX9b4uCcf2VVkgWnDb7ljyuWNybt-vbC0FjtdAylfmtJs75jHZgc7q1mGpn-neoOttbXEZ72979RE5PIy1uX_QMhG_v8a2bWWYtT6Nw6hMe3g7X_siHHx82ikst98FRgzAzX33l0DWFdYnOuu8",
                            "Rawhide" to "https://lh3.googleusercontent.com/aida-public/AB6AXuCzN-4DOjRYqTG1cAeYDmWD2-If0-zgUQfwY44b0IrWu23o1_kbzqMdW48OKBqO3QSIhYlwxFDrhBDyd5qBH4B7Jo39Xa_a-mmt-Boi4QjQ-nmsxRiN9Ajdg9yYfqOjC8tHUePJhpSwnWsU3vKnh7DfDSfQvW5pcLL3PA2eMQX4AWq7L-9d11qOifS5fiTAsyb7cQILpKSPu0q74JG3ngl9EzJYIljLuHiW7TodILNn8MTnxM7_II3GoNj0JU4M-z_j63G2eLP2L6A",
                            "Natural" to "https://lh3.googleusercontent.com/aida-public/AB6AXuCTVOz5bM-_A1YoHGzWiOWlxaNNh3u49iK6bdqr8U7TJpWDksdPyL4NzX0rcL9EgJNGIY5QF5kU8w0jBB3Yaa02GTSNO_aYmcH8mH2BaujkeAWRgbUqfzbG_ITXFn0vbRDWeXRozlwkUI9ki1kC4FClm-iwKtQA55iu4gM5uO3M3HfdZkaPCyC_M1Jsfnvzsag3-u1rP1wqZOp9qL8Mc3S4xfnjOjG-gsbCG4p4cxuFTYHPSFOv5pdOz-JLFUjKbKzgxzW8I7GOUn0"
                        )
                        "cat_toys" -> listOf(
                            "All Toys" to "",
                            "Chew" to "https://lh3.googleusercontent.com/aida-public/AB6AXuAR1ny6pOdEO4AE9-38Cdg9tkkxG-q-nWRhlK8Zl8s8Vy266yKxwmpxGQc_z55xpYW-wnCeJbQhI2rSYWOq38XfQh2AoAHJ13neVHdUg-Ta8PdQZ2XgDVdpzSsx3XtguKLcIlxaxW3VGYCyzD3jL2grKMWrzrAjIndKP2wpWBuViPApnzk96o5EveidjmZysnCVN-1yNpqvw18LalZLXT3LMu8M2xkgZP0uI-Xz8ydKsCRXFnbx7DibVEQTO32OpEFfnWgznNQAe80",
                            "Interactive" to "https://lh3.googleusercontent.com/aida-public/AB6AXuCrI6IU4jfFLEkB-dE1GVpX9pd6jA6Wa7f9R6OIPtc1qu2jmvi9HGA5LzjBIFoHL4clmQoNeQwtlY1jhObRMzj-J2dl4A52mC8V_k1YNpV74NWCd8fVrDhjrb5N9klLry1m4NpsDqi510mB1T0kAt_XmTFuCTOil7oJ-cOdYWKOU4J13n6M4I7t9ihiZNAb02D7c1tNCU2vsb4QabPWe1U910LpWPlBTqpMcwEjQ-QUsdE9qlQUOl2tyV_OuqCNtPMRTe4Y1Z8ytPo",
                            "Fetch" to "https://lh3.googleusercontent.com/aida-public/AB6AXuA-uJ6kCnWNSEz9inTIeo-DaO7_QJZroCBWtIv_4GHZ5MTwoVLIwVb2opWaSfRjqBV4yDpPXb3AZb5ZltALZNQNPe4drmxoj-NE1nktRdf6LR-QrEPkRjqQpn4IpzldkE-CVj0ZYgtstN0hCelB0Pm2yMKO4HbFWuRnrBLfMRbltqmW8cLP13r61EFsfTqJgZSvQ0S05m0_JyiaQtHu-hRoHpyUtCioASRjY1xjE6ojR6QqvOaZMIo7aIOQHL8vqrYvBn_lzqrcw_k",
                            "Tug" to "https://lh3.googleusercontent.com/aida-public/AB6AXuA1PsRjnPY40VYykkQcgfyYWkek5kEYR_kn22iVdoNTrOHJYk-jSD0YxPW9zzxO3LpQTla9MdVgFI-MpWoTwMnB_w3Yx2Dh9U7zQd4I__cxx-B2z3vkmTfzzPwWIRaG_SJ6cgE6sQXaW-iHyIuDQ3BgfKJJXnXDZ-NLJgkWmOjaMtiiSSpLmMtdH_BV-Po1EAngH31x4CtiXI2H1DWdgVC6oUi5nP6fZyXvWIDSIZEh_C4pwCMvb4-S9_tAr35rrNhBvXxy0-HEUNI"
                        )
                        "cat_travel" -> listOf(
                            "All Gear" to "",
                            "Harness" to "https://lh3.googleusercontent.com/aida-public/AB6AXuBqVfcoxARGwW9gaA7nx42d8FbDxWiCujm8_KKCckeRwBJyxAjoOZqQ3lJDuigUvz1EK56DrPoPLjbNoYvAUHEuLqv0aBLRKGD5bx6lX7Zwiwjd9OEv5XBHTgGBvS8Dq7O24CKxcUcTUEtbPhKyaDvXhZH26IXUsARdoOvP4JhO0hIz_2iR1GUHyJfueCMIa6ZYumUupFfnaMVzAfChSzE55_Qtxvk49WTaXUCQU0FpJv8gFkRfK850NMMnqbOufieHfe6TDzfpS0Q",
                            "Leash" to "https://lh3.googleusercontent.com/aida-public/AB6AXuBstmBxcbiDYtks9mq4NixOW-4D3L9VcAfquJWjfv1N0bCdho7p0KKgykp_qBcyTD0ENSGCyfIDfHQQH-gLSY8Kv-0v2MaS87Cs83aU9F4el_pyARk80ra1n2N_KxNkfpXizkatturIIYh8XSS28oOFnV4ePt4jX_kYYukkKH01CBX7aMlHG5MF_ddHTXG6wKRAVDyDvOfIh8ABrjnm0W_4SeeJcoKal0ugJu3MGoH5fSpdWNVoTPnc0aC4",
                            "Carrier" to "https://lh3.googleusercontent.com/aida-public/AB6AXuC0ipHQmI817cbTajzccC0_zvXkOdt6EOuMCl0fb7FFbUUGuMiQw86twslM8WkqKkflWYagr5MNIOdxfCl4YbF_w03ZqK9wkbBh2W3v4lfLCt6l58UJQQoE_qcVNCn41DozEiDKPL43F6j4cybIp-dcVmVLjygtybnMIl1VUPRaWkEn2K5FquFRv7SRqwk7O0o48a7njmhUMbjH60v_gWsuDUjQ_UVzikekYi8R4qENil-hX7ZV4IwzYAKbCB4LOCYYtIpp-eRXdpA"
                        )
                        "cat_furniture" -> listOf(
                            "All" to "",
                            "Beds" to "https://lh3.googleusercontent.com/aida-public/AB6AXuAyB22rbZoXa_TMwG1PEQTebwvY15P0IxP7v6TdhTCytfwZMgahnIef-ltbzHyLc_859V8B5Psd23oz7A9s8bSBmvfCPqBasyHBffdOkRPAxP9NvPH_pu4BzKT13fsIdR33FIlcOoXrfZ7cdUEMPQdN6rYxYtvXd9xBISfzj2aZ5-mVQ_WpQh7eb4G-GZwg-4swSJJf-HKfoZk7aXifTG1h6p-b0c3r3LMv3WzClpsDJGOEeVhCIqHh_FTY8RhpbCtjZMXZ41lMicM",
                            "Crates" to "https://lh3.googleusercontent.com/aida-public/AB6AXuCXTaMdebKGAKtAnT0cEV4gXbtJVxJp-x0WhP9EZpn46KATHlUE_gQjtwHH121xm7uc_f1tso3uPRX7woaDW4HcgQ7wgyrhmR1iXytIx-b_lUu_diRbY1ae0huSkyCCUcWZEaFLEXWJm07kdOBAaANZ3K25fOARhpTLgiRWXP2IBObfzKtZQh0SMC2Y-4dWVbFzgjLB9wSnHBkyRhoCHI7L6GUr98L4eTfbMUcvGPziVUHRrxUsK5bvkOO6fPIyag_K45MK81ZZYe0",
                            "Trees" to "https://lh3.googleusercontent.com/aida-public/AB6AXuDANZpzNndPE-rVda5l7w0EsRG9jdriO-kyH0RPe7HkiUuey3JzoINi3hZRsCiHBcRj05fSgtSah8ZTVwGxdhq4soYi2lJBTgbJLljinFCcZnJJGLVZSlo7CxiFhf-G2Ao7fYoYhfSGrurBcKYIDOCdiaw2Xmv2ryKNxUQ7X6YP3Fvdl62f_lbQZnKBsbXzYnfjZVvSTX-EB1UPCPURcdRqMSmqcjvdH9oX27krCGvl4bfZbkblpFvIql0dgeCSBobZ6IrEJ3aAtns",
                            "Loungers" to "https://lh3.googleusercontent.com/aida-public/AB6AXuCXnG9j1AnSA_q_kS_rzvrieaz-Tz0Bv5rvZ62mbtFF6j2ZfioOavFXmWUxEOtjCEiW0U823DiGseN9AS/14rdV8bBeCPBI2_EP75EOorHW0bZ8IileRfjvX3q3k-7rWTPhxldqgfldGzvHZZsOUk_UanXurbpU8_twp3rhl1wS7_NCb-ag2_UjTfjpKok60uuuqpmv0c04Fet2BvDCry4xFwL3w6pic3i29ZdaodjxuabsWI3oLrtAczVKQPvQJ0zq9esgLAf1_lg"
                        )
                        "cat_waste" -> listOf(
                            "All Supplies" to "",
                            "Bags" to "https://lh3.googleusercontent.com/aida-public/AB6AXuAmWBhmnG8zVtMYSmDr9dsFp9M7d1fBUVpE8ZEjOJ4m8XFzJhctZztGQTo8qVeRZGlWTb2YIBkudkgUIAJ7SWTOCvdeMdQxT39gexF1mQQ9gDJDRUJE8LF1w5uFEvySdRCQyS7DHvbfBQDfIWBFPOkcWqL0ZT5HgH5ODLKS9plD86cz3v-9dm0TbdyMVHX7osl0yYnR4Sowdf6KX_VhjOWm21YceyrPq7cnjPvPtWy2bbZAPCUJqIa-7ZMO70KlqDuWcqXjkR8Kzew",
                            "Litter" to "https://lh3.googleusercontent.com/aida-public/AB6AXuBTzIR8bpIFcWjR2Lh0gfMechDQI_jhSsTBDnIospTf0-ab09IKr7cGiStYy9DiawVBEq16ADbH_RyF_2Dgbg8Dua9HyaH863mJWQ-AoKvyOK-DgauseXhTdeu4XRZO0ahfnsXsJMaB8RcwwIAG3nZLGoRW-SIr9TMH68eSe4FDMkav0vL2onMVJDclebkMZltVYYUdL78CqQU3fqyMlzChHafWTjiIYQ6kbDaRu6YPBWgTbSgdEoBMYQn1PKXW6s6K_iz0jluliHM",
                            "Litter Box" to "https://lh3.googleusercontent.com/aida-public/AB6AXuAN4zDQO5H87AhGgUZyUTdyG2akkOm67ZzN0iMOCVTB-O91UY7BTd9BHkzq8G1oNps8NN8Pb9PTAUMAK30gQTZKiomZNjZiBzUqTdyJmtcgV6Fo04c1ZRBA3vi7vpG2jW9_VZQNRoLGAUMRI4z_gMFnQRGFsdUUtD62bO5VRLU3PdDiv1qeW_Lhq2C2hgkqsM4opEtKDytvmCt_YeTbUj97rzIpAJ-nXRThD9YHnIBMey3f1c-w7jGDcLO9XV-xGRtcRtN1K-MjZwQ",
                            "Cleaning" to "https://lh3.googleusercontent.com/aida-public/AB6AXuBE08T9Hz6NTH5n-IyVZv-EjObVVjdsoenmoIPbeSIWwdeUXNl2BJh_8fnHnGr6dDXdjotC4_S1_nNwuMPPwmLC05eWAmcPNDPOR8F_UjKrz_cDTuhM_zZ6P4cVNp0_um5Y8pKzjPEvZohN_X44gZPina_w1njKvhr5Vg7S52JzAPgsgEI2lSU_S9xK2ezNPTgOOkk0hEncpk-GRBCcO2a2q5gQj6mtpAH5CereunCfIK2uB55v7VuVX75G9_iexKID3qjcC0415XQ"
                        )
                        else -> emptyList()
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(chips) { (label, imageUrl) ->
                            val isSelected = selectedCategoryChip == label
                            Row(
                                modifier = Modifier
                                    .height(40.dp)
                                    .background(Color.White, CircleShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF004AC6) else Color(0xFFC3C6D7),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCategoryChip = label }
                                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF4FF))
                                ) {
                                    if (imageUrl.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "All",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF004AC6)
                                            )
                                        }
                                    } else {
                                        Image(
                                            painter = rememberAsyncImagePainter(imageUrl),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF004AC6) else Color(0xFF0B1C30)
                                )
                            }
                        }
                    }
                }

                // Action Row (Sort & Filter button)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredProducts.size} products",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF434655)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Sort pill
                            Row(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(20.dp))
                                    .border(1.dp, Color(0xFFC3C6D7), RoundedCornerShape(20.dp))
                                    .clickable {
                                        Toast.makeText(context, "Sort selected", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF0B1C30), modifier = Modifier.size(16.dp))
                                Text("Sort", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                            }

                            // Filter pill
                            Row(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(20.dp))
                                    .border(1.dp, Color(0xFFC3C6D7), RoundedCornerShape(20.dp))
                                    .clickable { showFiltersSheet = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF0B1C30), modifier = Modifier.size(14.dp))
                                Text("Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(0xFFFEA619), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("2", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF684000))
                                }
                            }
                        }
                    }
                }

                // Grid of products
                item {
                    val defaultShop = shops.firstOrNull() ?: ShopEntity(
                        id = "shop_hyd_1",
                        ownerId = "merchant_hyd_1",
                        cityId = "hyd",
                        name = "Pawsome General Store",
                        description = "Mock general pet accessories & care hub",
                        address = "Mock general address",
                        locality = "Banjara Hills",
                        phone = "9876543210",
                        email = "pawsome@store.com"
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        val rowCount = (filteredProducts.size + 1) / 2
                        for (i in 0 until rowCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val item1Index = i * 2
                                val item2Index = i * 2 + 1
                                
                                if (item1Index < filteredProducts.size) {
                                    val prod = filteredProducts[item1Index]
                                    ProductGridCard(
                                        product = prod.copy(price = prod.price, mrp = prod.mrp),
                                        shop = defaultShop,
                                        isPrimaryStyle = true,
                                        viewModel = viewModel,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                
                                if (item2Index < filteredProducts.size) {
                                    val prod = filteredProducts[item2Index]
                                    ProductGridCard(
                                        product = prod.copy(price = prod.price, mrp = prod.mrp),
                                        shop = defaultShop,
                                        isPrimaryStyle = true,
                                        viewModel = viewModel,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sliding Filters Bottom Sheet Overlay (Box inside same view)
        AnimatedVisibility(
            visible = showFiltersSheet,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sheet Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(onClick = { showFiltersSheet = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF0B1C30))
                            }
                            Text("Filters", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        }
                        Text(
                            text = "Reset All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBA1A1A),
                            modifier = Modifier.clickable {
                                selectedPetType = "Dog"
                                selectedLifeStage = "Puppy"
                                selectedBrands.clear()
                                priceRange = 0f..defaultMaxPrice
                            }
                        )
                    }

                    HorizontalDivider(color = Color(0xFFC3C6D7).copy(alpha = 0.5f))

                    // Sheet Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // 1. Pet Type Group
                        Text("Pet Type", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val petTypes = listOf("Dog", "Cat", "Small Pet", "Fish")
                            petTypes.forEach { type ->
                                val isSelected = selectedPetType == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFFE5EEFF) else Color(0xFFF8F9FF),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF004AC6) else Color(0xFFC3C6D7),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedPetType = type },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF004AC6) else Color(0xFF0B1C30)
                                    )
                                }
                            }
                        }

                        // 2. Life Stage Group
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Life Stage", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val stages = listOf("Puppy", "Adult", "Senior", "All Ages")
                            stages.forEach { stage ->
                                val isSelected = selectedLifeStage == stage
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFF004AC6) else Color(0xFFF8F9FF),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 0.dp else 1.dp,
                                            color = Color(0xFFC3C6D7),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedLifeStage = stage },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stage,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF0B1C30)
                                    )
                                }
                            }
                        }

                        // 3. Brands Group
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Brands", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val brands = when (categoryId) {
                                "cat_food" -> listOf("Pedigree", "Royal Canin", "Drools", "Orijen")
                                "cat_treats" -> listOf("Pedigree", "Orijen", "Drools", "Hill's Science Diet")
                                "cat_toys" -> listOf("Kong", "Outward Hound", "Chuckit!", "Pawsome")
                                "cat_travel" -> listOf("Luxury Pet Boutique", "Barking Heads Store", "LeatherCraft Pets", "JetSet Pets")
                                "cat_furniture" -> listOf("Pawsome Furniture", "The Cozy Den", "Feline Fine Designs", "Orijen")
                                else -> listOf("EcoPet Supplies", "CleanPaws Hub", "Feline Furnishings", "FreshHome Pets")
                            }
                            brands.forEach { brand ->
                                val isSelected = selectedBrands.contains(brand)
                                Box(
                                    modifier = Modifier
                                        .size(width = 84.dp, height = 56.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFFE5EEFF) else Color(0xFFF8F9FF),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF004AC6) else Color(0xFFC3C6D7),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (isSelected) selectedBrands.remove(brand)
                                            else selectedBrands.add(brand)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = brand,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0B1C30),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 12.sp
                                    )

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(Color(0xFF004AC6), CircleShape)
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-4).dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // 4. Price Range Group
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Price Range", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${currencySym}${priceRange.start.toInt()} - ${currencySym}${priceRange.endInclusive.toInt()}",
                            fontSize = 12.sp,
                            color = Color(0xFF434655)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RangeSlider(
                            value = priceRange,
                            onValueChange = { priceRange = it },
                            valueRange = 0f..5000f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF004AC6),
                                inactiveTrackColor = Color(0xFFE5EEFF),
                                thumbColor = Color(0xFF004AC6)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("₹0", fontSize = 11.sp, color = Color(0xFF737686))
                            Text("₹5000+", fontSize = 11.sp, color = Color(0xFF737686))
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    // Sticky Action Button at bottom
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(16.dp)
                            .border(width = 0.5.dp, color = Color(0xFFC3C6D7).copy(alpha = 0.5f))
                    ) {
                        Button(
                            onClick = { showFiltersSheet = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004AC6))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Show ${filteredProducts.size} Products",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGridCard(
    product: ProductEntity,
    shop: ShopEntity,
    isPrimaryStyle: Boolean,
    viewModel: PawsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { /* Detail view optionally */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Image Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(Color(0xFFEFF4FF), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(product.photos.firstOrNull() ?: ""),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Discount Badges
                val discountTag = product.tags.firstOrNull { it.contains("OFF") || it.contains("SELLER") }
                if (discountTag != null) {
                    val isBestSeller = discountTag.contains("SELLER")
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(
                                color = if (isBestSeller) Color(0xFFFEA619) else Color(0xFFBA1A1A),
                                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = discountTag,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isBestSeller) Color(0xFF684000) else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Brand
            Text(
                text = product.brand.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF434655)
            )

            // Name
            Text(
                text = product.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1C30),
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Weight tag
            val weightText = when (product.id) {
                "p_rc_mini_adult" -> "3kg"
                "p_pedigree_chicken_veg" -> "3kg"
                "p_drools_chicken_egg" -> "1.2kg"
                "p_orijen_grain_free" -> "2kg"
                "p_whiskas_dry" -> "3kg"
                "p_rc_wet_cat" -> "85g"
                "p_oxbow_rabbit" -> "1.5kg"
                else -> "1kg"
            }
            Box(
                modifier = Modifier
                    .background(Color(0xFFE5EEFF), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = weightText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF434655)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Price and Add Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹${product.price.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0B1C30)
                    )
                    if (product.mrp > product.price) {
                        Text(
                            text = "₹${product.mrp.toInt()}",
                            fontSize = 10.sp,
                            color = Color(0xFF737686),
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }

                // Add button FAB
                IconButton(
                    onClick = {
                        viewModel.addToCart(product, shop)
                        Toast.makeText(context, "${product.name} added to cart!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isPrimaryStyle) Color(0xFF004AC6) else Color(0xFFE5EEFF),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to cart",
                        tint = if (isPrimaryStyle) Color.White else Color(0xFF004AC6),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// GROOMING SERVICES EXTENSIONS AND COMPOSABLES
data class GroomingService(
    val id: String,
    val name: String,
    val price: Double,
    val mrp: Double = 0.0,
    val durationText: String,
    val durationMin: Int,
    val photo: String,
    val tag: String? = null,
    val packageType: String
)

fun getMockGroomingServices(): List<GroomingService> {
    return listOf(
        GroomingService(
            id = "gs_luxury_bath",
            name = "Luxury Bath & Dry",
            price = 1200.0,
            durationText = "60 mins",
            durationMin = 60,
            photo = "https://lh3.googleusercontent.com/aida-public/AB6AXuBiNmbvF7knPi8jL6qfiXrcU8BtI2b5OWDfn7WqOdIua6D0lzYYmTYwLr3_iwadr87TwYHDe6bOpqeT7N6wEyGeB0UX7BsgaCBeaubT6ABbCKuZFicvkeQYMOEnXr10iKkTZqi8qCnaF-d7BxPXdyYXn_bJkqT3Bu-M4TjK-5GlH5pxv1jQViJlRX96PC3F5dipPylSiy2aS5vvr0A9XIL6B2o8MeR_GuMZJZFZ72EFn6gt34vPjxWIsKq0ax-XiplXARepvkGe--I",
            tag = "Best Seller",
            packageType = "Bathing"
        ),
        GroomingService(
            id = "gs_breed_specific",
            name = "Breed Specific Styling",
            price = 2500.0,
            durationText = "90 mins",
            durationMin = 90,
            photo = "https://lh3.googleusercontent.com/aida-public/AB6AXuD_t9N_TOhIYRvUUuoElxLQZi88cBZNzybQ0qStP6P8WKDRRdpFnU3z_Bp2IYxFZXBTItdf1tuFnfNeA2bBbtX4PSXhbYvNNCTP1sLUymQn1vBKLyIYUWv0gUmB_1n3_8VdS1JU824SUtKS4j1I4Rx2JitSEuufoRpANJl58zCQdjMv_p_bNAPd7X1PTT1LA7mnH00lKxX7luM-CHw0iF2-uay8_Q4NaaVprdv90qNR-aOS4GIccT6uZcv2kFK-FRPgJtrUqq8Ki6Y",
            packageType = "Haircut"
        ),
        GroomingService(
            id = "gs_full_spa",
            name = "Full Spa Package",
            price = 3500.0,
            mrp = 4000.0,
            durationText = "120 mins",
            durationMin = 120,
            photo = "https://lh3.googleusercontent.com/aida/AP1WRLsL_yS_k1qapC94pr73cFCLge8Egim_5CZuxM7w9OVrUKioOzkKkrL7trXeqjCLLqz7sOTv6dd9WZAnqL9eQyyxiznQBvrM0_kO3xyhy6VxHaYq9KIzSYSx0w0cK2iCbZuQm4shpiqTgB55poMAG_cgSzdS469BWnOmn3V7VWS0HA4vbQlgb4h3_l_fwWm0Zq265QVXqpBdyrEf3wUI8L_aOHjYkH0OcxXNIpPEr0KDJ80cUqAgYDUiU48",
            tag = "Sale",
            packageType = "Full Grooming"
        ),
        GroomingService(
            id = "gs_medicated_wash",
            name = "Medicated Tick Wash",
            price = 900.0,
            durationText = "45 mins",
            durationMin = 45,
            photo = "https://lh3.googleusercontent.com/aida-public/AB6AXuBrKpd0N_HPeJRBHuYSq0xn2aNNr8vNv3rKJ9iViz82d6YH9tf3PMfLu7Zkmy2rAsy7M_OUMNuUMPmErSxoAnBOWiDeuxkhssEfm4AdTtSoU2o3cMiPRT7994dTPKiJSQ6W2HZuImYP0jo8akMnDoI5QRIBX7__HN3c1G29sEuxXYZG-6wbbuepWbB9QeP9ouUiCA3THD_5gl4xlZpMLQzLLffTADDg7L39mJk--Za96FHcKsa8iQClcbYxvU03f7OMFcgGa_kEp3U",
            packageType = "Bathing"
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroomingServicesScreen(viewModel: PawsViewModel) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Filters State
    var selectedCategoryChip by remember { mutableStateOf("All") }
    var showFiltersSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Bottom Sheet filter selections
    var selectedDurationFilter by remember { mutableStateOf("All Durations") }
    val selectedPackageTypes = remember { mutableStateListOf<String>() }
    var priceRange by remember { mutableStateOf(0f..5000f) }
    
    val mockServices = remember { getMockGroomingServices() }
    
    // Filter Logic
    val filteredServices = remember(
        selectedCategoryChip,
        selectedDurationFilter,
        selectedPackageTypes.toList(),
        priceRange,
        searchQuery
    ) {
        mockServices.filter { service ->
            // Match category chip
            val matchesCategoryChip = if (selectedCategoryChip == "All") true else {
                service.packageType == selectedCategoryChip
            }
            
            // Match search query
            val matchesSearch = if (searchQuery.isEmpty()) true else {
                service.name.contains(searchQuery, ignoreCase = true)
            }
            
            // Match Duration Filter
            val matchesDuration = when (selectedDurationFilter) {
                "Under 60 mins" -> service.durationMin < 60
                "60 - 90 mins" -> service.durationMin in 60..90
                "Over 90 mins" -> service.durationMin > 90
                else -> true
            }
            
            // Match Package Types
            val matchesPackageType = if (selectedPackageTypes.isEmpty()) true else {
                selectedPackageTypes.contains(service.packageType)
            }
            
            // Match Price Range
            val matchesPrice = service.price >= priceRange.start && service.price <= priceRange.endInclusive
            
            matchesCategoryChip && matchesSearch && matchesDuration && matchesPackageType && matchesPrice
        }
    }
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FF))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color.White)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF004AC6)
                    )
                }
                
                Text(
                    text = "Grooming Services",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B1C30),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                
                IconButton(onClick = { viewModel.navigateTo(Screen.Cart) }) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Color(0xFF434655)
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFBA1A1A), CircleShape)
                        )
                    }
                }
            }
            
            HorizontalDivider(color = Color(0xFFC3C6D7).copy(alpha = 0.5f))
            
            // Main Scrollable Area
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Title & Subtitle
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text(
                            text = "Professional Grooming",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B1C30)
                        )
                        Text(
                            text = "Pamper your pets with certified groomers.",
                            fontSize = 12.sp,
                            color = Color(0xFF434655),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
                
                // Search Bar
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search services...", fontSize = 13.sp, color = Color(0xFF737686)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF737686)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFEFF4FF),
                                unfocusedContainerColor = Color(0xFFEFF4FF),
                                focusedBorderColor = Color(0xFF004AC6),
                                unfocusedBorderColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }
                
                // Top categories (circular images)
                item {
                    val categoriesList = listOf(
                        "All" to "",
                        "Bathing" to "https://lh3.googleusercontent.com/aida-public/AB6AXuChmGZBcMSVBA6dCs9-thohlMGJ87CAP4IpP7Jmt8biR_P7RdrZKXjvdg0YIbqN0pOUiAnNEBEFAcB3JyJz8l--CEo8di4c3Gixg-c3VryWQIAbxZbCOLGZ4L32VzazaZt-r6kMLlVQsgSNExRiwJ-cXrMsMlrEc1PBU5PHS6ssREQIJkMZLjTkp0mwkDxOUsqp5P32ALl3UzFePgjrhmTdZgt9J4VAAhd9hoW-NrVg2hte-GHsKeNip27A2DC4sq0Vxd8A8nXWIqg",
                        "Haircut" to "https://lh3.googleusercontent.com/aida-public/AB6AXuA4g1rLGJfuUgH76sXydoMRmZGpJLDMntZCe9A65dWeOgW8sm4ea6RHgnVd6ArUw7dd7v6lIsPJ0KMcQskq3-JwQ8A2IjUhaV68b3pce2dEfm1_oX7j_eC-zyAO9cxgf99eRBuHazJCSUYYTOtLM3j_QOOK1AIFnBDKktbobZb1RME8sz4-wEHNR1baQYJm0t2kH-plrLNUISRZFj_3KFfparHqGVlj2axbp1vnDCrXDbe-sEi4cYrd0n9wJMl3sKCvyWvhgSr5paE",
                        "Full Grooming" to "https://lh3.googleusercontent.com/aida/AP1WRLsL_yS_k1qapC94pr73cFCLge8Egim_5CZuxM7w9OVrUKioOzkKkrL7trXeqjCLLqz7sOTv6dd9WZAnqL9eQyyxiznQBvrM0_kO3xyhy6VxHaYq9KIzSYSx0w0cK2iCbZuQm4shpiqTgB55poMAG_cgSzdS469BWnOmn3V7VWS0HA4vbQlgb4h3_l_fwWm0Zq265QVXqpBdyrEf3wUI8L_aOHjYkH0OcxXNIpPEr0KDJ80cUqAgYDUiU48"
                    )
                    
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(categoriesList) { (label, imageUrl) ->
                            val isSelected = selectedCategoryChip == label
                            Row(
                                modifier = Modifier
                                    .height(40.dp)
                                    .background(Color.White, CircleShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF004AC6) else Color(0xFFC3C6D7),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCategoryChip = label }
                                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF4FF))
                                ) {
                                    if (imageUrl.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "All",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF004AC6)
                                            )
                                        }
                                    } else {
                                        Image(
                                            painter = rememberAsyncImagePainter(imageUrl),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF004AC6) else Color(0xFF0B1C30)
                                )
                            }
                        }
                    }
                }
                
                // Action Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredServices.size} services",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF434655)
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Sort pill
                            Row(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(20.dp))
                                    .border(1.dp, Color(0xFFC3C6D7), RoundedCornerShape(20.dp))
                                    .clickable {
                                        Toast.makeText(context, "Sort selected", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF0B1C30), modifier = Modifier.size(16.dp))
                                Text("Sort", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                            }
                            
                            // Filter pill
                            Row(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(20.dp))
                                    .border(1.dp, Color(0xFFC3C6D7), RoundedCornerShape(20.dp))
                                    .clickable { showFiltersSheet = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF0B1C30), modifier = Modifier.size(14.dp))
                                Text("Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                            }
                        }
                    }
                }
                
                // Grid of Services
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        val rowCount = (filteredServices.size + 1) / 2
                        for (i in 0 until rowCount) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val item1Index = i * 2
                                val item2Index = i * 2 + 1
                                
                                if (item1Index < filteredServices.size) {
                                    GroomingServiceGridCard(
                                        service = filteredServices[item1Index],
                                        onClick = {
                                            Toast.makeText(context, "${filteredServices[item1Index].name} booked successfully!", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                
                                if (item2Index < filteredServices.size) {
                                    GroomingServiceGridCard(
                                        service = filteredServices[item2Index],
                                        onClick = {
                                            Toast.makeText(context, "${filteredServices[item2Index].name} booked successfully!", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Sliding Filters Bottom Sheet Overlay (Simulated Box)
        AnimatedVisibility(
            visible = showFiltersSheet,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Sheet Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            IconButton(onClick = { showFiltersSheet = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF0B1C30))
                            }
                            Text("Service Filters", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        }
                        Text(
                            text = "Reset All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBA1A1A),
                            modifier = Modifier.clickable {
                                selectedDurationFilter = "All Durations"
                                selectedPackageTypes.clear()
                                priceRange = 0f..5000f
                            }
                        )
                    }
                    
                    HorizontalDivider(color = Color(0xFFC3C6D7).copy(alpha = 0.5f))
                    
                    // Sheet Body
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        // 1. Duration filter
                        Text("Duration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val durations = listOf("All Durations", "Under 60 mins", "60 - 90 mins", "Over 90 mins")
                            durations.forEach { dur ->
                                val isSelected = selectedDurationFilter == dur
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFFE5EEFF) else Color(0xFFF8F9FF),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF004AC6) else Color(0xFFC3C6D7),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedDurationFilter = dur },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dur,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF004AC6) else Color(0xFF0B1C30),
                                        textAlign = TextAlign.Center,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                        
                        // 2. Package Type
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Package Type", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val pkgTypes = listOf("Bathing", "Haircut", "Full Grooming")
                            pkgTypes.forEach { pkg ->
                                val isSelected = selectedPackageTypes.contains(pkg)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .background(
                                            color = if (isSelected) Color(0xFF004AC6) else Color(0xFFF8F9FF),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 0.dp else 1.dp,
                                            color = Color(0xFFC3C6D7),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            if (isSelected) selectedPackageTypes.remove(pkg)
                                            else selectedPackageTypes.add(pkg)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pkg,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF0B1C30)
                                    )
                                }
                            }
                        }
                        
                        // 3. Price Range
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Price Range", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${priceRange.start.toInt()} - ₹${priceRange.endInclusive.toInt()}",
                            fontSize = 12.sp,
                            color = Color(0xFF434655)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RangeSlider(
                            value = priceRange,
                            onValueChange = { priceRange = it },
                            valueRange = 0f..5000f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF004AC6),
                                inactiveTrackColor = Color(0xFFE5EEFF),
                                thumbColor = Color(0xFF004AC6)
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("₹0", fontSize = 11.sp, color = Color(0xFF737686))
                            Text("₹5000+", fontSize = 11.sp, color = Color(0xFF737686))
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    
                    // Action button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(16.dp)
                            .border(width = 0.5.dp, color = Color(0xFFC3C6D7).copy(alpha = 0.5f))
                    ) {
                        Button(
                            onClick = { showFiltersSheet = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004AC6))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Show ${filteredServices.size} Services",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroomingServiceGridCard(
    service: GroomingService,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { /* Details optionally */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Image Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .background(Color(0xFFEFF4FF), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(service.photo),
                    contentDescription = service.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Tag badge
                if (service.tag != null) {
                    val isBestSeller = service.tag == "Best Seller"
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(
                                color = if (isBestSeller) Color(0xFFFEA619) else Color(0xFFBA1A1A),
                                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = service.tag,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isBestSeller) Color(0xFF684000) else Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Service Name
            Text(
                text = service.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1C30),
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Duration Text Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🕒",
                    fontSize = 12.sp
                )
                Text(
                    text = service.durationText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF434655)
                )
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Price & Book Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₹${service.price.toInt()}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0B1C30)
                    )
                    if (service.mrp > service.price) {
                        Text(
                            text = "₹${service.mrp.toInt()}",
                            fontSize = 10.sp,
                            color = Color(0xFF737686),
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004AC6)),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Book Now",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// =========================================================================
// STITCH SCREENS ADDITIONAL HIGH FIDELITY LAYOUTS & HELPERS
// =========================================================================

@Composable
fun PoshPawsShopDetailScreen(
    shop: ShopEntity,
    viewModel: PawsViewModel,
    productsList: List<ProductEntity>,
    filteredProductsList: List<ProductEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    wishlists: List<WishlistEntity>,
    cartItems: Map<String, Int>
) {
    val context = LocalContext.current
    val isFavorite = wishlists.any { it.shopId == shop.id }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF)),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("https://lh3.googleusercontent.com/aida-public/AB6AXuDO-FLWT7iQKhtxei9MNj4zRn2Giyn_JLl-A7mFm14gNsSAeh5ZQIPsRHCcYiDaBMgn4OvvvYNsn2hJAGB4NJqgsCZLmfZXT1t_I0OW5B9ERTOzyv9XW-sKjBz4N3uEweZFAIoMUmBW-aTLY6bu1WNxdHdZNuJ0kS8SEc2OEVnf0y6K56nEFOuyvzkPwL3dy743debiOCvJJvce4R8i5PUGfzN8BrQkGHuTEzwSZzEtL7zRhZEPQ54M-79FGR3NHN58I-ApJ8m6BlA")
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0B1C30))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { /* Search */ },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF0B1C30))
                        }
                        IconButton(
                            onClick = { /* More options */ },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.8f), CircleShape)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color(0xFF0B1C30))
                        }
                    }
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .offset(y = (-30).dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "The Posh Paws",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B1C30)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Premium Pet Supplies & Accessories",
                                fontSize = 13.sp,
                                color = Color(0xFF434655)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFF007D55), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        null,
                                        tint = Color(0xFFBDFFDB),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "4.8",
                                        color = Color(0xFFBDFFDB),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFEFF4FF), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        null,
                                        tint = Color(0xFF434655),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "20-30 mins",
                                        color = Color(0xFF434655),
                                        fontSize = 11.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color(0xFFEFF4FF), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        null,
                                        tint = Color(0xFF434655),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "1.2 km away",
                                        color = Color(0xFF434655),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleWishlist(shop.id) }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Follow",
                                tint = Color(0xFF004AC6),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFavorite) "Followed" else "Follow",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF004AC6)
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-10).dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val categories = listOf(
                        Triple(null, "All", Icons.Default.Star),
                        Triple("cat_food", "Food", Icons.Default.Restaurant),
                        Triple("cat_toys", "Toys", Icons.Default.PlayArrow),
                        Triple("cat_treats", "Treats", Icons.Default.Favorite),
                        Triple("cat_travel", "Apparel", Icons.Default.Person)
                    )
                    items(categories) { (catId, label, icon) ->
                        val isSelected = selectedCategoryId == catId
                        val containerColor = if (isSelected) Color(0xFF004AC6) else Color(0xFFEFF4FF)
                        val contentColor = if (isSelected) Color.White else Color(0xFF0B1C30)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(containerColor)
                                .clickable { onCategorySelected(catId) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = contentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending Highlights",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B1C30),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700)
                )
            }
        }

        val chunkedProducts = filteredProductsList.chunked(2)
        if (chunkedProducts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No products in this category.", color = Color.Gray)
                }
            }
        } else {
            items(chunkedProducts) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (product in rowItems) {
                        val quantity = cartItems[product.id] ?: 0
                        Box(modifier = Modifier.weight(1f)) {
                            BoutiqueProductCard(
                                product = product,
                                quantity = quantity,
                                onAdd = { viewModel.addToCart(product, shop) },
                                onRemove = { viewModel.removeFromCart(product.id) }
                            )
                        }
                    }
                    if (rowItems.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                color = Color(0xFFC3C6D7).copy(alpha = 0.2f)
            )
        }

        item {
            StoreInfoSection(shop = shop)
        }
    }
}

@Composable
fun BoutiqueProductCard(
    product: ProductEntity,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val isSale = product.id == "p_posh_1"
    val isBestseller = product.id == "p_posh_2"
    val isOutOfStock = !product.inStock || product.stockCount == 0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFFEFF4FF))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.photos.firstOrNull())
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (isOutOfStock) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xFF737686), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OUT OF STOCK",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isSale) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xFFBA1A1A), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SALE",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (isBestseller) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .background(Color(0xFFFEA619), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BESTSELLER",
                            color = Color(0xFF2A1700),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    val catLabel = when (product.categoryId) {
                        "cat_food" -> "Premium Nutrition"
                        "cat_toys" -> "Durable Toys"
                        "cat_treats" -> "Gourmet Treats"
                        "cat_furniture" -> "Sleep & Comfort"
                        else -> "Pet Supplies"
                    }
                    Text(
                        text = catLabel,
                        fontSize = 10.sp,
                        color = Color(0xFF434655),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = product.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B1C30),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$${String.format("%.2f", product.price)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B1C30)
                        )
                        if (product.mrp > product.price) {
                            Text(
                                text = "$${String.format("%.2f", product.mrp)}",
                                fontSize = 10.sp,
                                color = Color(0xFF737686),
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        }
                    }

                    if (isOutOfStock) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEFF4FF), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ADD",
                                color = Color(0xFF737686),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (quantity > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFF004AC6), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                                Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(
                                quantity.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(onClick = onAdd, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFF004AC6).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .background(Color(0xFF004AC6).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .clickable { onAdd() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ADD",
                                color = Color(0xFF004AC6),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreInfoSection(shop: ShopEntity) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Store Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1C30)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = Color(0xFF004AC6),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "123 Pet Avenue, Suite 4B",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B1C30)
                            )
                            Text(
                                text = "Metropolis, NY 10001",
                                fontSize = 12.sp,
                                color = Color(0xFF434655)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Get Directions",
                                fontSize = 11.sp,
                                color = Color(0xFF004AC6),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    val uri = Uri.parse("google.navigation:q=${shop.lat},${shop.lng}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Call,
                            null,
                            tint = Color(0xFF004AC6),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "(555) 123-4567",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B1C30)
                            )
                            Text(
                                text = "Tap to call",
                                fontSize = 12.sp,
                                color = Color(0xFF434655),
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${shop.phone}"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF4FF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                null,
                                tint = Color(0xFF004AC6),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Opening Hours",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B1C30)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                                Text("Open until 8:00 PM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007D55))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Mon - Fri", fontSize = 11.sp, color = Color(0xFF434655))
                                Text("9:00 AM - 8:00 PM", fontSize = 11.sp, color = Color(0xFF434655))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Saturday", fontSize = 11.sp, color = Color(0xFF434655))
                                Text("10:00 AM - 6:00 PM", fontSize = 11.sp, color = Color(0xFF434655))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sunday", fontSize = 11.sp, color = Color(0xFF434655))
                                Text("Closed", fontSize = 11.sp, color = Color(0xFF434655))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedVaccinationsTabletsScreen(viewModel: PawsViewModel, defaultTab: Int) {
    val reminders by viewModel.activeReminders.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(defaultTab) }

    val upcomingReminders = remember(reminders) {
        reminders.filter { !it.isCompleted }
    }
    val completedReminders = remember(reminders) {
        reminders.filter { it.isCompleted }
    }

    val completionPercentage = remember(reminders) {
        val total = reminders.size
        if (total == 0) 80 else (completedReminders.size * 100 / total)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF0B1C30))
            }
            Text(
                text = "Vaccinations & Tablets",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1C30),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { /* Notifications */ }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF0B1C30))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFEFF4FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Color(0xFFEFF4FF), CircleShape)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data("https://lh3.googleusercontent.com/aida-public/AB6AXuD4KdBQ6-mf1VyED8-6SACRuiyBSxrtR7EgrDXp3_t5OzzRwwX5ABy1McNAnNV35WjVzIe3TqCbtziE_1wsD0jpuA0NFiCnvvneMCp2LFPwv3Lvag7dvFjq2Pw2pTXdPAAugUOyaCQI2Xt-c-smX2jlSMSzxvBjIYhL0rhTiDvClEAHhKxGXAHgRHZYbmDOLQlQaK5SUoMEMmKW31D23Nelb67GHPwCOH6BuO8aDS6UDPhVcYRFh44LzwduRyvQOeFhxA80f0xg7jQ")
                                            .crossfade(true)
                                            .build()
                                    ),
                                    contentDescription = "Pet Profile",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Buddy",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0B1C30)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0xFFEFF4FF), RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color(0xFF007D55), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Healthy", fontSize = 11.sp, color = Color(0xFF007D55), fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Golden Retriever • 2 Years, 3 Mos", fontSize = 13.sp, color = Color(0xFF434655))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFEFF4FF))
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("NEXT DUE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF737686))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        null,
                                        tint = Color(0xFFBA1A1A),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("In 3 Days", fontSize = 12.sp, color = Color(0xFFBA1A1A), fontWeight = FontWeight.Bold)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("COMPLETION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF737686))
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = completionPercentage / 100f,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = Color(0xFF007D55),
                                        trackColor = Color(0xFFEFF4FF)
                                    )
                                    Text("${completionPercentage}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .background(Color(0xFFEFF4FF), RoundedCornerShape(24.dp))
                        .padding(4.dp)
                ) {
                    listOf("Upcoming", "History").forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0B1C30) else Color(0xFF434655)
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                item {
                    Text(
                        "Action Required",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B1C30),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (upcomingReminders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No action items required! 🎉", color = Color.Gray)
                        }
                    }
                } else {
                    items(upcomingReminders) { item ->
                        val isMed = item.type == "medication"
                        val isOverdue = item.id == "rem_med_1" || item.id == "rem_med_2" || item.notes.contains("Due")
                        val statusText = if (isOverdue) "Overdue" else "In 3 Days"
                        val badgeBg = if (isOverdue) Color(0xFFFFDAD6) else Color(0xFFFEA619).copy(alpha = 0.2f)
                        val badgeTextColor = if (isOverdue) Color(0xFFBA1A1A) else Color(0xFF855300)
                        val accentColor = if (isOverdue) Color(0xFFBA1A1A) else Color(0xFFFEA619)

                        val title = when (item.id) {
                            "rem_med_2" -> "NexGard Spectra Tablet"
                            "rem_vacc_1" -> "Annual C5 Booster"
                            "rem_med_1" -> "Heartworm Tablet"
                            "rem_vacc_2" -> "Rabies Vaccine"
                            else -> item.title
                        }
                        val notes = when (item.id) {
                            "rem_med_2" -> "Monthly flea, tick & worming treatment."
                            "rem_vacc_1" -> "Protects against Parvovirus, Distemper, Hepatitis."
                            else -> item.notes.substringBefore("|").trim()
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFEFF4FF)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(4.dp)
                                        .background(accentColor)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isMed) Icons.Default.Info else Icons.Default.Star,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0B1C30)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(badgeBg, RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(statusText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeTextColor)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(notes, fontSize = 11.sp, color = Color(0xFF434655))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.DateRange, null, tint = Color(0xFF737686), modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Due: ${item.dateString}", fontSize = 10.sp, color = Color(0xFF737686))
                                        }
                                    }

                                    if (isMed) {
                                        Button(
                                            onClick = {
                                                viewModel.toggleReminderCompletion(item.id, true)
                                                Toast.makeText(context, "${title} marked as done! 💊", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Mark as Done", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "Redirecting to book vet... 🏥", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004AC6)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Book Vet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "Recent Records",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B1C30),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (completedReminders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No completed records yet.", color = Color.Gray)
                        }
                    }
                } else {
                    items(completedReminders) { item ->
                        val isMed = item.type == "medication"
                        val title = when (item.id) {
                            "rem_med_2" -> "NexGard Spectra Tablet"
                            "rem_vacc_1" -> "Annual C5 Booster"
                            "rem_med_1" -> "Heartworm Tablet"
                            "rem_vacc_2" -> "Rabies Vaccine"
                            else -> item.title
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FF)),
                            border = BorderStroke(1.dp, Color(0xFFEFF4FF))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF007D55).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isMed) Icons.Default.Info else Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFF007D55),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0B1C30)
                                        )
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            null,
                                            tint = Color(0xFF007D55),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Given on ${item.dateString}", fontSize = 11.sp, color = Color(0xFF737686))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveOrderCard(
    shopName: String,
    orderId: String,
    time: String,
    status: String,
    deliveryInfo: String,
    itemsSummary: String,
    price: String,
    trackable: Boolean,
    onAction: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFF4FF))
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        null,
                        tint = Color(0xFF004AC6),
                        modifier = Modifier.size(24.dp).align(Alignment.Center)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(shopName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                    Text("Order $orderId • $time", fontSize = 11.sp, color = Color(0xFF737686))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF4FF)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFEA619), CircleShape)
                        )
                        Text(status, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF653E00))
                    }
                    Text(deliveryInfo, fontSize = 11.sp, color = Color(0xFF434655))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(itemsSummary, fontSize = 13.sp, color = Color(0xFF434655))
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFC3C6D7).copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(price, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004AC6)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (trackable) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Track", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PastOrderCard(
    shopName: String,
    date: String,
    itemsSummary: String,
    price: String,
    onReorder: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF4FF))
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        null,
                        tint = Color(0xFF004AC6),
                        modifier = Modifier.size(20.dp).align(Alignment.Center)
                    )
                }
                Column {
                    Text(shopName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                    Text(date, fontSize = 11.sp, color = Color(0xFF737686))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(itemsSummary, fontSize = 12.sp, color = Color(0xFF434655))
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFC3C6D7).copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(price, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                OutlinedButton(
                    onClick = onReorder,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF004AC6)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = Color(0xFF004AC6), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reorder", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF004AC6))
                }
            }
        }
    }
}

@Composable
fun SavedProductCard(
    product: ProductEntity,
    onRemove: () -> Unit,
    onAddToCart: () -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(product.photos.firstOrNull()),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Remove",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = product.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B1C30),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            null,
                            tint = Color(0xFFFFB95F),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "4.9",
                            fontSize = 10.sp,
                            color = Color(0xFF737686)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${String.format("%.2f", product.price)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF004AC6)
                    )

                    IconButton(
                        onClick = onAddToCart,
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFFEFF4FF), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Add to Cart",
                            tint = Color(0xFF004AC6),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedShopCard(
    shop: ShopEntity,
    onRemove: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
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
                        .background(Color.Black.copy(alpha = 0.2f))
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Remove",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF007D55)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = shop.name.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(shop.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFEA619), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(shop.rating.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        }
                    }
                    Text(shop.description, fontSize = 11.sp, color = Color(0xFF434655), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("30-45 min", fontSize = 10.sp, color = Color(0xFF737686))
                        Box(modifier = Modifier.size(3.dp).background(Color(0xFFC3C6D7), CircleShape))
                        Text("2.4 km", fontSize = 10.sp, color = Color(0xFF737686))
                    }
                }
            }
        }
    }
}

