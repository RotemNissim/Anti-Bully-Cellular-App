package com.example.antibully.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.antibully.data.models.ChildLocalData

@Dao
interface ChildDao {

    @Query("SELECT * FROM child_local_data WHERE parentUserId = :userId")
    suspend fun getChildrenForUser(userId: String): List<ChildLocalData>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChild(child: ChildLocalData)

    @Query("DELETE FROM child_local_data WHERE childId = :childId AND parentUserId = :userId")
    suspend fun deleteChild(childId: String, userId: String)

    @Query("UPDATE child_local_data SET name = :newName, imageUrl = :newUrl WHERE childId = :childId AND parentUserId = :userId")
    suspend fun updateChild(childId: String, userId: String, newName: String, newUrl: String)
}
