package com.example.antibully.data.models

data class AlertApiRequest(
    val _id: String,
    val childId: String,
    val summary: String,
    val imageUrl: String?,
    val severity: String,
    val timestamp: Long
)
