package com.app.walletcards.model

import kotlinx.serialization.Serializable

@Serializable
data class CardResponse(
    val code: Int,
    val status: String,
    val message: String,
    val data: List<CardItem>
)

