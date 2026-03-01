package com.app.walletcards.ui.theme

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import com.app.walletcards.R
import com.app.walletcards.model.ApplyCardRequest
import com.app.walletcards.model.CardItem
import com.app.walletcards.model.CardResponse
import com.app.walletcards.model.ChatMessage
import com.app.walletcards.network.CardApiService
import com.app.walletcards.util.countryCodes

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
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
    var refreshTrigger by remember { mutableIntStateOf(0) }

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
            .navigationBarsPadding() // Added navigationBarsPadding for general protection
    ) {

        // Top Row: App Icon + Wallet title + Icons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.mipmap.ic_launcher),
                    contentDescription = "App Icon",
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Wallet",
                    style = MaterialTheme.typography.headlineLarge
                )
            }

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
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp) // Added bottom padding to ensure the last card isn't hidden
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

    fun authenticateAndNavigate() {
        val biometricManager = BiometricManager.from(context)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
            BiometricManager.BIOMETRIC_SUCCESS
        ) {
            val intent = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Login")
                .setSubtitle("Authenticate to view card details")
                .setNegativeButtonText("Cancel")
                .build()
            // Actually trigger prompt
            BiometricPrompt(
                context as FragmentActivity,
                ContextCompat.getMainExecutor(context),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onViewClick()
                    }
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
                    verticalAlignment = Alignment.CenterVertically) {
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
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var currentStep by remember { mutableIntStateOf(0) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var address1 by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    var inputValue by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isBotTyping by remember { mutableStateOf(false) }

    val countries = remember {
        Locale.getISOCountries().map {
            @Suppress("DEPRECATION")
            val locale = Locale("", it)
            locale.displayCountry to it
        }.sortedBy { it.first }
    }

    val steps = listOf(
        "What is your first name?",
        "What is your last name?",
        "What is your date of birth?",
        "What is your address?",
        "What is your postal code?",
        "What is your city?",
        "What is your country?",
        "What is your state?",
        "What is your country phonecode?",
        "What is your phone number?"
    )

    val fields = listOf(
        "firstName", "lastName", "dob", "address1", "postalCode", "city", "country", "state", "countryCode", "phone"
    )

    LaunchedEffect(Unit) {
        isBotTyping = true
        delay(1500)
        isBotTyping = false
        messages.add(ChatMessage.Question(steps[0], fields[0]))
    }

    LaunchedEffect(messages.size, isBotTyping) {
        if (messages.isNotEmpty() || isBotTyping) {
            listState.animateScrollToItem(if (isBotTyping) messages.size else messages.size - 1)
        }
    }

    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun handleNext(customValue: String? = null) {
        val finalValue = customValue ?: inputValue
        if (finalValue.isBlank() && currentStep != 6 && currentStep != 2 && currentStep != 8) return

        // Phone number validation
        if (currentStep == 9) {
            if (!finalValue.all { it.isDigit() }) {
                messages.add(ChatMessage.Answer(finalValue))
                scope.launch {
                    inputValue = ""
                    isBotTyping = true
                    delay(1000)
                    isBotTyping = false
                    messages.add(ChatMessage.Question("Invalid input. The phone number should not include any spaces or special characters. Please re-enter your phone number.", fields[currentStep]))
                }
                return
            }
        }

        val displayValue = when (currentStep) {
            2 -> finalValue // dob
            6 -> countries.find { it.second == finalValue }?.first ?: finalValue
            8 -> "+$finalValue"
            else -> finalValue
        }

        messages.add(ChatMessage.Answer(displayValue))
        
        when (currentStep) {
            0 -> firstName = finalValue
            1 -> lastName = finalValue
            2 -> dob = finalValue
            3 -> address1 = finalValue
            4 -> postalCode = finalValue
            5 -> city = finalValue
            6 -> country = finalValue
            7 -> state = finalValue
            8 -> countryCode = finalValue
            9 -> phone = finalValue
        }

        if (currentStep < steps.size - 1) {
            scope.launch {
                currentStep++
                inputValue = ""
                isBotTyping = true
                delay(1500)
                isBotTyping = false
                messages.add(ChatMessage.Question(steps[currentStep], fields[currentStep]))
            }
        } else {
            isSubmitting = true
            keyboardController?.hide()
            scope.launch {
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
                    phone = phone.filter { it.isDigit() } // Ensure only digits are sent
                )
                val response = CardApiService.applyForNewVirtualCard(request)
                if (response?.status == "success" && response.depositaddress != null && response.subuserfee != null) {
                    onShowQrCode(Pair(response.depositaddress, response.subuserfee.toString()))
                } else if (response?.status == "success") {
                    Toast.makeText(context, response.message, Toast.LENGTH_LONG).show()
                    onCardApplied()
                } else {
                    Toast.makeText(context, response?.message ?: "Failed", Toast.LENGTH_LONG).show()
                    onDismiss()
                }
                isSubmitting = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize(),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                "Apply for New Card",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    when (message) {
                        is ChatMessage.Question -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                BotAvatar()
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp)
                                ) {
                                    Text(
                                        text = message.text,
                                        modifier = Modifier.padding(12.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                        is ChatMessage.Answer -> {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 12.dp)
                                ) {
                                    Text(
                                        text = message.text,
                                        modifier = Modifier.padding(12.dp),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (isBotTyping) {
                    item {
                        TypingIndicator()
                    }
                }

                if (isSubmitting) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }

            // Input Area
            if (!isSubmitting && !isBotTyping) {
                Surface(tonalElevation = 2.dp, shadowElevation = 8.dp) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        when (currentStep) {
                            2 -> { // DOB Step
                                var showDatePicker by remember { mutableStateOf(false) }
                                val datePickerState = rememberDatePickerState()
                                
                                OutlinedTextField(
                                    value = dob,
                                    onValueChange = {},
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    label = { Text("Select Date of Birth") },
                                    trailingIcon = {
                                        IconButton(onClick = { showDatePicker = true }) {
                                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                        }
                                    }
                                )
                                
                                if (showDatePicker) {
                                    DatePickerDialog(
                                        onDismissRequest = { showDatePicker = false },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                datePickerState.selectedDateMillis?.let {
                                                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                                    val date = sdf.format(Date(it))
                                                    dob = date
                                                    handleNext(date)
                                                }
                                                showDatePicker = false
                                            }) { Text("OK") }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                                        }
                                    ) {
                                        DatePicker(state = datePickerState)
                                    }
                                }
                            }
                            6 -> { // Country Step
                                var countrySearchText by remember { mutableStateOf("") }
                                val filteredCountries = countries.filter { it.first.contains(countrySearchText, ignoreCase = true) }

                                Column {
                                    if (countrySearchText.isNotEmpty() && filteredCountries.isNotEmpty()) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                                .padding(bottom = 8.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            tonalElevation = 8.dp,
                                            shadowElevation = 4.dp
                                        ) {
                                            LazyColumn {
                                                items(filteredCountries) { (name, code) ->
                                                    DropdownMenuItem(
                                                        text = { Text(name) },
                                                        onClick = {
                                                            handleNext(code)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = countrySearchText,
                                        onValueChange = { countrySearchText = it },
                                        label = { Text("Search Country") },
                                        trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                }
                            }
                            8 -> { // Country Code Step
                                var codeSearchText by remember { mutableStateOf("") }
                                val filteredCodes = countryCodes.filter {
                                    it.name.contains(codeSearchText, ignoreCase = true) ||
                                            it.code.contains(codeSearchText)
                                }

                                Column {
                                    if (codeSearchText.isNotEmpty() && filteredCodes.isNotEmpty()) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                                .padding(bottom = 8.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            tonalElevation = 8.dp,
                                            shadowElevation = 4.dp
                                        ) {
                                            LazyColumn {
                                                items(filteredCodes) { countryCodeItem ->
                                                    DropdownMenuItem(
                                                        text = { Text("${countryCodeItem.name} (+${countryCodeItem.code})") },
                                                        onClick = {
                                                            handleNext(countryCodeItem.code)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(),
                                        value = codeSearchText,
                                        onValueChange = { codeSearchText = it },
                                        label = { Text("Search Country Code") },
                                        placeholder = { Text("e.g. United States or 1") },
                                        trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        shape = RoundedCornerShape(24.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                            else -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = inputValue,
                                        onValueChange = { inputValue = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Type your answer...") },
                                        shape = RoundedCornerShape(24.dp),
                                        keyboardOptions = if (currentStep == 9) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { handleNext() },
                                        enabled = inputValue.isNotBlank(),
                                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Send", tint = Color.White)
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
