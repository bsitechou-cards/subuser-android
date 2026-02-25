package com.app.walletcards.model

import kotlinx.serialization.Serializable

@Serializable
data class ThreeDSResponse(
    val status: String,
    val data: ThreeDSData? = null,
    val code: String
)

@Serializable
data class ThreeDSData(
    val id: Int,
    val eventId: String,
    val cardId: String,
    val merchantName: String,
    val maskedPan: String,
    val merchantAmount: String,
    val merchantCurrency: String,
    val eventName: String,
    val status: String,
    val json: String,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String? = null
)