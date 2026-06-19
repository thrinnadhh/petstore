package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.AppointmentEntity
import com.example.data.DoctorEntity
import com.example.data.DoctorSlotEntity
import com.example.data.PaymentManager
import com.example.data.PetEntity
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DoctorSlotPickerScreen(
    viewModel: PawsViewModel,
    shopId: String,
    doctorId: String,
    serviceId: String,
    price: Double
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val pets by viewModel.activePets.collectAsState()
    
    var selectedPet by remember { mutableStateOf<PetEntity?>(null) }
    var petExpanded by remember { mutableStateOf(false) }
    var petInputName by remember { mutableStateOf("") }
    
    val concernList = remember {
        listOf(
            "Fever",
            "Ticks",
            "Itching",
            "Skin Issues",
            "Fracture",
            "Tick Fever"
        )
    }
    var selectedConcern by remember { mutableStateOf("Fever") }
    var concernExpanded by remember { mutableStateOf(false) }
    
    var doctorState by remember { mutableStateOf<DoctorEntity?>(null) }
    
    LaunchedEffect(doctorId) {
        viewModel.getDoctorById(doctorId) { doc ->
            doctorState = doc
        }
    }
    
    val dateList = remember {
        val list = mutableListOf<String>()
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val cal = java.util.Calendar.getInstance()
        for (i in 0 until 7) {
            list.add(format.format(cal.time))
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        list
    }
    
    var selectedDate by remember { mutableStateOf(dateList.firstOrNull() ?: "") }
    
    LaunchedEffect(shopId, doctorId, selectedDate) {
        if (selectedDate.isNotEmpty()) {
            viewModel.getOrGenerateDoctorSlotsForDate(shopId, doctorId, selectedDate) {}
        }
    }
    
    val slots by viewModel.getDoctorSlotsFlow(shopId, doctorId, selectedDate).collectAsState(initial = emptyList())
    var selectedSlot by remember { mutableStateOf<DoctorSlotEntity?>(null) }
    
    LaunchedEffect(selectedDate) {
        selectedSlot = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Consultation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            doctorState?.let { doc ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(2.dp, Color(0xFFD1FAE5))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(doc.photoUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(2.dp, Color(0xFF10B981), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(doc.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF065F46))
                            Text(doc.specialization, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF047857))
                            Text(doc.qualification, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Select Consultation Date", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(dateList) { dateStr ->
                    val isSelected = selectedDate == dateStr
                    val parsedDate = remember(dateStr) {
                        try {
                            val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            parser.parse(dateStr)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    val dayName = remember(parsedDate) {
                        if (parsedDate != null) {
                            java.text.SimpleDateFormat("EEE", java.util.Locale.US).format(parsedDate)
                        } else ""
                    }
                    val dayNumber = remember(parsedDate) {
                        if (parsedDate != null) {
                            java.text.SimpleDateFormat("d", java.util.Locale.US).format(parsedDate)
                        } else ""
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF10B981) else Color(0xFFF1F5F9))
                            .clickable { selectedDate = dateStr }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(dayName.uppercase(), fontSize = 10.sp, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                            Text(dayNumber, fontSize = 18.sp, color = if (isSelected) Color.White else Color.Black, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Select Available Slot", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))

            if (slots.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No available consultation slots for this date.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    slots.forEach { slot ->
                        val isBookedOut = slot.bookedCount >= slot.capacity
                        val isBlocked = slot.isBlocked
                        val isDisabled = isBookedOut || isBlocked
                        val isSelected = selectedSlot?.id == slot.id

                        val bg = when {
                            isSelected -> Color(0xFF10B981)
                            isDisabled -> Color(0xFFE2E8F0)
                            else -> Color(0xFFECFEFF)
                        }
                        val fg = when {
                            isSelected -> Color.White
                            isDisabled -> Color.Gray
                            else -> Color(0xFF0369A1)
                        }
                        val border = when {
                            isSelected -> Color.Transparent
                            isDisabled -> Color.Transparent
                            else -> Color(0xFF0EA5E9).copy(alpha = 0.3f)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                                .clickable(enabled = !isDisabled) { selectedSlot = slot }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = slot.slotTime,
                                    color = fg,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isDisabled) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Unavailable",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Select Concern / Reason for Consultation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { concernExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedConcern, color = Color.Black)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                }
                DropdownMenu(
                    expanded = concernExpanded,
                    onDismissRequest = { concernExpanded = false }
                ) {
                    concernList.forEach { concern ->
                        DropdownMenuItem(
                            text = { Text(concern) },
                            onClick = {
                                selectedConcern = concern
                                concernExpanded = false
                            }
                        )
                    }
                }
            }

            val isHighPriority = selectedConcern == "Fracture" || selectedConcern == "Tick Fever"
            if (isHighPriority) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFDC2626))
                    Text(
                        text = "🚨 High Priority! The doctor will be notified immediately to arrange a fast slot.",
                        color = Color(0xFF991B1B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Select or Enter Pet Name", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))

            if (pets.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { petExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedPet?.name ?: "Choose a Registered Pet", color = Color.Black)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                    DropdownMenu(
                        expanded = petExpanded,
                        onDismissRequest = { petExpanded = false }
                    ) {
                        pets.forEach { pet ->
                            DropdownMenuItem(
                                text = { Text("${pet.name} (${pet.breed})") },
                                onClick = {
                                    selectedPet = pet
                                    petInputName = pet.name
                                    petExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = petInputName,
                onValueChange = { petInputName = it },
                label = { Text("Pet Name") },
                placeholder = { Text("e.g. Buddy, Max") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Billing details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Consultation Fee", color = Color.Gray, fontSize = 13.sp)
                        Text("₹$price", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Platform Service Fee", color = Color.Gray, fontSize = 13.sp)
                        Text("₹30.0", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Text("₹${price + 30.0}", fontWeight = FontWeight.Black, color = Color(0xFF10B981), fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (selectedSlot == null) {
                        Toast.makeText(context, "Please select an available consultation slot.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (petInputName.trim().isEmpty()) {
                        Toast.makeText(context, "Please select or enter a pet name.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val currentUserVal = viewModel.currentUser.value
                    val userPhone = currentUserVal?.phone ?: "9876543210"
                    val userEmail = (currentUserVal?.fullName ?: "arjun").replace(" ", "").lowercase() + "@example.com"
                    val grandTotal = price + 30.0

                    PaymentManager.startRazorpayCheckout(
                        context = context,
                        amountInRupees = grandTotal,
                        orderId = "chk_doc_" + UUID.randomUUID().toString().take(6),
                        email = userEmail,
                        phone = userPhone,
                        onSuccess = { payment ->
                            val priorityStr = if (isHighPriority) "High" else "Normal"
                            viewModel.bookDoctorAppointment(
                                shopId = shopId,
                                serviceId = serviceId,
                                serviceName = "Doctor Consultation - ${doctorState?.name ?: "Vet"}",
                                price = grandTotal,
                                date = selectedDate,
                                time = selectedSlot?.slotTime ?: "",
                                petName = petInputName,
                                doctorId = doctorId,
                                slotId = selectedSlot?.id,
                                concern = selectedConcern,
                                priority = priorityStr,
                                onSuccess = {
                                    val notificationTitle = if (isHighPriority) "🚨 High Priority Consultation" else "Appointment Booked"
                                    val notificationMsg = if (isHighPriority) {
                                        "New high priority case ($selectedConcern) booked for ${doctorState?.name ?: "Vet"}! Please review slot or call customer at +91 $userPhone."
                                    } else {
                                        "Consultation booked for ${doctorState?.name ?: "Vet"} on $selectedDate at ${selectedSlot?.slotTime}."
                                    }
                                    com.example.data.NotificationManager.fireInstantNotification(context, notificationTitle, notificationMsg)

                                    Toast.makeText(context, "Consultation Booked Successfully! ID: ${payment.paymentId}", Toast.LENGTH_LONG).show()
                                    viewModel.navigateBack()
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Booking Failed: $err", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        onFailure = { error ->
                            Toast.makeText(context, "Payment Failed: $error", Toast.LENGTH_LONG).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Pay & Book Consultation • ₹${price + 30.0}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MerchantDoctorsScreen(viewModel: PawsViewModel) {
    val context = LocalContext.current
    val merchantShop by viewModel.merchantShop.collectAsState()
    val shop = merchantShop ?: return

    val doctorList by viewModel.getDoctorsForShopFlow(shop.id).collectAsState(initial = emptyList())

    var activeTab by remember { mutableStateOf("doctors") } // "doctors" | "queue"

    var isAddingDoctor by remember { mutableStateOf(false) }
    var docName by remember { mutableStateOf("") }
    var docPhoto by remember { mutableStateOf("") }
    var docQual by remember { mutableStateOf("") }
    var docSpec by remember { mutableStateOf("") }

    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDays by remember { mutableStateOf(setOf("Mon", "Tue", "Wed", "Thu", "Fri")) }

    val standardHourSlots = listOf(
        "09:00 AM", "10:00 AM", "11:00 AM", "12:00 PM",
        "01:00 PM", "02:00 PM", "03:00 PM", "04:00 PM",
        "05:00 PM", "06:00 PM"
    )
    var selectedSlots by remember { mutableStateOf(setOf("09:00 AM", "10:00 AM", "02:00 PM", "03:00 PM")) }

    var editingDoctorAvailability by remember { mutableStateOf<DoctorEntity?>(null) }
    var blockDateInput by remember { mutableStateOf("") }
    val blockedSlotsList = remember { mutableStateListOf<DoctorSlotEntity>() }

    var showRescheduleWarningAppt by remember { mutableStateOf<AppointmentEntity?>(null) }
    var showProposeRescheduleAppt by remember { mutableStateOf<AppointmentEntity?>(null) }
    var proposeApptDate by remember { mutableStateOf("") }
    var proposeApptTime by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consultation & Doctors", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray.copy(alpha = 0.2f))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == "doctors") Color.White else Color.Transparent)
                        .clickable { activeTab = "doctors" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Manage Doctors 🩺", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (activeTab == "doctors") Color(0xFF007D55) else Color.Gray)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activeTab == "queue") Color.White else Color.Transparent)
                        .clickable { activeTab = "queue" }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Appointments Queue 📅", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (activeTab == "queue") Color(0xFF007D55) else Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (activeTab == "doctors") {
                if (isAddingDoctor) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Add New Vet Profile 🩺", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            OutlinedTextField(
                                value = docName,
                                onValueChange = { docName = it },
                                label = { Text("Doctor's Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = docPhoto,
                                onValueChange = { docPhoto = it },
                                label = { Text("Photo URL") },
                                placeholder = { Text("Leave blank for default avatar") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = docQual,
                                onValueChange = { docQual = it },
                                label = { Text("Qualifications (e.g. BVSc, MVSc)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = docSpec,
                                onValueChange = { docSpec = it },
                                label = { Text("Specialization (e.g. Canine Surgery)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Text("Select Working Days", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                allDays.forEach { day ->
                                    val isChecked = selectedDays.contains(day)
                                    FilterChip(
                                        selected = isChecked,
                                        onClick = {
                                            selectedDays = if (isChecked) selectedDays - day else selectedDays + day
                                        },
                                        label = { Text(day) }
                                    )
                                }
                            }

                            Text("Select Default Hour Slots", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                standardHourSlots.forEach { slot ->
                                    val isChecked = selectedSlots.contains(slot)
                                    FilterChip(
                                        selected = isChecked,
                                        onClick = {
                                            selectedSlots = if (isChecked) selectedSlots - slot else selectedSlots + slot
                                        },
                                        label = { Text(slot) }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (docName.trim().isEmpty() || docQual.trim().isEmpty() || docSpec.trim().isEmpty()) {
                                            Toast.makeText(context, "Please enter name, qualification, and specialization.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        viewModel.saveDoctor(
                                            id = null,
                                            shopId = shop.id,
                                            name = docName,
                                            photoUrl = docPhoto,
                                            qualification = docQual,
                                            specialization = docSpec,
                                            workingDays = selectedDays.toList(),
                                            activeSlots = selectedSlots.toList(),
                                            isAvailable = true,
                                            onResult = {
                                                Toast.makeText(context, "Doctor Profile Saved!", Toast.LENGTH_SHORT).show()
                                                isAddingDoctor = false
                                                docName = ""
                                                docPhoto = ""
                                                docQual = ""
                                                docSpec = ""
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save Doctor")
                                }

                                OutlinedButton(
                                    onClick = { isAddingDoctor = false },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                } else {
                    Button(
                        onClick = { isAddingDoctor = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Add New Vet Profile")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Text("Registered Vet Doctors (${doctorList.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))

                if (doctorList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No doctors registered. Please add a doctor profile.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    doctorList.forEach { doc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = rememberAsyncImagePainter(doc.photoUrl.ifEmpty { "https://images.unsplash.com/photo-1584132967334-10e028bd69f7?w=200" }),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, Color(0xFF10B981), CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doc.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text(doc.specialization, fontSize = 12.sp, color = Color(0xFF007D55))
                                        Text(doc.qualification, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    IconButton(onClick = { viewModel.deleteDoctor(doc.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Working: ${doc.workingDays.joinToString(", ")}", fontSize = 11.sp, color = Color.Gray)
                                Text("Default Slots: ${doc.activeSlots.joinToString(", ")}", fontSize = 11.sp, color = Color.Gray)

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            editingDoctorAvailability = if (editingDoctorAvailability?.id == doc.id) null else doc
                                            blockDateInput = ""
                                            blockedSlotsList.clear()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(if (editingDoctorAvailability?.id == doc.id) "Close Editor" else "Manage Availability", fontSize = 11.sp)
                                    }
                                }

                                if (editingDoctorAvailability?.id == doc.id) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text("Manage Time Slots (Toggle Default Active Times)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        standardHourSlots.forEach { slotTime ->
                                            val isActive = doc.activeSlots.contains(slotTime)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isActive) Color(0xFFE8F5E9) else Color(0xFFF1F5F9))
                                                    .border(1.dp, if (isActive) Color(0xFF81C784) else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        val newSlots = if (isActive) doc.activeSlots - slotTime else doc.activeSlots + slotTime
                                                        viewModel.saveDoctor(
                                                            id = doc.id,
                                                            shopId = doc.shopId,
                                                            name = doc.name,
                                                            photoUrl = doc.photoUrl,
                                                            qualification = doc.qualification,
                                                            specialization = doc.specialization,
                                                            workingDays = doc.workingDays,
                                                            activeSlots = newSlots,
                                                            isAvailable = doc.isAvailable
                                                        )
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(slotTime, fontSize = 11.sp, color = if (isActive) Color(0xFF2E7D32) else Color.Black)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Block Specific Holiday/Date", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = blockDateInput,
                                            onValueChange = { blockDateInput = it },
                                            label = { Text("Date (YYYY-MM-DD)") },
                                            placeholder = { Text("e.g. 2026-10-24") },
                                            modifier = Modifier.weight(1f).height(56.dp),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                if (!blockDateInput.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                                                    Toast.makeText(context, "Enter a valid YYYY-MM-DD date.", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                viewModel.getOrGenerateDoctorSlotsForDate(doc.shopId, doc.id, blockDateInput) { slots ->
                                                    blockedSlotsList.clear()
                                                    blockedSlotsList.addAll(slots)
                                                }
                                            },
                                            modifier = Modifier.height(56.dp)
                                        ) {
                                            Text("Fetch Slots")
                                        }
                                    }

                                    if (blockedSlotsList.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Slots for date $blockDateInput:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            blockedSlotsList.forEachIndexed { index, slot ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(Color.White)
                                                        .border(1.dp, Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(slot.slotTime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(if (slot.isBlocked) "Blocked" else "Active", fontSize = 11.sp, color = if (slot.isBlocked) Color.Red else Color(0xFF2E7D32))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Switch(
                                                            checked = slot.isBlocked,
                                                            onCheckedChange = { checked ->
                                                                viewModel.toggleDoctorSlotBlocked(slot)
                                                                blockedSlotsList[index] = slot.copy(isBlocked = checked)
                                                            }
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
            } else {
                // Consultation Bookings Queue
                val appointments by viewModel.getAppointmentsForShopFlow(shop.id).collectAsState(initial = emptyList())
                val sortedAppts = remember(appointments) {
                    appointments.sortedWith(compareBy<AppointmentEntity> { it.appointmentDate }.thenBy { it.appointmentTime })
                }
                
                Text("Doctor Consultations Queue (${sortedAppts.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))

                if (sortedAppts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No doctor appointments booked at this clinic yet.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    sortedAppts.forEach { appt ->
                        var doctorName by remember { mutableStateOf("Vet Doctor") }
                        var ownerPhone by remember { mutableStateOf("") }
                        var ownerName by remember { mutableStateOf("") }
                        
                        LaunchedEffect(appt.id) {
                            appt.doctorId?.let { docId ->
                                viewModel.getDoctorById(docId) { doc -> doctorName = doc?.name ?: "Vet Doctor" }
                            }
                            viewModel.getProfileById(appt.consumerId) { p ->
                                ownerPhone = p?.phone ?: ""
                                ownerName = p?.fullName ?: ""
                            }
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Pet: ${appt.petName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val statusColor = when (appt.status) {
                                        "pending" -> Color(0xFFFFE4E6)
                                        "confirmed" -> Color(0xFFDCFCE7)
                                        "completed" -> Color(0xFFF1F5F9)
                                        else -> Color(0xFFFEE2E2)
                                    }
                                    val statusText = when (appt.status) {
                                        "pending" -> Color(0xFFE11D48)
                                        "confirmed" -> Color(0xFF16A34A)
                                        "completed" -> Color(0xFF475569)
                                        else -> Color(0xFFDC2626)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(statusColor)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(appt.status.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statusText)
                                    }
                                }
                                
                                Text("Doctor: $doctorName", fontSize = 12.sp, color = Color(0xFF007D55), fontWeight = FontWeight.Bold)
                                Text("Customer: $ownerName ($ownerPhone)", fontSize = 12.sp, color = Color.Gray)
                                Text("Scheduled: ${appt.appointmentDate} at ${appt.appointmentTime}", fontSize = 12.sp, color = Color.Gray)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Concern: ${appt.concern.ifEmpty { "General Consultation" }}", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                                    if (appt.priority == "High") {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFFFE4E6))
                                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("🚨 HIGH PRIORITY", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                                        }
                                    }
                                }

                                if (appt.status != "completed" && appt.status != "cancelled" && appt.status != "no_show" && appt.status != "reschedule_pending") {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.updateAppointmentStatus(appt.id, "completed") },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Complete", fontSize = 11.sp, color = Color.White)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.updateAppointmentStatus(appt.id, "no_show") },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("No-Show", fontSize = 11.sp, color = Color(0xFFEAB308))
                                        }
                                        Button(
                                            onClick = { showRescheduleWarningAppt = appt },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEA619)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Reschedule", fontSize = 11.sp, color = Color.White)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val slotId = "doc_slot_${appt.doctorId}_${appt.appointmentDate}_${appt.appointmentTime.replace(" ", "").replace(":", "")}"
                                                viewModel.cancelAppointmentWithRefund(appt, slotId) {
                                                    Toast.makeText(context, "Transaction refunded to customer online", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1.2f)
                                        ) {
                                            Text("Refund & Cancel", fontSize = 9.sp, color = Color.Red)
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

    // Reschedule warning dialog
    showRescheduleWarningAppt?.let { appt ->
        var ownerPhone by remember { mutableStateOf("") }
        LaunchedEffect(appt.id) {
            viewModel.getProfileById(appt.consumerId) { p ->
                ownerPhone = p?.phone ?: ""
            }
        }
        AlertDialog(
            onDismissRequest = { showRescheduleWarningAppt = null },
            title = { Text("⚠️ Contact Customer First") },
            text = {
                Text("Please call the customer at +91 $ownerPhone first to confirm they agree to reschedule.\n\nIf they agree, click Proceed. Otherwise, click Refund & Cancel to void the booking.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showProposeRescheduleAppt = appt
                        proposeApptDate = appt.appointmentDate
                        proposeApptTime = appt.appointmentTime
                        showRescheduleWarningAppt = null
                    }
                ) {
                    Text("Proceed to Reschedule")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        val slotId = "doc_slot_${appt.doctorId}_${appt.appointmentDate}_${appt.appointmentTime.replace(" ", "").replace(":", "")}"
                        viewModel.cancelAppointmentWithRefund(appt, slotId) {
                            Toast.makeText(context, "Transaction refunded to customer online", Toast.LENGTH_SHORT).show()
                        }
                        showRescheduleWarningAppt = null
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text("Refund & Cancel")
                }
            }
        )
    }

    // Propose Reschedule dialog
    showProposeRescheduleAppt?.let { appt ->
        AlertDialog(
            onDismissRequest = { showProposeRescheduleAppt = null },
            title = { Text("Propose Reschedule Slot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the new proposed date and time slot for doctor consultation.")
                    OutlinedTextField(
                        value = proposeApptDate,
                        onValueChange = { proposeApptDate = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = proposeApptTime,
                        onValueChange = { proposeApptTime = it },
                        label = { Text("Time (e.g. 10:30 AM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!proposeApptDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                            Toast.makeText(context, "Please enter a valid YYYY-MM-DD date", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (proposeApptTime.trim().isEmpty()) {
                            Toast.makeText(context, "Please enter a time slot", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.proposeReschedule(appt, proposeApptDate, proposeApptTime) { success ->
                            if (success) {
                                Toast.makeText(context, "Reschedule proposal sent to customer!", Toast.LENGTH_SHORT).show()
                                showProposeRescheduleAppt = null
                            }
                        }
                    }
                ) {
                    Text("Send Proposal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showProposeRescheduleAppt = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
