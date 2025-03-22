package com.example.antibully.data.models

data class AlertApiResponse(
    val id: String,
    val userId: String,
    val text: String,
    val imageUrl: String,
    val flagged: Boolean,
    val reason: String
)
