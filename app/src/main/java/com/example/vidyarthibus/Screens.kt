package com.example.vidyarthibus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- THEME COLORS ---
val PrimaryColor = Color(0xFF283593) // Indigo 800
val SecondaryColor = Color(0xFF5C6BC0) // Indigo 400
val SuccessColor = Color(0xFF4CAF50) // Green: Seats available
val WarningColor = Color(0xFFFFA000) // Orange: Busy
val DangerColor = Color(0xFFE53935) // Red: Full
val BackgroundColor = Color(0xFFF5F5F5)

const val REPORT_TIMEOUT_MS = 15 * 60 * 1000L // 15 mins expiry logic

val RouteLocations = mapOf(
    "Engineering College" to Pair(12.9716, 77.5946),
    "Law College" to Pair(12.9721, 77.5933),
    "Medical College" to Pair(12.9740, 77.5910),
    "Arts & Science College" to Pair(12.9700, 77.5900),
    "Commerce College" to Pair(12.9750, 77.5950)
)

data class ChatMsg(
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val type: String = "chat",
    val replyTo: String? = null,
    val replyText: String? = null
)

@Composable
fun SplashScreen(onAnimationFinished: () -> Unit) {
    val scale = remember { Animatable(0.2f) }
    LaunchedEffect(Unit) {
        scale.animateTo(targetValue = 1f, animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing))
        delay(500)
        onAnimationFinished()
    }
    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(id = R.drawable.bus_image), contentDescription = "Bus Image", modifier = Modifier.size(250.dp).scale(scale.value), contentScale = ContentScale.Fit)
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "VIDYARTHI BUS", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryColor, modifier = Modifier.scale(scale.value))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Crowdsourced Bus Alert", fontSize = 16.sp, color = Color.Gray, modifier = Modifier.scale(scale.value))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(savedEmail: String, savedPassword: String, onLoginSuccess: () -> Unit, onSignupClick: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(40.dp))
        Image(painter = painterResource(id = R.drawable.bus_image), contentDescription = "Bus Image", modifier = Modifier.height(150.dp).fillMaxWidth(), contentScale = ContentScale.Fit)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "VIDYARTHI BUS", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = PrimaryColor)
        Text(text = "Real-time crowdsourced bus status", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(40.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryColor) }, shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryColor) }, trailingIcon = { val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff; IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(image, contentDescription = "Toggle password visibility") } }, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), shape = RoundedCornerShape(12.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { if (email == savedEmail && password == savedPassword && email.isNotEmpty()) { onLoginSuccess() } else { Toast.makeText(context, "Invalid Email or Password", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor), shape = RoundedCornerShape(8.dp)) { Text("LOGIN", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Don't have an account? ", color = Color.Gray); Text("Sign Up", color = PrimaryColor, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onSignupClick() }) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(onSignupSuccess: (String, String, String, String) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Scaffold(topBar = { TopAppBar(title = { Text("Create Account", color = PrimaryColor, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryColor) } }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryColor) }, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = PrimaryColor) }, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = college, onValueChange = { college = it }, label = { Text("College Name") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = PrimaryColor) }, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryColor) }, trailingIcon = { val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff; IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(image, contentDescription = "Toggle password visibility") } }, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirm Password") }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryColor) }, trailingIcon = { val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff; IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(image, contentDescription = "Toggle password visibility") } }, visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { if (email.isNotEmpty() && password.isNotEmpty() && password == confirmPassword && college.isNotEmpty() && name.isNotEmpty()) { onSignupSuccess(name, email, college, password) } else if (password != confirmPassword) { Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show() } else { Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show() } }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor), shape = RoundedCornerShape(8.dp)) { Text("SIGN UP", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Already have an account? ", color = Color.Gray); Text("Login", color = PrimaryColor, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onBack() }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteScreen(currentScreen: String, onNavigate: (String) -> Unit, onSelect: (String, String) -> Unit) {
    val buses = listOf("Bus 101" to "Engineering College", "Bus 102" to "Law College", "Bus 103" to "Medical College", "Bus 104" to "Arts & Science College", "Bus 105" to "Commerce College")
    var selectedBusItem by remember { mutableStateOf(buses[0]) }
    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    Scaffold(topBar = { TopAppBar(title = { Text("Select Route", color = Color.White, fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)) }, bottomBar = { VidyarthiBottomBar(currentScreen, onNavigate) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text(text = "Choose your daily bus", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = "Crowdsourced updates from fellow students.", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), placeholder = { Text("Search bus or college...") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryColor) }, shape = RoundedCornerShape(12.dp), singleLine = true)
            val filteredBuses = buses.filter { it.first.contains(searchQuery, true) || it.second.contains(searchQuery, true) }
            if (filteredBuses.isEmpty()) { NoResultsView() } else {
                LazyColumn {
                    items(filteredBuses) { bus ->
                        val isSelected = bus == selectedBusItem
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedBusItem = bus; onSelect(bus.first, bus.second) }, shape = RoundedCornerShape(12.dp), border = if (isSelected) BorderStroke(2.dp, PrimaryColor) else null, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text(text = bus.first, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryColor); Text(text = bus.second, color = Color.Gray, fontSize = 14.sp) }
                                RadioButton(selected = isSelected, onClick = { selectedBusItem = bus; onSelect(bus.first, bus.second) }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryColor))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentScreen: String,
    studentName: String,
    studentCollege: String,
    selectedBus: String,
    selectedCollege: String,
    profileImageUri: Uri?,
    onNavigate: (String) -> Unit
) {
    Scaffold(
        bottomBar = { VidyarthiBottomBar(currentScreen, onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FE))
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Profile Icon at Top Right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(PrimaryColor, PrimaryColor.copy(alpha = 0.8f))
                        ),
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(top = 40.dp, bottom = 60.dp, start = 24.dp, end = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, ${studentName.split(" ").firstOrNull() ?: "Student"}!",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Student of $studentCollege",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    // Profile Icon at top right
                    IconButton(
                        onClick = { onNavigate("profile") },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    ) {
                        if (profileImageUri != null) {
                            AsyncImage(
                                model = profileImageUri,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Middle Part: Bus Image Card (The "New" look)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-30).dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bus_image),
                        contentDescription = "Bus Illustration",
                        modifier = Modifier.height(150.dp).fillMaxWidth(),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = selectedBus,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryColor
                    )
                    Text(
                        text = "Heading to: $selectedCollege",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).offset(y = (-10).dp)) {
                // Tracking Card
                TrackBusCard(selectedBus)
                Spacer(modifier = Modifier.height(20.dp))
                
                // Action Cards
                Row(modifier = Modifier.fillMaxWidth()) {
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Crowd Meter",
                        icon = Icons.Default.BarChart,
                        iconColor = SuccessColor,
                        content = "Live Status",
                        subContent = "Check occupancy",
                        onViewAll = { onNavigate("status") }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    SummaryCard(
                        modifier = Modifier.weight(1f),
                        title = "Quick Alert",
                        icon = Icons.Default.EditNotifications,
                        iconColor = WarningColor,
                        content = "Report Bus",
                        subContent = "Help students",
                        onViewAll = { onNavigate("report") }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                NextBusTimingsCard(onViewAll = { onNavigate("view_all_timings") })
                Spacer(modifier = Modifier.height(20.dp))
                CommunityChatCard(selectedBus, onViewAll = { onNavigate("view_all_chat") })
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrowdMeterScreen(selectedBus: String, currentScreen: String, onNavigate: (String) -> Unit) {
    var rawLevel by remember { mutableIntStateOf(0) }
    var timestamp by remember { mutableLongStateOf(0L) }
    val database = FirebaseDatabase.getInstance().reference
    val ref = database.child("crowd_meter").child(selectedBus.replace(" ", "_"))
    DisposableEffect(selectedBus) {
        val listener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                rawLevel = s.child("level").getValue(Int::class.java) ?: 0
                timestamp = s.child("timestamp").getValue(Long::class.java) ?: 0L
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while(true) { delay(1000); now = System.currentTimeMillis() } }
    
    val isFresh = (now - timestamp) < REPORT_TIMEOUT_MS
    val level = if (isFresh) rawLevel else 0
    
    // Core logic: Green = Seats, Red = Full
    val status = when { 
        !isFresh -> "NO RECENT DATA"
        level < 40 -> "SEATS AVAILABLE"
        level < 80 -> "CROWDED / BUSY"
        else -> "BUS FULL"
    }
    val color = when {
        status == "SEATS AVAILABLE" -> SuccessColor // Green
        status == "CROWDED / BUSY" -> WarningColor // Orange
        status == "BUS FULL" -> DangerColor // Red
        else -> Color.Gray
    }
    
    val animatedProgress by animateFloatAsState(targetValue = (level / 100f).coerceIn(0f, 1f), animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow), label = "progress")
    
    Scaffold(topBar = { TopAppBar(title = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Crowd Meter", color = PrimaryColor, fontWeight = FontWeight.Bold, fontSize = 24.sp) } }, navigationIcon = { IconButton(onClick = { onNavigate("home") }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = PrimaryColor) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) }, bottomBar = { VidyarthiBottomBar(currentScreen, onNavigate) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color.White).padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(20.dp)); Text("Status for $selectedBus", fontSize = 18.sp, color = Color.Black); Spacer(modifier = Modifier.height(60.dp))
            
            Text(text = if (isFresh && level > 0) "$level%" else "--", fontSize = 110.sp, fontWeight = FontWeight.ExtraBold, color = color); Spacer(modifier = Modifier.height(30.dp))
            
            // Horizontal Progress Bar for "Crowd Meter"
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(14.dp)),
                color = color,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(50.dp))
            Text(text = status, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = color, textAlign = TextAlign.Center)
            
            if (isFresh) {
                val diffMins = (now - timestamp) / 60000
                Text("Reported ${if (diffMins == 0L) "Just now" else "${diffMins}m ago"}", fontSize = 14.sp, color = Color.Gray)
            } else {
                Text("Information expired. Students: help by reporting status!", fontSize = 14.sp, color = DangerColor, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
            
            // Sugest alternative if bus is red (full)
            if (status == "BUS FULL") {
                Spacer(Modifier.height(30.dp))
                Button(onClick = { onNavigate("auto") }, colors = ButtonDefaults.buttonColors(containerColor = SecondaryColor), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(55.dp)) {
                    Icon(Icons.Default.LocalTaxi, null); Spacer(Modifier.width(8.dp)); Text("FIND ALTERNATIVE (SHARED AUTO)")
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { onNavigate("report") }, modifier = Modifier.fillMaxWidth().height(65.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor), shape = RoundedCornerShape(12.dp)) {
                Text("REPORT CURRENT STATUS", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(currentScreen: String, selectedBus: String, selectedCollege: String, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    var reporting by remember { mutableStateOf(false) }
    val fusedLoc = remember { LocationServices.getFusedLocationProviderClient(context) }
    val db = FirebaseDatabase.getInstance().reference.child("crowd_meter").child(selectedBus.replace(" ", "_"))
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (!it) Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show() }
    
    fun submit(level: Int, label: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION); return }
        reporting = true
        fusedLoc.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val target = RouteLocations[selectedCollege] ?: Pair(loc.latitude, loc.longitude)
                val dist = FloatArray(1)
                Location.distanceBetween(loc.latitude, loc.longitude, target.first, target.second, dist)
                if (dist[0] < 500) {
                    db.setValue(mapOf("level" to level, "timestamp" to ServerValue.TIMESTAMP)).addOnCompleteListener { 
                        reporting = false; Toast.makeText(context, "Thank you! Status: $label", Toast.LENGTH_SHORT).show(); onNavigate("status") 
                    }
                } else { reporting = false; Toast.makeText(context, "You must be on the bus to report!", Toast.LENGTH_LONG).show() }
            } else { reporting = false; Toast.makeText(context, "GPS must be enabled to report.", Toast.LENGTH_SHORT).show() }
        }.addOnFailureListener { reporting = false }
    }
    
    Scaffold(topBar = { TopAppBar(title = { Text("Report for $selectedBus") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Help fellow students by reporting the current status", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(48.dp))
            if (reporting) {
                CircularProgressIndicator(color = PrimaryColor)
            } else {
                // One-tap report actions as per user request
                StatusBtn("I'm on the bus, EMPTY (SEATS AVAILABLE)", SuccessColor) { submit(15, "Seats Available") }
                Spacer(Modifier.height(20.dp))
                StatusBtn("I'm on the bus, SEATED (NO MORE SEATS)", WarningColor) { submit(60, "Busy / No Seats") }
                Spacer(Modifier.height(20.dp))
                StatusBtn("I'M ON THE BUS, BUS FULL (NO SPACE)", DangerColor) { submit(95, "Full") }
            }
        }
    }
}

@Composable
fun StatusBtn(label: String, color: Color, onClick: () -> Unit) { 
    Button(onClick, Modifier.fillMaxWidth().height(85.dp), colors = ButtonDefaults.buttonColors(containerColor = color), shape = RoundedCornerShape(12.dp)) { 
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center) 
    } 
}

@Composable
fun TrackBusCard(busNo: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.DirectionsBus, null, tint = PrimaryColor, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(text = "Real-time Tracking", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(modifier = Modifier.height(12.dp)); Box(modifier = Modifier.fillMaxWidth().background(PrimaryColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) { Text(text = "Monitoring: $busNo", color = PrimaryColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            Spacer(modifier = Modifier.height(12.dp)); Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) { Text(text = "Bus approaching stop", color = SuccessColor, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(text = "Estimated 8 mins away", color = Color.Gray, fontSize = 12.sp) }
                Box(modifier = Modifier.size(140.dp, 80.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE3F2FD))) {
                    Canvas(modifier = Modifier.fillMaxSize()) { val routeColor = PrimaryColor; drawLine(routeColor, Offset(10.dp.toPx(), 60.dp.toPx()), Offset(110.dp.toPx(), 45.dp.toPx()), strokeWidth = 3.dp.toPx()); drawCircle(SuccessColor, 5.dp.toPx(), Offset(10.dp.toPx(), 60.dp.toPx())); drawCircle(DangerColor, 5.dp.toPx(), Offset(110.dp.toPx(), 45.dp.toPx())) }
                    Icon(Icons.Default.DirectionsBus, null, tint = PrimaryColor, modifier = Modifier.align(Alignment.Center).size(24.dp).offset(x = 10.dp))
                }
            }
        }
    }
}

@Composable
fun SummaryCard(modifier: Modifier = Modifier, title: String, icon: ImageVector, iconColor: Color, content: String, subContent: String, onViewAll: () -> Unit) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(4.dp)); Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                IconButton(onClick = { onViewAll() }, modifier = Modifier.size(16.dp)) { Icon(Icons.Default.ChevronRight, null, tint = PrimaryColor) }
            }
            Spacer(modifier = Modifier.height(12.dp)); Row(verticalAlignment = Alignment.Top) {
                Box(modifier = Modifier.size(30.dp).background(iconColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Text(text = content.take(1), color = iconColor, fontWeight = FontWeight.Bold) }
                Spacer(modifier = Modifier.width(8.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = content, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(text = subContent, fontSize = 10.sp, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
fun NextBusTimingsCard(onViewAll: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccessTime, null, tint = PrimaryColor, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(text = "Full Schedule", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Text(text = "View All", fontSize = 12.sp, color = PrimaryColor, modifier = Modifier.clickable { onViewAll() })
            }
            Spacer(modifier = Modifier.height(16.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) { BusTimingItem("7:30 AM", "Bus 101"); Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray)); BusTimingItem("7:45 AM", "Bus 102"); Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.LightGray)); BusTimingItem("8:00 AM", "Bus 103") }
        }
    }
}

@Composable
fun BusTimingItem(time: String, busNo: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(text = time, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.Black); Text(text = busNo, fontSize = 10.sp, color = Color.Gray) } }

@Composable
fun CommunityChatCard(selectedBus: String, onViewAll: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Groups, null, tint = PrimaryColor, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(text = "Student Community", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
                Text(text = "Open Hub >", fontSize = 12.sp, color = PrimaryColor, modifier = Modifier.clickable { onViewAll() })
            }
            Spacer(modifier = Modifier.height(12.dp)); Text("Stay updated with other students on $selectedBus", fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
fun VidyarthiBottomBar(currentScreen: String, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        val items = listOf("home" to (Icons.Default.Home to "Home"), "route_tab" to (Icons.Default.Map to "Routes"), "status" to (Icons.Default.BarChart to "Meter"), "auto" to (Icons.Default.PhoneInTalk to "Auto"))
        items.forEach { (r, pair) ->
            val (icon, label) = pair
            NavigationBarItem(selected = currentScreen == r, onClick = { onNavigate(r) }, icon = { Icon(icon, null) }, label = { Text(label) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryColor, selectedTextColor = PrimaryColor, indicatorColor = Color.Transparent))
        }
    }
}

@Composable
fun SharedAutoScreen(currentScreen: String, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    Scaffold(bottomBar = { VidyarthiBottomBar(currentScreen, onNavigate) }) { padding ->
        Column(Modifier.padding(padding).padding(24.dp)) {
            Text("Alternative Transport", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = DangerColor); Text("Local Shared Auto contact numbers nearby:", color = Color.Gray); Spacer(modifier = Modifier.height(24.dp))
            val autoServices = listOf(Triple("Main Gate Stand", "+91 9876543210", "Available"), Triple("College Gate Autos", "+91 9876543211", "Available"), Triple("City Junction Auto", "+91 9876543212", "On Call"))
            autoServices.forEach { (name, phone, status) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(50.dp).background(PrimaryColor.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.LocalTaxi, null, tint = PrimaryColor, modifier = Modifier.size(30.dp)) }
                        Column(Modifier.padding(start = 16.dp).weight(1f)) { Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(phone, color = Color.Gray, fontSize = 12.sp) }
                        IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$phone") }) }) { Icon(Icons.Default.Call, null, tint = SuccessColor, modifier = Modifier.size(32.dp)) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Find an alternative transport if the bus is full.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewAllScreen(type: String, studentName: String, selectedBus: String, onBack: () -> Unit) {
    val title = when (type) { "notices" -> "Official Alerts"; "messages" -> "Direct Chat"; "timings" -> "Full Schedule"; "chat" -> "Student Community"; else -> "Vidyarthi Bus" }
    var searchQuery by remember { mutableStateOf("") }; var inputMsg by remember { mutableStateOf("") }; var replyingTo by remember { mutableStateOf<ChatMsg?>(null) }
    val focusManager = LocalFocusManager.current; val listState = rememberLazyListState(); val scope = rememberCoroutineScope(); val context = LocalContext.current
    val dbRoot = FirebaseDatabase.getInstance().reference; val messagesRef = dbRoot.child("direct_messages").child(studentName.replace(" ", "_").ifEmpty { "Guest" }); val chatRef = dbRoot.child("community_chat").child(selectedBus.replace(" ", "_")); val noticesRef = dbRoot.child("notices")
    var liveData by remember { mutableStateOf(listOf<ChatMsg>()) }

    DisposableEffect(type, studentName, selectedBus) {
        val listener = object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                val list = mutableListOf<ChatMsg>()
                s.children.forEach { child -> child.getValue(ChatMsg::class.java)?.let { list.add(it) } }
                liveData = if (type == "messages") list.sortedBy { it.timestamp } else list.sortedByDescending { it.timestamp }
                if (type == "messages" && list.isNotEmpty()) { scope.launch { listState.animateScrollToItem(list.size - 1) } }
            }
            override fun onCancelled(e: DatabaseError) {}
        }
        val target = if (type == "messages") messagesRef else null
        target?.addValueEventListener(listener)
        onDispose { target?.removeEventListener(listener) }
    }

    fun sendMessage() {
        if (inputMsg.isBlank()) return
        val target = if (type == "messages") messagesRef.push() else return
        val msg = ChatMsg(sender = studentName.ifEmpty { "Guest" }, text = inputMsg, timestamp = System.currentTimeMillis(), replyTo = replyingTo?.sender, replyText = replyingTo?.text)
        target.setValue(msg).addOnSuccessListener { inputMsg = ""; replyingTo = null; focusManager.clearFocus() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryColor)) },
        bottomBar = {
            if (type == "messages") {
                Column {
                    AnimatedVisibility(visible = replyingTo != null) {
                        Surface(modifier = Modifier.fillMaxWidth(), color = PrimaryColor.copy(alpha = 0.1f)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) { Text("Replying to ${replyingTo?.sender}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryColor); Text(replyingTo?.text ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp) }
                                IconButton(onClick = { replyingTo = null }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) }
                            }
                        }
                    }
                    Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = inputMsg, onValueChange = { inputMsg = it }, modifier = Modifier.weight(1f), placeholder = { Text("Write something...") }, shape = RoundedCornerShape(24.dp), maxLines = 4)
                            Spacer(Modifier.width(8.dp))
                            FloatingActionButton(onClick = { sendMessage() }, containerColor = PrimaryColor, contentColor = Color.White, shape = CircleShape, modifier = Modifier.size(48.dp)) { Icon(Icons.AutoMirrored.Filled.Send, null) }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FD))) {
            if (type == "notices") {
                val demoNotices = listOf(Triple("Bus 101 Delay", "Traffic at main gate, 10 mins late.", "08:15 AM"), Triple("Holiday Alert", "College closed for local festival tomorrow.", "Yesterday"))
                LazyColumn(Modifier.padding(16.dp)) { items(demoNotices) { notice -> NotificationCard(notice.first, notice.second, notice.third, Icons.Default.Campaign, WarningColor) ; Spacer(Modifier.height(12.dp)) } }
            } else if (type == "chat") {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text("The route hub for live discussions.", textAlign = TextAlign.Center, color = Color.Gray) }
            } else if (type == "messages") {
                OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.fillMaxWidth().padding(16.dp), placeholder = { Text("Search...") }, leadingIcon = { Icon(Icons.Default.Search, null, tint = PrimaryColor) }, shape = RoundedCornerShape(25.dp), singleLine = true)
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    val filtered = liveData.filter { it.text.contains(searchQuery, true) || it.sender.contains(searchQuery, true) }
                    if (filtered.isEmpty() && liveData.isNotEmpty()) { item { NoResultsView() } } 
                    else if (liveData.isEmpty()) { item { Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("No live messages.", color = Color.Gray) } } }
                    items(filtered) { msg -> MessageCard(msg.sender, msg.text, formatTime(msg.timestamp), if(msg.sender == studentName) PrimaryColor else DangerColor, msg.replyTo, msg.replyText) { replyingTo = msg } ; Spacer(Modifier.height(12.dp)) }
                }
            } else if (type == "timings") { val timings = listOf("Bus 101" to "07:30 AM", "Bus 102" to "07:45 AM", "Bus 103" to "08:00 AM", "Bus 104" to "08:15 AM"); LazyColumn(Modifier.padding(16.dp)) { items(timings) { bus -> TimingCard(bus.first, bus.second); Spacer(Modifier.height(12.dp)) } } }
        }
    }
}

fun formatTime(timestamp: Long): String { if (timestamp == 0L) return ""; return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp)) }

@Composable
fun NotificationCard(title: String, subtitle: String, time: String, icon: ImageVector, color: Color) { Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) { Column(modifier = Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.Top) { Box(modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color) }; Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(subtitle, fontSize = 13.sp, color = Color.Gray); Text(time, fontSize = 11.sp, color = color) } } } } }

@Composable
fun MessageCard(name: String, message: String, time: String, color: Color, replyTo: String?, replyText: String?, onReply: () -> Unit) { Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) { Column(modifier = Modifier.padding(16.dp)) { if (replyTo != null) { Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), color = Color.LightGray.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) { Column(Modifier.padding(8.dp)) { Text(replyTo, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = PrimaryColor); Text(replyText ?: "", maxLines = 1, fontSize = 10.sp, overflow = TextOverflow.Ellipsis) } } }; Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(50.dp).background(color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) { Text(name.take(1), fontWeight = FontWeight.Bold, color = color, fontSize = 20.sp) }; Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) { Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(time, fontSize = 11.sp, color = Color.Gray) }; Text(message, fontSize = 13.sp, color = Color.DarkGray) } }; Text("Reply", color = PrimaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.align(Alignment.End).clickable { onReply() }) } } }

@Composable
fun TimingCard(busName: String, timing: String) { Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) { Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(busName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PrimaryColor); Text("Standard Route", fontSize = 12.sp, color = Color.Gray) }; Text(timing, fontWeight = FontWeight.Bold, fontSize = 16.sp) } } }

@Composable
fun NoResultsView() { Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.SearchOff, null, modifier = Modifier.size(64.dp), tint = Color.LightGray); Text("No matches found.", color = Color.Gray, fontSize = 16.sp) } } }
