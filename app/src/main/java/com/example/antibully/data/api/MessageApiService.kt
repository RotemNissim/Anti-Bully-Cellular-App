package com.example.antibully.data.api

import retrofit2.Response
import retrofit2.http.*
import com.example.antibully.data.models.MessageRequest
import com.example.antibully.data.models.MessageResponse
import com.example.antibully.data.models.Setup2FAResponse
import com.example.antibully.data.models.TwoFactorStatusResponse

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

//    @POST("setup")
//    suspend fun setup2FA(
//        @Header("Authorization") token: String
//    ): Setup2FAResponse
//
//    @POST("verify")
//    suspend fun verify2FA(
//        @Header("Authorization") token: String,
//        @Body body: Map<String, String>
//    ): Response<Unit>
//
//    @POST("registerFirebaseUser")
//    suspend fun registerFirebaseUser(
//        @Header("Authorization") token: String,
//        @Body body: Map<String, String>
//    ): Response<Unit>
//
//    @GET("check-2fa-status")
//    suspend fun checkTwoFactorStatus(
//        @Header("Authorization") token: String
//    ): TwoFactorStatusResponse
//    @POST("status")
//    suspend fun updateTwoFactorStatus(
//        @Header("Authorization") token: String,
//        @Body status: Map<String, Boolean>
//    ): Response<Unit>

}
