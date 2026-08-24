package com.multisectionbrowser.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface TabDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tab: TabEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tabs: List<TabEntity>)

    @Update
    suspend fun update(tab: TabEntity)

    @Query("DELETE FROM tabs WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tabs WHERE sessionId = :sessionId")
    suspend fun deleteAllForSession(sessionId: String)

    @Query("SELECT * FROM tabs WHERE sessionId = :sessionId ORDER BY lastAccessedAt DESC")
    fun getTabsForSession(sessionId: String): LiveData<List<TabEntity>>

    @Query("SELECT * FROM tabs WHERE sessionId = :sessionId ORDER BY lastAccessedAt DESC")
    suspend fun getTabsForSessionSync(sessionId: String): List<TabEntity>

    @Query("SELECT * FROM tabs WHERE id = :id")
    suspend fun getTab(id: String): TabEntity?

    @Query("SELECT * FROM tabs WHERE sessionId = :sessionId AND isActive = 1 LIMIT 1")
    suspend fun getActiveTab(sessionId: String): TabEntity?

    @Query("UPDATE tabs SET isActive = 0 WHERE sessionId = :sessionId")
    suspend fun deactivateAllForSession(sessionId: String)

    @Query("UPDATE tabs SET isActive = 1 WHERE id = :id")
    suspend fun setActive(id: String)

    @Query("UPDATE tabs SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: String, title: String)

    @Query("UPDATE tabs SET url = :url WHERE id = :id")
    suspend fun updateUrl(id: String, url: String)

    @Query("UPDATE tabs SET isLoading = :isLoading WHERE id = :id")
    suspend fun updateLoading(id: String, isLoading: Boolean)

    @Query("UPDATE tabs SET canGoBack = :canGoBack, canGoForward = :canGoForward WHERE id = :id")
    suspend fun updateNavigation(id: String, canGoBack: Boolean, canGoForward: Boolean)

    @Query("UPDATE tabs SET favicon = :favicon WHERE id = :id")
    suspend fun updateFavicon(id: String, favicon: String?)

    @Query("UPDATE tabs SET lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: String, timestamp: Long)
}