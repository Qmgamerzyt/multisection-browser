package com.multisectionbrowser.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_extension_settings",
    primaryKeys = ["sessionId", "extensionId"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExtensionEntity::class,
            parentColumns = ["id"],
            childColumns = ["extensionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("extensionId")]
)
data class SessionExtensionSettingsEntity(
    val sessionId: String,
    val extensionId: String,
    val isEnabled: Boolean = true,
    val triggerMode: Int = 0, // 0=AUTO, 1=OFF, 2=MANUAL
    val settingsJson: String = "{}", // Extension-specific settings
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TRIGGER_AUTO = 0
        const val TRIGGER_OFF = 1
        const val TRIGGER_MANUAL = 2
    }
}