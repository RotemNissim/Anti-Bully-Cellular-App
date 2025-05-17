package com.example.antibully.data.repository

import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.api.ApiHelper
import com.example.antibully.data.db.dao.AlertDao
import com.example.antibully.data.models.Alert
import kotlinx.coroutines.flow.Flow

class AlertRepository(
    private val alertDao: AlertDao
) {
    val allAlerts: Flow<List<Alert>> = alertDao.getAllAlerts()

    /**
     * Pull from server (opt. filtered by childId), then upsert into local DB.
     */
    suspend fun fetchAlertsFromApi(
        token: String,
        childId: String? = null
    ) {
        val bearer = "Bearer $token"
        val result = ApiHelper.safeApiCall {
            RetrofitClient.alertApiService.getAllAlerts(bearer, childId)
        }

        if (result.isSuccess) {
            val remoteList = result.getOrNull()!!
            val entities = remoteList.map { Alert.fromApi(it) }
            alertDao.insertAll(entities)
        } else {
            // TODO: log or surface the error
        }
    }

    fun getAlertsByReason(reason: String): Flow<List<Alert>> =
        alertDao.getAlertsByReason(reason)

    fun getAlertsForChild(childId: String): Flow<List<Alert>> =
        alertDao.getAlertsForChild(childId)

    suspend fun delete(alert: Alert) =
        alertDao.deleteAlert(alert)
}
