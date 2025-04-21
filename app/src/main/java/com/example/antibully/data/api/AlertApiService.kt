package com.example.antibully.data.api

import com.example.antibully.data.models.AlertApiRequest
import retrofit2.Response
import retrofit2.http.*

interface AlertApiService {

    /** GET  /alerts          → List all alerts (optionally filter by parentId) */
    @GET("alerts")
    suspend fun getAllAlerts(
        @Query("parentId") parentId: String? = null
    ): Response<List<AlertApiRequest>>
}
