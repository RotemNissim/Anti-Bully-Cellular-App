package com.example.antibully.data.repository

import android.util.Log
import com.example.antibully.data.api.AlertApiService
import com.example.antibully.data.api.ApiHelper
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.db.dao.AlertDao
import com.example.antibully.data.models.Alert
import kotlinx.coroutines.flow.Flow

class AlertRepository(
    private val alertDao: AlertDao,
    private val alertApiService: AlertApiService = RetrofitClient.alertApiService
) {
    val allAlerts: Flow<List<Alert>> = alertDao.getAllAlerts()

    /**
     * Pull from server for a specific child, then upsert into local DB.
     */
    suspend fun fetchAlertsFromApi(
        token: String,
        childId: String? = null
    ) {
        val bearer = "Bearer $token"
        
        if (childId != null) {
            Log.d("AlertRepository", "Fetching alerts for child: $childId")
            
            val result = ApiHelper.safeApiCall {
                alertApiService.getAlertsForChild(bearer, childId)
            }
            
            if (result.isSuccess) {
                val remoteList = result.getOrNull() ?: emptyList()
                Log.d("AlertRepository", "API returned ${remoteList.size} alerts for child $childId")
                
                // ✅ Convert API response to local Alert format
                val localAlerts = remoteList.map { apiAlert ->
                    Alert(
                        postId = apiAlert.id, // ✅ Use 'id' instead of '_id'
                        reporterId = apiAlert.childId, // ✅ This should match your child ID
                        text = apiAlert.severity,
                        reason = apiAlert.summary ?: "No reason provided",
                        imageUrl = apiAlert.imageUrl,
                        timestamp = apiAlert.timestamp
                    )
                }
                
                Log.d("AlertRepository", "Converted to ${localAlerts.size} local alerts")
                localAlerts.forEach { alert ->
                    Log.d("AlertRepository", "Alert: postId=${alert.postId}, reporterId=${alert.reporterId}, reason=${alert.reason}")
                }
                
                alertDao.insertAll(localAlerts)
                Log.d("AlertRepository", "Saved alerts to local database")
            } else {
                Log.e("AlertRepository", "Failed to fetch alerts: ${result.exceptionOrNull()?.message}")
                result.exceptionOrNull()?.printStackTrace()
            }
        } else {
            Log.w("AlertRepository", "childId is null, cannot fetch alerts")
        }
    }

    fun getAlertsByReason(reason: String): Flow<List<Alert>> =
        alertDao.getAlertsByReason(reason)

    fun getAlertsForChild(childId: String): Flow<List<Alert>> =
        alertDao.getAlertsForChild(childId)

    suspend fun delete(alert: Alert) =
        alertDao.deleteAlert(alert)
}
