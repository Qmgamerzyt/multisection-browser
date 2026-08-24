package com.multisectionbrowser.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val profileDir: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
) {
    fun toDomain(): com.multisectionbrowser.engine.BrowserSession {
        return com.multisectionbrowser.engine.BrowserSession(
            id = id,
            name = name,
            profileDir = profileDir,
            createdAt = createdAt,
            isActive = isActive
        )
    }

    companion object {
        fun fromDomain(session: com.multisectionbrowser.engine.BrowserSession): SessionEntity {
            return SessionEntity(
                id = session.id,
                name = session.name,
                profileDir = session.profileDir,
                createdAt = session.createdAt,
                isActive = session.isActive
            )
        }
    }
}