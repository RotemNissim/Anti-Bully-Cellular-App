package com.example.antibully.data.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.antibully.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.antibully.data.models.MessageRequest
import com.example.antibully.data.firestore.FirestoreManager
import com.example.antibully.data.api.RetrofitClient

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
            messageId = messageId,
            userId = request.userId,
            text = request.text,
            imageUrl = null,
            onSuccess = {
                Log.d("FIRESTORE", "Message added to Firestore")

                // Send message to MockAPI
                RetrofitClient.instance.flagMessage(request).enqueue(object : Callback<MessageRequest> {
                    override fun onResponse(call: Call<MessageRequest>, response: Response<MessageRequest>) {
                        if (response.isSuccessful) {
                            val result = response.body()
                            if (result?.flagged == true) {
                                // If flagged, update Firestore
                                FirestoreManager.updateMessageFlag(
                                    messageId = messageId,
                                    flagged = true,
                                    reason = result.reason,
                                    onSuccess = { Log.d("FIRESTORE", "Message flagged!") },
                                    onFailure = { e -> Log.e("FIRESTORE", "Failed to update: ${e.message}") }
                                )
                            }
                        }
                    }

                    override fun onFailure(call: Call<MessageRequest>, t: Throwable) {
                        Log.e("API_ERROR", "MockAPI failed: ${t.message}")
                    }
                })
            },
            onFailure = { e -> Log.e("FIRESTORE", "Failed to add: ${e.message}") }
        )
    }
}
