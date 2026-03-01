package com.app.walletcards.model

import kotlinx.serialization.Serializable

@Serializable
data class CardResponse(
    @Serializable(with = CodeSerializer::class)
    val code: String? = null,
    val status: String,
    val message: String,
    val data: List<CardItem> = emptyList(),
    val subuserfee: Double? = null
)
