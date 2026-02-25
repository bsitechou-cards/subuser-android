package com.app.walletcards.ui.theme

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.app.walletcards.R
import com.app.walletcards.model.CardItem
import com.app.walletcards.model.CardResponse
import com.app.walletcards.network.CardApiService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    navController: NavHostController
) {
    val auth = FirebaseAuth.getInstance()
    val userEmail = auth.currentUser?.email ?: ""

    var cardResponse by remember { mutableStateOf<CardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    var isSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            cardResponse = CardApiService.getAllDigitalCards(userEmail)
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        // Top Row: Wallet title + Logout icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Wallet",
                style = MaterialTheme.typography.headlineLarge
            )

            Row {
                IconButton(onClick = { isSheetOpen = true }) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = "Apply for card"
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_logout),
                        contentDescription = "Logout",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {

            val cards = cardResponse?.data ?: emptyList()

            if (cards.isEmpty()) {
                // No cards issued
                EmptyCardDesign()
            } else {
                // Show all cards in a scrollable list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(cards) { card ->
                        CardDesign(
                            card = card,
                            onViewClick = {
                                navController.navigate("cardDetails/${card.cardid}")
                            }
                        )
                    }
                }
            }
        }
    }

    if (isSheetOpen) {
        ApplyForCardBottomSheet(userEmail = userEmail) { isSheetOpen = false }
    }
}

@Composable
fun EmptyCardDesign() {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Text("Virtual Card", color = Color.White)

            Text(
                "$ 0.00",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Button(
                onClick = { /* TODO: Apply new card */ }
            ) {
                Text("Apply New Card")
            }
        }
    }
}

@Composable
fun CardDesign(
    card: CardItem,
    onViewClick: () -> Unit
) {
    val context = LocalContext.current

    // Launcher to trigger biometric auth
    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onViewClick()
        }
    }

    fun authenticateAndNavigate() {
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        if (biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
        ) {
            val intent = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Authenticate to view card details")
                .setNegativeButtonText("Cancel")
                .build()
            // Actually trigger prompt
            androidx.biometric.BiometricPrompt(
                context as androidx.fragment.app.FragmentActivity,
                ContextCompat.getMainExecutor(context),
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onViewClick()
                    }

                    override fun onAuthenticationFailed() { super.onAuthenticationFailed() }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { super.onAuthenticationError(errorCode, errString) }
                }
            ).authenticate(intent)
        } else {
            // No biometric support: fallback
            onViewClick()
        }
    }

    // --- Card UI ---
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Virtual Card", color = Color.White)
                TextButton(onClick = { authenticateAndNavigate() }) {
                    Text("👁", color = Color.White)
                }
            }

            Text(
                "**** **** **** ${card.lastfour}",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Text(card.nameoncard, color = Color.White)
            Text(card.type.uppercase(), color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyForCardBottomSheet(userEmail: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Text("Apply for a new card")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = userEmail, onValueChange = {}, readOnly = true, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = datePickerState.selectedDateMillis?.let { "${Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.DAY_OF_MONTH)}/${Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.MONTH) + 1}/${Calendar.getInstance().apply { timeInMillis = it }.get(Calendar.YEAR)}" } ?: "", 
                    onValueChange = {}, 
                    label = { Text("Date of Birth") }, 
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = { IconButton(onClick = { showDatePicker = true }) { Icon(painterResource(id = R.drawable.ic_calendar), contentDescription = null) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Postal Code") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Country") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("State") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Country Code") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { /* TODO: Implement API call */ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Submit Application")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}