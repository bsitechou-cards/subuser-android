package com.app.walletcards.model

import kotlinx.serialization.Serializable

@Serializable
data class ApplyCardResponse(
    val code: Int,
    val status: String = "",
    val message: String = ""
)
