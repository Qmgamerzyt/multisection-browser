package com.multisectionbrowser.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.multisectionbrowser.engine.BrowserSession
import com.multisectionbrowser.engine.BrowserTab
import com.multisectionbrowser.viewmodel.BrowserViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: BrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }

    @Composable
    private fun App() = MultiSectionTheme { BrowserScreen(viewModel) }
}

/* ------------------------------- theme ---------------------------------- */

@Composable
fun MultiSectionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF1A73E8),          // Chrome blue
            onPrimary = Color.White,
            secondary = Color(0xFF5F6368),
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFF8F9FA),
            surfaceVariant = Color(0xFFEEF1F5),
            secondaryContainer = Color(0xFFDCEBFB)
        ),
        content = content
    )
}

/* ------------------------------- screen --------------------------------- */

@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val booted by viewModel.booted.observeAsState(initial = false)
    val sessions by viewModel.sessions.observeAsState(initial = emptyList())
    val activeSession by viewModel.activeSession.observeAsState(initial = null)
    val tabs by viewModel.tabs.observeAsState(initial = emptyList())
    val activeTab by viewModel.activeTab.observeAsState(initial = null)

    var showTabs by remember { mutableStateOf(false) }
    var showSessions by remember { mutableStateOf(false) }
    var showScript by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()) {

        if (!booted) {
            // (A/6) splash gate — no blank white flash while DB/session restore runs
            Splash()
            return@Surface
        }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

            Column(Modifier.fillMaxSize()) {

                AddressBar(
                    url = activeTab?.url.orEmpty(),
                    isLoading = activeTab?.isLoading ?: false,
                    progress = 60, // GV gives coarse progress; bar shows activity state
                    canGoBack = activeTab?.canGoBack ?: false,
                    canGoForward = activeTab?.canGoForward ?: false,
                    onBack = viewModel::goBack,
                    onForward = viewModel::goForward,
                    onReload = viewModel::reload,
                    onSubmit = viewModel::submitUrl
                )

                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                ) {
                    val gs = viewModel.getGeckoSession(activeTab?.id)
                    if (gs != null && gs.isOpen) {
                        GeckoViewScreen(
                            geckoSession = gs,
                            onTitleChanged = viewModel::onTitleChanged,
                            onUrlChanged = viewModel::onUrlChanged,
                            onLoadingChanged = viewModel::onLoadingChanged,
                            onCanGoBackChanged = viewModel::onCanGoBack,
                            onCanGoForwardChanged = viewModel::onCanGoForward,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                BottomBar(
                    onTabsClick = { showTabs = true },
                    onSessionsClick = { showSessions = true },
                    onMenuClick = { showScript = true }
                )
            }

            // ---- overlays: two SEPARATE switcher pages (Chrome-style) ----
            if (showTabs) {
                TabSwitcherOverlay(
                    tabs = tabs.filter { it.sessionId == activeSession?.id },
                    activeTabId = activeTab?.id,
                    onSwitch = viewModel::switchTab,
                    onClose = viewModel::closeTab,
                    onNewTab = { viewModel.createTab(); showTabs = false },
                    onDismiss = { showTabs = false }
                )
            }
            if (showSessions) {
                SessionSwitcherOverlay(
                    sessions = sessions,
                    activeSessionId = activeSession?.id,
                    onSwitch = viewModel::switchSession,
                    onNewSession = {
                        viewModel.createSession("Session ${sessions.size + 1}")
                        showSessions = false
                    },
                    onRename = viewModel::renameSession,
                    onDelete = viewModel::deleteSession,
                    onDismiss = { showSessions = false }
                )
            }
            ScriptRunnerDialog(
                isOpen = showScript,
                onDismiss = { showScript = false },
                onRunUrl = { input ->
                    if (input.startsWith("javascript:", true)) viewModel.runJs(input)
                    else viewModel.submitUrl(input)
                    showScript = false
                },
                onRunJs = { js -> viewModel.runJs(js); showScript = false }
            )
        }
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("MultiSection",
                 style = MaterialTheme.typography.headlineMedium,
                 color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}

/** Minimal bottom toolbar: exactly the two labelled buttons + menu. */
@Composable
private fun BottomBar(
    onTabsClick: () -> Unit,
    onSessionsClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomButton(Icons.Filled.Apps, "Tabs", onTabsClick, Modifier.weight(1f))
        BottomButton(Icons.Filled.Person, "Sessions", onSessionsClick, Modifier.weight(1f))
        BottomButton(Icons.Filled.Build, "Menu", onMenuClick, Modifier.weight(1f))
    }
}

@Composable
private fun BottomButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.secondary,
             modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
             color = MaterialTheme.colorScheme.secondary)
    }
}
