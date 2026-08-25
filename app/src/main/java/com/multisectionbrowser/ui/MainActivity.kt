package com.multisectionbrowser.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.livedata.observeAsState
import com.multisectionbrowser.engine.BrowserSession
import com.multisectionbrowser.engine.BrowserTab
import com.multisectionbrowser.ui.components.FloatingBall
import com.multisectionbrowser.viewmodel.BrowserViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MultiSectionBrowserTheme {
                BrowserScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    var showScriptDialog by remember { mutableStateOf(false) }
    var headerVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val sessions by viewModel.sessions.observeAsState(initialValue = emptyList())
    val activeSession by viewModel.activeSession.observeAsState(initialValue = null)
    val tabs by viewModel.tabs.observeAsState(initialValue = emptyList())
    val activeTab by viewModel.activeTab.observeAsState(initialValue = null)

    // GeckoSession is created eagerly in TabManager; look it up for the active tab.
    val geckoSession = activeTab?.let { viewModel.getGeckoSession(it.id) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            if (headerVisible) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SessionSwitcher(
                        sessions = sessions,
                        activeSessionId = activeSession?.id,
                        onSessionClick = { id -> viewModel.setActiveSession(id) },
                        onNewSession = {
                            viewModel.createSession("Session ${sessions.size + 1}")
                        }
                    )

                    TabBar(
                        tabs = tabs,
                        activeTabId = activeTab?.id,
                        onTabClick = { id -> viewModel.setActiveTab(id) },
                        onTabClose = { id -> viewModel.closeTab(id) },
                        onNewTab = { viewModel.createTab() }
                    )

                    Omnibox(
                        url = activeTab?.url ?: "",
                        isLoading = activeTab?.isLoading ?: false,
                        canGoBack = activeTab?.canGoBack ?: false,
                        canGoForward = activeTab?.canGoForward ?: false,
                        onUrlSubmit = { url ->
                            activeTab?.let { viewModel.loadUrl(it.id, url) }
                        },
                        onGoBack = {
                            activeTab?.let { t -> scope.launch { viewModel.goBack(t.id) } }
                        },
                        onGoForward = {
                            activeTab?.let { t -> scope.launch { viewModel.goForward(t.id) } }
                        },
                        onRefresh = {
                            activeTab?.let { t -> scope.launch { viewModel.reload(t.id) } }
                        },
                        onStop = {
                            activeTab?.let { t -> scope.launch { viewModel.stop(t.id) } }
                        },
                        onShowScriptDialog = { showScriptDialog = true }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (geckoSession != null) {
                    GeckoViewScreen(
                        geckoSession = geckoSession,
                        onTitleChanged = { title ->
                            activeTab?.let { viewModel.updateTabTitle(it.id, title) }
                        },
                        onUrlChanged = { url ->
                            activeTab?.let { viewModel.updateTabUrl(it.id, url) }
                        },
                        onLoadingChanged = { loading ->
                            activeTab?.let { viewModel.updateTabLoading(it.id, loading) }
                        },
                        onCanGoBackChanged = { canGoBack ->
                            activeTab?.let { viewModel.updateTabCanGoBack(it.id, canGoBack) }
                        },
                        onCanGoForwardChanged = { canGoForward ->
                            activeTab?.let { viewModel.updateTabCanGoForward(it.id, canGoForward) }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Draggable floating ball — toggles header visibility.
        FloatingBall(
            onClick = { headerVisible = !headerVisible },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    ScriptRunnerDialog(
        isOpen = showScriptDialog,
        onDismiss = { showScriptDialog = false },
        onRunUrl = { input ->
            if (input.startsWith("javascript:", ignoreCase = true)) {
                activeTab?.let { viewModel.executeJavaScript(it.id, input) }
            } else {
                activeTab?.let { viewModel.loadUrl(it.id, input) }
            }
        },
        onRunJs = { js ->
            activeTab?.let { viewModel.executeJavaScript(it.id, js) }
        }
    )
}

@Composable
fun MultiSectionBrowserTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF7D5260),
            surface = Color.White,
            background = Color.White
        ),
        content = content
    )
}