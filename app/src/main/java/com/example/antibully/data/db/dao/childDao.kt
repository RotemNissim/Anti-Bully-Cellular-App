package com.example.antibully.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.antibully.data.models.ChildLocalData
import kotlinx.coroutines.flow.Flow

 
@Dao
interface ChildDao {

    @Query("SELECT * FROM child_local_data WHERE parentUserId = :userId")
    suspend fun getChildrenForUser(userId: String): List<ChildLocalData>
    
    @Query("SELECT * FROM child_local_data WHERE parentUserId = :userId")
    fun getChildrenForUserFlow(userId: String): Flow<List<ChildLocalData>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChild(child: ChildLocalData)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChildren(children: List<ChildLocalData>)

    @Query("DELETE FROM child_local_data WHERE childId = :childId AND parentUserId = :userId")
    suspend fun deleteChild(childId: String, userId: String)
    
    @Query("DELETE FROM child_local_data WHERE parentUserId = :userId")
    suspend fun deleteAllChildrenForUser(userId: String)

    @Query("UPDATE child_local_data SET name = :newName, imageUrl = :newUrl WHERE childId = :childId AND parentUserId = :userId")
    suspend fun updateChild(childId: String, userId: String, newName: String, newUrl: String)
    
    @Transaction
    suspend fun replaceChildrenForUser(userId: String, children: List<ChildLocalData>) {
        deleteAllChildrenForUser(userId)
        insertChildren(children)
    }
}
