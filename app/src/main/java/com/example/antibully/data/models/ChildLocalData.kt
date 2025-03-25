package com.example.antibully.data.models

import androidx.room.Entity

@Entity(tableName = "child_local_data", primaryKeys = ["childId", "parentUserId"])
data class ChildLocalData(
    val childId: String,
    val parentUserId: String,
    val localImagePath: String
)
