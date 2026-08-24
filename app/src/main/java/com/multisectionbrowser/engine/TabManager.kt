package com.multisectionbrowser.engine

import android.content.Context
import android.util.Log
import com.multisectionbrowser.data.repository.BrowserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.StorageController

class TabManager(
    private val context: Context,
    private val sessionManager: SessionManager
) {

    private val repository = BrowserRepository.getInstance(context)
    private val geckoSessions = mutableMapOf<String, GeckoSession>()
    private var activeTabId: String? = null
    private val geckoRuntime: GeckoRuntime? = (context.applicationContext as MultiSessionBrowserApp).getGeckoRuntime()

    companion object {
        private const val TAG = "TabManager"
    }

    suspend fun getAllTabs(sessionId: String): List<BrowserTab> {
        return withContext(Dispatchers.IO) {
            repository.getTabsForSessionSync(sessionId).map { it.toDomain() }
        }
    }

    suspend fun getActiveTab(): BrowserTab? {
        return withContext(Dispatchers.IO) {
            val session = sessionManager.getActiveSession()
            session?.let { repository.getActiveTab(it.id)?.toDomain() }
        }
    }

    suspend fun getActiveTab(sessionId: String): BrowserTab? {
        return withContext(Dispatchers.IO) {
            repository.getActiveTab(sessionId)?.toDomain()
        }
    }

    suspend fun getTab(tabId: String): BrowserTab? {
        return withContext(Dispatchers.IO) {
            repository.getTab(tabId)?.toDomain()
        }
    }

    fun getGeckoSession(tabId: String): GeckoSession? {
        return geckoSessions[tabId]
    }

    // Create a tab with isolated GeckoSession for the session's profile
    suspend fun createTab(sessionId: String, url: String = ""): BrowserTab? {
        val session = sessionManager.getSession(sessionId)
        val runtime = geckoRuntime
        if (session == null || runtime == null) return null

        return withContext(Dispatchers.IO) {
            val tabId = java.util.UUID.randomUUID().toString()
            
            // Create isolated GeckoSession for this session's profile
            val geckoSession = GeckoSession(runtime)

            val tab = BrowserTab(
                id = tabId,
                sessionId = sessionId,
                title = "New Tab",
                url = url,
                isActive = true
            )

            // Deactivate other tabs in this session
            val existingTabs = repository.getTabsForSessionSync(sessionId)
            existingTabs.forEach { t ->
                if (t.isActive) {
                    repository.updateTab(t.toDomain().copy(isActive = false))
                }
            }

            repository.insertTab(tab)
            geckoSessions[tabId] = geckoSession
            activeTabId = tabId

            if (!url.isEmpty()) {
                loadUrl(tabId, url)
            }

            Log.d(TAG, "Created tab: $tabId for session: $sessionId with isolated storage")
            tab
        }
    }

    suspend fun closeTab(tabId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val tab = repository.getTab(tabId)
            val geckoSession = geckoSessions.remove(tabId)
            geckoSession?.close()

            if (tab != null) {
                repository.deleteTab(tabId)
                if (activeTabId == tabId) {
                    val remainingTabs = repository.getTabsForSessionSync(tab.sessionId)
                    val nextTab = remainingTabs.firstOrNull()
                    nextTab?.let {
                        repository.setActiveTab(tab.sessionId, it.id)
                        activeTabId = it.id
                    } ?: run {
                        activeTabId = null
                    }
                }
                Log.d(TAG, "Closed tab: $tabId")
                true
            } else {
                false
            }
        }
    }

    suspend fun setActiveTab(tabId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val tab = repository.getTab(tabId)
            if (tab != null) {
                repository.setActiveTab(tab.sessionId, tabId)
                activeTabId = tabId
                Log.d(TAG, "Active tab: $tabId")
                true
            } else {
                false
            }
        }
    }

    suspend fun loadUrl(tabId: String, url: String) {
        withContext(Dispatchers.IO) {
            val geckoSession = geckoSessions[tabId]
            if (geckoSession != null) {
                val processedUrl = if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) {
                    url
                } else if (url.contains(".") && !url.contains(" ")) {
                    "https://$url"
                } else {
                    "https://www.google.com/search?q=${url.replace(" ", "+")}"
                }
                geckoSession.loadUri(processedUrl)
                repository.updateTabUrl(tabId, processedUrl)
            }
        }
    }

    suspend fun goBack(tabId: String) {
        geckoSessions[tabId]?.goBack()
    }

    suspend fun goForward(tabId: String) {
        geckoSessions[tabId]?.goForward()
    }

    suspend fun reload(tabId: String) {
        geckoSessions[tabId]?.reload()
    }

    suspend fun stop(tabId: String) {
        geckoSessions[tabId]?.stop()
    }

    suspend fun updateTabState(
        tabId: String,
        title: String? = null,
        url: String? = null,
        isLoading: Boolean? = null,
        canGoBack: Boolean? = null,
        canGoForward: Boolean? = null,
        favicon: String? = null
    ) {
        withContext(Dispatchers.IO) {
            val existingTab = repository.getTab(tabId)
            if (existingTab == null) return@withContext
            val tab = existingTab.toDomain()
            
            val newTitle = title ?: tab.title
            val newUrl = url ?: tab.url
            val newIsLoading = isLoading ?: tab.isLoading
            val newCanGoBack = canGoBack ?: tab.canGoBack
            val newCanGoForward = canGoForward ?: tab.canGoForward
            val newFavicon = favicon ?: tab.favicon
            
            val updatedTab = tab.copy(
                title = newTitle,
                url = newUrl,
                isLoading = newIsLoading,
                canGoBack = newCanGoBack,
                canGoForward = newCanGoForward,
                favicon = newFavicon
            )
            repository.updateTab(updatedTab)
        }
    }

    // Clear storage for a specific tab's session
    suspend fun clearTabStorage(tabId: String): Boolean {
        val geckoSession = geckoSessions[tabId]
        if (geckoSession != null) {
            return withContext(Dispatchers.IO) {
                val storageController = geckoSession.storageController
                storageController?.clearAllStorage()
                Log.d(TAG, "Cleared storage for tab: $tabId")
                true
            }
        }
        return false
    }

    // Get storage controller for a tab (for managing cookies, etc.)
    fun getStorageController(tabId: String): StorageController? {
        return geckoSessions[tabId]?.storageController
    }
}