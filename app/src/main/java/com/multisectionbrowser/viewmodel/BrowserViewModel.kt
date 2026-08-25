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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private fun loadInitialData() {
        viewModelScope.launch {
            refreshSessions()
            updateActiveSession()
            refreshTabs()
            updateActiveTab()
        }
    }

    private fun refreshSessions() {
        viewModelScope.launch {
            val sessions = sessionManager.getAllSessions()
            _sessions.postValue(sessions)
        }
    }

    private fun refreshTabs() {
        viewModelScope.launch {
            val session = _activeSession.value
            if (session != null) {
                val tabs = tabManager.getAllTabs(session.id)
                _tabs.postValue(tabs)
            }
        }
    }

    private fun updateActiveSession() {
        viewModelScope.launch {
            val session = sessionManager.getActiveSession()
            _activeSession.postValue(session)
        }
    }

    private fun updateActiveTab() {
        viewModelScope.launch {
            val session = _activeSession.value
            val tab = if (session != null) {
                tabManager.getActiveTab(session.id)
            } else {
                tabManager.getActiveTab()
            }
            _activeTab.postValue(tab)
        }
    }

    fun createSession(name: String) {
        viewModelScope.launch {
            val session = sessionManager.createSession(name)
            sessionManager.setActiveSession(session.id)
            refreshSessions()
            updateActiveSession()
            tabManager.createTab(session.id)
            refreshTabs()
            updateActiveTab()
        }
    }

    fun setActiveSession(sessionId: String) {
        viewModelScope.launch {
            if (sessionManager.setActiveSession(sessionId)) {
                updateActiveSession()
                refreshTabs()
                updateActiveTab()
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            if (sessionManager.deleteSession(sessionId)) {
                refreshSessions()
                updateActiveSession()
                refreshTabs()
                updateActiveTab()
            }
        }
    }

    suspend fun getSession(sessionId: String): BrowserSession? {
        return sessionManager.getSession(sessionId)
    }

    fun createTab(url: String = "") {
        viewModelScope.launch {
            val session = _activeSession.value
            session?.let { s ->
                tabManager.createTab(s.id, url)
                refreshTabs()
                updateActiveTab()
            }
        }
    }

    fun closeTab(tabId: String) {
        viewModelScope.launch {
            tabManager.closeTab(tabId)
            refreshTabs()
            updateActiveTab()
        }
    }

    fun setActiveTab(tabId: String) {
        viewModelScope.launch {
            if (tabManager.setActiveTab(tabId)) {
                updateActiveTab()
            }
        }
    }

    fun loadUrl(tabId: String, url: String) {
        viewModelScope.launch {
            tabManager.loadUrl(tabId, url)
        }
    }

    suspend fun goBack(tabId: String) {
        tabManager.goBack(tabId)
    }

    suspend fun goForward(tabId: String) {
        tabManager.goForward(tabId)
    }

    suspend fun reload(tabId: String) {
        tabManager.reload(tabId)
    }

    suspend fun stop(tabId: String) {
        tabManager.stop(tabId)
    }

    fun executeJavaScript(tabId: String, js: String) {
        val geckoSession = getGeckoSession(tabId)
        geckoSession?.evaluateJavaScript(js, null)
    }

    fun updateUrlInput(url: String) {
        _urlInput.value = url
    }

    fun updateTabTitle(tabId: String, title: String) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, title = title)
            refreshTabs()
        }
    }

    fun updateTabUrl(tabId: String, url: String) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, url = url)
            refreshTabs()
        }
    }

    fun updateTabLoading(tabId: String, isLoading: Boolean) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, isLoading = isLoading)
            refreshTabs()
        }
    }

    fun updateTabCanGoBack(tabId: String, canGoBack: Boolean) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, canGoBack = canGoBack)
            refreshTabs()
        }
    }

    fun updateTabCanGoForward(tabId: String, canGoForward: Boolean) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, canGoForward = canGoForward)
            refreshTabs()
        }
    }

    fun updateTabFavicon(tabId: String, favicon: String?) {
        viewModelScope.launch {
            tabManager.updateTabState(tabId, favicon = favicon)
            refreshTabs()
        }
    }

    fun getGeckoSession(tabId: String): GeckoSession? {
        return tabManager.getGeckoSession(tabId)
    }

    val tabsForActiveSession: List<BrowserTab>
        get() = _tabs.value ?: emptyList()
}