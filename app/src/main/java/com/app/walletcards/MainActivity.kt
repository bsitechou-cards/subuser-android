package com.app.walletcards

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.navigation.compose.*
import com.app.walletcards.ui.theme.HomeScreen
import com.app.walletcards.ui.theme.LoginScreen
import com.app.walletcards.ui.theme.RegisterScreen
import com.app.walletcards.ui.theme.SplashScreen
import com.app.walletcards.ui.theme.CardDetailsScreen
import com.app.walletcards.ui.theme.BotAvatarIcon
import com.app.walletcards.util.LocalizationUtil
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : androidx.fragment.app.FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Make the app fullscreen
        hideSystemUI()

        setContent {
            val context = LocalContext.current
            var showLanguageSelection by remember { mutableStateOf(!LocalizationUtil.isLanguageSet(context)) }
            
            LaunchedEffect(Unit) {
                LocalizationUtil.loadLanguage(context)
            }

            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color.Black,
                    background = Color.White
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val auth = FirebaseAuth.getInstance()

                    // Navigation Host
                    NavHost(
                        navController = navController,
                        startDestination = "splash"
                    ) {
                        composable("splash") {
                            SplashScreen(onTimeout = {
                                val destination = if (auth.currentUser != null) "home" else "login"
                                navController.navigate(destination) {
                                    popUpTo("splash") { inclusive = true } }
                            })
                        }
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                },
                                onRegisterClick = { navController.navigate("register") }
                            )
                        }
                        composable("register") {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("register") { inclusive = true }
                                    }
                                },
                                onLoginClick = { navController.popBackStack() }
                            )
                        }
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

                    if (showLanguageSelection) {
                        LanguageSelectionChatBox(onLanguageSelected = { lang ->
                            LocalizationUtil.saveLanguage(context, lang)
                            showLanguageSelection = false
                        })
                    }
                }
            }
        }
    }

    private fun hideSystemUI() {
        // This will hide the status bar and navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionChatBox(onLanguageSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val languages = LocalizationUtil.supportedLanguages
    var selectedText by remember { mutableStateOf("Select Language / Choisir la Langue") }

    Dialog(
        onDismissRequest = { /* Prevent dismissal without selection */ },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    BotAvatarIcon()
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        color = Color(0xFFF1F3F4),
                        shape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Hello! Please select your preferred language to continue.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bonjour! Veuillez sélectionner votre langue préférée pour continuer.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedText,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        languages.forEach { (label, code, _) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    selectedText = label
                                    expanded = false
                                    onLanguageSelected(code)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
