package com.multisectionbrowser.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SessionExtensionSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: SessionExtensionSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<SessionExtensionSettingsEntity>)

    @Update
    suspend fun update(settings: SessionExtensionSettingsEntity)

    @Query("DELETE FROM session_extension_settings WHERE sessionId = :sessionId AND extensionId = :extensionId")
    suspend fun delete(sessionId: String, extensionId: String)

    @Query("DELETE FROM session_extension_settings WHERE sessionId = :sessionId")
    suspend fun deleteAllForSession(sessionId: String)

    @Query("SELECT * FROM session_extension_settings WHERE sessionId = :sessionId")
    fun getSettingsForSession(sessionId: String): LiveData<List<SessionExtensionSettingsEntity>>

    @Query("SELECT * FROM session_extension_settings WHERE sessionId = :sessionId")
    suspend fun getSettingsForSessionSync(sessionId: String): List<SessionExtensionSettingsEntity>

    @Query("SELECT * FROM session_extension_settings WHERE sessionId = :sessionId AND extensionId = :extensionId")
    suspend fun getSetting(sessionId: String, extensionId: String): SessionExtensionSettingsEntity?

    @Query("SELECT * FROM session_extension_settings WHERE extensionId = :extensionId")
    suspend fun getSettingsForExtension(extensionId: String): List<SessionExtensionSettingsEntity>

    @Query("""
        SELECT ses.* FROM session_extension_settings ses
        JOIN extensions e ON ses.extensionId = e.id
        WHERE ses.sessionId = :sessionId AND ses.isEnabled = 1
        ORDER BY e.name ASC
    """)
    suspend fun getEnabledExtensionsForSession(sessionId: String): List<SessionExtensionSettingsEntity>

    @Query("""
        SELECT ses.* FROM session_extension_settings ses
        JOIN extensions e ON ses.extensionId = e.id
        WHERE ses.sessionId = :sessionId AND ses.triggerMode = :mode
        ORDER BY e.name ASC
    """)
    suspend fun getExtensionsForSessionByMode(sessionId: String, mode: Int): List<SessionExtensionSettingsEntity>

    @Query("UPDATE session_extension_settings SET isEnabled = :enabled WHERE sessionId = :sessionId AND extensionId = :extensionId")
    suspend fun setEnabled(sessionId: String, extensionId: String, enabled: Boolean)

    @Query("UPDATE session_extension_settings SET triggerMode = :mode WHERE sessionId = :sessionId AND extensionId = :extensionId")
    suspend fun setTriggerMode(sessionId: String, extensionId: String, mode: Int)

    @Query("UPDATE session_extension_settings SET settingsJson = :json WHERE sessionId = :sessionId AND extensionId = :extensionId")
    suspend fun updateSettingsJson(sessionId: String, extensionId: String, json: String)
}