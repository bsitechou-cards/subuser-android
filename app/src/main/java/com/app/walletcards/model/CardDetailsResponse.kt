package com.app.walletcards.model

import kotlinx.serialization.Serializable

@Serializable
data class CardDetailsResponse(
    val code: Int,
    val status: String,
    val message: String,
    val data: CardDetails
)

@Serializable
data class CardDetails(
    val card_number: String,
    val expiry_month: String,
    val expiry_year: String,
    val cvv: String,
    val nameoncard: String,
    val balance: Double,
    val transactions: TransactionsWrapper,
    val depositaddress: String? = null,
    val btcdepositaddress: String? = null,
    val ethdepositaddress: String? = null,
    val usdtdepositaddress: String? = null,
    val soldepositaddress: String? = null,
    val bnbdepositaddress: String? = null,
    val xrpdepositaddress: String? = null,
    val paxgdepositaddress: String? = null



)

@Serializable
data class TransactionsWrapper(
    val response: TransactionResponse
)

@Serializable
data class TransactionResponse(
    val items: List<TransactionItem>
)

@Serializable
data class TransactionItem(
    val id: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val paymentDateTime: String,
    val merchant: Merchant,
    val type: String
)

@Serializable
data class Merchant(
    val name: String,
    val city: String,
    val country: String
)