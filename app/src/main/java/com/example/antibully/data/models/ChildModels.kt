package com.example.antibully.data.models

data class LinkChildRequest(
    val discordId: String,
    val name: String,
    val imageUrl: String? = null
)

data class UpdateChildRequest(
    val name: String?,
    val imageUrl: String?
)

data class ChildResponse(
    val _id: String,
    val discordId: String,
    val name: String?,
    val imageUrl: String?,
    val parents: List<String>,
    val createdAt: String,
    val updatedAt: String
)

data class UnlinkChildResponse(
    val message: String
)