package com.example.antibully.data.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
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