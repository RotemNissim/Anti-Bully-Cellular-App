package com.example.antibully.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "child_local_data",
    primaryKeys = ["childId", "parentUserId"] // ✅ Composite primary key
)
data class ChildLocalData(
    val childId: String,
    val parentUserId: String,
    val name: String,
    val imageUrl: String? = null
)
