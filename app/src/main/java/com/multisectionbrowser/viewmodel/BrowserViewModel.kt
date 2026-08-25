package com.multisectionbrowser.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.multisectionbrowser.engine.BrowserSession
import com.multisectionbrowser.engine.BrowserTab
import com.multisectionbrowser.engine.SessionManager
import com.multisectionbrowser.engine.TabManager
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val tabManager = TabManager(application, sessionManager)

    private val _sessions = MutableLiveData<List<BrowserSession>>()
    val sessions: LiveData<List<BrowserSession>> = _sessions

    private val _activeSession = MutableLiveData<BrowserSession?>()
    val activeSession: LiveData<BrowserSession?> = _activeSession

    private val _tabs = MutableLiveData<List<BrowserTab>>()
    val tabs: LiveData<List<BrowserTab>> = _tabs

    private val _activeTab = MutableLiveData<BrowserTab?>()
    val activeTab: LiveData<BrowserTab?> = _activeTab

    private val _urlInput = MutableLiveData<String>("")
    val urlInput: LiveData<String> = _urlInput

    init {
        loadInitialData()
    }

    private fun refreshSessionsBlocking() {
        _sessions.value = _sessions.value
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val sessions = sessionManager.getAllSessions()
            _sessions.postValue(sessions)
            val active = sessionManager.getActiveSession()
            _activeSession.postValue(active)
            if (active != null) {
                val tabs = tabManager.getAllTabs(active.id)
                _tabs.postValue(tabs)
                _activeTab.postValue(tabManager.getActiveTab(active.id))
            } else {
                // First launch: create default session + tab
                val s = sessionManager.createSession("Default")
                sessionManager.setActiveSession(s.id)
                tabManager.createTab(s.id)
                _sessions.postValue(sessionManager.getAllSessions())
                _activeSession.postValue(s)
                _tabs.postValue(tabManager.getAllTabs(s.id))
                _activeTab.postValue(tabManager.getActiveTab(s.id))
            }
        }
    }

    private suspend fun reloadUi(sessionId: String?) {
        _sessions.postValue(sessionManager.getAllSessions())
        if (sessionId != null) {
            _activeSession.postValue(sessionManager.getSession(sessionId))
            _tabs.postValue(tabManager.getAllTabs(sessionId))
            _activeTab.postValue(tabManager.getActiveTab(sessionId))
        }
    }

    fun createSession(name: String) {
        viewModelScope.launch {
            val session = sessionManager.createSession(name)
            sessionManager.setActiveSession(session.id)
            tabManager.createTab(session.id)
            reloadUi(session.id)
        }
    }

    fun setActiveSession(sessionId: String) {
        viewModelScope.launch {
            sessionManager.setActiveSession(sessionId)
            reloadUi(sessionId)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            sessionManager.deleteSession(sessionId)
            val active = sessionManager.getActiveSession()
            reloadUi(active?.id)
        }
    }

    fun createTab(url: String = "") {
        viewModelScope.launch {
            val sid = _activeSession.value?.id ?: return@launch
            tabManager.createTab(sid, url)
            reloadUi(sid)
        }
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch {
            val sid = _activeSession.value?.id
            tabManager.closeTab(tabId)
            // If no tabs remain in this session, create one
            if (sid != null && tabManager.getAllTabs(sid).isEmpty()) {
                tabManager.createTab(sid)
            }
            reloadUi(_activeSession.value?.id)
        }
    }

    fun setActiveTab(tabId: String) {
        viewModelScope.launch {
            tabManager.setActiveTab(tabId)
            reloadUi(_activeSession.value?.id)
        }
    }

    fun loadUrl(tabId: String, url: String) {
        viewModelScope.launch { tabManager.loadUrl(tabId, url) }
    }

    fun goBack(tabId: String) = viewModelScope.launch { tabManager.goBack(tabId) }

    fun goForward(tabId: String) = viewModelScope.launch { tabManager.goForward(tabId) }

    fun reload(tabId: String) = viewModelScope.launch { tabManager.reload(tabId) }

    fun stop(tabId: String) = viewModelScope.launch { tabManager.stop(tabId) }

    /**
     * Run JS in the current page via the javascript: URI scheme.
     * GeckoView 129 removed the old evalJS helper; wrapping in an IIFE keeps
     * the page from navigating away.
     */
    fun executeJavaScript(tabId: String, js: String) {
        val geckoSession = getGeckoSession(tabId) ?: return
        val trimmed = js.trim().removePrefix("javascript:")
        geckoSession.loadUri("javascript:(function(){ $trimmed })();")
    }

    fun updateUrlInput(url: String) {
        _urlInput.value = url
    }

    fun updateTabTitle(tabId: String, title: String) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, title = title)
            _tabs.postValue(_activeSession.value?.id?.let { tabManager.getAllTabs(it) })
            _activeTab.postValue(tabManager.getTab(tabId))
        }
    }

    fun updateTabUrl(tabId: String, url: String) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, url = url)
            _activeTab.postValue(tabManager.getTab(tabId))
        }
    }

    fun updateTabLoading(tabId: String, isLoading: Boolean) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, isLoading = isLoading)
            _activeTab.postValue(tabManager.getTab(tabId))
        }
    }

    fun updateTabCanGoBack(tabId: String, canGoBack: Boolean) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, canGoBack = canGoBack)
            _activeTab.postValue(tabManager.getTab(tabId))
        }
    }

    fun updateTabCanGoForward(tabId: String, canGoForward: Boolean) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, canGoForward = canGoForward)
            _activeTab.postValue(tabManager.getTab(tabId))
        }
    }

    fun updateTabFavicon(tabId: String, favicon: String?) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, favicon = favicon)
        }
    }

    fun getGeckoSession(tabId: String): GeckoSession? {
        return tabManager.getGeckoSession(tabId)
    }
}