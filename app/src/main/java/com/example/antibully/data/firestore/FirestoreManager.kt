package com.example.antibully.data.firestore

import com.google.firebase.firestore.FirebaseFirestore

object FirestoreManager {
    private val db : FirebaseFirestore = FirebaseFirestore.getInstance()

    fun addMessage(
        messageId: String,
        userId: String,
        text: String?,
        imageUrl: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val message = hashMapOf(
            "messageId" to messageId,
            "userId" to userId,
            "text" to text,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis(),
            "flagged" to false,
            "reason" to null
        )

        db.collection("messages").document(messageId).set(message)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun updateMessageFlag(
        messageId: String,
        flagged: Boolean,
        reason: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("messages").document(messageId)
            .update("flagged", flagged, "reason", reason)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }
}
