package com.example.antibully.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.antibully.data.models.Alert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<Alert>)

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<Alert>>

    @Query("DELETE FROM alerts")
    suspend fun deleteAll()

    @Query("SELECT * FROM alerts WHERE postId = :postId LIMIT 1")
    fun getAlertByPostId(postId: String): LiveData<Alert>

    @Delete
    suspend fun deleteAlert(alert: Alert)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: Alert)

    @Query("SELECT * FROM alerts WHERE reason = :reason")
    fun getAlertsByReason(reason: String):Flow<List<Alert>>

    @Query("SELECT * FROM alerts WHERE reporterId = :childId")
    fun getAlertsForChild(childId: String): Flow<List<Alert>>

}

