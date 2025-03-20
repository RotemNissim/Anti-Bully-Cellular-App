package com.example.antibully.data.db.dao

import androidx.room.*
import com.example.antibully.data.models.Alert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: Alert)

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<Alert>>

    @Delete
    suspend fun deleteAlert(alert: Alert)
}
