package com.app.walletcards.network

import android.util.Log
import com.app.walletcards.model.ApplyCardRequest
import com.app.walletcards.model.ApplyCardResponse
import com.app.walletcards.model.CardDetailsResponse
import com.app.walletcards.model.CardResponse
import com.app.walletcards.model.SubUser
import com.app.walletcards.model.ThreeDSResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object CardApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    suspend fun subuseradd(subUser: SubUser): ApplyCardResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = json.encodeToString(subUser)
                Log.d("CardApiService", "subuseradd request: $jsonBody")

                val requestBody =
                    jsonBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(ApiConfig.BASE_URL + "subuseradd")
                    .post(requestBody)
                    .addHeader("publickey", ApiConfig.PUBLIC_KEY)
                    .addHeader("secretkey", ApiConfig.SECRET_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("CardApiService", "subuseradd response: $responseBody")

                responseBody?.let {
                    json.decodeFromString(ApplyCardResponse.serializer(), it)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun applyForNewVirtualCard(applyCardRequest: ApplyCardRequest): ApplyCardResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonBody = json.encodeToString(applyCardRequest)
                Log.d("CardApiService", "applyForNewsubuserCard request: $jsonBody")

                val requestBody =
                    jsonBody.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(ApiConfig.BASE_URL + "digitalnewsubusercard")
                    .post(requestBody)
                    .addHeader("publickey", ApiConfig.PUBLIC_KEY)
                    .addHeader("secretkey", ApiConfig.SECRET_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("CardApiService", "applyForNewsubuserCard response: $responseBody")

                responseBody?.let {
                    json.decodeFromString(ApplyCardResponse.serializer(), it)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

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
                    .url(ApiConfig.BASE_URL + "getsubuseralldigital")
                    .post(requestBody)
                    .addHeader("publickey", ApiConfig.PUBLIC_KEY)
                    .addHeader("secretkey", ApiConfig.SECRET_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                response.body?.string()?.let {
                    json.decodeFromString(CardResponse.serializer(), it)
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
                    .url(ApiConfig.BASE_URL + "getsubuserdigitalcard")
                    .post(requestBody)
                    .addHeader("publickey", ApiConfig.PUBLIC_KEY)
                    .addHeader("secretkey", ApiConfig.SECRET_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                response.body?.string()?.let {
                    json.decodeFromString(CardDetailsResponse.serializer(), it)
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
                    .url(ApiConfig.BASE_URL + "subusercheck3ds")
                    .post(requestBody)
                    .addHeader("publickey", ApiConfig.PUBLIC_KEY)
                    .addHeader("secretkey", ApiConfig.SECRET_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                response.body?.string()?.let {
                    json.decodeFromString(ThreeDSResponse.serializer(), it)
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
                    .url(ApiConfig.BASE_URL + "subuserapprove3ds")
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

    suspend fun blockDigitalCard(email: String, cardId: String): ApplyCardResponse? {
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
                    .url(ApiConfig.BASE_URL + "subuserblockdigital")
                    .post(requestBody)
                    .addHeader("publickey", ApiConfig.PUBLIC_KEY)
                    .addHeader("secretkey", ApiConfig.SECRET_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("CardApiService", "blockDigitalCard response: $responseBody")

                responseBody?.let {
                    json.decodeFromString(ApplyCardResponse.serializer(), it)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun unblockDigitalCard(email: String, cardId: String): ApplyCardResponse? {
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
                    .url(ApiConfig.BASE_URL + "subuserunblockdigital")
                    .post(requestBody)
                    .addHeader("publickey", ApiConfig.PUBLIC_KEY)
                    .addHeader("secretkey", ApiConfig.SECRET_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("CardApiService", "unblockDigitalCard response: $responseBody")

                responseBody?.let {
                    json.decodeFromString(ApplyCardResponse.serializer(), it)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
