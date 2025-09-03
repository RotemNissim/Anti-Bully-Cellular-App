package com.example.antibully.data.repository

import com.example.antibully.data.api.ChildLastSeenDto
import com.example.antibully.data.api.UserApiService
import com.example.antibully.data.api.RetrofitClient
import java.time.Instant

class NotificationsRepository(
    private val api: UserApiService = RetrofitClient.userApiService
) {
    suspend fun getMe(token: String) =
        api.getMe("Bearer $token")

    suspend fun markAllRead(token: String) {
        val res = api.markNotificationsRead("Bearer $token")
        if (!res.isSuccessful) error("markAllRead failed ${res.code()}")
    }

    suspend fun markAllReadForChild(token: String, childId: String) {
        val res = api.markNotificationsReadForChild("Bearer $token", childId)
        if (!res.isSuccessful) error("markAllReadForChild failed ${res.code()}")
    }

    suspend fun getNotificationsLastSeen(token: String): List<ChildLastSeenDto> =
        api.getNotificationsLastSeen("Bearer $token")

    fun isoToMillis(iso: String): Long =
        Instant.parse(iso).toEpochMilli()
}
