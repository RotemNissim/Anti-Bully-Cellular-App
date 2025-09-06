package com.example.antibully.data.repository

import android.util.Log
import com.example.antibully.data.api.AlertApiService
import com.example.antibully.data.api.ApiHelper
import com.example.antibully.data.api.RetrofitClient
import com.example.antibully.data.db.dao.AlertDao
import com.example.antibully.data.db.dao.DismissedAlertDao
import com.example.antibully.data.models.Alert
import com.example.antibully.data.models.AlertApiResponse
import com.example.antibully.data.models.DismissedAlert
import com.example.antibully.utils.Encryption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class AlertRepository(
    private val alertDao: AlertDao,
    private val dismissedDao: DismissedAlertDao,
    private val currentUserId: String,
    private val alertApiService: AlertApiService = RetrofitClient.alertApiService
) {
    val allAlerts: Flow<List<Alert>> = alertDao.getAllAlerts()

    // ---- server-safety: per-child mutex + rate-limit ----
    private val childMutexes = ConcurrentHashMap<String, Mutex>()
    private val lastFetchByChild = ConcurrentHashMap<String, Long>()
    private val minFetchIntervalMs = 12_000L
    // -----------------------------------------------------

    /**
     * Fetch all alerts for a child (no backend changes).
     * Returns the number of new/updated alerts inserted into Room.
     */
    suspend fun fetchAlertsFromApi(token: String, childId: String?): Int {
        val bearer = "Bearer $token"
        if (childId == null) {
            Log.w("AlertRepository", "childId is null, cannot fetch alerts")
            return 0
        }

        val now = System.currentTimeMillis()
        val last = lastFetchByChild[childId] ?: 0L
        if (now - last < minFetchIntervalMs) {
            Log.d("AlertRepository", "skip fetch (rate-limited) child=$childId")
            return 0
        }

        val mutex = childMutexes.getOrPut(childId) { Mutex() }
        return mutex.withLock {
            val now2 = System.currentTimeMillis()
            val last2 = lastFetchByChild[childId] ?: 0L
            if (now2 - last2 < minFetchIntervalMs) {
                Log.d("AlertRepository", "skip fetch (rate-limited after lock) child=$childId")
                return@withLock 0
            }
            lastFetchByChild[childId] = now2

            val encryptedChildId = Encryption.encrypt(childId)
            Log.d("AlertRepository", "FETCH child=$childId at=$now2")

            val result = ApiHelper.safeApiCall {
                // NOTE: no 'since' param – backend stays unchanged
                alertApiService.getAlertsForChild(bearer, encryptedChildId)
            }

            if (result.isSuccess) {
                val remote: List<AlertApiResponse> = result.getOrNull().orEmpty()
                val dismissed = dismissedDao.getDismissedIds(currentUserId).toSet()
                val locals = remote
                    .filter { it.id !in dismissed }
                    .map { api ->
                        Alert(
                            postId = api.id,
                            reporterId = api.childId,
                            text = api.severity,
                            reason = api.summary ?: "No reason provided",
                            imageUrl = api.imageUrl,
                            severity = api.severity,
                            timestamp = api.timestamp
                        )
                    }

                if (locals.isNotEmpty()) {
                    alertDao.insertAll(locals)
                }
                return@withLock locals.size
            } else {
                Log.e("AlertRepository", "Failed to fetch alerts: ${result.exceptionOrNull()?.message}")
                return@withLock 0
            }
        }
    }

    fun getAlertsByReason(reason: String): Flow<List<Alert>> =
        alertDao.getAlertsByReason(reason)

    fun getAlertsForChild(childId: String): Flow<List<Alert>> =
        alertDao.getAlertsForChild(childId)

    suspend fun delete(alert: Alert) {
        dismissedDao.insert(DismissedAlert(currentUserId, alert.postId))
        alertDao.delete(alert)
    }

    suspend fun deleteByPostId(postId: String) {
        dismissedDao.insert(DismissedAlert(currentUserId, postId))
        alertDao.deleteByPostId(postId)
    }

    suspend fun deleteRemote(token: String, postId: String) {
        val bearer = "Bearer $token"
        val res = ApiHelper.safeApiCall { alertApiService.deleteAlert(bearer, postId) }
        if (res.isSuccess) {
            alertDao.deleteByPostId(postId)
        } else {
            Log.w("AlertRepository", "Remote delete failed: ${res.exceptionOrNull()?.message}")
        }
    }
}
