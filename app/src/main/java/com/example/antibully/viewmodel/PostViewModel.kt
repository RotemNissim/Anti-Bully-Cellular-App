package com.example.antibully.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.antibully.data.models.Post
import com.example.antibully.data.repository.PostRepository
import kotlinx.coroutines.launch

class PostViewModel(private val repository: PostRepository) : ViewModel() {

    fun getPostsForAlert(alertId: String): LiveData<List<Post>> {
        return repository.getPostsForAlert(alertId).asLiveData()
    }

    fun insert(post: Post) = viewModelScope.launch {
        repository.insert(post)
    }

    fun delete(post: Post) = viewModelScope.launch {
        repository.delete(post)
    }

    fun update(post: Post) = viewModelScope.launch {
        repository.update(post)
    }

    fun syncPostsFromFirestore(alertId: String) = viewModelScope.launch {
        repository.syncPostsFromFirestore(alertId)
    }
}




class PostViewModelFactory(private val repository: PostRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}
