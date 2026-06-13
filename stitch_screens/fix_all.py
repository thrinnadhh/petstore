import re
import os

paws_app_path = "/Users/trinadh/projects/petstore/app/src/main/java/com/example/ui/PawsApp.kt"

with open(paws_app_path, "r") as f:
    content = f.read()

# 1. Delegate HomeScreen
orig_home = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PawsViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()"""

new_home = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: PawsViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    if (currentUser?.role == "captain") {
        CaptainDashboardScreen(viewModel = viewModel)
    } else {
        PawsappHomeScreen(viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OriginalHomeScreen(viewModel: PawsViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()"""

if orig_home in content:
    content = content.replace(orig_home, new_home)
    print("Delegated HomeScreen.")
else:
    print("HomeScreen signature not found!")

# 2. Delegate ShopDetailScreen
orig_shop = """@Composable
fun ShopDetailScreen(viewModel: PawsViewModel, shopId: String) {
    val context = LocalContext.current"""

new_shop = """@Composable
fun ShopDetailScreen(viewModel: PawsViewModel, shopId: String) {
    if (shopId == "mock_posh_paws") {
        val shopState = remember { mutableStateOf<ShopEntity?>(null) }
        var productsList by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
        var selectedCategoryId by remember { mutableStateOf<String?>(null) }
        val wishlists by viewModel.wishlists.collectAsState()
        val cartItems by viewModel.cartItems.collectAsState()
        val scope = rememberCoroutineScope()
        
        LaunchedEffect(shopId) {
            scope.launch {
                val s = viewModel.getShopById(shopId)
                shopState.value = s
                if (s != null) {
                    productsList = viewModel.getProductsByShop(s.id)
                }
            }
        }
        
        val shop = shopState.value
        if (shop != null) {
            val filteredProductsList = remember(productsList, selectedCategoryId) {
                if (selectedCategoryId != null) {
                    productsList.filter { it.categoryId == selectedCategoryId }
                } else {
                    productsList
                }
            }
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
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF004AC6))
            }
        }
    } else {
        OriginalShopDetailScreen(viewModel = viewModel, shopId = shopId)
    }
}

@Composable
fun OriginalShopDetailScreen(viewModel: PawsViewModel, shopId: String) {
    val context = LocalContext.current"""

if orig_shop in content:
    content = content.replace(orig_shop, new_shop)
    print("Delegated ShopDetailScreen.")
else:
    print("ShopDetailScreen signature not found!")

# 3. Fix VaccinationsScreen
vacc_pattern = re.compile(
    r'@OptIn\(ExperimentalMaterial3Api::class\)\s*@Composable\s*fun VaccinationsScreen\(viewModel:\s*PawsViewModel\)\s*\{.*?(?=(?://\s*={5,}\s*\n)?//\s*SCREEN:\s*FAVOURITES)',
    re.DOTALL
)

replacement_vacc = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaccinationsScreen(viewModel: PawsViewModel) {
    UnifiedVaccinationsTabletsScreen(viewModel = viewModel, defaultTab = 1)
}

"""

if vacc_pattern.search(content):
    content = vacc_pattern.sub(replacement_vacc, content)
    print("Fixed VaccinationsScreen.")
else:
    print("VaccinationsScreen pattern not matched!")

# 4. Replace TextFieldDefaults.outlinedTextFieldColors
content = content.replace(
    "TextFieldDefaults.outlinedTextFieldColors(\n                        containerColor = Color.White,",
    """OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,"""
)
content = content.replace(
    "TextFieldDefaults.outlinedTextFieldColors(\n                containerColor = Color.White,",
    """OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,"""
)

# 5. Fix Restaurant icon inside PoshPawsShopDetailScreen
content = content.replace("Icons.Default.Restaurant", "Icons.Default.ShoppingCart")

# 6. Append PawsappHomeScreen and PromoCarouselSection at the end
pawsapp_home_screen_code = """

// =========================================================================
// HIGH FIDELITY HOME SCREEN AND PROMO CAROUSEL
// =========================================================================

@Composable
fun PawsappHomeScreen(viewModel: PawsViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val shopsList by viewModel.shops.collectAsState()
    val syncState by viewModel.powerSyncState.collectAsState()
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showCityPickerSheet by remember { mutableStateOf(false) }
    var selectedCityId by remember { mutableStateOf("hyd") }
    val selectedCityName = if (selectedCityId == "hyd") "Bengaluru, Karnataka" else "Other City"
    
    val categoriesList = listOf(
        Triple("cat_food", "Food & Nutrition", Icons.Default.ShoppingCart),
        Triple("cat_treats", "Treats & Chews", Icons.Default.Favorite),
        Triple("cat_toys", "Toys & Enrichment", Icons.Default.PlayArrow),
        Triple("cat_travel", "Travel & Apparel", Icons.Default.Person),
        Triple("cat_furniture", "Furniture & Sleep", Icons.Default.Home),
        Triple("cat_waste", "Waste & Litter", Icons.Default.Info),
        Triple("cat_groom", "Grooming Services", Icons.Default.Person)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FF))
            .statusBarsPadding()
    ) {
        // TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Location Picker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showCityPickerSheet = true }
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color(0xFF004AC6),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Home",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0B1C30)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF434655),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Bengaluru, Karnataka",
                        fontSize = 11.sp,
                        color = Color(0xFF434655),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(100.dp)
                    )
                }
            }

            // User Profile
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.navigateTo(Screen.UserProfile) }
                    .padding(4.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFEA619), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Premium",
                            color = Color(0xFF2A1700),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Max (Golden Retriever)",
                        fontSize = 11.sp,
                        color = Color(0xFF434655),
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFFEA619), CircleShape)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            currentUser?.avatarUrl ?: "https://lh3.googleusercontent.com/aida-public/AB6AXuA2KsW7gWLv5UgbYQyePDKB5PUl6E_MlrrcsyIn8ExEFGpcuOuoWX_5iK1u6HVWAVgHbQ4KLaft9SCiE5Sf2svsAm5g4sTQIId5YJxG_QRIEf-VSN9S-9qvGxkChVs8K2gPIQ9dMJ1_0WdRywB1FEphKT3JKGzMWtBwX1TXW9FBDnV43dTvJCdttet_Fm7angT28GP180qAmNQAymxY0rohM0qZelZUSvPeeeQFF6H4JosS0U0Ka05a8vCriklgcplLDwqvatiVRFM"
                        ),
                        contentDescription = "Profile Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Sticky Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { viewModel.navigateTo(Screen.Search) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF737686)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Search for 'Pedigree' or 'Grooming'...",
                    fontSize = 14.sp,
                    color = Color(0xFF737686),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color(0xFFC3C6D7).copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Voice Search",
                    tint = Color(0xFF004AC6)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    items(categoriesList) { (catId, label, icon) ->
                        val screenToNav = when (catId) {
                            "cat_food" -> Screen.FoodNutrition
                            "cat_treats" -> Screen.TreatsChews
                            "cat_toys" -> Screen.ToysEnrichment
                            "cat_travel" -> Screen.TravelApparel
                            "cat_furniture" -> Screen.FurnitureSleep
                            "cat_waste" -> Screen.WasteManagement
                            "cat_groom" -> Screen.GroomingServices
                            else -> Screen.Home
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF004AC6).copy(alpha = 0.05f))
                                .border(1.dp, Color(0xFF004AC6).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .clickable { viewModel.navigateTo(screenToNav) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color(0xFF004AC6),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B1C30)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                PromoCarouselSection()
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val pills = listOf(
                        Triple("Favourites", Icons.Default.Favorite, Screen.Favourites),
                        Triple("Orders", Icons.Default.ShoppingCart, Screen.Orders),
                        Triple("Reports", Icons.Default.DateRange, Screen.ReportsDashboard)
                    )
                    pills.forEach { (label, icon, screen) ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.navigateTo(screen) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, null, tint = Color(0xFF004AC6), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF4FF))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, null, tint = Color(0xFF004AC6), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reports & Health", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable { viewModel.navigateTo(Screen.Vaccinations) }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFF006242), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Vaccinations", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30), textAlign = TextAlign.Center)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable { viewModel.navigateTo(Screen.TabletsIssued) }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Info, null, tint = Color(0xFF855300), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Tablets Issued", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30), textAlign = TextAlign.Center)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable { viewModel.navigateTo(Screen.Appointments) }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.DateRange, null, tint = Color(0xFF004AC6), modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Appointments", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Premium Shops Nearby 🏆",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B1C30),
                    modifier = Modifier.padding(horizontal = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val shopsNearby = shopsList.filter { it.id == "mock_posh_paws" || it.id == "mock_healthy_hounds" }
                    items(shopsNearby) { shop ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .width(220.dp)
                                .clickable { viewModel.navigateTo(Screen.ShopDetail(shop.id)) }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .background(Color.LightGray)
                                ) {
                                    val photoUrl = if (shop.id == "mock_posh_paws") {
                                        "https://lh3.googleusercontent.com/aida-public/AB6AXuDO-FLWT7iQKhtxei9MNj4zRn2Giyn_JLl-A7mFm14gNsSAeh5ZQIPsRHCcYiDaBMgn4OvvvYNsn2hJAGB4NJqgsCZLmfZXT1t_I0OW5B9ERTOzyv9XW-sKjBz4N3uEweZFAIoMUmBW-aTLY6bu1WNxdHdZNuJ0kS8SEc2OEVnf0y6K56nEFOuyvzkPwL3dy743debiOCvJJvce4R8i5PUGfzN8BrQkGHuTEzwSZzEtL7zRhZEPQ54M-79FGR3NHN58I-ApJ8m6BlA"
                                    } else {
                                        "https://images.unsplash.com/photo-1583337130417-3346a1be7dee?w=300"
                                    }
                                    Image(
                                        painter = rememberAsyncImagePainter(photoUrl),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.White, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(shop.rating.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(Icons.Default.Star, null, tint = Color(0xFFFEA619), modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(shop.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                                    Text(shop.description, fontSize = 11.sp, color = Color(0xFF434655), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0xFFEFF4FF), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.DateRange, null, tint = Color(0xFF004AC6), modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (shop.id == "mock_posh_paws") "20 mins" else "25 mins", fontSize = 10.sp, color = Color(0xFF0B1C30), fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Guides 🩺",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B1C30),
                    modifier = Modifier.padding(horizontal = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val guides = listOf(
                        Triple("Puppy Nutrition (0-2 mo)", "Dietary guide", "https://lh3.googleusercontent.com/aida-public/AB6AXuB5z2g3IHBH5gz3oR6QqQl6XDHPXhUN4b482F_jJ_bPPyD_OnMLA-gnGMdyNXz7v-jaFvfwW2nZgw5KX9NdTC9YFXzkoNU1GbbdvagvvRSdasnjCk7_elM2rSKuGbzmVkaxSgZdguhWDkjbumkNBU7ppWfcO0BHE2XmNjU2nF4ild_5dbokZ4jck5r_IU4B0KaW73XkasFSbOjZBQL9xAMihZ9AWDirYg99ysJl5RAKEqRVNyjhtIeMcQILmQFS97_A-HBozb9Kz-k"),
                        Triple("Puppy Growth (2-12 mo)", "Milestone tracking", "https://lh3.googleusercontent.com/aida-public/AB6AXuCwFpaCtKFAmO7u4LFhgbudFkcsabYvyZjr-jxBxNF4vzO99nSWyB1ctF35zFChTfG2kDD7lTnLyX3yqYMgslq2MKyTOED_O2D5wQ2IngywdmQOVbanZTXPwBa9bpdjzT3ViUDhNrkfbU-HKFLtGNI_9NRmi_NOj_ELxWiZJ-ZMpz9hOQhsTHA133HKuZbZbwUsiQeLUzzEbOmVrpylOe0dkYv5ib-0mHIRkyZllWTFC9L8NfD9mD0yqX9w1ck6HEsH3Z0tUxdpdpQ"),
                        Triple("Hair & Coat Health", "Grooming tips", "https://lh3.googleusercontent.com/aida-public/AB6AXuDYKJG83KcL1yNh-w9EyZpJJHjgLNuCQIwoxOy4oxO9897FscAQj38VOtNLWetFhV0UcGvbpvYFMlMNisc1N7np5cd_0qaZcKNYGqSiaBeZDsParI4mxGmOxyw6mMU4RnJGckXQcWZv9-HU08XqZzmVBHFvSqAiJicfb1bes3T14Iv-yfAJJflwwAUl-CIk_HMUPFxRcCa1f_RtBSqklHewyESVhtAzbgZgixnF5Psbz6VhIkMXq-m2KovO2SB4RSYINa5KONreaS8")
                    )
                    items(guides) { (title, subtitle, imgUrl) ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .width(180.dp)
                                .height(160.dp)
                        ) {
                            Column {
                                Image(
                                    painter = rememberAsyncImagePainter(imgUrl),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Column(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .weight(1f),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                    Text(subtitle, fontSize = 10.sp, color = Color(0xFF737686), textAlign = TextAlign.Center)
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
fun PromoCarouselSection() {
    val banners = listOf(
        Triple("Flat 20% Off Premium Gear", "Mega Sale", "https://lh3.googleusercontent.com/aida-public/AB6AXuD6AeLhg79H-Qs95EgMOGxSG2HjJa3jlosvBXsvXE5r1rnCSeml3ETIaLhK2r2kkY8l014vbMq3CO9DvXCeF_udak6kApRBpmmenOLebPTGJDX0lNUEn2c_IpOj50T6QSmzoOy6xOPOOdMY2evfXi4nMgs9TZbCWytwpJvN8ZpQmxIi2hs9iM4G6ZZ-KAAlvmuhSUGUnp0BytQTvH3Yv5djAj6xrWVYt-TSyCg82T7EA5rORY6blClGGYC9o-eRv02kkJ1gcOfYEjg"),
        Triple("Discover Fresh Essentials", "New Arrivals", "https://lh3.googleusercontent.com/aida-public/AB6AXuASfM4FE2gFaBN0OhgeTBOjES2tuJHOL72sgaRGgO-tENBpVYDnBud9une2vRaHplLerDL25aSx0vh9cJz69DTuFIW1egWGJvltzY6_RQn4GF_mmvas_iU801N87_y6-JFB3H3zQFxvQwyYXfEgQuQ8JQuV0F3BI5heqbe6Fn_zOitcCR1esBTCKNBI4NVMHkzRxgVe8mC0fGuNb2htuR3f91sz8odhN4x_vfPmxh9MBA5fDQuWEqnrBtDvcw7nJsN8Qi7g7AzJId8"),
        Triple("Top Rated by Pet Parents", "Community Picks", "https://lh3.googleusercontent.com/aida-public/AB6AXuDP0Bso5NdUTuYfQmdxjGfrU18IgCERDgWEobR1RzRKk0phJmTjprXxrZ2e6MSiBzYHNllyH_O29w9XG-5RgKbo7sx9KygQhzOHoPj74CO-x1GqUujm5wEjiMN462Jd5zLvEnUDGElVK2fb7LGOI7ziuz25lE42roHK7gbnIVfpE7H3TXg8vXkDQ8iQBLfj3YiIarAthoLCqep7tQ7gY0S0wJwunX30RA3VqJ-IO10PEIcc7YJiBsxCcI9DaozwNmO6Uu30bknpLIc")
    )
    var currentSlide by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while(true) {
            delay(4000)
            currentSlide = (currentSlide + 1) % banners.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        val banner = banners[currentSlide]
        Image(
            painter = rememberAsyncImagePainter(banner.third),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = banner.second.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFEA619)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = banner.first,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.width(200.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFF004AC6), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Shop Now", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            banners.forEachIndexed { idx, _ ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (idx == currentSlide) Color.White else Color.White.copy(alpha = 0.4f),
                            CircleShape
                        )
                )
            }
        }
    }
}
"""

with open(paws_app_path, "w") as f:
    f.write(content + "\n" + pawsapp_home_screen_code)

print("Fix completed.")
