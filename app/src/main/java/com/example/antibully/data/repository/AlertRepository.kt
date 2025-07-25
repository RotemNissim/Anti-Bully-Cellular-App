package com.example.antibully.data.repository

import android.util.Log
import com.example.antibully.MyApp
import com.example.antibully.data.api.AlertApiService
import com.example.antibully.data.api.ApiHelper
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.db.dao.AlertDao
import com.example.antibully.data.db.dao.UnreadAlertCount
import com.example.antibully.data.models.Alert
import kotlinx.coroutines.flow.Flow
import com.example.antibully.utils.Encryption

class AlertRepository(
    private val alertDao: AlertDao,
    private val alertApiService: AlertApiService = RetrofitClient.alertApiService
) {
    val allAlerts: Flow<List<Alert>> = alertDao.getAllAlerts()

    suspend fun fetchAlertsFromApi(
        token: String,
        childId: String? = null
    ) {
        val bearer = "Bearer $token"

        if (childId != null) {
            val encryptedChildId = Encryption.encrypt(childId)
            Log.d("AlertRepository", "Fetching alerts for child: $childId")

            val result = ApiHelper.safeApiCall {
                alertApiService.getAlertsForChild(bearer, encryptedChildId)
            }

            if (result.isSuccess) {
                val remoteList = result.getOrNull() ?: emptyList()
                Log.d("AlertRepository", "API returned ${remoteList.size} alerts for child $childId")

                // ✅ Get existing alerts to check which are new
                val existingAlerts = alertDao.getAllAlertsSync()
                val existingPostIds = existingAlerts.map { it.postId }.toSet()

                // ✅ Convert API response to local Alert format
                val localAlerts = remoteList.map { apiAlert ->
                    val isNewAlert = apiAlert.id !in existingPostIds

                    // ✅ פשוט: התראות חדשות הן unread, קיימות נשארות כמו שהן
                    val existingAlert = existingAlerts.find { it.postId == apiAlert.id }
                    val shouldBeUnread = if (existingAlert != null) {
                        // אם ההתראה כבר קיימת, שמור על הסטטוס הנוכחי שלה
                        !existingAlert.isRead
                    } else {
                        // התראה חדשה - תמיד unread
                        true
                    }

                    Log.d("AlertRepository", "Alert ${apiAlert.id}: isNew=$isNewAlert, shouldBeUnread=$shouldBeUnread")

                    Alert(
                        postId = apiAlert.id,
                        reporterId = apiAlert.childId,
                        text = apiAlert.severity,
                        reason = apiAlert.summary ?: "No reason provided",
                        imageUrl = apiAlert.imageUrl,
                        timestamp = apiAlert.timestamp,
                        isRead = !shouldBeUnread
                    )
                }

                Log.d("AlertRepository", "Converted to ${localAlerts.size} local alerts")
                localAlerts.forEach { alert ->
                    Log.d("AlertRepository", "Alert: postId=${alert.postId}, reporterId=${alert.reporterId}, reason=${alert.reason}, isRead=${alert.isRead}")
                }

                alertDao.insertAll(localAlerts)
                Log.d("AlertRepository", "Saved alerts to local database")
            } else {
                Log.e("AlertRepository", "Failed to fetch alerts: ${result.exceptionOrNull()?.message}")
                result.exceptionOrNull()?.printStackTrace()
            }
        } else {
            Log.w("AlertRepository", "childId is null, cannot fetch alerts")
        }
    }

    // ✅ פונקציה לסינון התראות לפי סוג - נחוצה לפילטרים בעתיד
    fun getAlertsByReason(reason: String): Flow<List<Alert>> =
        alertDao.getAlertsByReason(reason)

    // ✅ פונקציה לקבלת התראות לפי ילד - נחוצה לתצוגת התראות של ילד ספציפי
    fun getAlertsForChild(childId: String): Flow<List<Alert>> =
        alertDao.getAlertsForChild(childId)

    suspend fun delete(alert: Alert) =
        alertDao.deleteAlert(alert)

    // ✅ Add method to mark alert as read
    suspend fun markAlertAsRead(postId: String) =
        alertDao.markAlertAsRead(postId)

    // ✅ פונקציה לספירת התראות שלא נקראו - נחוצה לבאדג'ים ולוגיקת UI
    fun getUnreadAlertsCount(): Flow<Int> =
        alertDao.getUnreadAlertsCount()

    fun getGroupedAlerts(): Flow<List<UnreadAlertCount>> =
        alertDao.getGroupedUnreadAlerts()

    // ✅ Add method to mark all alerts for a child as read
    suspend fun markAllAlertsAsReadForChild(childId: String) =
        alertDao.markAllAlertsAsReadForChild(childId)

    // ✅ פונקציה לספירת התראות שלא נקראו לפי ילד - נחוצה ליצירת קבוצות התראות
    suspend fun getUnreadAlertsCountByChild(): List<UnreadAlertCount> =
        alertDao.getUnreadAlertsCountByChild()

    // ✅ הוסף פונקציה לעדכון זמן פעילות בשרת
    suspend fun updateUserActivity(token: String) {
        val bearer = "Bearer $token"
        try {
            val result = ApiHelper.safeApiCall {
                RetrofitClient.authApiService.updateActivity(bearer)
            }

            if (result.isSuccess) {
                Log.d("AlertRepository", "Successfully updated user activity on server")
            } else {
                Log.e("AlertRepository", "Failed to update user activity: ${result.exceptionOrNull()?.message}")
            }
        } catch (e: Exception) {
            Log.e("AlertRepository", "Error updating user activity: ${e.message}")
        }
    }
}
//package com.example.antibully.data.repository
//
//import android.util.Log
//import com.example.antibully.data.api.AlertApiService
//import com.example.antibully.data.api.ApiHelper
//import com.example.antibully.data.api.RetrofitClient
//import com.example.antibully.data.db.dao.AlertDao
//import com.example.antibully.data.models.Alert
//import kotlinx.coroutines.flow.Flow
//import com.example.antibully.utils.Encryption
//
//
//class AlertRepository(
//    private val alertDao: AlertDao,
//    private val alertApiService: AlertApiService = RetrofitClient.alertApiService
//
//) {
//    val allAlerts: Flow<List<Alert>> = alertDao.getAllAlerts()
//
//    suspend fun fetchAlertsFromApi(
//        token: String,
//        childId: String? = null
//    ) {
//        val bearer = "Bearer $token"
//
//        if (childId != null) {
//            val encryptedChildId = Encryption.encrypt(childId)
//            Log.d("AlertRepository", "Fetching alerts for child: $childId $encryptedChildId ")
//
//
//
//
//            val result = ApiHelper.safeApiCall {
//                alertApiService.getAlertsForChild(bearer, encryptedChildId)
//            }
//
//            if (result.isSuccess) {
//                val remoteList = result.getOrNull() ?: emptyList()
//                Log.d("AlertRepository", "API returned ${remoteList.size} alerts for child $childId $encryptedChildId")
//
//                // ✅ Convert API response to local Alert format
//                val localAlerts = remoteList.map { apiAlert ->
//                    Alert(
//                        postId = apiAlert.id, // ✅ Use 'id' instead of '_id'
//                        reporterId = apiAlert.childId, // ✅ This should match your child ID
//                        text = apiAlert.severity,
//                        reason = apiAlert.summary ?: "No reason provided",
//                        imageUrl = apiAlert.imageUrl,
//                        timestamp = apiAlert.timestamp
//                    )
//                }
//
//                Log.d("AlertRepository", "Converted to ${localAlerts.size} local alerts")
//                localAlerts.forEach { alert ->
//                    Log.d("AlertRepository", "Alert: postId=${alert.postId}, reporterId=${alert.reporterId}, reason=${alert.reason}")
//                }
//
//                alertDao.insertAll(localAlerts)
//                Log.d("AlertRepository", "Saved alerts to local database")
//            } else {
//                Log.e("AlertRepository", "Failed to fetch alerts: ${result.exceptionOrNull()?.message}")
//                result.exceptionOrNull()?.printStackTrace()
//            }
//        } else {
//            Log.w("AlertRepository", "childId is null, cannot fetch alerts")
//        }
//    }
//
//    fun getAlertsByReason(reason: String): Flow<List<Alert>> =
//        alertDao.getAlertsByReason(reason)
//
//    fun getAlertsForChild(childId: String): Flow<List<Alert>> =
//        alertDao.getAlertsForChild(childId)
//
//    suspend fun delete(alert: Alert) =
//        alertDao.deleteAlert(alert)
//}
