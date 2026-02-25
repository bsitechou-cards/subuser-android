package com.app.walletcards.network

import com.app.walletcards.model.CardDetailsResponse
import com.app.walletcards.model.ThreeDSResponse
import com.app.walletcards.model.CardResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

object CardApiService {
    private val client = OkHttpClient()

    suspend fun getAllDigitalCards(userEmail: String): CardResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = """
                    {
                        "useremail": "$userEmail"
                    }
                """.trimIndent()

                val requestBody =
                    jsonBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(ApiConfig.BASE_URL + "getalldigital")
                    .post(requestBody)
                    .addHeader("publickey", ApiConfig.PUBLIC_KEY)
                    .addHeader("secretkey", ApiConfig.SECRET_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                response.body?.string()?.let {
                    Json { ignoreUnknownKeys = true }
                        .decodeFromString(CardResponse.serializer(), it)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun getDigitalCardDetails(email: String, cardId: String): CardDetailsResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = """
                    {
                        "useremail": "$email",
                        "cardid": "$cardId"
                    }
                """.trimIndent()

                val requestBody =
                    jsonBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(ApiConfig.BASE_URL + "getdigitalcard")
                    .post(requestBody)
                    .addHeader("publickey", ApiConfig.PUBLIC_KEY)
                    .addHeader("secretkey", ApiConfig.SECRET_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                response.body?.string()?.let {
                    Json { ignoreUnknownKeys = true }
                        .decodeFromString(CardDetailsResponse.serializer(), it)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun check3ds(email: String, cardId: String): ThreeDSResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = """
                    {
                        "useremail": "$email",
                        "cardid": "$cardId"
                    }
                """.trimIndent()

                val requestBody =
                    jsonBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(ApiConfig.BASE_URL + "check3ds")
                    .post(requestBody)
                    .addHeader("publickey", ApiConfig.PUBLIC_KEY)
                    .addHeader("secretkey", ApiConfig.SECRET_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                response.body?.string()?.let {
                    Json { ignoreUnknownKeys = true }
                        .decodeFromString(ThreeDSResponse.serializer(), it)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun approve3ds(email: String, cardId: String, eventId: String): okhttp3.Response? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = """
                    {
                        "useremail": "$email",
                        "cardid": "$cardId",
                        "eventId": "$eventId"
                    }
                """.trimIndent()

                val requestBody =
                    jsonBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(ApiConfig.BASE_URL + "approve3ds")
                    .post(requestBody)
                    .addHeader("publickey", ApiConfig.PUBLIC_KEY)
                    .addHeader("secretkey", ApiConfig.SECRET_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute()

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}