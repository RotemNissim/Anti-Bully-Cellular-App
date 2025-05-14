package com.example.antibully.data.repository

import com.example.antibully.data.api.AuthRetrofitClient
import com.example.antibully.data.models.Setup2FAResponse
import retrofit2.Response

class TwoFactorAuthRepository {
    suspend fun setup2FA(token: String): Setup2FAResponse {
        return AuthRetrofitClient.authService.setup2FA("Bearer $token")
    }


    suspend fun verify2FA(token: String, code: String): Response<Unit> {
        return AuthRetrofitClient.authService.verify2FA(
            token = "Bearer $token",
            body = mapOf("twoFactorCode" to code)
        )
    }
}
