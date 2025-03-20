package com.example.antibully.data.repository

import com.example.antibully.data.db.dao.PostDao
import com.example.antibully.data.models.Post
import kotlinx.coroutines.flow.Flow

class PostRepository(private val postDao: PostDao) {

    val allPosts: Flow<List<Post>> = postDao.getAllPosts()

    suspend fun insert(post: Post) {
        postDao.insertPost(post)
    }

    suspend fun delete(post: Post) {
        postDao.deletePost(post)
    }

    suspend fun update(post: Post) {
        postDao.updatePost(post)
    }

}
