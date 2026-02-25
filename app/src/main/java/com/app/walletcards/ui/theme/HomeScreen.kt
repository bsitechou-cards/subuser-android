package com.app.walletcards.ui.theme

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.app.walletcards.R
import com.app.walletcards.model.ApplyCardRequest
import com.app.walletcards.model.CardItem
import com.app.walletcards.model.CardResponse
import com.app.walletcards.network.CardApiService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

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
    var refreshTrigger by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    var isSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger) {
        isLoading = true
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
        ApplyForCardBottomSheet(userEmail = userEmail, onDismiss = { isSheetOpen = false }, onCardApplied = {
            isSheetOpen = false
            refreshTrigger++
        })
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
            val intent = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Authenticate to view card details")
                .setNegativeButtonText("Cancel")
                .build()
            // Actually trigger prompt
            BiometricPrompt(
                context as androidx.fragment.app.FragmentActivity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
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
fun ApplyForCardBottomSheet(userEmail: String, onDismiss: () -> Unit, onCardApplied: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var address1 by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var selectedCountryName by remember { mutableStateOf("") }
    var isCountryMenuExpanded by remember { mutableStateOf(false) }
    val countries = remember {
        Locale.getISOCountries().map {
            val locale = Locale("", it)
            locale.displayCountry to it
        }
    }
    var state by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Text("Apply for a new card")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(value = userEmail, onValueChange = {}, readOnly = true, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") }, modifier = Modifier.fillMaxWidth())
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
                OutlinedTextField(value = address1, onValueChange = { address1 = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = postalCode, onValueChange = { postalCode = it }, label = { Text("Postal Code") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = isCountryMenuExpanded,
                    onExpandedChange = { isCountryMenuExpanded = !isCountryMenuExpanded }
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        value = selectedCountryName,
                        onValueChange = { },
                        label = { Text("Country") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCountryMenuExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isCountryMenuExpanded,
                        onDismissRequest = { isCountryMenuExpanded = false }
                    ) {
                        countries.forEach { (name, code) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    country = code
                                    selectedCountryName = name
                                    isCountryMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = countryCode, onValueChange = { countryCode = it }, label = { Text("Country Code") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isSubmitting = true
                        scope.launch {
                            val dob = datePickerState.selectedDateMillis?.let {
                                val cal = Calendar.getInstance().apply { timeInMillis = it }
                                "${cal.get(Calendar.YEAR)}-${String.format(Locale.US, "%02d", cal.get(Calendar.MONTH) + 1)}-${String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))}"
                            } ?: ""

                            val request = ApplyCardRequest(
                                useremail = userEmail,
                                firstname = firstName,
                                lastname = lastName,
                                dob = dob,
                                address1 = address1,
                                postalcode = postalCode,
                                city = city,
                                country = country,
                                state = state,
                                countrycode = countryCode,
                                phone = phone
                            )

                            val response = CardApiService.applyForNewVirtualCard(request)
                            if (response != null) {
                                Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                                onCardApplied()
                            } else {
                                Toast.makeText(context, "Application failed. Please try again.", Toast.LENGTH_LONG).show()
                            }
                            isSubmitting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSubmitting) {
                        Box(modifier = Modifier.size(24.dp)) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    } else {
                        Text("Submit Application")
                    }
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
