package com.example.antibully.data.firestore

import android.net.Uri
import android.util.Log
import com.example.antibully.data.models.MessageRequest
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.models.MessageResponse
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

object FirestoreManager {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val apiService = RetrofitClient.apiService

    fun uploadImageToStorage(imageUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}")
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString()) // Get downloadable image URL
                }
            }
            .addOnFailureListener {
                onFailure(it)
            }
    }

    fun updatePostInFirestore(postId: String, newText: String, newImageUrl: String?, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val updateData = mutableMapOf<String, Any>("text" to newText)
        newImageUrl?.let { updateData["imageUrl"] = it }

        FirebaseFirestore.getInstance().collection("posts").document(postId)
            .update(updateData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun addMessage(
        messageId: String,
        userId: String,
        text: String?,
        imageUrl: String?,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val message = mutableMapOf<String, Any>(
            "userId" to userId,
            "text" to (text ?: ""),
            "imageUrl" to (imageUrl ?: ""),
            "timestamp" to System.currentTimeMillis(),
            "flagged" to false
        )

        db.collection("messages").document(messageId).set(message)
            .addOnSuccessListener {
                val apiMessage = MessageRequest(
                    messageId = messageId,
                    userId = userId,
                    text = text ?: "",
                    flagged = false,
                    reason = null
                )

                // 🔥 LAUNCH A COROUTINE to call suspend API
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val response = apiService.addMessage(apiMessage)
                        if (response.isSuccessful) {
                            withContext(kotlinx.coroutines.Dispatchers.Main) {
                                onSuccess(message)
                            }
                        } else {
                            Log.e("MockAPI", "API error: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e("MockAPI", "API request failed", e)
                    }
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateMessageFlag(
        messageId: String,
        flagged: Boolean,
        reason: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val updateData = mutableMapOf<String, Any>(
            "flagged" to flagged
        )
        reason?.let { updateData["reason"] = it }

        db.collection("messages").document(messageId)
            .update(updateData)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun getMessageById(
        messageId: String,
        onSuccess: (document: com.google.firebase.firestore.DocumentSnapshot) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("messages").document(messageId)
            .get()
            .addOnSuccessListener { document ->
                onSuccess(document)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getAllFlaggedMessages(
        onSuccess: (querySnapshot: com.google.firebase.firestore.QuerySnapshot) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("messages")
            .whereEqualTo("flagged", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                onSuccess(querySnapshot)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun deleteMessage(
        messageId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("messages").document(messageId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }


}
