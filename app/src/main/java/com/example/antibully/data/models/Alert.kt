package com.example.antibully.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey val postId:String,
    val reporterId: String,
    val text: String,
    val reason: String,
    val imageUrl: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
) {
    companion object {
        fun fromApi(response: AlertApiResponse): Alert {
            return Alert(
                postId = response.id,
                reporterId = response.childId,
                text = response.severity,
                reason = response.summary ?:"No reason provided",
                imageUrl = response.imageUrl,
                timestamp = response.timestamp,
                isRead = false
            )
        }
    }
}
