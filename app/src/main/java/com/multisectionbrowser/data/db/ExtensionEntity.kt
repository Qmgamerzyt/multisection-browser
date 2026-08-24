package com.multisectionbrowser.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extensions")
data class ExtensionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val sourceUrl: String, // AMO URL or local file path
    val xpiPath: String? = null, // Local path if downloaded
    val isInstalled: Boolean = false,
    val installMethod: Int = 0, // 0=AMO, 1=Manual XPI
    val permissions: String = "", // JSON array of permissions
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)