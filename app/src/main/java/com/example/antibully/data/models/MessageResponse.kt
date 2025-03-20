package com.example.antibully.data.models

data class MessageResponse(
    val messageId: String,
    val flagged: Boolean,
    val reason: String?
)
