package com.example.antibully.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object DeviceTokenManager {
    private const val TAG = "DeviceTokenManager"
    private const val PREFS_NAME = "device_token_prefs"
    private const val KEY_TOKEN = "fcm_token"
    private const val KEY_USER_ID = "user_id"
    private const val BASE_URL = "http://193.106.55.138:3000"
    
    private val client = OkHttpClient()
    private val gson = Gson()
    
    data class RegisterTokenRequest(
        val userId: String,
        val token: String,
        val platform: String = "android"
    )
    
    fun initializeToken(context: Context, userId: String) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_USER_ID, userId).apply()
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM Registration Token: $token")

            prefs.edit().putString(KEY_TOKEN, token).apply()

            sendTokenToServer(context, token)
        }
    }
    
    fun sendTokenToServer(context: Context, token: String) {
        val prefs = getPrefs(context)
        val userId = prefs.getString(KEY_USER_ID, null)
        
        if (userId == null) {
            Log.w(TAG, "User ID not found, cannot send token to server")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = RegisterTokenRequest(userId, token)
                val json = gson.toJson(request)
                val body = json.toRequestBody("application/json".toMediaType())
                
                val httpRequest = Request.Builder()
                    .url("$BASE_URL/api/device/register")
                    .post(body)
                    .build()
                
                client.newCall(httpRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Failed to send token to server", e)
                    }
                    
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            Log.d(TAG, "Token successfully sent to server")
                        } else {
                            Log.e(TAG, "Server responded with error: ${response.code}")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error sending token to server", e)
            }
        }
    }
    
    fun unregisterToken(context: Context) {
        val prefs = getPrefs(context)
        val token = prefs.getString(KEY_TOKEN, null)
        
        if (token == null) {
            Log.w(TAG, "No token found to unregister")
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = """{"token": "$token"}"""
                val body = json.toRequestBody("application/json".toMediaType())
                
                val request = Request.Builder()
                    .url("$BASE_URL/api/device/unregister")
                    .delete(body)
                    .build()
                
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Failed to unregister token", e)
                    }
                    
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            Log.d(TAG, "Token successfully unregistered")
                            // Clear local storage
                            prefs.edit().remove(KEY_TOKEN).remove(KEY_USER_ID).apply()
                        } else {
                            Log.e(TAG, "Server responded with error: ${response.code}")
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering token", e)
            }
        }
    }
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}