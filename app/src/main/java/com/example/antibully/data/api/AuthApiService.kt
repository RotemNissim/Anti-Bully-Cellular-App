package com.example.antibully.data.api

import com.example.antibully.data.models.EditUserDTO
import com.example.antibully.data.models.Setup2FAResponse
import com.example.antibully.data.models.TwoFactorStatusResponse
import com.example.antibully.data.models.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

interface AuthApiService {
    @POST("setup")
    suspend fun setup2FA(
        @Header("Authorization") token: String
    ): Setup2FAResponse

    @POST("verify")
    suspend fun verify2FA(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @POST("registerFirebaseUser")
    suspend fun registerFirebaseUser(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @GET("check-2fa-status")
    suspend fun checkTwoFactorStatus(
        @Header("Authorization") token: String
    ): TwoFactorStatusResponse

    @POST("status")
    suspend fun updateTwoFactorStatus(
        @Header("Authorization") token: String,
        @Body status: Map<String, Boolean>
    ): Response<Unit>

    @POST("firebase-login")
    suspend fun loginWithFirebase(
        @Header("Authorization") token: String
    ): Response<Unit>

    @PUT("edit-profile")
    suspend fun editProfileMongo(
        @Header("Authorization") token: String,
        @Body body: EditUserDTO
    ): Response<User>
}