package com.example.antibully.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.antibully.data.models.DismissedAlert

@Dao
interface DismissedAlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(d: DismissedAlert)

    @Query("SELECT postId FROM dismissed_alerts WHERE userId = :userId")
    suspend fun getDismissedIds(userId: String): List<String>

    @Query("DELETE FROM dismissed_alerts WHERE userId = :userId AND postId = :postId")
    suspend fun remove(userId: String, postId: String)
}
