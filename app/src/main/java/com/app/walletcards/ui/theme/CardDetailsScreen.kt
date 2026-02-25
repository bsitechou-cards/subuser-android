package com.app.walletcards.ui.theme

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.app.walletcards.R
import com.app.walletcards.model.CardDetails
import com.app.walletcards.model.CardDetailsResponse
import com.app.walletcards.model.ThreeDSResponse
import com.app.walletcards.network.CardApiService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()
    var isSheetOpen by remember { mutableStateOf(false) }
    var is3dsSheetOpen by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Load card details
    LaunchedEffect(Unit) {
        scope.launch {
            response = CardApiService.getDigitalCardDetails(userEmail, cardId)
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {

            Spacer(modifier = Modifier.height(48.dp)) // space for overlay top row

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (response?.data != null) {
                val card = response!!.data!!

                // Flippable card
                FlippableCard(card = card)

                Spacer(modifier = Modifier.height(32.dp))

                // Balance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Balance: $${"%.2f".format(card.balance)}",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    IconButton(onClick = { isSheetOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Show deposit addresses"
                        )
                    }
                    IconButton(onClick = { 
                        scope.launch {
                            val apiResponse = CardApiService.check3ds(userEmail, cardId)
                            if (apiResponse == null) {
                                Toast.makeText(context, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show()
                            } else if (apiResponse.code == "422") {
                                Toast.makeText(context, "No 3DS Request", Toast.LENGTH_SHORT).show()
                            } else if (apiResponse.code == "200") {
                                threeDSResponse = apiResponse
                                is3dsSheetOpen = true
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "3DS"
                        )
                    }
                }

                // Transactions title
                Text(
                    "Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Transaction list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(card.transactions.response.items) { transaction ->
                        val isPayment = transaction.type.lowercase() == "payment"
                        val amountColor = if (isPayment) Color(0xFFEA4335) else Color(0xFF34A853)
                        val iconRes = if (isPayment) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = transaction.type,
                                    modifier = Modifier.size(36.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = transaction.merchant.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black
                                    )
                                    val rawDate = transaction.paymentDateTime // e.g., "2026-02-24T08:15:00Z"
                                    val formattedDate = try {
                                        val parsed = ZonedDateTime.parse(rawDate)
                                        parsed.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
                                    } catch (e: Exception) {
                                        rawDate // fallback if parsing fails
                                    }
                                    Text(
                                        text = formattedDate,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }

                                Text(
                                    text = if (isPayment) "-$${"%.2f".format(transaction.amount)}" else "+$${"%.2f".format(transaction.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    color = amountColor
                                )
                            }
                        }
                    }
                }
            } else {
                Text("Could not load card details.", modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }

        // Overlayed top row: Title left, back button right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Card Details",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back"
                )
            }
        }

        if (isSheetOpen) {
            ModalBottomSheet(
                onDismissRequest = { isSheetOpen = false },
                sheetState = sheetState,
            ) {
                response?.data?.let { card ->
                    LazyColumn(
                        modifier = Modifier.navigationBarsPadding(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        item { 
                            Text(
                                "Deposit Addresses",
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }

                        item { DepositAddressRow("USDC", card.depositaddress?.removePrefix("USDC-POLYGON-")) }
                        item { DepositAddressRow("BTC", card.btcdepositaddress?.removePrefix("BTC-")) }
                        item { DepositAddressRow("ETH", card.ethdepositaddress?.removePrefix("ETH-")) }
                        item { DepositAddressRow("USDT", card.usdtdepositaddress?.removePrefix("USDT-BSC|BEP20-")) }
                        item { DepositAddressRow("SOL", card.soldepositaddress?.removePrefix("SOL-")) }
                        item { DepositAddressRow("BNB", card.bnbdepositaddress?.removePrefix("BNB-BSC-")) }
                        item { DepositAddressRow("XRP", card.xrpdepositaddress?.removePrefix("XRP-BSC-")) }
                        item { DepositAddressRow("PAXG", card.paxgdepositaddress?.removePrefix("PAXG-")) }

                        item { Spacer(modifier = Modifier.height(16.dp)) }
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

@Composable
fun DepositAddressRow(label: String, address: String?) {
    if (!address.isNullOrBlank()) {
        val clipboardManager = LocalClipboardManager.current
        val context = LocalContext.current
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { 
                    clipboardManager.setText(AnnotatedString(address))
                    Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Address")
                }
            }
            Text(text = address, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Divider(modifier = Modifier.padding(top = 8.dp))
        }
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
                        colors = listOf(Color(0xFF4B79A1), Color(0xFF283E51))
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
                        Text("Cardholder", color = Color.LightGray, fontSize = 12.sp)
                        Text(
                            card.nameoncard.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Expiry", color = Color.LightGray, fontSize = 12.sp)
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
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End)
                )

                // Hologram placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .align(Alignment.Start)
                )
            }
        }
    }
}