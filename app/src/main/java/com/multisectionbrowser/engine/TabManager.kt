package com.multisectionbrowser.engine

import android.content.Context
import android.util.Log
import com.multisectionbrowser.MultiSessionBrowserApp
import com.multisectionbrowser.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

/**
 * Owns every live [GeckoSession] (one per tab) and wires the required
 * delegates BEFORE any URL is loaded so navigation/touch never hits a
 * half-initialised session.
 */
class TabManager(
    private val context: Context,
    private val sessionManager: SessionManager
) {

    interface UiListener { fun onTabChanged(tabId: String) }
    @Volatile var uiListener: UiListener? = null

    private val repository = BrowserRepository.getInstance(context)
    private val geckoSessions = mutableMapOf<String, GeckoSession>()
    private var activeTabId: String? = null
    private val geckoRuntime: GeckoRuntime?
        get() = (context.applicationContext as MultiSessionBrowserApp).geckoRuntime

    companion object {
        private const val TAG = "TabManager"
        private const val HOME_URL = "https://www.google.com"
        fun normalize(input: String): String = when {
            input.startsWith("http://") || input.startsWith("https://") ||
                input.startsWith("file://") || input.startsWith("javascript:") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://www.google.com/search?q=${android.net.Uri.encode(input)}"
        }
    }

    // ------------------------------------------------------------------ queries

    suspend fun getAllTabs(sessionId: String): List<BrowserTab> = withContext(Dispatchers.IO) {
        repository.getTabsForSessionSync(sessionId).map { it.toDomain() }
    }

    suspend fun getActiveTab(): BrowserTab? = withContext(Dispatchers.IO) {
        sessionManager.getActiveSession()?.let {
            repository.getActiveTab(it.id)?.toDomain()
        }
    }

    suspend fun getActiveTab(sessionId: String): BrowserTab? = withContext(Dispatchers.IO) {
        repository.getActiveTab(sessionId)?.toDomain()
    }

    suspend fun getTab(tabId: String): BrowserTab? = withContext(Dispatchers.IO) {
        repository.getTab(tabId)?.toDomain()
    }

    fun getGeckoSession(tabId: String): GeckoSession? = geckoSessions[tabId]

    // ------------------------------------------------------------------ lifecycle

    /**
     * Creates a tab whose GeckoSession is:
     *   constructed -> OPENED on the shared runtime -> fully delegated
     *   -> only then loads a URL.
     */
    suspend fun createTab(sessionId: String, url: String = ""): BrowserTab? {
        return withContext(Dispatchers.IO) {
            val runtime = geckoRuntime ?: return@withContext null
            val tabId = java.util.UUID.randomUUID().toString()

            val geckoSession = GeckoSession()
            
            // CRITICAL FIX: Open session and wait for it to be ready
            // GeckoSession.open() is synchronous in GV 129
            geckoSession.open(runtime)
            
            wireDelegates(tabId, geckoSession)          // delegates BEFORE first load

            val startUrl = url.ifBlank { HOME_URL }
            val tab = BrowserTab(
                id = tabId,
                sessionId = sessionId,
                title = "New Tab",
                url = startUrl,
                isActive = true
            )

            repository.getTabsForSessionSync(sessionId)
                .filter { it.isActive }
                .forEach { repository.updateTab(it.toDomain().copy(isActive = false)) }

            repository.insertTab(tab)
            synchronized(geckoSessions) { geckoSessions[tabId] = geckoSession }
            activeTabId = tabId

            // Now safe to load - session is fully open
            geckoSession.loadUri(normalize(startUrl))
            Log.d(TAG, "Created+opened tab $tabId for session $sessionId")
            tab
        }
    }

    suspend fun closeTab(tabId: String): Boolean = withContext(Dispatchers.IO) {
        val tab = repository.getTab(tabId) ?: return@withContext false
        val gs = synchronized(geckoSessions) { geckoSessions.remove(tabId) }
        try { gs?.close() } catch (e: Exception) { Log.w(TAG, "close", e) }
        repository.deleteTab(tabId)
        if (activeTabId == tabId) {
            val next = repository.getTabsForSessionSync(tab.sessionId).firstOrNull()
            next?.let {
                repository.setActiveTab(tab.sessionId, it.id)
                activeTabId = it.id
            } ?: run {
                createTabInternalLocked(tab.sessionId)
            }
        }
        Log.d(TAG, "Closed tab $tabId")
        true
    }

    /** Must be called from IO thread; used when the last tab of a session closes. */
    private suspend fun createTabInternalLocked(sessionId: String) {
        val tabId = java.util.UUID.randomUUID().toString()
        val runtime = geckoRuntime ?: return
        
        val gs = GeckoSession()
        gs.open(runtime)
        wireDelegates(tabId, gs)
        
        val home = HOME_URL
        repository.insertTab(
            BrowserTab(id = tabId, sessionId = sessionId, title = "New Tab",
                       url = home, isActive = true)
        )
        synchronized(geckoSessions) { geckoSessions[tabId] = gs }
        activeTabId = tabId
        gs.loadUri(home)
    }

    suspend fun setActiveTab(tabId: String): Boolean = withContext(Dispatchers.IO) {
        val tab = repository.getTab(tabId) ?: return@withContext false
        repository.setActiveTab(tab.sessionId, tabId)
        activeTabId = tabId
        true
    }

    // ------------------------------------------------------------------ navigation

    suspend fun loadUrl(tabId: String, rawUrl: String) {
        val target = normalize(rawUrl.trim())
        if (target.startsWith("javascript:")) { evalJs(tabId, target.removePrefix("javascript:")); return }
        withContext(Dispatchers.IO) {
            val gs = synchronized(geckoSessions) { geckoSessions[tabId] } ?: return@withContext
            if (!gs.isOpen) return@withContext
            gs.loadUri(target)
            repository.updateTabUrl(tabId, target)
        }
    }

    fun evalJs(tabId: String, js: String) {
        val gs = synchronized(geckoSessions) { geckoSessions[tabId] } ?: return
        if (!gs.isOpen) return
        gs.loadUri("javascript:(function(){ ${js.trim().removePrefix("javascript:")} })();")
    }

    suspend fun goBack(tabId: String)    { synchronized(geckoSessions){geckoSessions[tabId]}?.goBack() }
    suspend fun goForward(tabId: String) { synchronized(geckoSessions){geckoSessions[tabId]}?.goForward() }
    suspend fun reload(tabId: String)    { synchronized(geckoSessions){geckoSessions[tabId]}?.reload() }
    suspend fun stop(tabId: String)      { synchronized(geckoSessions){geckoSessions[tabId]}?.stop() }

    // ------------------------------------------------------------------ state persist

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
            val existing = repository.getTab(tabId) ?: return@withContext
            val t = existing.toDomain()
            repository.updateTab(
                t.copy(
                    title = title ?: t.title,
                    url = url ?: t.url,
                    isLoading = isLoading ?: t.isLoading,
                    canGoBack = canGoBack ?: t.canGoBack,
                    canGoForward = canGoForward ?: t.canGoForward,
                    favicon = favicon ?: t.favicon
                )
            )
        }
    }

    // ------------------------------------------------------------------ delegates

    private fun wireDelegates(tabId: String, gs: GeckoSession) {
        gs.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onProgressChange(session: GeckoSession, progress: Int) {
                launchIo { updateTabState(tabId, isLoading = progress in 1..99) ; notify(tabId) }
            }
        }
        gs.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) { launchIo { updateTabState(tabId, url = url ?: "") ; notify(tabId) } }

            override fun onCanGoBack(session: GeckoSession, value: Boolean) =
                launchIo { updateTabState(tabId, canGoBack = value) }

            override fun onCanGoForward(session: GeckoSession, value: Boolean) =
                launchIo { updateTabState(tabId, canGoForward = value) }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? = null // allow everything
        }
        gs.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                launchIo { updateTabState(tabId, title = title ?: "") ; notify(tabId) }
            }
        }
        // Present-but-empty PromptDelegate keeps JS alert/confirm/prompt from NPE-ing.
        gs.promptDelegate = object : GeckoSession.PromptDelegate {}
        gs.permissionDelegate = object : GeckoSession.PermissionDelegate {}
    }

    private fun notify(tabId: String) { uiListener?.onTabChanged(tabId) }

    private fun launchIo(block: suspend () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try { block() } catch (e: Exception) { Log.w(TAG, "delegate update", e) }
        }
    }
}