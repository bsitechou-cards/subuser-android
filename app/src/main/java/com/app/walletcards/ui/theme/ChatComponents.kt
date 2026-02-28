package com.app.walletcards.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.app.walletcards.R

@Composable
fun BotAvatar() {
    Box(modifier = Modifier.size(40.dp)) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color.LightGray.copy(alpha = 0.2f)
        ) {
            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_round),
                contentDescription = "Bot Avatar",
                modifier = Modifier.fillMaxSize()
            )
        }
        // Online indicator
        Box(
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.BottomEnd)
                .background(Color(0xFF4CAF50), CircleShape)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.2f at 0
                1f at 200
                0.2f at 400
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )
    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.2f at 100
                1f at 300
                0.2f at 500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )
    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 600
                0.2f at 200
                1f at 400
                0.2f at 600
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BotAvatar()
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .background(Color.LightGray.copy(alpha = 0.2f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Dot(alpha = dotAlpha1)
            Dot(alpha = dotAlpha2)
            Dot(alpha = dotAlpha3)
        }
    }
}

@Composable
private fun Dot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(Color.Gray.copy(alpha = alpha), CircleShape)
    )
}
