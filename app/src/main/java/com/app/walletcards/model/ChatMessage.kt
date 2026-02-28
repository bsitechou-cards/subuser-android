package com.app.walletcards.model

sealed class ChatMessage {
    data class Question(val text: String, val field: String) : ChatMessage()
    data class Answer(val text: String, val isSensitive: Boolean = false) : ChatMessage()
}
