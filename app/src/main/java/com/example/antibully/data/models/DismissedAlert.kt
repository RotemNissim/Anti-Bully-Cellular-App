package com.example.antibully.data.models

import androidx.room.Entity

@Entity(tableName = "dismissed_alerts", primaryKeys = ["userId", "postId"])
data class DismissedAlert(
    val userId: String,
    val postId: String,
    val dismissedAt: Long = System.currentTimeMillis()
)
