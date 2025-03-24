package com.example.antibully.data.ui.test

import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.antibully.R
import com.example.antibully.data.firestore.FirestoreManager
import java.util.UUID
import com.example.antibully.data.api.*
class FirestoreTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.firestore_test_activity)

        // Generate a random message ID
        val messageId = UUID.randomUUID().toString()
        val userId = "testUser123"

        // Find buttons by ID
        val btnAddMessage = findViewById<Button>(R.id.btnAddMessage)
        val btnGetMessage = findViewById<Button>(R.id.btnGetMessage)
        val btnGetAllMessages = findViewById<Button>(R.id.btnGetAllMessages)
        val btnUpdateMessage = findViewById<Button>(R.id.btnUpdateMessage)
        val btnDeleteMessage = findViewById<Button>(R.id.btnDeleteMessage)

        // 🔹 1. Test Adding a Message
        btnAddMessage.setOnClickListener {
            FirestoreManager.addMessageToFirestoreOnly(
                messageId = messageId,
                userId = userId,
                text = "Hello Firestore!",
                imageUrl = null,
                timestamp = System.currentTimeMillis(),
                onSuccess = { Log.d("FirestoreTest", "Message added successfully!") },
                onFailure = { e -> Log.e("FirestoreTest", "Failed to add message", e) }
            )
        }

        // 🔹 2. Test Retrieving a Message
        btnGetMessage.setOnClickListener {
            FirestoreManager.getMessageById(
                messageId = messageId,
                onSuccess = { document ->
                    if (document.exists()) {
                        Log.d("FirestoreTest", "Message Retrieved: ${document.data}")
                    } else {
                        Log.d("FirestoreTest", "No such message found.")
                    }
                },
                onFailure = { e -> Log.e("FirestoreTest", "Failed to retrieve message", e) }
            )
        }

        // 🔹 3. Test Retrieving All Messages
        btnGetAllMessages.setOnClickListener {
            FirestoreManager.getAllFlaggedMessages(
                onSuccess = { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        Log.d("FirestoreTest", "Message: ${document.data}")
                    }
                },
                onFailure = { e -> Log.e("FirestoreTest", "Failed to retrieve messages", e) }
            )
        }

        // 🔹 4. Test Updating a Message Flag
        btnUpdateMessage.setOnClickListener {
            FirestoreManager.updateMessageFlag(
                messageId = messageId,
                flagged = true,
                reason = "Inappropriate content",
                onSuccess = { Log.d("FirestoreTest", "Message flagged successfully!") },
                onFailure = { e -> Log.e("FirestoreTest", "Failed to flag message", e) }
            )
        }

        // 🔹 5. Test Deleting a Message
        btnDeleteMessage.setOnClickListener {
            FirestoreManager.deleteMessage(
                messageId = messageId,
                onSuccess = { Log.d("FirestoreTest", "Message deleted successfully!") },
                onFailure = { e -> Log.e("FirestoreTest", "Failed to delete message", e) }
            )
        }
    }
}
