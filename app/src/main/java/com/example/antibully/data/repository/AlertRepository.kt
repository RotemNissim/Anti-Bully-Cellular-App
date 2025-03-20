package com.example.antibully.data.repository

import com.example.antibully.data.db.dao.AlertDao
import com.example.antibully.data.models.Alert
import kotlinx.coroutines.flow.Flow

class AlertRepository(private val alertDao: AlertDao) {

    val allAlerts: Flow<List<Alert>> = alertDao.getAllAlerts()

    suspend fun insert(alert: Alert) {
        alertDao.insertAlert(alert)
    }

    suspend fun delete(alert: Alert) {
        alertDao.deleteAlert(alert)
    }
}
