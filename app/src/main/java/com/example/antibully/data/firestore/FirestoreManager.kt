package com.example.antibully.data.firestore

import android.util.Log
import com.example.antibully.api.Message
import com.example.antibully.api.RetrofitClient
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object FirestoreManager {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val apiService = RetrofitClient.apiService

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
                val apiMessage = Message(
                    id = messageId,
                    userId = userId,
                    text = text ?: "",
                    imageUrl = imageUrl,
                    flagged = false,
                    reason = null
                )
                apiService.addMessage(apiMessage).enqueue(object : Callback<Message> {
                    override fun onResponse(call: Call<Message>, response: Response<Message>) {
                        if (response.isSuccessful) onSuccess(message)
                    }
                    override fun onFailure(call: Call<Message>, t: Throwable) {
                        Log.e("MockAPI", "API request failed", t)
                    }
                })
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getMessageById(
        messageId: String,
        onSuccess: (Map<String, Any>?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("messages").document(messageId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firestoreData = document.data
                    if (firestoreData != null) {
                        val apiFormattedData = mapOf<String, Any>(
                            "id" to messageId,
                            "userId" to (firestoreData["userId"] ?: ""),
                            "text" to (firestoreData["text"] ?: ""),
                            "ImageUrl" to (firestoreData["imageUrl"] ?: ""),
                            "flagged" to (firestoreData["flagged"] ?: false),
                            "reason" to (firestoreData["reason"] ?: "None")
                        )
                        onSuccess(apiFormattedData)
                    } else {
                        onSuccess(null)
                    }
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun getAllMessages(
        onSuccess: (List<Map<String, Any>>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("messages").orderBy("timestamp").get()
            .addOnSuccessListener { querySnapshot ->
                val messages = querySnapshot.documents.mapNotNull { document ->
                    val firestoreData = document.data
                    firestoreData?.let {
                        mapOf<String, Any>(
                            "id" to document.id,
                            "userId" to (it["userId"] ?: ""),
                            "text" to (it["text"] ?: ""),
                            "ImageUrl" to (it["imageUrl"] ?: ""),
                            "flagged" to (it["flagged"] ?: false),
                            "reason" to (it["reason"] ?: "None")
                        )
                    }
                }
                onSuccess(messages)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateMessageFlag(
        messageId: String,
        flagged: Boolean,
        reason: String?,
        onSuccess: (Map<String, Any>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val updateData = mutableMapOf<String, Any>(
            "flagged" to flagged
        )
        reason?.let { updateData["reason"] = it }

        db.collection("messages").document(messageId)
            .update(updateData)
            .addOnSuccessListener {
                db.collection("messages").document(messageId).get()
                    .addOnSuccessListener { document ->
                        val firestoreData = document.data
                        if (firestoreData != null) {
                            val apiFormattedData = mapOf<String, Any>(
                                "id" to messageId,
                                "userId" to (firestoreData["userId"] ?: ""),
                                "text" to (firestoreData["text"] ?: ""),
                                "ImageUrl" to (firestoreData["imageUrl"] ?: ""),
                                "flagged" to (firestoreData["flagged"] ?: false),
                                "reason" to (firestoreData["reason"] ?: "None")
                            )
                            onSuccess(apiFormattedData)
                        }
                    }
            }
            .addOnFailureListener { onFailure(it) }
    }
}
