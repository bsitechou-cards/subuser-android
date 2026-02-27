package com.app.walletcards.model

import kotlinx.serialization.Serializable

@Serializable
data class SubUser(
    val useremail: String,
    val userpass: String
)
