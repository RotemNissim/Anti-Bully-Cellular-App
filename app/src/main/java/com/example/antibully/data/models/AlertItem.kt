package com.example.antibully.data.models
import com.example.antibully.data.models.Alert

sealed class AlertItem {
    data class UnreadGroup(
        val childId: String,
        val count: Int
    ) : AlertItem()

    data class SingleAlert(val alert: Alert) : AlertItem()
}
