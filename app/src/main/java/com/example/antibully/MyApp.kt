package com.example.antibully

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.example.antibully.utils.SessionManager

class MyApp : Application() {

    companion object {
        var isAppInForeground = false
        var isAlertsFragmentVisible = false
        var isFeedFragmentVisible = false // ✅ Add feed fragment tracking
        var lastAppPauseTime = 0L
    }

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

        // ✅ Add lifecycle tracking for alert read status
        registerActivityLifecycleCallbacks(AppLifecycleTracker())
    }

    private class AppLifecycleTracker : ActivityLifecycleCallbacks {
        private var activityCount = 0

        override fun onActivityStarted(activity: Activity) {
            if (activityCount == 0) {
                // App came to foreground
                isAppInForeground = true
            }
            activityCount++
        }

        override fun onActivityStopped(activity: Activity) {
            activityCount--
            if (activityCount == 0) {
                // App went to background
                isAppInForeground = false
                lastAppPauseTime = System.currentTimeMillis()
            }
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }
}
//package com.example.antibully
//
//
//import android.app.Application
//import com.google.firebase.FirebaseApp
//import com.example.antibully.utils.SessionManager
//
//
//class MyApp : Application() {
//    override fun onCreate() {
//        super.onCreate()
//        FirebaseApp.initializeApp(this) // ✅ Initialize Firebase here
//
//        // Initialize FCM token if user is already logged in
//        if (SessionManager.isLoggedIn(this)) {
//            val userId = SessionManager.getCurrentUserId(this)
//            if (userId != null) {
//                com.example.antibully.utils.DeviceTokenManager.initializeToken(this, userId)
//            }
//        }
//    }
//}
