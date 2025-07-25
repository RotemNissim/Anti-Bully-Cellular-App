package com.example.antibully.data.api

import com.example.antibully.data.models.Setup2FAResponse
import com.example.antibully.data.models.TwoFactorStatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/setup")
    suspend fun setup2FA(
        @Header("Authorization") token: String
    ): Setup2FAResponse

    @POST("api/verify")
    suspend fun verify2FA(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @POST("api/registerFirebaseUser")
    suspend fun registerFirebaseUser(
        @Header("Authorization") token: String,
        @Body body: Map<String, String>
    ): Response<Unit>

    @GET("api/check-2fa-status")
    suspend fun checkTwoFactorStatus(
        @Header("Authorization") token: String
    ): TwoFactorStatusResponse

    @POST("api/status")
    suspend fun updateTwoFactorStatus(
        @Header("Authorization") token: String,
        @Body status: Map<String, Boolean>
    ): Response<Unit>

    @POST("api/firebase-login")
    suspend fun loginWithFirebase(
        @Header("Authorization") token: String
    ): Response<Unit>

    /** POST /auth/update-activity → Update user activity timestamp on server */
    @POST("api/update-activity") // ✅ הוסף API call לעדכון זמן פעילות
    suspend fun updateActivity(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>
}