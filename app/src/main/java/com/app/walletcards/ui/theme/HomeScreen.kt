package com.app.walletcards.ui.theme

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.app.walletcards.util.LocalizationUtil

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    navController: NavHostController
) {
    val auth = FirebaseAuth.getInstance()
    val userEmail = auth.currentUser?.email ?: ""
    val context = LocalContext.current

    var cardResponse by remember { mutableStateOf<CardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    var isSheetOpen by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var depositAddress by remember { mutableStateOf<String?>(null) }
    var subuserFee by remember { mutableStateOf<Double?>(null) }


    LaunchedEffect(refreshTrigger) {
        isLoading = true
        scope.launch {
            val response = CardApiService.getAllDigitalCards(userEmail)
            if (response?.code == "401") {
                Toast.makeText(context, LocalizationUtil.getString("user_not_found"), Toast.LENGTH_LONG).show()
                onLogout()
            } else {
                cardResponse = response
            }
            isLoading = false
        }
    }

    val isRefreshing = isLoading && cardResponse != null
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = { refreshTrigger++ })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .navigationBarsPadding()
    ) {

        // Top Row: App Icon + Dynamic App Name + Quick Actions
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "App Icon",
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        fontSize = 18.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionItem(
                        icon = Icons.Default.Add,
                        label = LocalizationUtil.getString("apply_new"),
                        size = 38.dp,
                        onClick = { isSheetOpen = true }
                    )
                    QuickActionItem(
                        icon = Icons.Default.Settings,
                        label = LocalizationUtil.getString("settings"),
                        size = 38.dp,
                        onClick = { isSettingsOpen = true }
                    )
                }
            }
        }

        Box(Modifier.pullRefresh(pullRefreshState)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                val cards = cardResponse?.data ?: emptyList()
                val issuedCards = cards.filter { it.cardid != null }
                val pendingCards = cards.filter { it.cardid == null }

                // 1. HERO SECTION: MY CARDS
                item {
                    Text(
                        LocalizationUtil.getString("my_cards"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 12.dp)
                    )
                }

                if (isLoading && cardResponse == null) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                            ShimmerCardItem()
                        }
                    }
                } else if (issuedCards.isEmpty()) {
                    item {
                        EmptyCardDesign(onApplyClick = { isSheetOpen = true })
                    }
                } else {
                    item {
                        val pagerState = rememberPagerState { issuedCards.size }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                pageSpacing = 16.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                CardDesign(
                                    card = issuedCards[page],
                                    onViewClick = {
                                        navController.navigate("cardDetails/${issuedCards[page].cardid}")
                                    }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Pager Indicator
                            Row(
                                modifier = Modifier.padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(issuedCards.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else Color.LightGray
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(if (pagerState.currentPage == iteration) 12.dp else 8.dp)
                                            .height(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. PENDING ACTIONS SECTION
                if (pendingCards.isNotEmpty()) {
                    item {
                        Text(
                            LocalizationUtil.getString("action_required"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, top = 32.dp, bottom = 12.dp)
                        )
                    }

                    items(pendingCards) { card ->
                        PendingCardItem(
                            card = card,
                            onPayNowClick = {
                                depositAddress = card.depositaddress
                                subuserFee = cardResponse?.subuserfee
                                showQrCodeDialog = true
                            }
                        )
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
        ApplyForCardBottomSheet(
            userEmail = userEmail,
            subuserFee = cardResponse?.subuserfee ?: 0.0,
            onDismiss = { isSheetOpen = false },
            onCardApplied = {
                isSheetOpen = false
                refreshTrigger++
            },
            onShowQrCode = {
                isSheetOpen = false
                depositAddress = it.first
                subuserFee = it.second.toDoubleOrNull()
                showQrCodeDialog = true
            }
        )
    }

    if (isSettingsOpen) {
        SettingsBottomSheet(
            onDismiss = { isSettingsOpen = false },
            onLogout = {
                isSettingsOpen = false
                onLogout()
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(onDismiss: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showLanguagePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            Text(
                LocalizationUtil.getString("settings"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(24.dp)
            )

            // Language Option
            ListItem(
                headlineContent = { Text(LocalizationUtil.getString("language")) },
                supportingContent = { 
                    val currentLang = LocalizationUtil.supportedLanguages.find { it.second == LocalizationUtil.selectedLanguage }
                    Text(currentLang?.first ?: "English")
                },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                modifier = Modifier.clickable { showLanguagePicker = true }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)

            // Logout Option
            ListItem(
                headlineContent = { Text(LocalizationUtil.getString("logout"), color = Color.Red) },
                leadingContent = { Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Color.Red) },
                modifier = Modifier.clickable { onLogout() }
            )
        }
    }

    if (showLanguagePicker) {
        AlertDialog(
            onDismissRequest = { showLanguagePicker = false },
            title = { Text(LocalizationUtil.getString("select_language")) },
            text = {
                LazyColumn {
                    items(LocalizationUtil.supportedLanguages) { (label, code, _) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    LocalizationUtil.saveLanguage(context, code)
                                    showLanguagePicker = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = LocalizationUtil.selectedLanguage == code, onClick = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    label: String, 
    size: androidx.compose.ui.unit.Dp = 56.dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon, 
                    contentDescription = label, 
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp
        )
    }
}

@Composable
fun PendingCardItem(card: com.app.walletcards.model.CardItem, onPayNowClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color.LightGray,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            LocalizationUtil.getString("awaiting_payment"),
                            color = Color(0xFFE65100),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Virtual Card for ${card.nameoncard}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onPayNowClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(LocalizationUtil.getString("pay_now"), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ShimmerCardItem() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(brush, shape = RoundedCornerShape(16.dp))
    )
}

@Composable
fun EmptyCardDesign(onApplyClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CreditCard,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("No active cards yet", color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onApplyClick) {
            Text(LocalizationUtil.getString("apply_new"))
        }
    }
}

@Composable
fun CardDesign(
    card: com.app.walletcards.model.CardItem,
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
            onViewClick()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF2B2B2B), Color(0xFF000000))
                    )
                )
                .clickable { authenticateAndNavigate() }
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
                    Text("Virtual Card", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                    Icon(
                        imageVector = Icons.Default.RemoveRedEye,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    "**** **** **** ${card.lastfour}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(card.nameoncard.uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        Text(card.type.uppercase(), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }

                    Image(
                        painter = painterResource(id = R.drawable.mastercard_logo),
                        contentDescription = "Card Logo",
                        modifier = Modifier.size(44.dp)
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
    subuserFee: Double,
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

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    var countrySearchText by remember { mutableStateOf("") }
    var codeSearchText by remember { mutableStateOf("") }

    val countries = remember {
        Locale.getISOCountries().map {
            @Suppress("DEPRECATION")
            val locale = Locale("", it)
            locale.displayCountry to it
        }.sortedBy { it.first }
    }

    val steps = listOf(
        LocalizationUtil.getString("step_fee").format(subuserFee + 5),
        LocalizationUtil.getString("step_first_name"),
        LocalizationUtil.getString("step_last_name"),
        LocalizationUtil.getString("step_dob"),
        LocalizationUtil.getString("step_country_code"),
        LocalizationUtil.getString("step_phone"),
        LocalizationUtil.getString("step_address"),
        LocalizationUtil.getString("step_city"),
        LocalizationUtil.getString("step_state"),
        LocalizationUtil.getString("step_country"),
        LocalizationUtil.getString("step_postal")
    )

    val fields = listOf(
        "feeConfirmation", "firstName", "lastName", "dob", "countryCode", "phone", "address1", "city", "state", "country", "postalCode"
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
        if (finalValue.isBlank() && currentStep != 9 && currentStep != 3 && currentStep != 4 && currentStep != 0 && currentStep != 10) return

        if (currentStep == 5) {
            if (!finalValue.all { it.isDigit() }) {
                messages.add(ChatMessage.Answer(finalValue))
                scope.launch {
                    inputValue = ""
                    isBotTyping = true
                    delay(1000)
                    isBotTyping = false
                    messages.add(ChatMessage.Question(LocalizationUtil.getString("invalid_phone"), fields[currentStep]))
                }
                return
            }
        }

        val displayValue = when (currentStep) {
            0 -> LocalizationUtil.getString("confirm")
            3 -> finalValue
            4 -> "+$finalValue"
            9 -> countries.find { it.second == finalValue }?.first ?: finalValue
            else -> finalValue
        }

        messages.add(ChatMessage.Answer(displayValue))
        
        when (currentStep) {
            1 -> firstName = finalValue
            2 -> lastName = finalValue
            3 -> dob = finalValue
            4 -> countryCode = finalValue
            5 -> phone = finalValue
            6 -> address1 = finalValue
            7 -> city = finalValue
            8 -> state = finalValue
            9 -> country = finalValue
            10 -> postalCode = finalValue
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
            keyboardController?.hide()
            scope.launch {
                isBotTyping = true
                delay(1000)
                isBotTyping = false
                messages.add(ChatMessage.Question(LocalizationUtil.getString("thank_you_apply"), "done"))
                delay(1500)
                isSubmitting = true
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
                    phone = phone.filter { it.isDigit() }
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
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Text(
                LocalizationUtil.getString("apply_for_card"),
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
                                BotAvatarIcon()
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color(0xFFF1F3F4),
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
                                    val displayText = if (message.isSensitive) {
                                        "*".repeat(message.text.length)
                                    } else {
                                        message.text
                                    }
                                    Text(
                                        text = displayText,
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
                        BotTypingIndicator()
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

            if (!isSubmitting && !isBotTyping) {
                Surface(tonalElevation = 2.dp, shadowElevation = 8.dp) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        when (currentStep) {
                            0 -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                                        Text(LocalizationUtil.getString("cancel"))
                                    }
                                    Button(onClick = { handleNext("apply") }, modifier = Modifier.weight(1f)) {
                                        Text(LocalizationUtil.getString("apply"))
                                    }
                                }
                            }
                            3 -> {
                                OutlinedTextField(
                                    value = dob,
                                    onValueChange = {},
                                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                                    readOnly = true,
                                    enabled = false,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    label = { Text(LocalizationUtil.getString("select_dob")) },
                                    trailingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) }
                                )
                                
                                if (showDatePicker) {
                                    DatePickerDialog(
                                        onDismissRequest = { showDatePicker = false },
                                        confirmButton = {
                                            TextButton(onClick = {
                                                datePickerState.selectedDateMillis?.let {
                                                    val date = dateFormatter.format(Date(it))
                                                    dob = date
                                                    handleNext(date)
                                                }
                                                showDatePicker = false
                                            }) { Text(LocalizationUtil.getString("done")) }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showDatePicker = false }) { Text(LocalizationUtil.getString("cancel")) }
                                        }
                                    ) {
                                        DatePicker(state = datePickerState)
                                    }
                                }
                            }
                            4 -> {
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
                                                            codeSearchText = ""
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
                                        label = { Text(LocalizationUtil.getString("search_country_code")) },
                                        placeholder = { Text("e.g. United States or 1") },
                                        trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        shape = RoundedCornerShape(24.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                            9 -> {
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
                                                            countrySearchText = ""
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
                                        label = { Text(LocalizationUtil.getString("search_country")) },
                                        trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        shape = RoundedCornerShape(24.dp)
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
                                        keyboardOptions = if (currentStep == 4 || currentStep == 5) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default
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

@Composable
fun BotAvatarIcon() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun BotTypingIndicator() {
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BotAvatarIcon()
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            color = Color(0xFFF1F3F4),
            shape = RoundedCornerShape(0.dp, 12.dp, 12.dp, 12.dp)
        ) {
            Text(
                text = "...",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
