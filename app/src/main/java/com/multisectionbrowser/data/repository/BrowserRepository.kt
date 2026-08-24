package com.multisectionbrowser.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.multisectionbrowser.data.db.AppDatabase
import com.multisectionbrowser.data.db.SessionEntity
import com.multisectionbrowser.data.db.TabEntity
import com.multisectionbrowser.data.db.ExtensionEntity
import com.multisectionbrowser.data.db.SessionExtensionSettingsEntity
import com.multisectionbrowser.engine.BrowserSession
import com.multisectionbrowser.engine.BrowserTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrowserRepository(private val db: AppDatabase) {

    companion object {
        @Volatile private var INSTANCE: BrowserRepository? = null

        fun getInstance(context: Context): BrowserRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = BrowserRepository(AppDatabase.getDatabase(context))
                INSTANCE = instance
                instance
            }
        }
    }

    // Session operations
    suspend fun insertSession(session: BrowserSession) {
        db.sessionDao().insert(SessionEntity.fromDomain(session))
    }

    suspend fun updateSession(session: BrowserSession) {
        db.sessionDao().update(SessionEntity.fromDomain(session))
    }

    suspend fun deleteSession(sessionId: String) {
        db.sessionDao().delete(sessionId)
        db.tabDao().deleteAllForSession(sessionId)
        db.sessionExtensionSettingsDao().deleteAllForSession(sessionId)
    }

    fun getAllSessions(): LiveData<List<SessionEntity>> {
        return db.sessionDao().getAllSessions()
    }

    suspend fun getAllSessionsSync(): List<SessionEntity> {
        return db.sessionDao().getAllSessionsSync()
    }

    suspend fun getSession(sessionId: String): SessionEntity? {
        return db.sessionDao().getSession(sessionId)
    }

    suspend fun getActiveSession(): SessionEntity? {
        return db.sessionDao().getActiveSession()
    }

    suspend fun setActiveSession(sessionId: String) {
        db.sessionDao().deactivateAll()
        db.sessionDao().setActive(sessionId)
    }

    // Tab operations
    suspend fun insertTab(tab: BrowserTab) {
        db.tabDao().insert(TabEntity.fromDomain(tab))
    }

    suspend fun insertTabs(tabs: List<BrowserTab>) {
        db.tabDao().insertAll(tabs.map { TabEntity.fromDomain(it) })
    }

    suspend fun updateTab(tab: BrowserTab) {
        db.tabDao().update(TabEntity.fromDomain(tab))
    }

    suspend fun deleteTab(tabId: String) {
        db.tabDao().delete(tabId)
    }

    suspend fun deleteAllTabsForSession(sessionId: String) {
        db.tabDao().deleteAllForSession(sessionId)
    }

    fun getTabsForSession(sessionId: String): LiveData<List<TabEntity>> {
        return db.tabDao().getTabsForSession(sessionId)
    }

    suspend fun getTabsForSessionSync(sessionId: String): List<TabEntity> {
        return db.tabDao().getTabsForSessionSync(sessionId)
    }

    suspend fun getTab(tabId: String): TabEntity? {
        return db.tabDao().getTab(tabId)
    }

    suspend fun getActiveTab(sessionId: String): TabEntity? {
        return db.tabDao().getActiveTab(sessionId)
    }

    suspend fun setActiveTab(sessionId: String, tabId: String) {
        db.tabDao().deactivateAllForSession(sessionId)
        db.tabDao().setActive(tabId)
    }

    suspend fun updateTabTitle(tabId: String, title: String) {
        db.tabDao().updateTitle(tabId, title)
    }

    suspend fun updateTabUrl(tabId: String, url: String) {
        db.tabDao().updateUrl(tabId, url)
    }

    suspend fun updateTabLoading(tabId: String, isLoading: Boolean) {
        db.tabDao().updateLoading(tabId, isLoading)
    }

    suspend fun updateTabNavigation(tabId: String, canGoBack: Boolean, canGoForward: Boolean) {
        db.tabDao().updateNavigation(tabId, canGoBack, canGoForward)
    }

    suspend fun updateTabFavicon(tabId: String, favicon: String?) {
        db.tabDao().updateFavicon(tabId, favicon)
    }

    suspend fun updateTabLastAccessed(tabId: String, timestamp: Long = System.currentTimeMillis()) {
        db.tabDao().updateLastAccessed(tabId, timestamp)
    }

    // Extension operations
    suspend fun insertExtension(extension: ExtensionEntity) {
        db.extensionDao().insert(extension)
    }

    suspend fun updateExtension(extension: ExtensionEntity) {
        db.extensionDao().update(extension)
    }

    suspend fun deleteExtension(extensionId: String) {
        db.extensionDao().delete(extensionId)
    }

    fun getAllExtensions(): LiveData<List<ExtensionEntity>> {
        return db.extensionDao().getAllExtensions()
    }

    suspend fun getAllExtensionsSync(): List<ExtensionEntity> {
        return db.extensionDao().getAllExtensionsSync()
    }

    suspend fun getExtension(extensionId: String): ExtensionEntity? {
        return db.extensionDao().getExtension(extensionId)
    }

    suspend fun getExtensionBySourceUrl(url: String): ExtensionEntity? {
        return db.extensionDao().getExtensionBySourceUrl(url)
    }

    // Session Extension Settings operations
    suspend fun upsertSessionExtensionSetting(setting: SessionExtensionSettingsEntity) {
        db.sessionExtensionSettingsDao().insert(setting)
    }

    suspend fun upsertSessionExtensionSettings(settings: List<SessionExtensionSettingsEntity>) {
        db.sessionExtensionSettingsDao().insertAll(settings)
    }

    suspend fun deleteSessionExtensionSetting(sessionId: String, extensionId: String) {
        db.sessionExtensionSettingsDao().delete(sessionId, extensionId)
    }

    fun getSettingsForSession(sessionId: String): LiveData<List<SessionExtensionSettingsEntity>> {
        return db.sessionExtensionSettingsDao().getSettingsForSession(sessionId)
    }

    suspend fun getSettingsForSessionSync(sessionId: String): List<SessionExtensionSettingsEntity> {
        return db.sessionExtensionSettingsDao().getSettingsForSessionSync(sessionId)
    }

    suspend fun getSetting(sessionId: String, extensionId: String): SessionExtensionSettingsEntity? {
        return db.sessionExtensionSettingsDao().getSetting(sessionId, extensionId)
    }

    suspend fun getEnabledExtensionsForSession(sessionId: String): List<SessionExtensionSettingsEntity> {
        return db.sessionExtensionSettingsDao().getEnabledExtensionsForSession(sessionId)
    }

    suspend fun getExtensionsForSessionByMode(sessionId: String, mode: Int): List<SessionExtensionSettingsEntity> {
        return db.sessionExtensionSettingsDao().getExtensionsForSessionByMode(sessionId, mode)
    }

    suspend fun setExtensionEnabled(sessionId: String, extensionId: String, enabled: Boolean) {
        db.sessionExtensionSettingsDao().setEnabled(sessionId, extensionId, enabled)
    }

    suspend fun setExtensionTriggerMode(sessionId: String, extensionId: String, mode: Int) {
        db.sessionExtensionSettingsDao().setTriggerMode(sessionId, extensionId, mode)
    }

    suspend fun updateExtensionSettingsJson(sessionId: String, extensionId: String, json: String) {
        db.sessionExtensionSettingsDao().updateSettingsJson(sessionId, extensionId, json)
    }

    // Initialize default session settings when a session is created
    suspend fun initializeSessionExtensionSettings(sessionId: String) {
        val extensions = getAllExtensionsSync()
        val settings = extensions.map { ext ->
            SessionExtensionSettingsEntity(
                sessionId = sessionId,
                extensionId = ext.id,
                isEnabled = true,
                triggerMode = SessionExtensionSettingsEntity.TRIGGER_AUTO
            )
        }
        upsertSessionExtensionSettings(settings)
    }
}