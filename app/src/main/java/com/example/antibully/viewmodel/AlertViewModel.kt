package com.example.antibully.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.antibully.data.models.Alert
import com.example.antibully.data.repository.AlertRepository
import kotlinx.coroutines.launch

class AlertViewModel(private val repository: AlertRepository) : ViewModel() {

    val allAlerts = repository.allAlerts

    fun fetchAlerts() = viewModelScope.launch {
        repository.fetchAlertsFromApi()
    }

    fun insert(alert: Alert) = viewModelScope.launch {
        repository.insert(alert)
    }

    fun delete(alert: Alert) = viewModelScope.launch {
        repository.delete(alert)
    }
}

class AlertViewModelFactory(private val repository: AlertRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlertViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlertViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}