package com.app.walletcards.ui.theme

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.app.walletcards.R
import com.app.walletcards.model.CardDetails
import com.app.walletcards.model.CardDetailsResponse
import com.app.walletcards.model.Deposit
import com.app.walletcards.model.ThreeDSResponse
import com.app.walletcards.model.TransactionItem
import com.app.walletcards.network.CardApiService
import com.app.walletcards.util.LocalizationUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun CardDetailsScreen(
    cardId: String,
    navController: NavHostController
) {
    val auth = FirebaseAuth.getInstance()
    val userEmail = auth.currentUser?.email ?: ""

    var response by remember { mutableStateOf<CardDetailsResponse?>(null) }
    var threeDSResponse by remember { mutableStateOf<ThreeDSResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isChecking3ds by remember { mutableStateOf(false) }
    var isTogglingBlock by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetOpen by remember { mutableStateOf(false) }
    var is3dsSheetOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Load card details
    LaunchedEffect(refreshTrigger) {
        isLoading = true
        scope.launch {
            response = CardApiService.getDigitalCardDetails(userEmail, cardId)
            isLoading = false
        }
    }

    val isRefreshing = isLoading && response != null
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = { refreshTrigger++ })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .navigationBarsPadding()
    ) {
        // Top Row Header: Dynamic App Name
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
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

                QuickActionItem(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = LocalizationUtil.getString("back"),
                    size = 40.dp,
                    onClick = { navController.popBackStack() }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(Modifier.pullRefresh(pullRefreshState)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoading && response == null) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (response?.data != null) {
                        val card = response!!.data!!

                        FlippableCard(card = card)

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = LocalizationUtil.getString("current_balance"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "$${"%.2f".format(card.balance)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(onClick = { isSheetOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Show deposit addresses"
                                    )
                                }

                                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                                    if (isChecking3ds) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        IconButton(onClick = {
                                            scope.launch {
                                                isChecking3ds = true
                                                try {
                                                    val apiResponse = CardApiService.check3ds(userEmail, cardId)
                                                    if (apiResponse == null) {
                                                        Toast.makeText(context, "An error occurred.", Toast.LENGTH_SHORT).show()
                                                    } else if (apiResponse.code == "422") {
                                                        Toast.makeText(context, "No 3DS Request", Toast.LENGTH_SHORT).show()
                                                    } else if (apiResponse.code == "200") {
                                                        threeDSResponse = apiResponse
                                                        is3dsSheetOpen = true
                                                    }
                                                } finally {
                                                    isChecking3ds = false
                                                }
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = "3DS"
                                            )
                                        }
                                    }
                                }

                                Switch(
                                    checked = card.status == "active",
                                    onCheckedChange = {
                                        scope.launch {
                                            isTogglingBlock = true
                                            try {
                                                val apiResponse = if (card.status == "active") {
                                                    CardApiService.blockDigitalCard(userEmail, cardId)
                                                } else {
                                                    CardApiService.unblockDigitalCard(userEmail, cardId)
                                                }
                                                if (apiResponse != null) {
                                                    Toast.makeText(context, apiResponse.message, Toast.LENGTH_SHORT).show()
                                                    refreshTrigger++
                                                } else {
                                                    Toast.makeText(context, "An error occurred.", Toast.LENGTH_SHORT).show()
                                                }
                                            } finally {
                                                isTogglingBlock = false
                                            }
                                        }
                                    },
                                    thumbContent = if (isTogglingBlock) {
                                        {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF006400)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val tabTitles = listOf(
                            LocalizationUtil.getString("transactions"),
                            LocalizationUtil.getString("deposits")
                        )
                        val pagerState = rememberPagerState { tabTitles.size }

                        Column(modifier = Modifier.fillMaxSize()) {
                            TabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = Color.Transparent,
                                divider = {},
                                indicator = { tabPositions ->
                                    TabRowDefaults.SecondaryIndicator(
                                        Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            ) {
                                tabTitles.forEachIndexed { index, title ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                        text = { 
                                            Text(
                                                title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                            ) 
                                        }
                                    )
                                }
                            }

                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.Top
                            ) { pageIndex ->
                                when (pageIndex) {
                                    0 -> {
                                        val groupedTransactions = remember(card.transactions.response.items) {
                                            groupItemsByDate(card.transactions.response.items) { it.paymentDateTime }
                                        }
                                        TransactionList(groupedTransactions)
                                    }
                                    1 -> {
                                        val groupedDeposits = remember(card.deposits) {
                                            groupItemsByDate(card.deposits) { it.createdAt }
                                        }
                                        DepositList(groupedDeposits)
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Could not load card details.", modifier = Modifier.align(Alignment.CenterHorizontally))
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
            ModalBottomSheet(
                onDismissRequest = { isSheetOpen = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                response?.data?.let { card ->
                    var expandedCurrency by remember { mutableStateOf<String?>(null) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                LocalizationUtil.getString("deposit_crypto"),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { isSheetOpen = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }
                        
                        Text(
                            LocalizationUtil.getString("disclaimer_non_usdc"),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                        )

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val depositOptions = listOf(
                                Triple("USDC", "Polygon Network", card.depositaddress?.removePrefix("USDC-POLYGON-")) to Color(0xFF8247E5),
                                Triple("BTC", "Bitcoin Network", card.btcdepositaddress?.removePrefix("BTC-")) to Color(0xFFF7931A),
                                Triple("ETH", "Ethereum Network", card.ethdepositaddress?.removePrefix("ETH-")) to Color(0xFF627EEA),
                                Triple("USDT", "BSC | BEP20", card.usdtdepositaddress?.removePrefix("USDT-BSC|BEP20-")) to Color(0xFF26A17B),
                                Triple("SOL", "Solana Network", card.soldepositaddress?.removePrefix("SOL-")) to Color(0xFF14F195),
                                Triple("BNB", "Binance Smart Chain", card.bnbdepositaddress?.removePrefix("BNB-BSC-")) to Color(0xFFF3BA2F),
                                Triple("XRP", "Ripple Network", card.xrpdepositaddress?.removePrefix("XRP-BSC-")) to Color(0xFF23292F),
                                Triple("PAXG", "Pax Gold Network", card.paxgdepositaddress?.removePrefix("PAXG-")) to Color(0xFFE6B34B)
                            )

                            items(depositOptions) { (info, color) ->
                                val (currency, network, address) = info
                                if (!address.isNullOrBlank()) {
                                    PremiumDepositCard(
                                        currency = currency,
                                        network = network,
                                        address = address,
                                        networkColor = color,
                                        isExpanded = expandedCurrency == currency,
                                        onExpandClick = {
                                            expandedCurrency = if (expandedCurrency == currency) null else currency
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (is3dsSheetOpen) {
            threeDSResponse?.let {
                ThreeDSBottomSheet(it, cardId, userEmail) { is3dsSheetOpen = false }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionList(groupedTransactions: Map<String, List<TransactionItem>>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groupedTransactions.forEach { (date, transactions) ->
            stickyHeader {
                DateHeader(date)
            }
            items(transactions) { transaction ->
                TransactionRow(transaction)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DepositList(groupedDeposits: Map<String, List<Deposit>>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groupedDeposits.forEach { (date, deposits) ->
            stickyHeader {
                DateHeader(date)
            }
            items(deposits) { deposit ->
                DepositRow(deposit)
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun DateHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F9FA))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = date.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun TransactionRow(transaction: TransactionItem) {
    val isPayment = transaction.type.lowercase() == "payment"
    val amountColor = if (isPayment) Color.Black else Color(0xFF34A853)
    val prefix = if (isPayment) "-" else "+"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Styled Icon Circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F3F4)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = transaction.merchant.name.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.merchant.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = try {
                    val parsed = ZonedDateTime.parse(transaction.paymentDateTime)
                    parsed.format(DateTimeFormatter.ofPattern("hh:mm a"))
                } catch (e: Exception) { "" },
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        Text(
            text = "$prefix$${"%.2f".format(transaction.amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
fun DepositRow(deposit: Deposit) {
    val amount = deposit.amount / 1_000_000.0
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = null,
                tint = Color(0xFF34A853),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = LocalizationUtil.getString("crypto_deposit"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = deposit.transactionHash.take(8) + "..." + deposit.transactionHash.takeLast(8),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1
            )
        }

        Text(
            text = "+$${"%.2f".format(amount)}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF34A853)
        )
    }
}

fun <T> groupItemsByDate(
    items: List<T>,
    dateSelector: (T) -> String
): Map<String, List<T>> {
    return items.sortedByDescending { dateSelector(it) }.groupBy { item ->
        try {
            val dateTime = ZonedDateTime.parse(dateSelector(item))
            val date = dateTime.toLocalDate()
            val now = LocalDate.now()
            
            when {
                date == now -> LocalizationUtil.getString("today")
                date == now.minusDays(1) -> LocalizationUtil.getString("yesterday")
                else -> date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
            }
        } catch (e: Exception) {
            "Unknown Date"
        }
    }
}

@Composable
fun PremiumDepositCard(
    currency: String,
    network: String,
    address: String?,
    networkColor: Color,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    if (address.isNullOrBlank()) return

    var isCopied by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(networkColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            currency.take(1),
                            color = networkColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(currency, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Surface(
                            color = networkColor.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                network,
                                color = networkColor,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(address))
                        isCopied = true
                        scope.launch {
                            delay(2000)
                            isCopied = false
                        }
                    }) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = if (isCopied) Color(0xFF34A853) else Color.Gray
                        )
                    }
                    IconButton(onClick = { onExpandClick() }) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Code",
                            tint = Color.Gray
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        address,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    val qrCodeBitmap = remember(address) { generateQrCode(address) }
                    qrCodeBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(160.dp)
                                .shadow(4.dp, RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        "Send only $currency to this address via $network network.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun generateQrCode(text: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

@Composable
fun FlippableCard(card: CardDetails) {
    var flipped by remember { mutableStateOf(false) }
    val animatedRotationY by animateFloatAsState(targetValue = if (flipped) 180f else 0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clickable { flipped = !flipped }
    ) {

        // FRONT SIDE
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = animatedRotationY
                    cameraDistance = 12 * density
                    alpha = if (animatedRotationY <= 90f) 1f else 0f
                }
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF2B2B2B), Color(0xFF000000))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Row: Chip + Logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp, 30.dp)
                            .background(Color.Yellow, shape = RoundedCornerShape(4.dp))
                    ) // Simulated chip
                    Image(
                        painter = painterResource(id = R.drawable.mastercard_logo), // replace with your logo drawable
                        contentDescription = "Card Logo",
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Card number
                Text(
                    text = card.card_number.chunked(4).joinToString(" "),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                // Cardholder and Expiry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(LocalizationUtil.getString("cardholder"), color = Color.LightGray, fontSize = 12.sp)
                        Text(
                            card.nameoncard.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(LocalizationUtil.getString("expiry"), color = Color.LightGray, fontSize = 12.sp)
                        Text(
                            "${card.expiry_month}/${card.expiry_year}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // BACK SIDE
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = animatedRotationY - 180f // flip the back
                    cameraDistance = 12 * density
                    alpha = if (animatedRotationY > 90f) 1f else 0f
                }
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF2B2B2B), Color(0xFF4A4A4A))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                // Signature panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.7f))
                ) {
                    Text(
                        "Authorized Signature",
                        color = Color.Black,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                    )
                }

                // CVV
                Text(
                    "CVV: ${card.cvv}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )

                // Billing Address
                Column(modifier = Modifier.align(Alignment.Start)) {
                    Text(LocalizationUtil.getString("billing_address"), color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${card.address1 ?: ""}, ${card.city ?: ""}, ${card.state ?: ""}, ${"United Kingdom"}, ${card.postalCode ?: ""}", color = Color.White,fontSize = 16.sp,)
                }

                // Hologram placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .align(Alignment.End)
                )
            }
        }
    }
}
