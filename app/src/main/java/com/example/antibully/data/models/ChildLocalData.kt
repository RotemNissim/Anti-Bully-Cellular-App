package com.example.antibully.data.models

import androidx.room.Entity
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
@Entity(
    tableName = "child_local_data",
    primaryKeys = ["childId", "parentUserId"]
)
data class ChildLocalData(
    val childId: String = "",        // default for no-arg ctor
    val parentUserId: String = "",   // default for no-arg ctor
    val name: String = "",           // default for no-arg ctor
    val imageUrl: String? = null     // nullable with default
)
