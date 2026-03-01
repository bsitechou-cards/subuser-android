package com.app.walletcards.model

import kotlinx.serialization.Serializable

@Serializable
data class ApplyCardResponse(
    @Serializable(with = CodeSerializer::class)
    val code: String? = null,
    val status: String = "",
    val message: String = "",
    val depositaddress: String? = null,
    val subuserfee: Double? = null
)
