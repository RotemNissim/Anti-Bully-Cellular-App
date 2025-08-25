package com.example.antibully.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.antibully.data.models.Alert
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlertViewModel(
    private val repository: AlertRepository
) : ViewModel() {

    val allAlerts: Flow<List<Alert>> = repository.allAlerts

    private val _searchQuery = MutableStateFlow<String>("")
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    private val _children = MutableStateFlow<List<ChildLocalData>>(emptyList())
    fun setChildIds(children: List<ChildLocalData>) { _children.value = children }

    val visibleAlerts: StateFlow<List<Alert>> =
        combine(allAlerts, _searchQuery, _children) { alerts, query, children ->
            val q = query.trim().lowercase()
            val filteredChildren = children.filter { child -> q.isEmpty() || child.name.contains(q, ignoreCase = true) }
            val filteredChildIds = filteredChildren.map { child -> child.childId }
            val childIds = children.map { child -> child.childId }

            val filteredAlerts = alerts.filter { a ->
                (a.reporterId in childIds) &&
                        (q.isEmpty() ||
                                a.text.contains(q, ignoreCase = true) ||
                                a.reason.contains(q, ignoreCase = true))
            }

            val filteredAlertsByChild = alerts.filter { a -> a.reporterId in filteredChildIds }

            (filteredAlertsByChild + filteredAlerts).toSet().toList()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
