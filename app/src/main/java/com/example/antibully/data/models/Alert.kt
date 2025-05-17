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
        fun fromApi(response: AlertApiRequest): Alert {
            return Alert(
                postId = response._id,
                reporterId = response.childId,
                text = response.severity,
                reason = response.summary ?:"No reason provided",
                imageUrl = response.imageUrl,
                timestamp = response.timestamp
            )
        }
    }
}
