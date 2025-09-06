package com.example.antibully.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.antibully.data.repository.ChildRepository
import com.example.antibully.data.models.ChildLocalData
import com.example.antibully.data.repository.LinkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChildViewModel(private val repository: ChildRepository) : ViewModel() {
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    fun getChildrenForUser(userId: String): Flow<List<ChildLocalData>> {
        return repository.getChildrenForUser(userId)
    }
    
    fun fetchChildrenFromApi(token: String, parentId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repository.fetchChildrenFromApi(token, parentId)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    suspend fun linkChild(
        token: String,
        parentId: String,
        discordId: String,
        name: String,
        imageUrl: String? = null
    ): LinkResult = repository.linkChild(token, parentId, discordId, name, imageUrl)

    fun linkChild(
        token: String,
        parentId: String,
        discordId: String,
        name: String,
        imageUrl: String? = null,
        onResult: (LinkResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.linkChild(token, parentId, discordId, name, imageUrl)
            onResult(result)
        }
    }
    fun updateChild(token: String, parentId: String, discordId: String, name: String?, imageUrl: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val success = repository.updateChild(token, parentId, discordId, name, imageUrl)
                onResult(success)
            } catch (e: Exception) {
                _error.value = e.message
                onResult(false)
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun unlinkChild(token: String, parentId: String, discordId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val success = repository.unlinkChild(token, parentId, discordId)
                onResult(success)
            } catch (e: Exception) {
                _error.value = e.message
                onResult(false)
            } finally {
                _loading.value = false
            }
        }
    }
}

class ChildViewModelFactory(private val repository: ChildRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChildViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChildViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}