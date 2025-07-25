package com.example.antibully.data.models

data class NotificationGroup(
    val childId: String,
    val childName: String,
    val childImageUrl: String?,
    val unreadCount: Int,
    val alerts: List<Alert>,
    val latestTimestamp: Long
)