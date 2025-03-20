package com.example.antibully.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: String,
    val reporterId: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
