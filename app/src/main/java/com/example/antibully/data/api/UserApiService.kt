package com.example.antibully.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.Path

data class MeResponse(
    val id: String,
    val email: String,
    val username: String?,
    val notificationsLastSeenAt: String?
)

interface UserApiService {
    @GET("api/me")
    suspend fun getMe(@Header("Authorization") bearer: String): MeResponse

    @PATCH("api/me/notifications/last-seen")
    suspend fun markNotificationsRead(@Header("Authorization") bearer: String): Response<Unit>

    @PATCH("api/me/notifications/{childId}/last-seen")
    suspend fun markNotificationsReadForChild(
        @Header("Authorization") bearer: String,
        @Path("childId") childId: String
    ): Response<Unit>
}
