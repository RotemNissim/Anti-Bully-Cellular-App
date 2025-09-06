package com.example.antibully.utils

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREF_NAME = "user_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun login(context: Context, userId: String) {
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_USER_ID, userId)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()

        DeviceTokenManager.initializeToken(context, userId)
    }
    
    fun logout(context: Context) {
        DeviceTokenManager.unregisterToken(context)

        val prefs = getPrefs(context)
        prefs.edit().clear().apply()
    }
    
    fun getCurrentUserId(context: Context): String? {
        return getPrefs(context).getString(KEY_USER_ID, null)
    }
    
    fun isLoggedIn(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }
}