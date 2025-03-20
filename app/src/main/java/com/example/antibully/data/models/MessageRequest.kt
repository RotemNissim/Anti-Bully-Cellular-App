package com.example.antibully.data.models

data class MessageRequest(
    val messageId: String,
    val userId: String,
    val text: String,
    val flagged: Boolean,
    val reason: String?
)
