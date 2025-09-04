package com.example.antibully.data.api

import com.example.antibully.data.models.AlertApiResponse // ✅ Use AlertApiResponse instead of Alert
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface AlertApiService {

    /** GET  /alerts          → List all alerts (optionally filter by parentId) */
    @GET("api/alerts") // ✅ Add 'api/' prefix
    suspend fun getAlertsForChild(
        @Header("Authorization") token: String,
        @Query("childId") childId: String
    ): Response<List<AlertApiResponse>> // ✅ Use AlertApiResponse

    @DELETE("api/alerts/{id}")
        suspend fun deleteAlert(
        @Header("Authorization") token: String,
        @Path("id") alertId: String
    ): Response<Unit>

}
