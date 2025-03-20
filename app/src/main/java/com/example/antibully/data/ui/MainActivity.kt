package com.example.antibully.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.antibully.R
import com.example.antibully.data.models.MessageRequest
import com.example.antibully.data.models.MessageResponse
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.firestore.FirestoreManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
        FirestoreManager.addMessage(
            messageId = request.messageId,
            userId = request.userId,
            text = request.text,
            imageUrl = null,
            onSuccess = {
                Log.d("FIRESTORE", "Message added to Firestore")

                // Send message to Mock API (FIX: Use `apiService`, NOT `instance`)
                RetrofitClient.apiService.addMessage(request).enqueue(object : Callback<MessageResponse> {
                    override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                        if (response.isSuccessful) {
                            val result = response.body()
                            if (result?.flagged == true) {
                                // If flagged, update Firestore
                                FirestoreManager.updateMessageFlag(
                                    messageId = request.messageId,
                                    flagged = true,
                                    reason = result.reason ?: "Not specified",
                                    onSuccess = { Log.d("FIRESTORE", "Message flagged!") },
                                    onFailure = { e -> Log.e("FIRESTORE", "Failed to update: ${e.message}") }
                                )
                            }
                        } else {
                            Log.e("API_ERROR", "MockAPI returned error: ${response.errorBody()?.string()}")
                        }
                    }

                    override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                        Log.e("API_ERROR", "MockAPI failed: ${t.message}")
                    }
                })
            },
            onFailure = { e -> Log.e("FIRESTORE", "Failed to add: ${e.message}") }
        )
    }
}
