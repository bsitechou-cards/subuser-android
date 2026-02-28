package com.app.walletcards.ui.theme

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.app.walletcards.R
import com.app.walletcards.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()
    
    var currentStep by remember { mutableIntStateOf(0) } // 0: Registered?, 1: Email, 2: Password
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var inputValue by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isBotTyping by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isBotTyping = true
        delay(1500)
        isBotTyping = false
        messages.add(ChatMessage.Question("Welcome! Are you already registered?", "is_registered"))
    }

    LaunchedEffect(messages.size, isBotTyping) {
        if (messages.isNotEmpty() || isBotTyping) {
            listState.animateScrollToItem(if (isBotTyping) messages.size else messages.size - 1)
        }
    }

    fun handleNext(customValue: String? = null) {
        val finalValue = customValue ?: inputValue
        if (finalValue.isBlank() && currentStep != 0) return

        val isSensitive = currentStep == 2
        messages.add(ChatMessage.Answer(finalValue, isSensitive))

        when (currentStep) {
            0 -> {
                if (finalValue.lowercase() == "yes") {
                    scope.launch {
                        currentStep = 1
                        inputValue = ""
                        isBotTyping = true
                        delay(1500)
                        isBotTyping = false
                        messages.add(ChatMessage.Question("Great! Please enter your email address.", "email"))
                    }
                } else {
                    onRegisterClick()
                }
            }
            1 -> {
                email = finalValue
                scope.launch {
                    currentStep = 2
                    inputValue = ""
                    isBotTyping = true
                    delay(1500)
                    isBotTyping = false
                    messages.add(ChatMessage.Question("Now, please enter your password.", "password"))
                }
            }
            2 -> {
                password = finalValue
                if (password.length != 6 || !password.all { it.isLetterOrDigit() }) {
                    scope.launch {
                        inputValue = ""
                        isBotTyping = true
                        delay(1000)
                        isBotTyping = false
                        messages.add(ChatMessage.Question("Password must be 6 alphanumeric characters. Please try again.", "password"))
                    }
                } else {
                    isLoading = true
                    keyboardController?.hide()
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                scope.launch {
                                    val errorMsg = task.exception?.message ?: "Invalid credentials"
                                    isBotTyping = true
                                    delay(1500)
                                    isBotTyping = false
                                    messages.add(ChatMessage.Question("Login failed: $errorMsg. Let's try your email again.", "email"))
                                    currentStep = 1
                                    inputValue = ""
                                }
                            }
                        }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                "Login",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

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
                    TypingIndicator()
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        // Input Area
        if (!isLoading && !isBotTyping) {
            Surface(tonalElevation = 2.dp, shadowElevation = 8.dp) {
                Column {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        when (currentStep) {
                            0 -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { handleNext("Yes") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Yes")
                                    }
                                    OutlinedButton(
                                        onClick = { handleNext("No") },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("No")
                                    }
                                }
                            }
                            else -> {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = inputValue,
                                        onValueChange = { inputValue = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { 
                                            Text(if (currentStep == 1) "Enter email..." else "Enter password...") 
                                        },
                                        shape = RoundedCornerShape(24.dp),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = if (currentStep == 1) KeyboardType.Email else KeyboardType.Password
                                        ),
                                        visualTransformation = if (currentStep == 2) PasswordVisualTransformation() else VisualTransformation.None
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
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = {
                            if (email.isNotBlank()) {
                                auth.sendPasswordResetEmail(email).addOnCompleteListener {
                                    Toast.makeText(
                                        context, 
                                        "If this email is registered, a password reset link has been sent.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                Toast.makeText(context, "Please enter your email address in the chat first.", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Forgot Password?", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
