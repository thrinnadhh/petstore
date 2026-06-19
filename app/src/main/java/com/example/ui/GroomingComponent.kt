package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

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
    
    val backgroundModifier = remember {
        Modifier
            .fillMaxSize()
            .drawBehind {
                // 1. Draw Skyblue Gradient background
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFBFDBFE), // Soft Blue
                        Color(0xFFEFF6FF), // Ice/Light Sky Blue
                        Color.White
                    )
                )
                drawRect(brush = gradientBrush)
                
                // 2. Draw Subtle bubbles in the background
                val width = size.width
                val height = size.height
                val backgroundBubbles = listOf(
                    Triple(0.08f, 0.15f, 12.dp.toPx()),
                    Triple(0.88f, 0.12f, 22.dp.toPx()),
                    Triple(0.18f, 0.40f, 15.dp.toPx()),
                    Triple(0.80f, 0.45f, 28.dp.toPx()),
                    Triple(0.12f, 0.70f, 20.dp.toPx()),
                    Triple(0.85f, 0.75f, 14.dp.toPx()),
                    Triple(0.48f, 0.85f, 26.dp.toPx()),
                    Triple(0.92f, 0.30f, 18.dp.toPx())
                )
                backgroundBubbles.forEach { (xPercent, yPercent, radius) ->
                    drawCircle(
                        color = Color(0x1538BDF8), // Translucent sky blue
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(width * xPercent, height * yPercent)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(width * xPercent, height * yPercent),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
    }
    
    Box(modifier = backgroundModifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E3A8A)
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
                                focusedContainerColor = Color.White.copy(alpha = 0.8f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.8f),
                                focusedBorderColor = Color(0xFF3B82F6),
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
                                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFFC3C6D7),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCategoryChip = label }
                                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    shape = CircleShape,
                                    modifier = Modifier.size(32.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF))
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
                                                color = Color(0xFF1E3A8A)
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
                                    color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF0B1C30)
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
                                            color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8F9FF),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFFC3C6D7),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedDurationFilter = dur },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dur,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF0B1C30),
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
                                            color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFFF8F9FF),
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
                                activeTrackColor = Color(0xFF1E3A8A),
                                inactiveTrackColor = Color(0xFFEFF6FF),
                                thumbColor = Color(0xFF1E3A8A)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316))
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
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
                        Triple("cat_food", "Food", Icons.Default.ShoppingCart),
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
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
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
                val appts by viewModel.activeAppointments.collectAsState()
                val bookings by viewModel.myGroomingBookings.collectAsState(initial = emptyList())

                val rescheduleAppts = remember(appts) {
                    appts.filter { it.status == "reschedule_pending" }
                }
                val rescheduleBookings = remember(bookings) {
                    bookings.filter { it.status == "reschedule_pending" }
                }

                if (rescheduleAppts.isNotEmpty() || rescheduleBookings.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Pending Reschedule Requests 🗓️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        rescheduleAppts.forEach { appt ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, Color(0xFFFEF3C7)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = appt.serviceName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Proposed New Slot: ${appt.rescheduleDate} at ${appt.rescheduleTime}",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.acceptReschedule(appt) { success ->
                                                    if (success) {
                                                        Toast.makeText(context, "Reschedule Accepted!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Accept", color = Color.White, fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.declineReschedule(appt) { success ->
                                                    if (success) {
                                                        Toast.makeText(context, "Reschedule Declined (Booking Cancelled)", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            border = BorderStroke(1.dp, Color.Red),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Decline", color = Color.Red, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        rescheduleBookings.forEach { booking ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, Color(0xFFFEF3C7)),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    val bookingTitle = remember(booking.serviceId) {
                                        val parts = booking.serviceId.split("_")
                                        if (parts.size >= 4) {
                                            parts[3].replaceFirstChar { it.uppercase() } + " Grooming"
                                        } else {
                                            "Grooming Package"
                                        }
                                    }
                                    Text(
                                        text = bookingTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = "Proposed New Slot: ${booking.rescheduleDate} at ${booking.rescheduleTime}",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.getOrGenerateSlotsForDate(booking.shopId, booking.rescheduleDate ?: "") { slotsList ->
                                                    val newTimeStr = booking.rescheduleTime ?: ""
                                                    val match = slotsList.find { it.slotTime == newTimeStr }
                                                    if (match != null) {
                                                        viewModel.acceptGroomingReschedule(booking, match.id, booking.rescheduleDate ?: "", newTimeStr) { success ->
                                                            if (success) {
                                                                Toast.makeText(context, "Reschedule Accepted!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "No available slots found for proposed time.", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Accept", color = Color.White, fontSize = 12.sp)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.declineGroomingReschedule(booking) { success ->
                                                    if (success) {
                                                        Toast.makeText(context, "Reschedule Declined (Booking Cancelled)", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            border = BorderStroke(1.dp, Color.Red),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Decline", color = Color.Red, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    data class PillItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tint: androidx.compose.ui.graphics.Color, val screen: Screen)
                    val pills = listOf(
                        PillItem("Favourites", Icons.Default.Favorite, androidx.compose.ui.graphics.Color(0xFFBA1A1A), Screen.Favourites),
                        PillItem("Orders", Icons.Default.ShoppingCart, androidx.compose.ui.graphics.Color(0xFF855300), Screen.Orders),
                        PillItem("Reports", Icons.Default.DateRange, androidx.compose.ui.graphics.Color(0xFF006242), Screen.ReportsDashboard)
                    )
                    pills.forEach { pill ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.navigateTo(pill.screen) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(pill.icon, null, tint = pill.tint, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(pill.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
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
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val shopsNearby = shopsList.filter { it.id == "mock_posh_paws" || it.id == "mock_healthy_hounds" }
                    items(shopsNearby) { shop ->
                        val isAvailable = shop.shopEnabled
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = if (isAvailable) Color.White else Color(0xFFF1F5F9)),
                            modifier = Modifier
                                .width(220.dp)
                                .then(if (!isAvailable) Modifier.alpha(0.5f) else Modifier)
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
                                    if (!isAvailable) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Unavailable",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
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
                    "Hospitals Nearby 🏥",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B1C30),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val hospitalsNearby = shopsList.filter { it.id == "mock_city_hospital" || it.id == "mock_petcare_wellness" || (it.vetClinicEnabled && it.id.startsWith("shop_")) }
                    items(hospitalsNearby) { hospital ->
                        val isAvailable = hospital.vetClinicEnabled
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = if (isAvailable) Color.White else Color(0xFFF1F5F9)),
                            modifier = Modifier
                                .width(220.dp)
                                .then(if (!isAvailable) Modifier.alpha(0.5f) else Modifier)
                                .clickable { viewModel.navigateTo(Screen.ShopDetail(hospital.id)) }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .background(Color.LightGray)
                                ) {
                                    val photoUrl = if (hospital.id == "mock_city_hospital") {
                                        "https://lh3.googleusercontent.com/aida-public/AB6AXuB5z2g3IHBH5gz3oR6QqQl6XDHPXhUN4b482F_jJ_bPPyD_OnMLA-gnGMdyNXz7v-jaFvfwW2nZgw5KX9NdTC9YFXzkoNU1GbbdvagvvRSdasnjCk7_elM2rSKuGbzmVkaxSgZdguhWDkjbumkNBU7ppWfcO0BHE2XmNjU2nF4ild_5dbokZ4jck5r_IU4B0KaW73XkasFSbOjZBQL9xAMihZ9AWDirYg99ysJl5RAKEqRVNyjhtIeMcQILmQFS97_A-HBozb9Kz-k"
                                    } else {
                                        "https://lh3.googleusercontent.com/aida-public/AB6AXuDYKJG83KcL1yNh-w9EyZpJJHjgLNuCQIwoxOy4oxO9897FscAQj38VOtNLWetFhV0UcGvbpvYFMlMNisc1N7np5cd_0qaZcKNYGqSiaBeZDsParI4mxGmOxyw6mMU4RnJGckXQcWZv9-HU08XqZzmVBHFvSqAiJicfb1bes3T14Iv-yfAJJflwwAUl-CIk_HMUPFxRcCa1f_RtBSqklHewyESVhtAzbgZgixnF5Psbz6VhIkMXq-m2KovO2SB4RSYINa5KONreaS8"
                                    }
                                    Image(
                                        painter = rememberAsyncImagePainter(photoUrl),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (!isAvailable) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Unavailable",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.White, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(hospital.rating.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(Icons.Default.Star, null, tint = Color(0xFFFEA619), modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(hospital.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                                    Text(hospital.description, fontSize = 11.sp, color = Color(0xFF434655), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0xFFEFF4FF), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFF004AC6), modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (hospital.id == "mock_city_hospital") "1.2 km away" else "2.5 km away", fontSize = 10.sp, color = Color(0xFF0B1C30), fontWeight = FontWeight.Medium)
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
                    "Grooming Nearby ✂️",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B1C30),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val groomingNearby = shopsList.filter { it.id == "mock_paws_bubbles" || it.id == "mock_grooming_room" || (it.groomingEnabled && it.id.startsWith("shop_")) }
                    items(groomingNearby) { groomer ->
                        val isAvailable = groomer.groomingEnabled
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFC3C6D7).copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = if (isAvailable) Color.White else Color(0xFFF1F5F9)),
                            modifier = Modifier
                                .width(220.dp)
                                .then(if (!isAvailable) Modifier.alpha(0.5f) else Modifier)
                                .clickable { viewModel.navigateTo(Screen.ShopDetail(groomer.id)) }
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                        .background(Color.LightGray)
                                ) {
                                    val photoUrl = if (groomer.id == "mock_paws_bubbles") {
                                        "https://lh3.googleusercontent.com/aida-public/AB6AXuCLDcsiQzTJ35jcCpCNHSC0CPGtsB--0Xdb-LVHpAoteDtktABgPSTQMMPGcfAgwvMEa22Twz_PWoxMANUVHDlfmcOgn53ytuQl7eHMq2kD2oBJX8mNowGEJjxAIHOdSyARgHYwDg6TFxoXYoYnVogC8c3QqEQxzKXQHBhPxhv1VK3mWc1o8kwr-eyteIwsACN_yi3C9LZwRdXcVVbk_7sQFr6t-JFQsx7yaIuZTVNVZeEEPbhBBDvdW00lu99huqxwo4ClJpdhVnY"
                                    } else {
                                        "https://lh3.googleusercontent.com/aida-public/AB6AXuA8-OnbYbH6ervRc4iDKjRxKLt6mO6wKvK8uA3YF7QqP3s6MzG7DILE7cEzhjoG1QhhOujkvk6kROOkrlX_HL2AqoacPYkIXR9PWO8eOCuNrkd24m2rUzV3v_SsO_Tt-eng-sTQpDJE-rHj2Ksx8Qw8uGaUZB-6jpIsSfhmFTkAVrxBXvue6givMDI98jjybom420pH3sbIUeml2Io6RygcKD0Xk279U3oRRXPXcZSjpIgZMptmDBLqWFDLWZce7mlSIJJ-aZXYgOs"
                                    }
                                    Image(
                                        painter = rememberAsyncImagePainter(photoUrl),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    if (!isAvailable) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Unavailable",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .background(Color.White, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(groomer.rating.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(Icons.Default.Star, null, tint = Color(0xFFFEA619), modifier = Modifier.size(10.dp))
                                        }
                                    }
                                }
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(groomer.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B1C30))
                                    Text(groomer.description, fontSize = 11.sp, color = Color(0xFF434655), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .background(Color(0xFFEFF4FF), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFF004AC6), modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (groomer.id == "mock_paws_bubbles") "0.8 km away" else "1.9 km away", fontSize = 10.sp, color = Color(0xFF0B1C30), fontWeight = FontWeight.Medium)
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
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
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

// =========================================================================
// GROOMING MODULE SCREENS (SCREENS B, C, D, E, F, G)
// =========================================================================

// --- SCREEN B: SLOT PICKER ---
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun GroomingSlotPickerScreen(
    viewModel: PawsViewModel,
    shopId: String,
    serviceId: String,
    variantName: String,
    price: Double,
    durationMinutes: Int,
    petSizeCategory: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Pets List — use activePets StateFlow which is already filtered for current user
    val pets by viewModel.activePets.collectAsState()
    var selectedPet by remember { mutableStateOf<PetEntity?>(null) }
    var petExpanded by remember { mutableStateOf(false) }
    
    // Auto-detect or confirm size
    var confirmedSize by remember(selectedPet) {
        val weightVal = selectedPet?.weight?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull()
        val detected = when {
            weightVal == null -> null
            weightVal < 10.0 -> "small"
            weightVal <= 25.0 -> "medium"
            else -> "large"
        }
        mutableStateOf(detected)
    }

    // Load active variants for this shop to see if size is available
    val activeServices by viewModel.getActiveGroomingServicesForShopFlow(shopId).collectAsState(initial = emptyList())
    val variantServices = remember(activeServices, variantName) {
        activeServices.filter { it.variantName == variantName }
    }
    
    // Find matching service for the confirmed pet size category
    val matchingService = remember(variantServices, confirmedSize) {
        variantServices.find { it.petSizeCategory == confirmedSize }
    }

    // Calendar
    val calendarDates = remember {
        val list = mutableListOf<String>()
        val formatCorrect = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        for (i in 0 until 14) {
            list.add(formatCorrect.format(cal.time))
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        list
    }
    
    // Auto-generate slots in database
    LaunchedEffect(shopId) {
        calendarDates.forEach { date ->
            viewModel.getOrGenerateSlotsForDate(shopId, date) { }
        }
    }
    
    // Collect all slots for date range to disable dates with zero slots
    val slotsRangeState by remember(shopId) {
        viewModel.getGroomingSlotsForDateRangeFlow(shopId, calendarDates.first(), calendarDates.last())
    }.collectAsState(initial = emptyList())
    
    var selectedDate by remember { mutableStateOf("") }
    
    // Observe slots for the selected date
    val slotsForDate = remember(slotsRangeState, selectedDate) {
        slotsRangeState.filter { it.slotDate == selectedDate }
    }
    
    var selectedSlot by remember(selectedDate) { mutableStateOf<GroomingSlotEntity?>(null) }
    var specialInstructions by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Grooming Slot Picker",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // STEP 1: Select Pet
            Text("Step 1: Select Pet Profile", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E3A8A))
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { petExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                ) {
                    Text(
                        text = selectedPet?.name ?: "Choose Pet Profile",
                        fontWeight = FontWeight.Medium
                    )
                }
                
                DropdownMenu(
                    expanded = petExpanded,
                    onDismissRequest = { petExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    pets.forEach { pet ->
                        DropdownMenuItem(
                            text = { Text("${pet.name} (${pet.breed} - ${pet.weight})") },
                            onClick = {
                                selectedPet = pet
                                petExpanded = false
                            }
                        )
                    }
                    if (pets.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No pets registered. Tap to register") },
                            onClick = {
                                petExpanded = false
                                Toast.makeText(context, "Please create a pet profile first!", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Auto-detected / confirmed size chips
            if (selectedPet != null) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val weightText = selectedPet?.weight ?: ""
                        Text(
                            text = "Pet Weight: $weightText",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Confirm Size Category:",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("small", "medium", "large").forEach { sz ->
                                val isSzSel = confirmedSize == sz
                                FilterChip(
                                    selected = isSzSel,
                                    onClick = { confirmedSize = sz },
                                    label = { Text(sz.capitalize()) }
                                )
                            }
                        }
                        
                        if (confirmedSize != null) {
                            if (matchingService == null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Not available for your pet's size.",
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "Price: ₹${matchingService.price.toInt()}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                    Text(
                                        text = "Duration: ${matchingService.durationMinutes} mins",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E3A8A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // STEP 2: Calendar
            Text("Step 2: Select Date", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E3A8A))
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(calendarDates) { date ->
                    val slotsForThisDate = slotsRangeState.filter { it.slotDate == date }
                    val isAvailable = slotsForThisDate.isNotEmpty() && slotsForThisDate.any { !it.isBlocked && it.bookedCount < it.capacity }
                    val isSel = selectedDate == date
                    
                    val dateFriendly = try {
                        val inF = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        val outF = java.text.SimpleDateFormat("EEE\ndd MMM", java.util.Locale.US)
                        outF.format(inF.parse(date))
                    } catch(e: Exception) { date }
                    
                    val bg = when {
                        isSel -> Color(0xFF1E3A8A)
                        !isAvailable -> Color(0xFFF1F5F9)
                        else -> Color.White
                    }
                    val textCol = when {
                        isSel -> Color.White
                        !isAvailable -> Color.LightGray
                        else -> Color(0xFF1E293B)
                    }
                    val borderCol = when {
                        isSel -> Color.Transparent
                        !isAvailable -> Color(0xFFE2E8F0)
                        else -> Color(0xFFCBD5E1)
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                            .clickable(enabled = isAvailable) { selectedDate = date }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dateFriendly,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textCol,
                                textAlign = TextAlign.Center
                            )
                            if (!isAvailable) {
                                Text(
                                    text = "Booked",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // STEP 3: Time slots
            if (selectedDate.isNotEmpty()) {
                Text("Step 3: Select Time Slot", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E3A8A))
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slotsForDate.forEach { slot ->
                        val isBlocked = slot.isBlocked
                        val isFull = slot.bookedCount >= slot.capacity
                        val isSlotSel = selectedSlot?.id == slot.id
                        
                        val chipBg = when {
                            isSlotSel -> Color(0xFF1E3A8A)
                            isBlocked || isFull -> Color(0xFFE2E8F0)
                            else -> Color.White
                        }
                        val chipTextCol = when {
                            isSlotSel -> Color.White
                            isBlocked || isFull -> Color(0xFF94A3B8)
                            else -> Color(0xFF334155)
                        }
                        val chipBorder = when {
                            isSlotSel -> Color.Transparent
                            isBlocked || isFull -> Color.Transparent
                            else -> Color(0xFFCBD5E1)
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(8.dp))
                                .clickable(enabled = !isBlocked && !isFull) { selectedSlot = slot }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = slot.slotTime,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = chipTextCol
                                )
                                if (isBlocked) {
                                    Text("Blocked", fontSize = 8.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                } else if (isFull) {
                                    Text("Fully Booked", fontSize = 8.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // STEP 4: Special Instructions
            Text("Step 4: Special Instructions (Optional)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E3A8A))
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = specialInstructions,
                onValueChange = { if (it.length <= 300) specialInstructions = it },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                placeholder = { Text("Enter any details e.g. coat condition, allergies, or size details (max 300 chars)") },
                shape = RoundedCornerShape(8.dp)
            )
            Text(
                text = "${specialInstructions.length}/300 chars",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // STEP 5: Summary card & Confirm button
            if (selectedPet != null && selectedDate.isNotEmpty() && selectedSlot != null && matchingService != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Booking Summary", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E3A8A))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Service:", fontSize = 13.sp, color = Color.Gray)
                            Text("$variantName (${confirmedSize?.capitalize()})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pet:", fontSize = 13.sp, color = Color.Gray)
                            Text(selectedPet?.name ?: "", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Date & Time:", fontSize = 13.sp, color = Color.Gray)
                            Text("$selectedDate at ${selectedSlot?.slotTime}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Duration:", fontSize = 13.sp, color = Color.Gray)
                            Text("${matchingService.durationMinutes} mins", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFBFDBFE))
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Price:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("₹${matchingService.price.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF16A34A))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.bookGroomingSlot(
                            shopId = shopId,
                            serviceId = matchingService.id,
                            slotId = selectedSlot!!.id,
                            petId = selectedPet!!.id,
                            petSizeCategory = confirmedSize!!,
                            specialInstructions = specialInstructions,
                            totalPrice = matchingService.price,
                            onSuccess = { bookingId ->
                                isSubmitting = false
                                viewModel.navigateTo(Screen.GroomingBookingConfirmation(bookingId))
                            },
                            onError = { errMsg ->
                                isSubmitting = false
                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    enabled = !isSubmitting
                ) {
                    Text(
                        text = if (isSubmitting) "Confirming..." else "Confirm Booking",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// --- SCREEN C: BOOKING CONFIRMATION ---
@Composable
fun GroomingBookingConfirmationScreen(viewModel: PawsViewModel, bookingId: String) {
    var bookingState by remember { mutableStateOf<GroomingBookingEntity?>(null) }
    var serviceState by remember { mutableStateOf<GroomingServiceEntity?>(null) }
    var shopState by remember { mutableStateOf<ShopEntity?>(null) }
    var petState by remember { mutableStateOf<PetEntity?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(bookingId) {
        viewModel.getGroomingBookingById(bookingId) { bk ->
            bookingState = bk
        }
        bookingState?.let { bk ->
            viewModel.getGroomingServiceById(bk.serviceId) { s -> serviceState = s }
            shopState = viewModel.getShopById(bk.shopId)
            viewModel.getPetsForOwnerFlow(bk.consumerId).collect { plist ->
                petState = plist.find { p -> p.id == bk.petId }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFFDCFCE7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Booking Confirmed!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1E3A8A)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Your grooming appointment is registered successfully.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val booking = bookingState
        if (booking != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Booking ID:", fontSize = 12.sp, color = Color.Gray)
                        Text("#${booking.id}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Shop Name:", fontSize = 12.sp, color = Color.Gray)
                        Text(shopState?.name ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Service Name:", fontSize = 12.sp, color = Color.Gray)
                        Text(serviceState?.variantName ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pet Name:", fontSize = 12.sp, color = Color.Gray)
                        Text(petState?.name ?: "Buddy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Size Category:", fontSize = 12.sp, color = Color.Gray)
                        Text(booking.petSizeCategory.capitalize(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Price:", fontSize = 12.sp, color = Color.Gray)
                        Text("₹${booking.totalPrice.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                data = android.provider.CalendarContract.Events.CONTENT_URI
                                putExtra(android.provider.CalendarContract.Events.TITLE, "Pet Grooming: ${serviceState?.variantName} for ${petState?.name ?: "Buddy"}")
                                putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Shop: ${shopState?.name}. Booking ID: #${booking.id}")
                            }
                            context.startActivity(intent)
                        } catch(e: Exception) {
                            Toast.makeText(context, "Calendar application not found.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add to Calendar")
                }
                
                Button(
                    onClick = {
                        viewModel.clearHistoryAndNavigate(Screen.Home)
                        viewModel.navigateTo(Screen.MyGroomingBookings)
                    },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                ) {
                    Text("View My Bookings", color = Color.White)
                }
            }
        }
    }
}

// --- SCREEN D: MY GROOMING BOOKINGS (CONSUMER) ---
@Composable
fun MyGroomingBookingsScreen(viewModel: PawsViewModel) {
    val bookings by viewModel.myGroomingBookings.collectAsState(initial = emptyList())
    var selectedTab by remember { mutableStateOf(0) } // 0 = Upcoming, 1 = Past
    val context = androidx.compose.ui.platform.LocalContext.current

    val (upcoming, past) = remember(bookings) {
        bookings.partition {
            it.status == "pending" || it.status == "confirmed" || it.status == "in_progress"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "My Grooming Bookings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE2E8F0))
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selectedTab == 0) Color.White else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Upcoming", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selectedTab == 1) Color.White else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Past History", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
            }
        }

        val displayList = if (selectedTab == 0) upcoming else past

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(displayList) { booking ->
                var serviceState by remember { mutableStateOf<GroomingServiceEntity?>(null) }
                var shopState by remember { mutableStateOf<ShopEntity?>(null) }
                var petState by remember { mutableStateOf<PetEntity?>(null) }
                
                LaunchedEffect(booking.id) {
                    viewModel.getGroomingServiceById(booking.serviceId) { s -> serviceState = s }
                    shopState = viewModel.getShopById(booking.shopId)
                    viewModel.getPetsForOwnerFlow(booking.consumerId).collect { plist ->
                        petState = plist.find { p -> p.id == booking.petId }
                    }
                }
                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = serviceState?.variantName ?: "Grooming Service",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            val statusBg = when (booking.status) {
                                "pending" -> Color(0xFFFFE4E6)
                                "confirmed" -> Color(0xFFDCFCE7)
                                "in_progress" -> Color(0xFFE0F2FE)
                                "completed" -> Color(0xFFF1F5F9)
                                else -> Color(0xFFFEE2E2)
                            }
                            val statusText = when (booking.status) {
                                "pending" -> Color(0xFFE11D48)
                                "confirmed" -> Color(0xFF16A34A)
                                "in_progress" -> Color(0xFF0284C7)
                                "completed" -> Color(0xFF475569)
                                else -> Color(0xFFDC2626)
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = booking.status.capitalize(),
                                    color = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "Shop: ${shopState?.name ?: ""}", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(text = "Pet Name: ${petState?.name ?: "Buddy"}", fontSize = 12.sp, color = Color(0xFF64748B))
                        
                        val dateFriendly = try {
                            val inF = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            val outF = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
                            outF.format(inF.parse(booking.slotId.split("_").getOrNull(1) ?: booking.slotId))
                        } catch(e: Exception) { booking.slotId }
                        
                        val timeVal = booking.slotId.split("_").getOrNull(2) ?: ""
                        
                        Text(text = "Date/Time: $dateFriendly at $timeVal", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(text = "Price: ₹${booking.totalPrice.toInt()}", fontSize = 12.sp, color = Color(0xFF64748B))
                        
                        val cancelEligible = remember(booking) {
                            try {
                                val slotDate = booking.slotId.split("_").getOrNull(1) ?: ""
                                val slotTime = booking.slotId.split("_").getOrNull(2) ?: ""
                                val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                                val slotDateTime = format.parse("$slotDate $slotTime")
                                val diff = (slotDateTime?.time ?: 0L) - System.currentTimeMillis()
                                (booking.status == "pending" || booking.status == "confirmed") && diff > (2 * 60 * 60 * 1000)
                            } catch(e: Exception) {
                                false
                            }
                        }
                        
                        if (cancelEligible && selectedTab == 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.cancelGroomingBooking(booking.id) {
                                        Toast.makeText(context, "Booking cancelled successfully.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel Booking", color = Color.White)
                            }
                        }
                    }
                }
            }
            if (displayList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No grooming bookings found.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// --- SCREEN E: MERCHANT SERVICES MANAGER ---
@Composable
fun MerchantGroomingServicesScreen(viewModel: PawsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val merchantShop by viewModel.merchantShop.collectAsState()
    val allServices by viewModel.getAllGroomingServicesForShopFlow(merchantShop?.id ?: "").collectAsState(initial = emptyList())

    val allTaxonomy = listOf(
        Pair("bath", "Basic Bath"),
        Pair("bath", "Medicated Bath"),
        Pair("bath", "De-shedding Bath"),
        Pair("bath", "Flea & Tick Bath"),
        Pair("haircut", "Breed-Standard Cut"),
        Pair("haircut", "Puppy Cut"),
        Pair("haircut", "Lion Cut"),
        Pair("haircut", "Summer Cut"),
        Pair("haircut", "Paw & Face Trim Only"),
        Pair("bath_and_haircut", "Full Groom Package"),
        Pair("nail_trim", "Nail Trim"),
        Pair("ear_cleaning", "Ear Cleaning")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Grooming Service Manager",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(allTaxonomy) { (serviceType, variantName) ->
                val matchingServices = allServices.filter { it.variantName == variantName && it.serviceType == serviceType }
                val isActive = matchingServices.any { it.isActive }
                
                var offered by remember(matchingServices) { mutableStateOf(isActive) }
                
                // Form states
                val firstMatch = matchingServices.firstOrNull()
                var description by remember(matchingServices) { mutableStateOf(firstMatch?.description ?: "") }
                var durationStr by remember(matchingServices) { mutableStateOf(firstMatch?.durationMinutes?.toString() ?: "45") }
                
                var priceSmall by remember(matchingServices) { mutableStateOf(matchingServices.find { it.petSizeCategory == "small" }?.price?.toInt()?.toString() ?: "") }
                var priceMedium by remember(matchingServices) { mutableStateOf(matchingServices.find { it.petSizeCategory == "medium" }?.price?.toInt()?.toString() ?: "") }
                var priceLarge by remember(matchingServices) { mutableStateOf(matchingServices.find { it.petSizeCategory == "large" }?.price?.toInt()?.toString() ?: "") }
                
                // Images state (minimum 1, maximum 5)
                val initialImages = remember(matchingServices) {
                    firstMatch?.imageUrls ?: listOf("https://images.unsplash.com/photo-1516734212186-a967f81ad0d7?w=400")
                }
                val imageUrls = remember(matchingServices) { mutableStateListOf<String>().apply { addAll(initialImages) } }
                var newUrlInput by remember { mutableStateOf("") }
                
                // Combo specific states
                var selectedBathVariant by remember { mutableStateOf("") }
                var selectedHaircutVariant by remember { mutableStateOf("") }
                var discountStr by remember { mutableStateOf("100") }
                
                // Populate initial combo choices
                LaunchedEffect(matchingServices) {
                    if (serviceType == "bath_and_haircut") {
                        val desc = firstMatch?.description ?: ""
                        // simple parsing e.g. "Combo: Basic Bath + Puppy Cut"
                        val parts = desc.replace("Combo: ", "").split(" + ")
                        selectedBathVariant = parts.getOrNull(0) ?: "Basic Bath"
                        selectedHaircutVariant = parts.getOrNull(1) ?: "Puppy Cut"
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(variantName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                                Text(serviceType.uppercase(), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            Switch(
                                checked = offered,
                                onCheckedChange = { offered = it }
                            )
                        }

                        if (offered) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Description
                            Text("Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = { Text("Write brief service description") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            // Duration
                            Text("Duration (Minutes)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = durationStr,
                                onValueChange = { durationStr = it.filter { c -> c.isDigit() } },
                                placeholder = { Text("Duration e.g. 45") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // PRICING INPUTS (Combo vs Regular)
                            if (serviceType == "bath_and_haircut") {
                                Text("Configure Combo Package Parts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                val activeBaths = allServices.filter { it.serviceType == "bath" && it.isActive }.map { it.variantName }.distinct()
                                val activeHaircuts = allServices.filter { it.serviceType == "haircut" && it.isActive }.map { it.variantName }.distinct()
                                
                                Text("Select Bath Variant Component:", fontSize = 10.sp, color = Color.Gray)
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    (if(activeBaths.isEmpty()) listOf("Basic Bath") else activeBaths).forEach { name ->
                                        FilterChip(
                                            selected = selectedBathVariant == name || (selectedBathVariant.isEmpty() && name == "Basic Bath"),
                                            onClick = { selectedBathVariant = name },
                                            label = { Text(name, fontSize = 10.sp) }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Select Haircut Variant Component:", fontSize = 10.sp, color = Color.Gray)
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    (if(activeHaircuts.isEmpty()) listOf("Puppy Cut") else activeHaircuts).forEach { name ->
                                        FilterChip(
                                            selected = selectedHaircutVariant == name || (selectedHaircutVariant.isEmpty() && name == "Puppy Cut"),
                                            onClick = { selectedHaircutVariant = name },
                                            label = { Text(name, fontSize = 10.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Combo Package Discount (₹)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = discountStr,
                                    onValueChange = { discountStr = it.filter { c -> c.isDigit() } },
                                    placeholder = { Text("Discount amount e.g. 100") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                
                                // Calculate combo price dynamically
                                val discountVal = discountStr.toDoubleOrNull() ?: 0.0
                                val bathName = selectedBathVariant.ifEmpty { "Basic Bath" }
                                val haircutName = selectedHaircutVariant.ifEmpty { "Puppy Cut" }
                                
                                val bathSmall = allServices.find { it.variantName == bathName && it.petSizeCategory == "small" && it.isActive }?.price ?: 0.0
                                val bathMed = allServices.find { it.variantName == bathName && it.petSizeCategory == "medium" && it.isActive }?.price ?: 0.0
                                val bathLarge = allServices.find { it.variantName == bathName && it.petSizeCategory == "large" && it.isActive }?.price ?: 0.0
                                
                                val hairSmall = allServices.find { it.variantName == haircutName && it.petSizeCategory == "small" && it.isActive }?.price ?: 0.0
                                val hairMed = allServices.find { it.variantName == haircutName && it.petSizeCategory == "medium" && it.isActive }?.price ?: 0.0
                                val hairLarge = allServices.find { it.variantName == haircutName && it.petSizeCategory == "large" && it.isActive }?.price ?: 0.0
                                
                                val calcSmall = if (bathSmall > 0 && hairSmall > 0) Math.max(0.0, bathSmall + hairSmall - discountVal) else 0.0
                                val calcMed = if (bathMed > 0 && hairMed > 0) Math.max(0.0, bathMed + hairMed - discountVal) else 0.0
                                val calcLarge = if (bathLarge > 0 && hairLarge > 0) Math.max(0.0, bathLarge + hairLarge - discountVal) else 0.0
                                
                                priceSmall = if (calcSmall > 0) calcSmall.toInt().toString() else ""
                                priceMedium = if (calcMed > 0) calcMed.toInt().toString() else ""
                                priceLarge = if (calcLarge > 0) calcLarge.toInt().toString() else ""
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Calculated Pricing Tiers (Read-Only):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Small", fontSize = 10.sp, color = Color.Gray)
                                        Text(if(calcSmall > 0) "₹${calcSmall.toInt()}" else "N/A", fontWeight = FontWeight.Bold)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Medium", fontSize = 10.sp, color = Color.Gray)
                                        Text(if(calcMed > 0) "₹${calcMed.toInt()}" else "N/A", fontWeight = FontWeight.Bold)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Large", fontSize = 10.sp, color = Color.Gray)
                                        Text(if(calcLarge > 0) "₹${calcLarge.toInt()}" else "N/A", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text("Pricing Tiers (₹) - Enter at least 1 size to activate", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = priceSmall,
                                        onValueChange = { priceSmall = it.filter { c -> c.isDigit() } },
                                        placeholder = { Text("Small") },
                                        label = { Text("Small") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = priceMedium,
                                        onValueChange = { priceMedium = it.filter { c -> c.isDigit() } },
                                        placeholder = { Text("Medium") },
                                        label = { Text("Medium") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    OutlinedTextField(
                                        value = priceLarge,
                                        onValueChange = { priceLarge = it.filter { c -> c.isDigit() } },
                                        placeholder = { Text("Large") },
                                        label = { Text("Large") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Image Upload management
                            Text("Photos (1 - 5 images)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                imageUrls.forEachIndexed { index, url ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(url),
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Text(
                                            text = url.take(30) + "...",
                                            fontSize = 11.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        // Move Up
                                        if (index > 0) {
                                            IconButton(
                                                onClick = {
                                                    val temp = imageUrls[index]
                                                    imageUrls[index] = imageUrls[index - 1]
                                                    imageUrls[index - 1] = temp
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        // Move Down
                                        if (index < imageUrls.size - 1) {
                                            IconButton(
                                                onClick = {
                                                    val temp = imageUrls[index]
                                                    imageUrls[index] = imageUrls[index + 1]
                                                    imageUrls[index + 1] = temp
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        // Remove Button
                                        IconButton(
                                            onClick = { imageUrls.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                if (imageUrls.size < 5) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newUrlInput,
                                            onValueChange = { newUrlInput = it },
                                            placeholder = { Text("Paste new Image URL") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Button(
                                            onClick = {
                                                if (newUrlInput.trim().isNotEmpty()) {
                                                    imageUrls.add(newUrlInput.trim())
                                                    newUrlInput = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Add")
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Enforce 1 photo rule
                            val photoError = imageUrls.isEmpty()
                            if (photoError) {
                                Text(
                                    text = "Add at least 1 photo to activate.",
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    // Save service for active prices
                                    val finalDesc = if (serviceType == "bath_and_haircut") {
                                        "Combo: ${selectedBathVariant.ifEmpty { "Basic Bath" }} + ${selectedHaircutVariant.ifEmpty { "Puppy Cut" }}"
                                    } else description
                                    
                                    val parsedDuration = durationStr.toIntOrNull() ?: 45
                                    
                                    // Delete all first for this variant, then save new active ones
                                    matchingServices.forEach { s ->
                                        viewModel.deleteGroomingService(s.id)
                                    }
                                    
                                    val pSmall = priceSmall.toDoubleOrNull() ?: 0.0
                                    if (pSmall > 0) {
                                        viewModel.saveGroomingService(serviceType, variantName, finalDesc, "small", pSmall, parsedDuration, imageUrls.toList(), true)
                                    }
                                    val pMed = priceMedium.toDoubleOrNull() ?: 0.0
                                    if (pMed > 0) {
                                        viewModel.saveGroomingService(serviceType, variantName, finalDesc, "medium", pMed, parsedDuration, imageUrls.toList(), true)
                                    }
                                    val pLarge = priceLarge.toDoubleOrNull() ?: 0.0
                                    if (pLarge > 0) {
                                        viewModel.saveGroomingService(serviceType, variantName, finalDesc, "large", pLarge, parsedDuration, imageUrls.toList(), true)
                                    }
                                    
                                    Toast.makeText(context, "${variantName} saved successfully!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !photoError,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
                            ) {
                                Text("Save Variant Settings", color = Color.White)
                            }
                        } else {
                            // If toggled off, make sure we deactivate them in database
                            if (matchingServices.any { it.isActive }) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        matchingServices.forEach { s ->
                                            viewModel.updateGroomingService(s.copy(isActive = false))
                                        }
                                        Toast.makeText(context, "${variantName} toggled inactive", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)
                                ) {
                                    Text("Apply Deactivation", color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SCREEN F: MERCHANT SLOT MANAGER ---
@Composable
fun MerchantGroomingSlotsScreen(viewModel: PawsViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val merchantShop by viewModel.merchantShop.collectAsState()
    
    // Calendar 30 days
    val dates = remember {
        val list = mutableListOf<String>()
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        for (i in 0 until 30) {
            list.add(format.format(cal.time))
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        list
    }
    
    var selectedDate by remember { mutableStateOf(dates.firstOrNull() ?: "") }
    
    // Slots for the selected date
    val shopId = merchantShop?.id ?: ""
    val slots by viewModel.getGroomingSlotsForShopAndDateFlow(shopId, selectedDate).collectAsState(initial = emptyList())
    
    // Pre-generate slots on date change
    LaunchedEffect(shopId, selectedDate) {
        if (shopId.isNotEmpty() && selectedDate.isNotEmpty()) {
            viewModel.getOrGenerateSlotsForDate(shopId, selectedDate) { }
        }
    }

    // Bulk edit inputs
    var showBulkDialog by remember { mutableStateOf(false) }
    var bulkStart by remember { mutableStateOf(dates.first()) }
    var bulkEnd by remember { mutableStateOf(dates.last()) }
    val bulkDays = remember { mutableStateListOf(2, 3, 4, 5, 6, 7) } // Monday-Saturday (2-7 in Calendar)
    var bulkCapacity by remember { mutableStateOf("2") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Grooming Slot Manager",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Bulk Edit trigger
            Button(
                onClick = { showBulkDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A))
            ) {
                Text("Bulk-Edit Slot Capacity", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date horizontal strip
            Text("Select Date to View Slots", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
            Spacer(modifier = Modifier.height(6.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(64.dp)
            ) {
                items(dates) { date ->
                    val isSel = selectedDate == date
                    val bg = if (isSel) Color(0xFF1E3A8A) else Color.White
                    val textCol = if (isSel) Color.White else Color(0xFF334155)
                    val borderCol = if (isSel) Color.Transparent else Color(0xFFE2E8F0)
                    
                    val friendlyDate = try {
                        val inF = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        val outF = java.text.SimpleDateFormat("EEE\ndd", java.util.Locale.US)
                        outF.format(inF.parse(date))
                    } catch(e: Exception) { date }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg)
                            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
                            .clickable { selectedDate = date }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = friendlyDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textCol,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Time Slots List
            Text("Time Slots for $selectedDate", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEach { slot ->
                    val isBlocked = slot.isBlocked
                    val isFull = slot.bookedCount >= slot.capacity
                    
                    val colorSt = when {
                        isBlocked -> Color(0xFF94A3B8) // Grey
                        isFull -> Color(0xFFEF4444) // Red
                        slot.bookedCount > 0 -> Color(0xFFEAB308) // Yellow
                        else -> Color(0xFF22C55E) // Green
                    }

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(colorSt, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(slot.slotTime, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Capacity: ${slot.capacity} • Booked: ${slot.bookedCount}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            
                            // Block/Unblock toggle button
                            Button(
                                onClick = { viewModel.toggleSlotBlocked(slot) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (slot.isBlocked) Color(0xFF22C55E) else Color(0xFF94A3B8)
                                )
                            ) {
                                Text(if (slot.isBlocked) "Unblock" else "Block", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }
                if (slots.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Loading slots...", color = Color.Gray)
                    }
                }
            }
        }
    }

    // Bulk Dialog
    if (showBulkDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDialog = false },
            title = { Text("Bulk-Edit Capacity") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Date Range:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bulkStart,
                            onValueChange = { bulkStart = it },
                            label = { Text("Start (YYYY-MM-DD)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bulkEnd,
                            onValueChange = { bulkEnd = it },
                            label = { Text("End (YYYY-MM-DD)", fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Select Days:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    // Sun = 1, Mon = 2, ...
                    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (d in 1..7) {
                            val active = bulkDays.contains(d)
                            FilterChip(
                                selected = active,
                                onClick = {
                                    if (active) bulkDays.remove(d) else bulkDays.add(d)
                                },
                                label = { Text(dayNames[d - 1], fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = bulkCapacity,
                        onValueChange = { bulkCapacity = it.filter { c -> c.isDigit() } },
                        label = { Text("Capacity (e.g. 2)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cap = bulkCapacity.toIntOrNull() ?: 1
                        viewModel.bulkEditSlotCapacity(shopId, bulkStart, bulkEnd, bulkDays.toList(), cap) {
                            showBulkDialog = false
                            Toast.makeText(context, "Bulk capacity updated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Apply Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- SCREEN G: MERCHANT BOOKINGS QUEUE ---
@Composable
fun MerchantGroomingQueueScreen(viewModel: PawsViewModel) {
    val bookings by viewModel.merchantGroomingBookings.collectAsState(initial = emptyList())
    var selectedBookingDetail by remember { mutableStateOf<GroomingBookingEntity?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    var showRescheduleWarningBooking by remember { mutableStateOf<GroomingBookingEntity?>(null) }
    var showProposeRescheduleBooking by remember { mutableStateOf<GroomingBookingEntity?>(null) }
    var proposeRescheduleDate by remember { mutableStateOf("") }
    var proposeRescheduleTime by remember { mutableStateOf("") }

    // Sort queue by Date & Time
    val sortedQueue = remember(bookings) {
        bookings.sortedWith(compareBy<GroomingBookingEntity> { it.slotId.split("_").getOrNull(1) ?: "" }
            .thenBy { it.slotId.split("_").getOrNull(2) ?: "" })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Grooming Bookings Queue",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(sortedQueue) { booking ->
                var serviceState by remember { mutableStateOf<GroomingServiceEntity?>(null) }
                var ownerState by remember { mutableStateOf<ProfileEntity?>(null) }
                var petState by remember { mutableStateOf<PetEntity?>(null) }

                LaunchedEffect(booking.id) {
                    viewModel.getGroomingServiceById(booking.serviceId) { s -> serviceState = s }
                    viewModel.getProfileById(booking.consumerId) { owner -> ownerState = owner }
                    viewModel.getPetsForOwnerFlow(booking.consumerId).collect { plist ->
                        petState = plist.find { p -> p.id == booking.petId }
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth().clickable { selectedBookingDetail = booking }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "${serviceState?.variantName ?: "Grooming"} • ${booking.petSizeCategory.uppercase()}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            val statusBg = when (booking.status) {
                                "pending" -> Color(0xFFFFE4E6)
                                "confirmed" -> Color(0xFFDCFCE7)
                                "in_progress" -> Color(0xFFE0F2FE)
                                "completed" -> Color(0xFFF1F5F9)
                                else -> Color(0xFFFEE2E2)
                            }
                            val statusText = when (booking.status) {
                                "pending" -> Color(0xFFE11D48)
                                "confirmed" -> Color(0xFF16A34A)
                                "in_progress" -> Color(0xFF0284C7)
                                "completed" -> Color(0xFF475569)
                                else -> Color(0xFFDC2626)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(statusBg)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = booking.status.capitalize(),
                                    color = statusText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Pet: ${petState?.name ?: "Buddy"} (${petState?.breed ?: ""})", fontSize = 12.sp, color = Color.Gray)
                        
                        val dateFriendly = try {
                            val inF = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            val outF = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US)
                            outF.format(inF.parse(booking.slotId.split("_").getOrNull(1) ?: booking.slotId))
                        } catch(e: Exception) { booking.slotId }
                        
                        val timeVal = booking.slotId.split("_").getOrNull(2) ?: ""
                        
                        Text("Scheduled: $dateFriendly at $timeVal", fontSize = 12.sp, color = Color.Gray)
                        
                        if (booking.specialInstructions?.isNotEmpty() == true) {
                            Text(
                                text = "Notes: \"${booking.specialInstructions}\"",
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = Color(0xFF1D4ED8),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status Progression Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (booking.status) {
                                "pending" -> {
                                    Button(
                                        onClick = { viewModel.updateGroomingBookingStatus(booking.id, "confirmed") },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Confirm", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                                "confirmed" -> {
                                    Button(
                                        onClick = { viewModel.updateGroomingBookingStatus(booking.id, "in_progress") },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Start Groom", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                                "in_progress" -> {
                                    Button(
                                        onClick = { viewModel.updateGroomingBookingStatus(booking.id, "completed") },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Complete", color = Color.White, fontSize = 11.sp)
                                    }
                                }
                            }
                            
                            if (booking.status != "completed" && booking.status != "cancelled" && booking.status != "no_show" && booking.status != "reschedule_pending") {
                                OutlinedButton(
                                    onClick = { viewModel.updateGroomingBookingStatus(booking.id, "no_show") },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("No-Show", color = Color(0xFFEAB308), fontSize = 11.sp)
                                }
                                Button(
                                    onClick = { showRescheduleWarningBooking = booking },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEA619)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Reschedule", color = Color.White, fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.declineGroomingReschedule(booking) {
                                            Toast.makeText(context, "Transaction refunded to customer online", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    Text("Refund & Cancel", color = Color.Red, fontSize = 9.sp)
                                }
                            }
                        }
                    }
                }
            }
            if (sortedQueue.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No direct grooming bookings in the queue.", color = Color.Gray)
                    }
                }
            }
        }
    }

    // Detail Dialog
    val detail = selectedBookingDetail
    if (detail != null) {
        var serviceState by remember { mutableStateOf<GroomingServiceEntity?>(null) }
        var ownerState by remember { mutableStateOf<ProfileEntity?>(null) }
        var petState by remember { mutableStateOf<PetEntity?>(null) }

        LaunchedEffect(detail.id) {
            viewModel.getGroomingServiceById(detail.serviceId) { s -> serviceState = s }
            viewModel.getProfileById(detail.consumerId) { owner -> ownerState = owner }
            viewModel.getPetsForOwnerFlow(detail.consumerId).collect { plist ->
                petState = plist.find { p -> p.id == detail.petId }
            }
        }

        AlertDialog(
            onDismissRequest = { selectedBookingDetail = null },
            title = { Text("Booking Detail Information") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Booking ID: #${detail.id}", fontWeight = FontWeight.Bold)
                    Text("Service: ${serviceState?.variantName ?: ""}")
                    Text("Pet: ${petState?.name ?: "Buddy"} (${petState?.breed ?: ""}, Age: ${petState?.ageText ?: ""}, Weight: ${petState?.weight ?: ""})")
                    Text("Size Category: ${detail.petSizeCategory.uppercase()}")
                    Text("Special Allergies: ${petState?.allergies ?: "None"}", color = Color.Red)
                    Text("Total Paid: ₹${detail.totalPrice.toInt()}")
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text("Owner Details:", fontWeight = FontWeight.Bold)
                    Text("Name: ${ownerState?.fullName ?: ""}")
                    Text("Phone: ${ownerState?.phone ?: ""}")
                    Text("Email: ${ownerState?.email ?: ""}")
                }
            },
            confirmButton = {
                Button(onClick = { selectedBookingDetail = null }) {
                    Text("Close Details")
                }
            }
        )
    }

    // Rescheduling warning and proposing dialogs
    showRescheduleWarningBooking?.let { booking ->
        var ownerPhone by remember { mutableStateOf("") }
        LaunchedEffect(booking.id) {
            viewModel.getProfileById(booking.consumerId) { profile ->
                ownerPhone = profile?.phone ?: ""
            }
        }
        AlertDialog(
            onDismissRequest = { showRescheduleWarningBooking = null },
            title = { Text("⚠️ Contact Customer First") },
            text = {
                Text("Please call the customer at +91 $ownerPhone first to confirm they agree to reschedule.\n\nIf they agree, click Proceed. Otherwise, click Refund & Cancel to void the booking.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProposeRescheduleBooking = booking
                        proposeRescheduleDate = booking.slotId.split("_").getOrNull(1) ?: ""
                        proposeRescheduleTime = booking.slotId.split("_").getOrNull(2) ?: ""
                        showRescheduleWarningBooking = null
                    }
                ) {
                    Text("Proceed to Reschedule")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.declineGroomingReschedule(booking) {
                            Toast.makeText(context, "Transaction refunded to customer online", Toast.LENGTH_SHORT).show()
                        }
                        showRescheduleWarningBooking = null
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Refund & Cancel")
                }
            }
        )
    }

    showProposeRescheduleBooking?.let { booking ->
        AlertDialog(
            onDismissRequest = { showProposeRescheduleBooking = null },
            title = { Text("Propose Reschedule Slot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the new proposed date and time slot for grooming.")
                    OutlinedTextField(
                        value = proposeRescheduleDate,
                        onValueChange = { proposeRescheduleDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = proposeRescheduleTime,
                        onValueChange = { proposeRescheduleTime = it },
                        label = { Text("Time (e.g. 10:00 AM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!proposeRescheduleDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                            Toast.makeText(context, "Please enter a valid YYYY-MM-DD date", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (proposeRescheduleTime.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a time slot", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.proposeGroomingReschedule(booking, proposeRescheduleDate, proposeRescheduleTime) { success ->
                            if (success) {
                                Toast.makeText(context, "Reschedule proposal sent to customer!", Toast.LENGTH_SHORT).show()
                                showProposeRescheduleBooking = null
                            }
                        }
                    }
                ) {
                    Text("Send Proposal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProposeRescheduleBooking = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}


