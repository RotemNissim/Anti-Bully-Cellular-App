package com.example.antibully.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.antibully.data.models.Alert
import com.example.antibully.data.models.AlertItem
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.repository.AlertRepository
import com.example.antibully.data.repository.NotificationsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import com.example.antibully.utils.extractCatsFromSummary
import com.example.antibully.utils.isImageFromSummary
import kotlinx.coroutines.launch



class AlertViewModel(
    private val repository: AlertRepository
) : ViewModel() {
    private val notifications = NotificationsRepository()

    private fun tsMillis(t: Long) = if (t < 1_000_000_000_000L) t * 1000 else t

    private val _globalLastSeenMillis = MutableStateFlow<Long?>(null)

    private val _lastSeenMillisByChild =
        MutableStateFlow<Map<String, Long>>(emptyMap())

    val lastSeenMillis: StateFlow<Long?> = _globalLastSeenMillis.asStateFlow()

    fun deleteByPostId(postId: String) = viewModelScope.launch {
        repository.deleteByPostId(postId)
    }

    fun delete(alert: Alert) = viewModelScope.launch {
        repository.delete(alert)
    }

    private val _expandedUnread = MutableStateFlow(false)
    fun toggleUnread() { _expandedUnread.value = !_expandedUnread.value }

    private val _searchQuery = MutableStateFlow("")
    fun setSearchQuery(q: String) { _searchQuery.value = q }

    private val _children = MutableStateFlow<List<ChildLocalData>>(emptyList())
    fun setChildIds(children: List<ChildLocalData>) { _children.value = children }

    private val _selectedCategory = MutableStateFlow<String?>(null)

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    private val _imagesOnly = MutableStateFlow(false)
    fun setImagesOnly(enabled: Boolean) { _imagesOnly.value = enabled }

    val allAlerts: Flow<List<Alert>> = repository.allAlerts

    val visibleAlerts: StateFlow<List<Alert>> =
        combine(
            allAlerts,
            _searchQuery,
            _children,
            _selectedCategory,
            _imagesOnly
        ) { alerts, query, children, selectedCat, imagesOnly ->

            val q = query.trim().lowercase()
            val childNameById = children.associate { it.childId to (it.name ?: "").lowercase() }
            val childIds = childNameById.keys
            val selected = selectedCat?.lowercase()

            alerts.filter { a ->
                if (a.reporterId !in childIds) return@filter false

                val matchesSearch =
                    q.isEmpty() ||
                            a.text.contains(q, ignoreCase = true) ||
                            a.reason.contains(q, ignoreCase = true) ||
                            (childNameById[a.reporterId]?.contains(q) == true)
                if (!matchesSearch) return@filter false

                if (imagesOnly && !isImageFromSummary(a.reason)) return@filter false

                if (selected == null) return@filter true
                val cats = extractCatsFromSummary(a.reason)
                cats.contains(selected)
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val rows: StateFlow<List<AlertItem>> =
        combine(visibleAlerts, _globalLastSeenMillis, _lastSeenMillisByChild, _expandedUnread) { alerts, globalLastSeen, perChild, _ ->
            val (unread, read) = if (globalLastSeen == null && perChild.isEmpty()) {
                alerts to emptyList()
            } else {
                alerts.partition { alert ->
                    val childSeen = perChild[alert.reporterId]
                    when {
                        childSeen != null -> tsMillis(alert.timestamp) > childSeen
                        globalLastSeen != null -> tsMillis(alert.timestamp) > globalLastSeen
                        else -> true
                    }
                }
            }

            val unreadByChild: Map<String, Int> =
                unread.groupBy { it.reporterId }.mapValues { it.value.size }

            buildList {
                unreadByChild.forEach { (childId, count) ->
                    add(AlertItem.UnreadGroup(childId = childId, count = count))
                }
                addAll(read.map { AlertItem.SingleAlert(it) })
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun refreshLastSeen(token: String) = viewModelScope.launch {
        val me = notifications.getMe(token)
        _globalLastSeenMillis.value = me.notificationsLastSeenAt?.let { notifications.isoToMillis(it) }
    }

    fun markAllRead(token: String, childId: String) = viewModelScope.launch {
        notifications.markAllReadForChild(token, childId)
        val now = System.currentTimeMillis()
        _lastSeenMillisByChild.value = _lastSeenMillisByChild.value.toMutableMap().apply {
            put(childId, now)
        }
    }

    fun fetchAlerts(token: String, childId: String? = null) = viewModelScope.launch {
        repository.fetchAlertsFromApi(token, childId)
    }

    fun getUnreadForChild(childId: String): Flow<List<Alert>> =
        combine(allAlerts, _globalLastSeenMillis, _lastSeenMillisByChild) { alerts, globalLastSeen, perChild ->
            alerts.filter { a ->
                a.reporterId == childId &&
                        run {
                            val childSeen = perChild[childId]
                            when {
                                childSeen != null -> tsMillis(a.timestamp) > childSeen
                                globalLastSeen != null -> tsMillis(a.timestamp) > globalLastSeen
                                else -> true
                            }
                        }
            }
        }

    fun getLastSeenForChild(childId: String): Flow<Long?> =
        combine(_globalLastSeenMillis, _lastSeenMillisByChild) { globalLast, perChild ->
            perChild[childId] ?: globalLast
        }

    fun loadLastSeenForChildren(token: String) = viewModelScope.launch {
        try {
            val list = notifications.getNotificationsLastSeen(token)
            val newMap = list.associate { dto ->
                dto.discordId to notifications.isoToMillis(dto.lastSeenAt)
            }
            _lastSeenMillisByChild.value = newMap
        } catch (e: Exception) {
            android.util.Log.e("AlertViewModel", "Failed to load per-child lastSeen: ${e.message}", e)
        }
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
