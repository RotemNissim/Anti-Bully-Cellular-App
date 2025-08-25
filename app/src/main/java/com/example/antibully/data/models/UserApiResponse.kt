package com.example.antibully.data.models

data class UserApiResponse(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profilePictureUrl: String? = null
)

data class EditUserDTO(
    val userId: String,
    val username: String? = null,
    val profileImageUrl: String? = null
)