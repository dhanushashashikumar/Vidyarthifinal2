package com.example.vidyarthibus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VidyarthiBusApp()
        }
    }
}

@Composable
fun VidyarthiBusApp() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("VidyarthiBus", Context.MODE_PRIVATE)

    var showSplash by remember { mutableStateOf(true) }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }

    var registeredName by remember {
        mutableStateOf(sharedPreferences.getString("name", "") ?: "")
    }
    var registeredEmail by remember {
        mutableStateOf(sharedPreferences.getString("email", "") ?: "")
    }
    var registeredCollege by remember {
        mutableStateOf(sharedPreferences.getString("college", "") ?: "")
    }
    var registeredPassword by remember {
        mutableStateOf(sharedPreferences.getString("password", "") ?: "")
    }
    var selectedBus by remember {
        mutableStateOf(sharedPreferences.getString("selectedBus", "Bus 101") ?: "Bus 101")
    }
    var selectedCollegeRoute by remember {
        mutableStateOf(sharedPreferences.getString("selectedCollegeRoute", "Engineering College") ?: "Engineering College")
    }

    var currentScreen by remember {
        mutableStateOf(
            when {
                sharedPreferences.getBoolean("isLoggedIn", false) -> "home"
                registeredEmail.isEmpty() -> "signup"
                else -> "login"
            }
        )
    }

    if (showSplash) {
        SplashScreen(onAnimationFinished = { showSplash = false })
    } else {
        when (currentScreen) {
            "login" -> LoginScreen(
                savedEmail = registeredEmail,
                savedPassword = registeredPassword,
                onLoginSuccess = {
                    sharedPreferences.edit().putBoolean("isLoggedIn", true).apply()
                    currentScreen = "home" 
                },
                onSignupClick = {
                    currentScreen = "signup"
                }
            )

            "signup" -> SignupScreen(
                onSignupSuccess = { name, email, college, password ->
                    registeredName = name
                    registeredEmail = email
                    registeredCollege = college
                    registeredPassword = password
                    sharedPreferences.edit()
                        .putString("name", name)
                        .putString("email", email)
                        .putString("college", college)
                        .putString("password", password)
                        .apply()
                    currentScreen = "login"
                },
                onBack = {
                    currentScreen = "login"
                }
            )

            "route", "route_tab" -> {
                BackHandler { currentScreen = "home" }
                RouteScreen(
                    currentScreen = currentScreen, 
                    onNavigate = { currentScreen = it }, 
                    onSelect = { bus, college ->
                        selectedBus = bus
                        selectedCollegeRoute = college
                        sharedPreferences.edit()
                            .putString("selectedBus", bus)
                            .putString("selectedCollegeRoute", college)
                            .apply()
                        currentScreen = "status"
                    }
                )
            }

            "status" -> {
                BackHandler { currentScreen = "home" }
                CrowdMeterScreen(
                    selectedBus = selectedBus,
                    currentScreen = "status",
                    onNavigate = { currentScreen = it }
                )
            }

            "home" -> {
                BackHandler { /* Exit logic */ }
                HomeScreen(
                    currentScreen = "home",
                    studentName = registeredName,
                    studentCollege = registeredCollege,
                    selectedBus = selectedBus,
                    selectedCollege = selectedCollegeRoute,
                    profileImageUri = profileImageUri,
                    onNavigate = { currentScreen = it }
                )
            }

            "profile" -> {
                BackHandler { currentScreen = "home" }
                ProfileScreen(
                    name = registeredName,
                    email = registeredEmail,
                    college = registeredCollege,
                    route = selectedBus,
                    profileImageUri = profileImageUri,
                    onBack = { currentScreen = "home" },
                    onLogout = {
                        sharedPreferences.edit().putBoolean("isLoggedIn", false).apply()
                        currentScreen = "login"
                    }
                )
            }

            "view_all_notices", "view_all_messages", "view_all_timings", "view_all_chat" -> {
                val type = currentScreen.removePrefix("view_all_")
                BackHandler { currentScreen = "home" }
                ViewAllScreen(
                    type = type, 
                    studentName = registeredName,
                    selectedBus = selectedBus,
                    onBack = { currentScreen = "home" }
                )
            }

            "report" -> {
                BackHandler { currentScreen = "home" }
                ReportScreen(
                    currentScreen = "report",
                    selectedBus = selectedBus,
                    selectedCollege = selectedCollegeRoute,
                    onNavigate = { currentScreen = it }
                )
            }

            "auto" -> {
                BackHandler { currentScreen = "home" }
                SharedAutoScreen(
                    currentScreen = "auto",
                    onNavigate = { currentScreen = it }
                )
            }
        }
    }
}
