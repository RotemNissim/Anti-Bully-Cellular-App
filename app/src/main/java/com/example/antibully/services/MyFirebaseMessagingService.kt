package com.example.antibully.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.antibully.R
import com.example.antibully.data.ui.MainActivity
import com.example.antibully.utils.DeviceTokenManager
import com.example.antibully.utils.SessionManager // ✅ Add this import
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "🔔 NOTIFICATION RECEIVED!")
        Log.d(TAG, "From: ${remoteMessage.from}")
        Log.d(TAG, "Data: ${remoteMessage.data}")
        Log.d(TAG, "Notification: ${remoteMessage.notification}")
        
        // ✅ Force show a notification regardless of content
        showNotification("FCM Test", "onMessageReceived was called at ${System.currentTimeMillis()}")
        
        // Also handle the actual message
        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "Alert"
            
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "New alert received"
            
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "🔄 New FCM token received: ${token.take(20)}...")
        
        val context = applicationContext
        val userId = SessionManager.getCurrentUserId(context)
        
        if (userId != null) {
            DeviceTokenManager.sendTokenToServer(context, token)
        } else {
            Log.w(TAG, "No user ID found, cannot send new token to server")
        }
    }

    private fun showNotification(title: String, body: String) {
        Log.d(TAG, "📱 Showing notification: $title - $body")
        
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "alert_notifications"
        
        // ✅ Force create the notification channel every time
        createNotificationChannel()
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_mail)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        Log.d(TAG, "✅ Notification displayed with ID: $notificationId")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "alert_notifications"
            val channel = NotificationChannel(
                channelId,
                "Alert Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for bullying alerts"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                setBypassDnd(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "✅ Notification channel created: $channelId")
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}