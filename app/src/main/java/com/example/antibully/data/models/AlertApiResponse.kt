package com.example.antibully.data.models

import com.google.gson.annotations.SerializedName

data class AlertApiResponse(
    @SerializedName("_id")
    val id: String,
    
    @SerializedName("discordId") // ✅ Change from "childId" to "discordId" to match your backend
    val childId: String,
    
    @SerializedName("summary")
    val summary: String?,
    
    @SerializedName("severity")
    val severity: String,
    
    @SerializedName("imageUrl")
    val imageUrl: String?,
    
    @SerializedName("timestamp")
    val timestamp: Long, // ✅ This should now be a number from your backend
    
    @SerializedName("createdAt")
    val createdAt: String?,
    
    @SerializedName("updatedAt")
    val updatedAt: String?
)
