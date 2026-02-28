package com.app.walletcards.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.walletcards.R
import com.app.walletcards.model.ChatMessage
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    val messagesList = listOf(
        "Virtual card issuing at its best",
        "We are available in 100+ countries",
        "Add to wallets Google pay and Apple pay(Comingsoon)"
    )

    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        messagesList.forEach { msg ->
            delay(1000)
            chatMessages.add(ChatMessage.Question(msg, ""))
        }
        delay(1500)
        onTimeout()
    }

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher),
                contentDescription = "App Icon",
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Virtual MasterCards",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatMessages) { message ->
                if (message is ChatMessage.Question) {
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
            }
        }

        // Dummy Input Area to match Login/Register style
        Surface(tonalElevation = 2.dp, shadowElevation = 8.dp) {
            Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = { },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type your answer...") },
                        shape = RoundedCornerShape(24.dp),
                        enabled = false // Disabled during splash
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { },
                        enabled = false,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
