package com.example.antibully.data.db.dao

import androidx.room.*
import com.example.antibully.data.models.Post
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<Post>)

    @Query("SELECT * FROM posts WHERE alertId = :alertId ORDER BY timestamp DESC")
    fun getPostsForAlert(alertId: String): Flow<List<Post>>

    @Delete
    suspend fun deletePost(post: Post)

    @Update
    suspend fun updatePost(post: Post)
}
