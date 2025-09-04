package com.example.antibully.data.repository

import android.util.Log
import com.example.antibully.data.api.AlertApiService
import com.example.antibully.data.api.ApiHelper
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.db.dao.AlertDao
import com.example.antibully.data.db.dao.DismissedAlertDao
import com.example.antibully.data.models.Alert
import com.example.antibully.data.models.DismissedAlert
import com.example.antibully.data.models.AlertApiResponse
import com.example.antibully.utils.Encryption
import kotlinx.coroutines.flow.Flow

class AlertRepository(
    private val alertDao: AlertDao,
    private val dismissedDao: DismissedAlertDao,
    private val currentUserId: String,
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
            val remote: List<AlertApiResponse> = result.getOrNull().orEmpty()
            val dismissed = dismissedDao.getDismissedIds(currentUserId).toSet()
            val locals = remote
                .filter { it.id !in dismissed }
                .map { api ->
                    Alert(
                        postId = api.id,
                        reporterId = api.childId,
                        text = api.severity,
                        reason = api.summary ?: "No reason provided",
                        imageUrl = api.imageUrl,
                        severity = api.severity,
                        timestamp = api.timestamp
                    )
                }
            alertDao.insertAll(locals)
        } else {
            Log.e("AlertRepository", "Failed to fetch alerts: ${result.exceptionOrNull()?.message}")
        }
    }

    fun getAlertsByReason(reason: String): Flow<List<Alert>> =
        alertDao.getAlertsByReason(reason)

    fun getAlertsForChild(childId: String): Flow<List<Alert>> =
        alertDao.getAlertsForChild(childId)

    suspend fun delete(alert: Alert) {
        dismissedDao.insert(DismissedAlert(currentUserId, alert.postId))
        alertDao.delete(alert)
    }

    suspend fun deleteByPostId(postId: String) {
        dismissedDao.insert(DismissedAlert(currentUserId, postId))
        alertDao.deleteByPostId(postId)
    }

    suspend fun deleteRemote(token: String, postId: String) {
        val bearer = "Bearer $token"
        val res = ApiHelper.safeApiCall { alertApiService.deleteAlert(bearer, postId) }
        if (res.isSuccess) {
            alertDao.deleteByPostId(postId)
        } else {
            Log.w("AlertRepository", "Remote delete failed: ${res.exceptionOrNull()?.message}")
        }
    }
}
