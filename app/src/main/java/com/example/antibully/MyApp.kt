package com.example.antibully


import android.app.Application
import com.google.firebase.FirebaseApp
import com.example.antibully.utils.SessionManager


class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this) // ✅ Initialize Firebase here

        // Initialize FCM token if user is already logged in
        if (SessionManager.isLoggedIn(this)) {
            val userId = SessionManager.getCurrentUserId(this)
            if (userId != null) {
                com.example.antibully.utils.DeviceTokenManager.initializeToken(this, userId)
            }
        }
    }
}
