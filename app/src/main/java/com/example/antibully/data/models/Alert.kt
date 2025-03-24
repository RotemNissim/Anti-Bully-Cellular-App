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
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromApi(response: MessageRequest): Alert {
            return Alert(
                postId = response.messageId,
                reporterId = response.userId,
                text = response.text,
                reason = response.reason ?:"No reason provided",
                imageUrl = response.imageUrl,
                timestamp = response.timestamp  * 1000
            )
        }
    }
}
