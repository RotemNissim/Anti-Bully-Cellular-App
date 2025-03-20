package com.example.antibully.api

import retrofit2.Call
import retrofit2.http.*

data class Message(
    val id: String,
    val userId: String,
    val text: String,
    val imageUrl: String?,
    val flagged: Boolean,
    val reason: String?
)

interface MessageApiService {
    @GET("flaggedMessages")
    fun getAllFlaggedMessages(): Call<List<Message>>

    @POST("flaggedMessages")
    fun addMessage(@Body message: Message): Call<Message>

    @PATCH("flaggedMessages/{id}")
    fun updateMessageFlag(@Path("id") id: String, @Body updateData: Map<String, Any>): Call<Message>

    @DELETE("flaggedMessages/{id}")
    fun deleteMessage(@Path("id") id: String): Call<Void>
}
