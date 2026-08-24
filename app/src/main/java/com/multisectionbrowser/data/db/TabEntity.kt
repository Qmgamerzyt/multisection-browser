package com.multisectionbrowser.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tabs",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class TabEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val title: String = "New Tab",
    val url: String = "",
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val favicon: String? = null,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): com.multisectionbrowser.engine.BrowserTab {
        return com.multisectionbrowser.engine.BrowserTab(
            id = id,
            sessionId = sessionId,
            title = title,
            url = url,
            isLoading = isLoading,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            favicon = favicon,
            isActive = isActive,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(tab: com.multisectionbrowser.engine.BrowserTab): TabEntity {
            return TabEntity(
                id = tab.id,
                sessionId = tab.sessionId,
                title = tab.title,
                url = tab.url,
                isLoading = tab.isLoading,
                canGoBack = tab.canGoBack,
                canGoForward = tab.canGoForward,
                favicon = tab.favicon,
                isActive = tab.isActive,
                createdAt = tab.createdAt,
                lastAccessedAt = System.currentTimeMillis()
            )
        }
    }
}