package com.app.walletcards.model

import kotlinx.serialization.Serializable

@Serializable
data class ApplyCardRequest(
    val useremail: String,
    val firstname: String,
    val lastname: String,
    val dob: String,
    val address1: String,
    val postalcode: String,
    val city: String,
    val country: String,
    val state: String,
    val countrycode: String,
    val phone: String
)
