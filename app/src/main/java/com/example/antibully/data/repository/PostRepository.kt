package com.example.antibully.data.repository

import android.util.Log
import com.example.antibully.data.db.dao.PostDao
import com.example.antibully.data.models.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostRepository(private val postDao: PostDao, private val firestore: FirebaseFirestore) {

    // Get posts from ROOM based on alertId (so we can link them to an alert)
    fun getPostsForAlert(alertId: String): Flow<List<Post>> {
        return postDao.getPostsForAlert(alertId)
    }

    // Insert post into ROOM + Firestore
    suspend fun insert(post: Post) {
        postDao.insertPost(post)  // Save locally first
        savePostToFirestore(post) // Sync to Firestore
    }

    // Delete post from ROOM + Firestore
    suspend fun delete(post: Post) {
        postDao.deletePost(post)
        deletePostFromFirestore(post.id)
    }

    // Update post in ROOM + Firestore
    suspend fun update(post: Post) {
        postDao.updatePost(post)
        updatePostInFirestore(post)
    }

    // Sync ROOM with Firestore (in case new posts were added from another device)
    suspend fun syncPostsFromFirestore() = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("posts").get().await() // Use await() for suspend
            val posts = snapshot.documents.map { doc ->
                Post(
                    id = doc.getLong("id")?.toInt() ?: 0,
                    alertId = doc.getString("alertId") ?: "",
                    userId = doc.getString("userId") ?: "",
                    text = doc.getString("text") ?: "",
                    imageUrl = doc.getString("imageUrl"),
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                )
            }
            postDao.insertAll(posts) // Now inside coroutine, so it works!
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to sync posts", e)
        }
    }


    private fun savePostToFirestore(post: Post) {
        val data = mapOf(
            "id" to post.id,
            "alertId" to post.alertId,
            "userId" to post.userId,
            "text" to post.text,
            "imageUrl" to post.imageUrl,
            "timestamp" to post.timestamp
        )
        firestore.collection("posts").document(post.id.toString()).set(data)
    }

    private fun deletePostFromFirestore(postId: Int) {
        firestore.collection("posts").document(postId.toString()).delete()
    }

    private fun updatePostInFirestore(post: Post) {
        firestore.collection("posts").document(post.id.toString())
            .update("text", post.text, "imageUrl", post.imageUrl)
    }
}
