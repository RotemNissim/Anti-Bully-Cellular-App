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

    fun getPostsForAlert(alertId: String): Flow<List<Post>> {
        return postDao.getPostsForAlert(alertId)
    }

    suspend fun insert(post: Post) {
        val postWithFirebaseId = if (post.firebaseId.isEmpty()) {
            post.copy(firebaseId = firestore.collection("posts").document().id)
        } else {
            post
        }
        postDao.insertPost(postWithFirebaseId)
        savePostToFirestore(postWithFirebaseId)
    }

    suspend fun delete(post: Post) {
        postDao.deletePost(post)
        deletePostFromFirestore(post.firebaseId)
    }

    suspend fun update(post: Post) {
        postDao.updatePost(post)
        updatePostInFirestore(post)
    }

    suspend fun syncPostsFromFirestore(alertId: String) = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("alertId", alertId)
                .get()
                .await()

            val posts = snapshot.documents.mapNotNull { doc ->
                val firebaseId = doc.getString("firebaseId") ?: return@mapNotNull null
                Post(
                    firebaseId = firebaseId,
                    alertId = alertId,
                    userId = doc.getString("userId") ?: "",
                    text = doc.getString("text") ?: "",
                    imageUrl = doc.getString("imageUrl"),
                    timestamp = doc.getLong("timestamp") ?: 0
                )
            }
            postDao.insertAll(posts)
        } catch (e: Exception) {
            Log.e("Firestore", "Failed to sync posts", e)
        }
    }

    private fun savePostToFirestore(post: Post) {
        val data = mapOf(
            "firebaseId" to post.firebaseId,
            "alertId" to post.alertId,
            "userId" to post.userId,
            "text" to post.text,
            "imageUrl" to post.imageUrl,
            "timestamp" to post.timestamp
        )
        firestore.collection("posts").document(post.firebaseId).set(data)
    }

    private fun deletePostFromFirestore(postId: String) {
        firestore.collection("posts").document(postId).delete()
    }

    private fun updatePostInFirestore(post: Post) {
        firestore.collection("posts").document(post.firebaseId)
            .update("text", post.text, "imageUrl", post.imageUrl)
    }
}
