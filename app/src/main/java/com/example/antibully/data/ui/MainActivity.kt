package com.example.antibully.data.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.antibully.R
import com.example.antibully.data.models.MessageRequest
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.firestore.FirestoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val messageId = "msg123"
        val request = MessageRequest(
            messageId = messageId,
            userId = "user456",
            text = "You're ugly!",
            flagged = false,
            reason = null
        )

        // Save message in Firestore first
        FirestoreManager.addMessageToFirestoreOnly(
            messageId = request.messageId,
            userId = request.userId,
            text = request.text,
            imageUrl = null,
            onSuccess = {
                Log.d("FIRESTORE", "Message added to Firestore")

                // 🔥 Use coroutine for suspend API call
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = RetrofitClient.apiService.addMessage(request)
                        if (response.isSuccessful) {
                            val result = response.body()
                            if (result?.flagged == true) {
                                withContext(Dispatchers.Main) {
                                    FirestoreManager.updateMessageFlag(
                                        messageId = request.messageId,
                                        flagged = true,
                                        reason = result.reason ?: "Not specified",
                                        onSuccess = {
                                            Log.d("FIRESTORE", "Message flagged!")
                                        },
                                        onFailure = { e ->
                                            Log.e("FIRESTORE", "Failed to update: ${e.message}")
                                        }
                                    )
                                }
                            }
                        } else {
                            Log.e("API_ERROR", "MockAPI returned error: ${response.errorBody()?.string()}")
                        }
                    } catch (e: Exception) {
                        Log.e("API_ERROR", "MockAPI failed: ${e.message}")
                    }
                }
            },
            onFailure = { e -> Log.e("FIRESTORE", "Failed to add: ${e.message}") }
        )
    }
}
