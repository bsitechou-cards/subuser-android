package com.app.walletcards.ui.theme

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
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
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var depositAddress by remember { mutableStateOf<String?>(null) }
    var subuserFee by remember { mutableStateOf<Double?>(null) }


    LaunchedEffect(refreshTrigger) {
        isLoading = true
        scope.launch {
            cardResponse = CardApiService.getAllDigitalCards(userEmail)
            isLoading = false
        }
    }

    val isRefreshing = isLoading && cardResponse != null
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = { refreshTrigger++ })

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

        Box(Modifier.pullRefresh(pullRefreshState)) {
            if (isLoading && cardResponse == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val cards = cardResponse?.data ?: emptyList()

                if (cards.isEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) { // Wrap in LazyColumn to enable pull-refresh on empty screen
                        item { EmptyCardDesign(onApplyClick = { isSheetOpen = true }) }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(cards) { card ->
                            if (card.cardid == null && card.paidcard == 0) {
                                PaymentPendingCard(card = card, onPayNowClick = {
                                    depositAddress = card.depositaddress
                                    subuserFee = cardResponse?.subuserfee
                                    showQrCodeDialog = true
                                })
                            } else {
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

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (isSheetOpen) {
        ApplyForCardBottomSheet(userEmail = userEmail, onDismiss = { isSheetOpen = false }, onCardApplied = {
            isSheetOpen = false
            refreshTrigger++
        }, onShowQrCode = {
            isSheetOpen = false
            depositAddress = it.first
            subuserFee = it.second.toDoubleOrNull()
            showQrCodeDialog = true
        })
    }

    if (showQrCodeDialog) {
        Dialog(
            onDismissRequest = { showQrCodeDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            QrCodeScreen(
                depositAddress = depositAddress!!,
                subuserFee = subuserFee!!,
                onClose = {
                    showQrCodeDialog = false
                    refreshTrigger++
                }
            )
        }
    }
}

@Composable
fun EmptyCardDesign(onApplyClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onApplyClick) {
            Text("Apply New Card")
        }
    }
}

@Composable
fun PaymentPendingCard(card: CardItem, onPayNowClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Gray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Payment Pending", color = Color.White)
                Button(
                    onClick = onPayNowClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonetizationOn,
                        contentDescription = "Pay Now",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("Pay Now", color = Color.White)
                }
            }

            Text(
                "**** **** **** ****",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Column {
                Text(card.nameoncard, color = Color.White)
                Text("Virtual".uppercase(), color = Color.White)
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
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF4B79A1), Color(0xFF283E51))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Virtual Card", color = Color.White)
                    TextButton(onClick = { authenticateAndNavigate() }) {
                        Icon(
                            imageVector = Icons.Filled.RemoveRedEye,
                            contentDescription = "View Now",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("View Now", color = Color.White)
                    }
                }

                Text(
                    "**** **** **** ${card.lastfour}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(card.nameoncard, color = Color.White)
                        Text(card.type.uppercase(), color = Color.White)
                    }

                    Image(
                        painter = painterResource(id = R.drawable.mastercard_logo),
                        contentDescription = "Card Logo",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyForCardBottomSheet(
    userEmail: String,
    onDismiss: () -> Unit,
    onCardApplied: () -> Unit,
    onShowQrCode: (Pair<String, String>) -> Unit
) {
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
        }.sortedBy { it.first }
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
                        onValueChange = {
                            selectedCountryName = it
                            isCountryMenuExpanded = true
                        },
                        label = { Text("Country") },
                        readOnly = false,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCountryMenuExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = isCountryMenuExpanded,
                        onDismissRequest = { isCountryMenuExpanded = false }
                    ) {
                        countries.filter { it.first.contains(selectedCountryName, ignoreCase = true) }.forEach { (name, code) ->
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
                            if (response?.status == "success" && response.depositaddress != null && response.subuserfee != null) {
                                onShowQrCode(Pair(response.depositaddress, response.subuserfee.toString()))
                            } else if (response?.status == "success") {
                                Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                                onCardApplied()
                            } else if (response?.status == "failure") {
                                Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, response?.message ?: "Application failed. Please try again.", Toast.LENGTH_LONG).show()
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
