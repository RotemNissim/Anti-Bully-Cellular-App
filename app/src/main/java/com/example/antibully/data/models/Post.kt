package com.example.antibully.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val alertId: String,
    val userId: String,
    val text: String,
    val imageUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)
