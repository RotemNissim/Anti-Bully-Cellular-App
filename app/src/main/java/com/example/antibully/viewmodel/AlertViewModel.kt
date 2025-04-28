package com.example.antibully.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.antibully.data.models.Alert
import com.example.antibully.data.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AlertViewModel(
    private val repository: AlertRepository
) : ViewModel() {

    val allAlerts: Flow<List<Alert>> = repository.allAlerts

    /**
     * Fetch alerts from server; pass null for childId to get all.
     */
    fun fetchAlerts(token: String, childId: String? = null) = viewModelScope.launch {
        repository.fetchAlertsFromApi(token, childId)
    }

    fun getAlertsByReason(reason: String): Flow<List<Alert>> =
        repository.getAlertsByReason(reason)

    fun getAlertsForChild(childId: String): Flow<List<Alert>> =
        repository.getAlertsForChild(childId)
}

class AlertViewModelFactory(
    private val repository: AlertRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlertViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
