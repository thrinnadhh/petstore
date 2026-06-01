package com.example.ui

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
                    is Screen.ChatList -> ChatListScreen(viewModel = viewModel)
                    is Screen.ChatDetail -> ChatDetailScreen(viewModel = viewModel, shopId = screen.shopId)
                    is Screen.MerchantOrders -> MerchantOrdersScreen(viewModel = viewModel)
                    is Screen.MerchantMenu -> MerchantMenuScreen(viewModel = viewModel)
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
                contentDescription = "Swiggy Paws Icon",
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(32.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Swiggy Paws",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "Premium Pet Shop Delivery Platform",
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
            title = "City-Wide Pet Shops & Foods",
            desc = "Find premium nutritional kibbles, grain-free snacks, and toys from recruited pet stores across the entire city.",
            illustration = "https://images.unsplash.com/photo-1589924691106-073b1381cb35?w=600&auto=format&fit=crop&q=80"
        ),
        OnboardSlide(
            title = "Premium Grooming & Spas",
            desc = "Connect with top-rated salons and vet clinics in your city. Schedule professional bathes and pet therapy easily.",
            illustration = "https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=600&auto=format&fit=crop&q=80"
        ),
        OnboardSlide(
            title = "Express Delivery Anywhere",
            desc = "Order from top recruited pet stores in the city and get ultra-fast delivery straight to your doorstep.",
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
                    "Login",
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
                    "Register",
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
                    label = { Text("Email or Phone Number") },
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
                    label = { Text("Password") },
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
                        "Login with Password",
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
                    Text("Forgot Password? Login via Phone OTP 🔑", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                // Phone Number & OTP Login
                OutlinedTextField(
                    value = loginPhone,
                    onValueChange = { if (it.length <= 10) loginPhone = it },
                    label = { Text("Phone Number") },
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
                        onValueChange = { if (it.length <= 4) loginOtp = it },
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
                        if (!isLoginOtpSent) "Send OTP" else "Verify & Login",
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
                    Text("Back to Email/Password Login 🔐", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        } else {
            // REGISTER TAB VIEW
            OutlinedTextField(
                value = regName,
                onValueChange = { regName = it },
                label = { Text("Full Name") },
                placeholder = { Text("e.g. Arjun Kumar") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, null) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = regPhone,
                onValueChange = { if (it.length <= 10) regPhone = it },
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
                Text(
                    "Select Selfie with your Dog (Profile Pic):",
                    fontSize = 12.sp,
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
                    onValueChange = { regAadharNumber = it },
                    label = { Text("Aadhar Card Number 🆔") },
                    placeholder = { Text("e.g. 123456789012") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
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
                    onValueChange = { if (it.length <= 4) regOtp = it },
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
                    if (regRole == "consumer" && regPetName.trim().isEmpty()) {
                        Toast.makeText(context, "Please enter your pet's name", Toast.LENGTH_SHORT).show()
                        return@Button
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
            "Swiggy Paws delivers premium pet foods, grooming & toys from top pet shops across the city.",
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
    val context = LocalContext.current
    var isShiftOnline by remember { mutableStateOf(false) }

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
                                onCheckedChange = { isShiftOnline = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF2DB37A)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                                Text("₹0.00", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
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
                                Text("0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
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
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isShiftOnline) {
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
                                // A pulsing effect represented by a progress bar / indicator
                                CircularProgressIndicator(color = Color(0xFFFC8019))
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
    val dynamicBanners by viewModel.targetedBanners.collectAsState()
    val context = LocalContext.current
    val syncState by viewModel.powerSyncState.collectAsState()
    val isSwiggyOne by viewModel.isSwiggyOneSubscriber.collectAsState()

    // Query state links
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryIds by viewModel.selectedCategoryIds.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val filterOpenNow by viewModel.filterOpenNow.collectAsState()
    val filterDelivery by viewModel.filterDelivery.collectAsState()
    val filterRating by viewModel.filterRating.collectAsState()

    val allOrders by viewModel.allOrders.collectAsState()
    val allOrderItems by viewModel.allOrderItems.collectAsState()

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

    Row(modifier = Modifier.fillMaxSize()) {
        // ── LEFT SIDEBAR: PET REMEDIES FOR COMMON PROBLEMS ─────────────────────────
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 16.dp)
        ) {
            Text(
                text = "Pet Remedies for Common Problems 🩺",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFC8019),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                text = "Select a concern to see recommended treatments and products",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            )
            
            if (petProblems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
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
            } else {
                val selectedProblem = petProblems.find { it.id == selectedProblemId } ?: petProblems.firstOrNull()
                if (selectedProblemId == null && selectedProblem != null) {
                    selectedProblemId = selectedProblem.id
                }
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item {
                        Text(
                            text = "COMMON CONCERNS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }
                    
                    items(petProblems) { problem ->
                        val isSelected = selectedProblemId == problem.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { selectedProblemId = problem.id },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFFC8019).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFFFC8019) else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = problem.emoji.ifEmpty { "🩺" },
                                    fontSize = 20.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = problem.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color(0xFFFC8019) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    
                    if (selectedProblem != null) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFC8019).copy(alpha = 0.05f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFFC8019).copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = selectedProblem.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color(0xFFFC8019)
                                    )
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
                                        val cartItems by viewModel.cartItems.collectAsState()
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
                                                    
                                                    // Quantity buttons
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
                    }
                }
            }
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
            thickness = 1.dp
        )

        // ── RIGHT SIDE: MAIN FEED CONTENT ──────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
        // App Header Row
        item {
            HomeHeader(
                cityName = selectedCityName,
                userName = currentUser?.fullName ?: "Pet Owner",
                avatarUrl = currentUser?.avatarUrl ?: "",
                onCityClick = { showCityPickerSheet = true },
                onProfileClick = { viewModel.navigateTo(Screen.UserProfile) },
                onChatClick = { viewModel.navigateTo(Screen.ChatList) },
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

        // ── COLLABORATIVE GROUP AUCTION WIDGET ─────────────────────────────────────
        item {
            val currentRfqSessionId by viewModel.currentRfqSessionId.collectAsState()
            val activeSession by viewModel.activeRfqSession.collectAsState()
            val memberItems by viewModel.activeRfqMemberItems.collectAsState()
            val quotations by viewModel.activeRfqQuotations.collectAsState()
            
            var showJoinDialog by remember { mutableStateOf(false) }
            var rfqInputId by remember { mutableStateOf("") }
            
            if (currentRfqSessionId == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
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
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = { viewModel.leaveGroupRfqSession() },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Leave Session", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
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
                            isSelected = selectedCategoryIds.isEmpty(),
                            onClick = { viewModel.clearSelectedCategories() }
                        )
                    }
                    items(categoryList) { cat ->
                        CategoryChip(
                            name = cat.name,
                            iconUrl = cat.iconUrl,
                            isSelected = cat.id in selectedCategoryIds,
                            onClick = { viewModel.toggleSelectedCategory(cat.id) }
                        )
                    }
                }
            }
        }

        // ── SWIGGY PAWS MERCHANT RECRUITMENT PROGRAM ──────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clickable { viewModel.navigateTo(Screen.MerchantShopSetup) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Partner with Swiggy Paws 🐾", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("RECRUITING", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Own a pet shop? Onboard your clinic, grooming salon, or boutique store today and deliver city-wide!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(30.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("Register ➔", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

        // Feed list
        if (processedShops.isEmpty()) {
            item {
                EmptyShopsState()
            }
        } else {
            item {
                Text(
                    "Showing ${processedShops.size} Pet Shops in $selectedCityName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            items(processedShops) { shop ->
                val totalOrders = allOrders.count { it.shopId == shop.id }
                val deliveredOrderIds = allOrders.filter { it.shopId == shop.id && it.status == "delivered" }.map { it.id }.toSet()
                val totalDeliveredProducts = allOrderItems.filter { it.orderId in deliveredOrderIds }.sumOf { it.quantity }

                ShopItemCard(
                    shop = shop,
                    totalOrders = totalOrders,
                    totalDeliveredProducts = totalDeliveredProducts,
                    onClick = { viewModel.navigateTo(Screen.ShopDetail(shop.id)) }
                )
            }
        }
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
    }
}

// Sub Component: Header
@Composable
fun HomeHeader(
    cityName: String,
    userName: String,
    avatarUrl: String,
    onCityClick: () -> Unit,
    onProfileClick: () -> Unit,
    onChatClick: () -> Unit,
    syncState: PowerSyncManager.SyncState,
    onSyncClick: () -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { onSyncClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
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
                        is PowerSyncManager.SyncState.Syncing -> "Sync: Syncing..."
                        is PowerSyncManager.SyncState.Paused -> "Sync: Paused"
                        is PowerSyncManager.SyncState.Offline -> "Sync: Offline"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat envelope icon
            IconButton(
                onClick = onChatClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Chats",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

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

// Shop Card representation
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

                // Dynamic performance metrics badge row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📋", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Orders Taken: ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = totalOrders.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📦", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Delivered: ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = totalDeliveredProducts.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

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
                    Text("Invoice Summary", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Item Subtotal", fontSize = 13.sp)
                        Text("₹$computedSubtotal", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery Partner Fee", fontSize = 13.sp)
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
                        Text("Grand Total to Pay", fontWeight = FontWeight.Black, fontSize = 15.sp)
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
            Text("PROCEED TO PAY • ₹$grandTotal", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
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
                
                if (order.status == "out_for_delivery" || order.status == "delivered") {
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
                                Text("Ramesh Kumar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("⭐ 4.9 ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDB7C00))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("• Vaccinated Partner ✓", fontSize = 11.sp, color = Color(0xFF3F8F27), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        IconButton(
                            onClick = { Toast.makeText(context, "Calling Ramesh Kumar (+91 9876543210)...", Toast.LENGTH_SHORT).show() },
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
                    TimelineStep("Order Placed", "Your order has been registered by Swiggy Paws", "pending"),
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
    val allProducts by viewModel.allProducts.collectAsState()
    val categoryList by viewModel.categories.collectAsState()
    val activeTab by viewModel.searchTab.collectAsState() // "Shops" | "Products"
    val context = LocalContext.current

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

    // Filtered matched shops
    val matchedShops = remember(searchQuery, shopsList) {
        shopsList.filter { 
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
                text = if (activeTab == "Shops") "Matched Stores (${matchedShops.size})" else "Matched Products (${matchedProducts.size})",
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
                if (activeTab == "Shops") {
                    if (matchedShops.isEmpty()) {
                        item {
                            Text("No matching pet stores found.", modifier = Modifier.padding(20.dp), color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        items(matchedShops) { shop ->
                            ShopItemCard(shop = shop, onClick = { viewModel.navigateTo(Screen.ShopDetail(shop.id)) })
                        }
                    }
                } else {
                    if (matchedProducts.isEmpty()) {
                        item {
                            Text("No matching pet products found.", modifier = Modifier.padding(20.dp), color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        items(matchedProducts) { product ->
                            val productShop = shopsList.find { it.id == product.shopId }
                            val shopName = productShop?.name ?: "Local Shop"
                            SearchProductRow(
                                product = product,
                                viewModel = viewModel,
                                shopName = shopName,
                                onAdd = { 
                                    viewModel.addToCart(product, productShop ?: shopsList.firstOrNull() ?: return@SearchProductRow)
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
                ProfileOptionRow(icon = Icons.Default.DateRange, title = "Care Calendar & Reminders", subtitle = "Manage vaccine schedules and birthdays", onClick = { showCareCalendar = true })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.LocationOn, title = "Change City Location", subtitle = "Current city Hyderabad", onClick = { viewModel.navigateTo(Screen.LocationSelect) })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                
                // Show Super Admin Controls if they have the role
                if (currentUser?.role == "superadmin" || currentUser?.role == "admin") {
                    ProfileOptionRow(icon = Icons.Default.Settings, title = "Super Admin Controls", subtitle = "Approve pet stores and push banners", onClick = { viewModel.navigateTo(Screen.SuperAdmin) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
                
                ProfileOptionRow(icon = Icons.Default.Person, title = "Fictional Role: Merchant Toggle", subtitle = "Switch context to Suresh (Pet Shop Owner)", onClick = {
                    viewModel.loginWithPhone("8765432109", onSuccess = {}, onError = {})
                })
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ProfileOptionRow(icon = Icons.Default.Lock, title = "Fictional Role: Admin Toggle", subtitle = "Switch context to Super Admin Control Panel", onClick = {
                    viewModel.loginWithPhone("9999999999", onSuccess = {}, onError = {})
                })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val fcmToken by viewModel.fcmToken.collectAsState()
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google Firebase FCM Service Credentials",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Registration token:",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fcmToken,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copy",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(fcmToken))
                            Toast.makeText(context, "FCM token copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
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
            Text("Log out of Swiggy Paws", fontWeight = FontWeight.Bold)
        }
    }

    if (showCareCalendar) {
        CareCalendarSheet(
            viewModel = viewModel,
            onDismiss = { showCareCalendar = false }
        )
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
                            .clickable { selectedTab = tabId },
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
