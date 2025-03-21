package com.example.antibully.data.api

import retrofit2.Response
import retrofit2.http.*
import com.example.antibully.data.models.MessageRequest
import com.example.antibully.data.models.MessageResponse

interface MessageApiService {

    @GET("flaggedMessages")
    suspend fun getAllFlaggedMessages(): Response<List<MessageRequest>>

    @POST("flaggedMessages")
    suspend fun addMessage(@Body message: MessageRequest): Response<MessageResponse>

    @PATCH("flaggedMessages/{id}")
    suspend fun updateMessageFlag(
        @Path("id") id: String,
        @Body updateData: Map<String, Any>
    ): Response<MessageRequest>

    @DELETE("flaggedMessages/{id}")
    suspend fun deleteMessage(@Path("id") id:String): Response<Void>
}
