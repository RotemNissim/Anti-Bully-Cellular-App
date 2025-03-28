package com.example.antibully.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val localProfileImagePath: String,
    val profileImageUrl: String?
) {
    companion object {
        fun fromApi(api: UserApiResponse, localImagePath: String): User {
            return User(
                id = api.id,
                name = api.name,
                email = api.email,
                localProfileImagePath = localImagePath,
                profileImageUrl = api.profilePictureUrl
            )
        }
    }
}
