package com.example.antibully.data.firestore

import android.net.Uri
import android.util.Log
import com.example.antibully.data.models.MessageRequest
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.models.MessageResponse
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
                val apiMessage = MessageRequest(  // FIXED HERE
                    messageId = messageId,
                    userId = userId,
                    text = text ?: "",
                    flagged = false,
                    reason = null
                )
                    RetrofitClient.apiService.addMessage(apiMessage).enqueue(object : Callback<MessageResponse> {
                    override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                        if (response.isSuccessful) onSuccess(message)
                    }
                    override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                        Log.e("MockAPI", "API request failed", t)
                    }
                })
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

}
