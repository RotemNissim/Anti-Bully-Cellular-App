package com.example.antibully.data.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class MessageRequest(
    val messageId: String,
    val userId: String,
    val text: String?,
    val flagged: Boolean,
    val reason: String?
)

interface FlagApiService {
    @POST("flaggedMessages") // No need to add prefix, it's handled in RetrofitClient
    fun flagMessage(@Body request: MessageRequest): Call<MessageRequest>
}
