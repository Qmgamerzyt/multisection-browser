package com.multisectionbrowser.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.multisectionbrowser.engine.BrowserSession
import com.multisectionbrowser.engine.BrowserTab
import com.multisectionbrowser.engine.SessionManager
import com.multisectionbrowser.engine.TabManager
import kotlinx.coroutines.launch

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

    /** False until first session+tab are restored/created — drives the splash gate. */
    private val _booted = MutableLiveData(false)
    val booted: LiveData<Boolean> = _booted

    init {
        tabManager.uiListener = object : TabManager.UiListener {
            override fun onTabChanged(tabId: String) { refreshActiveTab(tabId) }
        }
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                var active = sessionManager.getActiveSession()
                if (active == null) {
                    active = sessionManager.createSession("Session 1")
                    sessionManager.setActiveSession(active.id)
                }
                if (tabManager.getAllTabs(active.id).isEmpty()) {
                    tabManager.createTab(active.id)
                }
                publishState(active.id)
            } finally {
                _booted.postValue(true)
            }
        }
    }

    private fun refreshActiveTab(tabId: String) {
        viewModelScope.launch {
            tabManager.getTab(tabId)?.let {
                if (_activeTab.value?.id == tabId) _activeTab.postValue(it)
                val sid = it.sessionId
                _tabs.postValue(tabManager.getAllTabs(sid))
            }
        }
    }

    private suspend fun publishState(sessionId: String?) {
        _sessions.postValue(sessionManager.getAllSessions())
        if (sessionId == null) return
        _activeSession.postValue(sessionManager.getSession(sessionId))
        _tabs.postValue(tabManager.getAllTabs(sessionId))
        _activeTab.postValue(tabManager.getActiveTab(sessionId))
    }

    // ------------------------------------------------------------ sessions

    fun createSession(name: String) = viewModelScope.launch {
        val s = sessionManager.createSession(name.ifBlank { "Session" })
        sessionManager.setActiveSession(s.id)
        tabManager.createTab(s.id)
        publishState(s.id)
    }

    fun switchSession(sessionId: String) = viewModelScope.launch {
        sessionManager.setActiveSession(sessionId)
        if (tabManager.getAllTabs(sessionId).isEmpty()) tabManager.createTab(sessionId)
        publishState(sessionId)
    }

    fun deleteSession(sessionId: String) = viewModelScope.launch {
        sessionManager.deleteSession(sessionId)
        val remaining = sessionManager.getAllSessions()
        if (remaining.isEmpty()) {
            val s = sessionManager.createSession("Session 1")
            sessionManager.setActiveSession(s.id)
            tabManager.createTab(s.id)
            publishState(s.id)
        } else {
            val sid = sessionManager.getActiveSession()?.id ?: remaining.first().id
            sessionManager.setActiveSession(sid)
            publishState(sid)
        }
    }

    fun renameSession(sessionId: String, newName: String) = viewModelScope.launch {
        sessionManager.renameSession(sessionId, newName.ifBlank { "Session" })
        publishState(_activeSession.value?.id)
    }

    // ------------------------------------------------------------ tabs

    fun createTab(url: String = "") = viewModelScope.launch {
        val sid = _activeSession.value?.id ?: return@launch
        tabManager.createTab(sid, url)
        publishState(sid)
    }

    fun closeTab(tabId: String) = viewModelScope.launch {
        val sid = _activeSession.value?.id ?: return@launch
        tabManager.closeTab(tabId)
        publishState(sid)
    }

    fun switchTab(tabId: String) = viewModelScope.launch {
        tabManager.setActiveTab(tabId)
        publishState(_activeSession.value?.id)
    }

    // ------------------------------------------------------------ navigation

    fun submitUrl(raw: String) {
        val t = _activeTab.value ?: return
        viewModelScope.launch { tabManager.loadUrl(t.id, raw) }
    }

    fun goBack()    { _activeTab.value?.let { viewModelScope.launch { tabManager.goBack(it.id) } } }
    fun goForward() { _activeTab.value?.let { viewModelScope.launch { tabManager.goForward(it.id) } } }
    fun reload()    { _activeTab.value?.let { viewModelScope.launch { tabManager.reload(it.id) } } }
    fun stop()      { _activeTab.value?.let { viewModelScope.launch { tabManager.stop(it.id) } } }
    fun runJs(js: String) { _activeTab.value?.let { tabManager.evalJs(it.id, js) } }

    fun onTitleChanged(t: String)   { _activeTab.value?.let { tab -> viewModelScope.launch { tabManager.updateTabState(tab.id, title=t); _activeTab.postValue(tabManager.getTab(tab.id)) } } }
    fun onUrlChanged(u: String)     { _activeTab.value?.let { tab -> viewModelScope.launch { tabManager.updateTabState(tab.id, url=u);   _activeTab.postValue(tabManager.getTab(tab.id)) } } }
    fun onLoadingChanged(l: Boolean){ _activeTab.value?.let { tab -> viewModelScope.launch { tabManager.updateTabState(tab.id, isLoading=l); _activeTab.postValue(tabManager.getTab(tab.id)) } } }
    fun onCanGoBack(v: Boolean)     { _activeTab.value?.let { tab -> viewModelScope.launch { tabManager.updateTabState(tab.id, canGoBack=v); _activeTab.postValue(tabManager.getTab(tab.id)) } } }
    fun onCanGoForward(v: Boolean)  { _activeTab.value?.let { tab -> viewModelScope.launch { tabManager.updateTabState(tab.id, canGoForward=v); _activeTab.postValue(tabManager.getTab(tab.id)) } } }

    fun getGeckoSession(tabId: String?) = tabId?.let { tabManager.getGeckoSession(it) }
}