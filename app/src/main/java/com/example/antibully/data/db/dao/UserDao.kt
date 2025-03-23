package com.example.antibully.data.db.dao

import androidx.room.*
import com.example.antibully.data.models.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): User?

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
