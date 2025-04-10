package com.example.antibully.data.firestore

import android.net.Uri
import android.util.Log
import com.example.antibully.data.models.MessageRequest
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.models.MessageResponse
import com.example.antibully.data.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

object FirestoreManager {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val apiService = RetrofitClient.apiService

    fun fetchAllUsers(onResult: (Map<String, User>) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val userMap = mutableMapOf<String, User>()
                for (doc in querySnapshot.documents) {
                    val id = doc.id // Firebase UID
                    val name = doc.getString("fullName") ?: ""
                    val email = doc.getString("email") ?: ""
                    val profileImage = doc.getString("profileImageUrl") ?: ""

                    userMap[id] = User(
                        id = id,
                        name = name,
                        email = email,
                        localProfileImagePath = profileImage,
                        profileImageUrl = profileImage
                    )
                }
                onResult(userMap)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreFetch", "Failed to fetch users", e)
                onResult(emptyMap()) // fallback so your adapter still initializes
            }
    }


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

    fun addMessageToFirestoreOnly(
        messageId: String,
        userId: String,
        text: String?,
        imageUrl: String?,
        timestamp: Long,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val message = mutableMapOf<String, Any>(
            "userId" to userId,
            "text" to (text ?: ""),
            "imageUrl" to (imageUrl ?: ""),
            "timestamp" to timestamp,
            "flagged" to false
        )

        db.collection("messages").document(messageId).set(message)
            .addOnSuccessListener { onSuccess(message) }
            .addOnFailureListener { onFailure(it) }
    }


    fun addMessageWithApi(
        messageId: String,
        userId: String,
        text: String?,
        imageUrl: String?,
        timestamp: Long,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val message = mutableMapOf<String, Any>(
            "userId" to userId,
            "text" to (text ?: ""),
            "imageUrl" to (imageUrl ?: ""),
            "timestamp" to timestamp,
            "flagged" to false
        )

        db.collection("messages").document(messageId).set(message)
            .addOnSuccessListener {
                val apiMessage = MessageRequest(
                    messageId = messageId,
                    userId = userId,
                    text = text ?: "",
                    flagged = false,
                    reason = null,
                    imageUrl = imageUrl,
                    timestamp = timestamp
                )

                // 🔥 LAUNCH A COROUTINE to call suspend API
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    try {
                        val response = apiService.addMessage(apiMessage)
                        if (response.isSuccessful) {
                            withContext(Dispatchers.Main) {
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

    fun fetchAndStoreFlaggedMessagesFromApi() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getAllFlaggedMessages()
                if (response.isSuccessful) {
                    val flaggedMessages = response.body() ?: emptyList()

                    // Optionally store in Firestore
                    FirebaseFirestore.getInstance()
                        .collection("flaggedMessagesCache")
                        .document("latest")
                        .set(mapOf("messages" to flaggedMessages))
                }
            } catch (e: Exception) {
                Log.e("FirestoreManager", "API fetch failed", e)
            }
        }
    }



}
