package com.example.antibully.data.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.antibully.data.db.AppDatabase
import com.example.antibully.data.repository.ChildRepository
import com.example.antibully.data.repository.LinkResult
import com.example.antibully.viewmodel.ChildViewModel
import com.example.antibully.viewmodel.ChildViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class DiscordRedirectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDiscordRedirect(intent)
    }

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
        Log.d("OAuth", "Starting Discord OAuth flow with code: ${code.take(10)}...")
        
        val client = OkHttpClient()

        val json = JSONObject()
        json.put("code", code)
        json.put("redirectUri", "http://10.0.2.2:3000/api/oauth/discord/callback")

        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://10.0.2.2:3000/api/oauth/discord/exchange")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("OAuth", "Backend call failed", e)
                finish()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("OAuth", "Backend response code: ${response.code}")
                Log.d("OAuth", "Backend response: $responseBody")

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody ?: "{}")
                    val discordId = json.optString("id")
                    val discordUsername = json.optString("username")
                    val discordFullName = json.optString("fullName", discordUsername) // ✅ Use global_name if available

                    Log.d("OAuth", "Discord ID: $discordId, Username: $discordUsername, Full Name: $discordFullName")

                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId == null) {
                        Log.e("OAuth", "No Firebase user found")
                        return finish()
                    }

                    val childName = if (discordFullName.isNotEmpty()) {
                        "$discordFullName ($discordUsername)"
                    } else {
                        discordUsername
                    }

                    Log.d("OAuth", "Linking child '$childName' with Discord ID '$discordId' to parent '$userId'")

                    // Initialize repository and viewmodel
                    val childDao = AppDatabase.getDatabase(this@DiscordRedirectActivity).childDao()
                    val childRepository = ChildRepository(childDao)
                    val factory = ChildViewModelFactory(childRepository)
                    val childViewModel = ViewModelProvider(this@DiscordRedirectActivity as ViewModelStoreOwner, factory)[ChildViewModel::class.java]

                    runOnUiThread {
                        CoroutineScope(Dispatchers.Main).launch {
                            val token = FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.await()?.token
                            if (token != null) {
                                Log.d("OAuth", "Got Firebase token, calling linkChild API...")
                                childViewModel.linkChild(token, userId, discordId, childName) { result ->
                                    Log.d("OAuth", "Link child result: $result")
                                    when (result) {
                                        is LinkResult.Linked -> {
                                            // Success → go to Profile
                                            val intent = Intent(this@DiscordRedirectActivity, MainActivity::class.java)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            intent.putExtra("navigateToProfile", true)
                                            startActivity(intent)
                                            finish()
                                        }
                                        is LinkResult.AlreadyLinked -> {
                                            // Show friendly popup, then go to Profile
                                            runOnUiThread {
                                                androidx.appcompat.app.AlertDialog.Builder(this@DiscordRedirectActivity)
                                                    .setTitle("Child account already added")
                                                    .setMessage("This child is already linked to your account.")
                                                    .setPositiveButton("OK") { _, _ ->
                                                        val intent = Intent(this@DiscordRedirectActivity, MainActivity::class.java)
                                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        intent.putExtra("navigateToProfile", true)
                                                        startActivity(intent)
                                                        finish()
                                                    }
                                                    .setOnCancelListener {
                                                        val intent = Intent(this@DiscordRedirectActivity, MainActivity::class.java)
                                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        intent.putExtra("navigateToProfile", true)
                                                        startActivity(intent)
                                                        finish()
                                                    }
                                                    .show()
                                            }
                                        }
                                        is LinkResult.Error -> {
                                            Log.e("OAuth", "Failed to link child (code=${result.code})")
                                            finish()
                                        }
                                    }
                                }

                            } else {
                                Log.e("OAuth", "Failed to get Firebase token")
                                finish()
                            }
                        }
                    }
                } else {
                    Log.e("OAuth", "Discord OAuth failed with code: ${response.code}")
                    finish()
                }
            }
        })
    }
}