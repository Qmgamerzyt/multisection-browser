package com.multisectionbrowser.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.multisectionbrowser.engine.BrowserSession
import com.multisectionbrowser.engine.BrowserTab
import com.multisectionbrowser.engine.SessionManager
import com.multisectionbrowser.engine.TabManager
import com.multisectionbrowser.ui.components.FloatingBall
import com.multisectionbrowser.viewmodel.BrowserViewModel
import org.mozilla.geckoview.GeckoSession

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
    var showSessionPanel by remember { mutableStateOf(false) }
    var headerVisible by remember { mutableStateOf(true) }

    val activeSession = viewModel.activeSession
    val activeTab = viewModel.activeTab
    val geckoSession = activeTab?.let { viewModel.getGeckoSession(it.id) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        if (headerVisible) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Session Switcher
                SessionSwitcher(
                    sessions = viewModel.sessions,
                    activeSessionId = activeSession?.id,
                    onSessionClick = { id ->
                        viewModel.setActiveSession(id)
                        showSessionPanel = false
                    },
                    onNewSession = {
                        viewModel.createSession("Session ${viewModel.sessions.size + 1}")
                    }
                )

                // Tab Bar
                TabBar(
                    tabs = viewModel.tabsForActiveSession,
                    activeTabId = activeTab?.id,
                    onTabClick = { id -> viewModel.setActiveTab(id) },
                    onTabClose = { id -> viewModel.closeTab(id) },
                    onNewTab = { viewModel.createTab() }
                )

                // Omnibox
                Omnibox(
                    url = activeTab?.url ?: "",
                    isLoading = activeTab?.isLoading ?: false,
                    canGoBack = activeTab?.canGoBack ?: false,
                    canGoForward = activeTab?.canGoForward ?: false,
                    onUrlChange = { viewModel.updateUrlInput(it) },
                    onSubmit = { url ->
                        activeTab?.let { viewModel.loadUrl(it.id, url) }
                    },
                    onGoBack = { activeTab?.let { viewModel.goBack(it.id) } },
                    onGoForward = { activeTab?.let { viewModel.goForward(it.id) } },
                    onRefresh = { activeTab?.let { viewModel.reload(it.id) } },
                    onStop = { activeTab?.let { viewModel.stop(it.id) } },
                    onShowScriptDialog = { showScriptDialog = true }
                )
            }
        }

        // GeckoView
        Box(modifier = Modifier.fillMaxSize()) {
            geckoSession?.let { session ->
                GeckoViewScreen(
                    geckoSession = session,
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
                    onFaviconChanged = { favicon ->
                        activeTab?.let { viewModel.updateTabFavicon(it.id, favicon) }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Script Runner Dialog
        ScriptRunnerDialog(
            isOpen = showScriptDialog,
            onDismiss = { showScriptDialog = false },
            onRunUrl = { url ->
                activeTab?.let { viewModel.loadUrl(it.id, url) }
            },
            onRunJs = { js ->
                activeTab?.let { viewModel.executeJavaScript(it.id, js) }
            }
        )

        // Floating Ball
        FloatingBall(
            onClick = { headerVisible = !headerVisible },
            modifier = Modifier.padding(16.dp).align(Alignment.BottomEnd)
        )
    }
}

@Composable
fun MultiSectionBrowserTheme(content: @Composable () -> Unit) {
    val darkTheme = false // TODO: Add dynamic theme
    androidx.compose.material3.MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF6750A4),
            secondary = Color(0xFF625B71),
            tertiary = Color(0xFF7D5260),
            surface = Color.White,
            background = Color.White
        ),
        content = content
    )
}