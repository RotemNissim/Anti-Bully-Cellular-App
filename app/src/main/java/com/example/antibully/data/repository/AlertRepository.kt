package com.example.antibully.data.repository

import com.example.antibully.data.api.MessageApiService
import com.example.antibully.data.api.ApiHelper
import com.example.antibully.data.api.RetrofitClient.alertApiService
import com.example.antibully.data.models.Alert
import com.example.antibully.data.db.dao.AlertDao
import kotlinx.coroutines.flow.Flow

class AlertRepository(private val alertDao: AlertDao, private val apiService: MessageApiService) {

    val allAlerts: Flow<List<Alert>> = alertDao.getAllAlerts()

    suspend fun fetchAlertsFromApi() {
        val response = ApiHelper.safeApiCall { alertApiService.getAllAlerts() }

        response.onSuccess { apiMessages ->
            val alerts = apiMessages
                .map { Alert.fromApi(it) }

            alertDao.insertAll(alerts) // Save to local Room DB
        }

        response.onFailure { error ->
            println("API Error: ${error.message}") // Handle failure
        }
    }



    suspend fun insert(alert: Alert) {
        alertDao.insertAlert(alert)
    }

    suspend fun delete(alert: Alert) {
        alertDao.deleteAlert(alert)
    }

    fun getAlertsByReason(reason: String): Flow<List<Alert>> {
        return alertDao.getAlertsByReason(reason)
    }

    fun getAlertsForChild(childId: String): Flow<List<Alert>> {
        return alertDao.getAlertsForChild(childId)
    }

}
