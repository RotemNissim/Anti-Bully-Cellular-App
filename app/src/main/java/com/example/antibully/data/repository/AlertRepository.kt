package com.example.antibully.data.repository

import android.util.Log
import com.example.antibully.data.api.AlertApiService
import com.example.antibully.data.api.ApiHelper
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.db.dao.AlertDao
import com.example.antibully.data.models.Alert
import com.example.antibully.utils.Encryption
import kotlinx.coroutines.flow.Flow

class AlertRepository(
    private val alertDao: AlertDao,
    private val alertApiService: AlertApiService = RetrofitClient.alertApiService
) {
    val allAlerts: Flow<List<Alert>> = alertDao.getAllAlerts()

    suspend fun fetchAlertsFromApi(token: String, childId: String?) {
        val bearer = "Bearer $token"
        if (childId == null) {
            Log.w("AlertRepository", "childId is null, cannot fetch alerts")
            return
        }
        val encryptedChildId = Encryption.encrypt(childId)
        val result = ApiHelper.safeApiCall {
            alertApiService.getAlertsForChild(bearer, encryptedChildId)
        }
        if (result.isSuccess) {
            val remoteList = result.getOrNull() ?: emptyList()
            val localAlerts = remoteList.map { apiAlert ->
                Alert(
                    postId = apiAlert.id,
                    reporterId = apiAlert.childId,
                    text = apiAlert.severity,
                    reason = apiAlert.summary ?: "No reason provided",
                    imageUrl = apiAlert.imageUrl,
                    severity = apiAlert.severity,
                    timestamp = apiAlert.timestamp
                )
            }
            alertDao.insertAll(localAlerts)
        } else {
            Log.e("AlertRepository", "Failed to fetch alerts: ${result.exceptionOrNull()?.message}")
        }
    }

    fun getAlertsByReason(reason: String): Flow<List<Alert>> =
        alertDao.getAlertsByReason(reason)

    fun getAlertsForChild(childId: String): Flow<List<Alert>> =
        alertDao.getAlertsForChild(childId)

    suspend fun delete(alert: Alert) =
        alertDao.delete(alert)

    suspend fun deleteByPostId(postId: String) =
        alertDao.deleteByPostId(postId)

    suspend fun deleteRemote(token: String, postId: String) {
        val bearer = "Bearer $token"
        val result = ApiHelper.safeApiCall { alertApiService.deleteAlert(bearer, postId) }
        if (result.isSuccess) {
            alertDao.deleteByPostId(postId)
        } else {
            Log.e("AlertRepository", "Remote delete failed: ${result.exceptionOrNull()?.message}")
        }
    }
}
