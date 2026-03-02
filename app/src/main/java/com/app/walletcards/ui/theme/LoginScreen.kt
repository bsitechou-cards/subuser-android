package com.app.walletcards.ui.theme

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.walletcards.R
import com.app.walletcards.model.ChatMessage
import com.app.walletcards.util.LocalizationUtil
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
    var isSettingsOpen by remember { mutableStateOf(false) }

    LaunchedEffect(LocalizationUtil.selectedLanguage) {
        messages.clear()
        currentStep = 0 // Reset step to 0 when language changes to match the welcome message
        inputValue = ""
        isBotTyping = true
        delay(1000)
        isBotTyping = false
        messages.add(ChatMessage.Question(LocalizationUtil.getString("welcome_registered"), "is_registered"))
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
                val yesTranslated = LocalizationUtil.getString("yes")
                if (finalValue.equals(yesTranslated, ignoreCase = true) || 
                    finalValue.equals("yes", ignoreCase = true) || 
                    finalValue.equals("oui", ignoreCase = true)) {
                    scope.launch {
                        currentStep = 1
                        inputValue = ""
                        isBotTyping = true
                        delay(1000)
                        isBotTyping = false
                        messages.add(ChatMessage.Question(LocalizationUtil.getString("great_email"), "email"))
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
                    delay(1000)
                    isBotTyping = false
                    messages.add(ChatMessage.Question(LocalizationUtil.getString("now_password"), "password"))
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
                        messages.add(ChatMessage.Question(LocalizationUtil.getString("invalid_password_format"), "password"))
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
                                    delay(1000)
                                    isBotTyping = false
                                    messages.add(ChatMessage.Question("${LocalizationUtil.getString("login_failed")}: $errorMsg", "email"))
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
        // Top Bar: Identical to Home/Details screen
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                    icon = Icons.Default.Settings,
                    label = LocalizationUtil.getString("settings"),
                    size = 38.dp,
                    onClick = { isSettingsOpen = true }
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
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
                                        onClick = { handleNext(LocalizationUtil.getString("yes")) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(LocalizationUtil.getString("yes"))
                                    }
                                    OutlinedButton(
                                        onClick = { handleNext(LocalizationUtil.getString("no")) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(LocalizationUtil.getString("no"))
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
                                            Text(if (currentStep == 1) LocalizationUtil.getString("enter_email") else LocalizationUtil.getString("enter_password")) 
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
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onRegisterClick) {
                            Text(LocalizationUtil.getString("register"), style = MaterialTheme.typography.bodySmall)
                        }
                        Text("|", color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))
                        TextButton(onClick = {
                            if (email.isNotBlank()) {
                                auth.sendPasswordResetEmail(email).addOnCompleteListener {
                                    Toast.makeText(
                                        context, 
                                        LocalizationUtil.getString("reset_link_sent"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                Toast.makeText(context, LocalizationUtil.getString("enter_email_first"), Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text(LocalizationUtil.getString("forgot_password"), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (isSettingsOpen) {
        LoginSettingsBottomSheet(onDismiss = { isSettingsOpen = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSettingsBottomSheet(onDismiss: () -> Unit) {
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
