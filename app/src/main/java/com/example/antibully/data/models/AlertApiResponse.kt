package com.example.antibully.data.models

import com.google.gson.annotations.SerializedName

data class AlertApiResponse(
    @SerializedName("_id")
    val id: String,
    
    @SerializedName("discordId")
    val childId: String,
    
    @SerializedName("summary")
    val summary: String?,
    
    @SerializedName("severity")
    val severity: String,
    
    @SerializedName("imageUrl")
    val imageUrl: String?,
    
    @SerializedName("timestamp")
    val timestamp: Long, //
    
    @SerializedName("createdAt")
    val createdAt: String?,
    
    @SerializedName("updatedAt")
    val updatedAt: String?
)
