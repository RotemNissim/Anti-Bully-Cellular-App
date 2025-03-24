package com.example.antibully.data.models

import com.google.gson.annotations.SerializedName

data class MessageRequest(
    @SerializedName("id")
    val messageId: String,
    val userId: String,
    val text: String,
    val flagged: Boolean,
    val reason: String?,
    @SerializedName("imageUrl")
    val imageUrl: String?,
    @SerializedName("messageReceived")
    val timestamp: Long
)
