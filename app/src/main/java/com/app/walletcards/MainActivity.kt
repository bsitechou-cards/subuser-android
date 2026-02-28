package com.app.walletcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.navigation.compose.*
import com.app.walletcards.ui.theme.HomeScreen
import com.app.walletcards.ui.theme.LoginScreen
import com.app.walletcards.ui.theme.RegisterScreen
import com.app.walletcards.ui.theme.SplashScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import com.app.walletcards.ui.theme.CardDetailsScreen

class MainActivity : androidx.fragment.app.FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        setContent {

            // Minimal Black & White Theme
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = androidx.compose.ui.graphics.Color.Black,
                    background = androidx.compose.ui.graphics.Color.White
                )
            ) {

                val navController = rememberNavController()
                val auth = FirebaseAuth.getInstance()

                // Navigation Host
                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {

                    // Splash Screen
                    composable("splash") {
                        SplashScreen(onTimeout = {
                            val destination = if (auth.currentUser != null) "home" else "login"
                            navController.navigate(destination) {
                                popUpTo("splash") { inclusive = true }
                            }
                        })
                    }

                    // Login Screen
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onRegisterClick = {
                                navController.navigate("register")
                            }
                        )
                    }

                    // Register Screen
                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("home") {
                                    popUpTo("register") { inclusive = true }
                                }
                            },
                            onLoginClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // Home Screen (Dashboard)
                    composable("home") {
                        HomeScreen(
                            onLogout = {
                                auth.signOut()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                }
                            },
                            navController = navController
                        )
                    }

                    composable("cardDetails/{cardId}") { backStackEntry ->
                        val cardId = backStackEntry.arguments?.getString("cardId") ?: ""
                        CardDetailsScreen(cardId = cardId, navController = navController)
                    }
                }
            }
        }
    }
}
