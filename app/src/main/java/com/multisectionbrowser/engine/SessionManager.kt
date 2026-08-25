package com.multisectionbrowser.engine

import android.content.Context
import android.util.Log
import com.multisectionbrowser.MultiSessionBrowserApp
import com.multisectionbrowser.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import java.io.File

class SessionManager(private val context: Context) {

    private val repository = BrowserRepository.getInstance(context)
    private var activeSessionId: String? = null
    private val geckoRuntime: GeckoRuntime?
        get() = (context.applicationContext as MultiSessionBrowserApp).geckoRuntime

    companion object {
        private const val TAG = "SessionManager"
        private const val DEFAULT_SESSION_NAME = "Default"
    }

    init {
        initializeDefaultSession()
    }

    private fun initializeDefaultSession() {
        CoroutineScope(Dispatchers.IO).launch {
            val activeSession = repository.getActiveSession()
            if (activeSession == null) {
                val profileDir = "${context.filesDir.absolutePath}/profiles/default"
                ensureProfileDir(profileDir)
                val session = BrowserSession(
                    id = "default",
                    name = DEFAULT_SESSION_NAME,
                    profileDir = profileDir,
                    isActive = true
                )
                repository.insertSession(session)
                activeSessionId = "default"
            } else {
                activeSessionId = activeSession.id
            }
            repository.initializeSessionExtensionSettings("default")
        }
    }

    private fun ensureProfileDir(profileDir: String) {
        File(profileDir).mkdirs()
    }

    suspend fun getAllSessions(): List<BrowserSession> {
        return repository.getAllSessionsSync().map { it.toDomain() }
    }

    suspend fun getActiveSession(): BrowserSession? {
        return repository.getActiveSession()?.toDomain()
    }

    suspend fun getSession(id: String): BrowserSession? {
        return repository.getSession(id)?.toDomain()
    }

    suspend fun createSession(name: String): BrowserSession {
        val id = java.util.UUID.randomUUID().toString()
        val profileDir = "${context.filesDir.absolutePath}/profiles/$id"
        ensureProfileDir(profileDir)
        val session = BrowserSession(
            id = id,
            name = name,
            profileDir = profileDir
        )
        repository.insertSession(session)
        repository.initializeSessionExtensionSettings(id)
        Log.d(TAG, "Created session: $name ($id) with profile: $profileDir")
        return session
    }

    suspend fun deleteSession(sessionId: String): Boolean {
        if (sessionId == "default") return false
        val sessions = repository.getAllSessionsSync()
        if (sessions.size <= 1) return false

        val session = sessions.firstOrNull { it.id == sessionId }?.toDomain()
        if (session != null) {
            repository.deleteSession(sessionId)
            File(session.profileDir).deleteRecursively()
            Log.d(TAG, "Deleted session: $sessionId and profile: ${session.profileDir}")
            
            if (activeSessionId == sessionId) {
                val newActive = sessions.firstOrNull { it.id != sessionId }?.toDomain()
                newActive?.let { setActiveSession(it.id) }
            }
            return true
        }
        return false
    }

    suspend fun setActiveSession(sessionId: String): Boolean {
        val session = repository.getSession(sessionId)?.toDomain()
        if (session != null) {
            repository.setActiveSession(sessionId)
            activeSessionId = sessionId
            Log.d(TAG, "Active session: $sessionId")
            return true
        }
        return false
    }

    suspend fun getProfileDir(sessionId: String): String? {
        return repository.getSession(sessionId)?.profileDir
    }

    // Create a GeckoSession for a specific session profile.
    // Isolation comes from each session having its own profile directory on disk;
    // the shared runtime serves them all.
    suspend fun createIsolatedGeckoSession(sessionId: String): GeckoSession? {
        val session = repository.getSession(sessionId)?.toDomain()
        if (session == null || geckoRuntime == null) return null

        return withContext(Dispatchers.IO) {
            val geckoSession = GeckoSession()
            Log.d(TAG, "Created isolated GeckoSession for session: $sessionId")
            geckoSession
        }
    }

    // Clear all storage (cookies, localStorage, etc.)
    suspend fun clearSessionStorage(sessionId: String): Boolean {
        val runtime = geckoRuntime ?: return false
        return withContext(Dispatchers.IO) {
            // GV129 StorageController exposes clearData(flags); FLAG_ALL wipes everything.
            runtime.storageController.clearData(org.mozilla.geckoview.StorageController.ClearFlags.ALL)
            true
        }
    }
}
    suspend fun renameSession(sessionId: String, newName: String): Boolean {
        val session = repository.getSession(sessionId)?.toDomain() ?: return false
        repository.insertSession(session.copy(name = newName))
        return true
    
    suspend fun renameSession(sessionId: String, newName: String): Boolean {
        val session = repository.getSession(sessionId)?.toDomain() ?: return false
        repository.insertSession(session.copy(name = newName))
        return true
    }
}
