package com.app.walletcards.model

import kotlinx.serialization.Serializable

@Serializable
data class CardItem(
    val cardid: String?,
    val nameoncard: String,
    val useremail: String,
    val lastfour: String = "",
    val brand: String,
    val type: String,
    val paidcard: Int = 1,
    val depositaddress: String? = null
)
