package com.example.antibully.data.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.antibully.data.db.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.example.antibully.data.models.ChildLocalData
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiscordRedirectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDiscordRedirect(intent)
    }

//    override fun onNewIntent(intent: Intent?) {
//        super.onNewIntent(intent)
//        handleDiscordRedirect(intent)
//    }

    private fun handleDiscordRedirect(intent: Intent?) {
        val uri: Uri? = intent?.data
        val code = uri?.getQueryParameter("code")

        if (code != null) {
            Log.d("OAuth", "Received code: $code")
            sendCodeToBackend(code)
        } else {
            Log.e("OAuth", "No code received")
            finish()
        }
    }

    private fun sendCodeToBackend(code: String) {
        val client = OkHttpClient()

        val json = JSONObject()
        json.put("code", code)
        json.put("redirectUri", "http://10.0.2.2:3000/api/oauth/discord/callback")

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://10.0.2.2:3000/api/oauth/discord/exchange") // use local IP on same WiFi
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OAuth", "Backend call failed", e)
                finish()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("OAuth", "Backend response: $responseBody")

                val json = JSONObject(responseBody ?: "{}")
                val discordId = json.optString("id")
                val discordUsername = json.optString("username")
                val discordFullName = json.optString("fullName")

                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return finish()

                val child = ChildLocalData(
                    childId = discordId,
                    parentUserId = userId,
                    name =  "$discordFullName ($discordUsername)",
                    imageUrl = null
                )
                val db = FirebaseFirestore.getInstance()
                db.collection("users").document(userId)
                    .collection("children").document(discordId)
                    .set(child)
                    .addOnSuccessListener {
                        // Save to ROOM too
                        runOnUiThread {
                            CoroutineScope(Dispatchers.Main).launch {
                                withContext(Dispatchers.IO) {
                                    AppDatabase.getDatabase(this@DiscordRedirectActivity)
                                        .childDao()
                                        .insertChild(child)
                                }

                                val intent = Intent(this@DiscordRedirectActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra("navigateToProfile", true)
                                startActivity(intent)
                                finish()
                            }
                        }

                    }
                    .addOnFailureListener {
                        Log.e("OAuth", "Failed to save to Firestore", it)
                        finish()
                    }



                runOnUiThread {
                    // 🔥 Save the Discord ID or use it in your app
                    Log.d("OAuth", "Child Discord ID: $discordId")
                    // Optionally send it to Firestore or pass to parent activity
                    finish()
                }
            }
        })
    }
}