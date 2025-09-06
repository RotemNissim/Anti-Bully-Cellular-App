package com.example.antibully.data.models

import androidx.room.Entity
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(
    tableName = "child_local_data",
    primaryKeys = ["childId", "parentUserId"]
)
data class ChildLocalData(
    val childId: String = "",
    val parentUserId: String = "",
    val name: String = "",
    val imageUrl: String? = null
)
