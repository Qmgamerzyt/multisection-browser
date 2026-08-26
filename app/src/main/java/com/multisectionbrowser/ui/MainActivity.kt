package com.multisectionbrowser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.core.splashscreen.SplashScreenCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.gestures.rememberScrollState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multisectionbrowser.MultiSessionBrowserApp
import com.multisectionbrowser.engine.BrowserSession
import com.multisectionbrowser.engine.BrowserTab
import com.multisectionbrowser.viewmodel.BrowserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
private fun BottomBar(
    onTabsClick: () -> Unit,
    onSessionsClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        BottomButton(Icons.Filled.Tab, "Tabs", onTabsClick, Modifier.weight(1f))
        BottomButton(Icons.Filled.Apps, "Sessions", onSessionsClick, Modifier.weight(1f))
        BottomButton(Icons.Filled.MoreVert, "Menu", onMenuClick, Modifier.weight(1f))
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
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxSize()
            .padding(vertical = 2.dp)
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: BrowserViewModel by viewModels()
    private var splashScreenDismissed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = SplashScreenCompat.installSplashScreen(this)
        splashScreen.setKeepOnScreenCondition { !splashScreenDismissed }
        super.onCreate(savedInstanceState)
        setContent { App() }
    }

    @Composable
    private fun App() = MultiSectionTheme { BrowserScreen() }
}

@Composable
fun MultiSectionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Color(0xFF1A73E8),
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

@Composable
fun BrowserScreen(viewModel: BrowserViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val booted by viewModel.booted.observeAsState(initial = false)
    val sessions by viewModel.sessions.observeAsState(initial = emptyList())
    val activeSession by viewModel.activeSession.observeAsState(initial = null)
    val tabs by viewModel.tabs.observeAsState(initial = emptyList())
    val activeTab by viewModel.activeTab.observeAsState(initial = null)
    
    val app = (androidx.compose.ui.platform.LocalContext.current as android.content.Context).applicationContext as MultiSessionBrowserApp
    val initError by remember { mutableStateOf(app.getLastInitError()) }
    var retryTrigger by remember { mutableStateOf(0) }
    
    var showTabs by remember { mutableStateOf(false) }
    var showSessions by remember { mutableStateOf(false) }
    var showScript by remember { mutableStateOf(false) }

    // Handle retry trigger via LaunchedEffect
    LaunchedEffect(retryTrigger) {
        if (retryTrigger > 0) {
            app.retryInit()
        }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        if (initError != null) {
            CrashScreen(error = initError!!, onRetry = { retryTrigger++ })
            return@Surface
        }

        if (!booted) { Splash(); return@Surface }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(Modifier.fillMaxSize()) {
                AddressBar(url = activeTab?.url.orEmpty(), isLoading = activeTab?.isLoading ?: false, progress = 60, canGoBack = activeTab?.canGoBack ?: false, canGoForward = activeTab?.canGoForward ?: false, onBack = viewModel::goBack, onForward = viewModel::goForward, onReload = viewModel::reload, onSubmit = viewModel::submitUrl)
                Box(Modifier.fillMaxWidth().weight(1f).background(Color.White)) {
                    val gs = viewModel.getGeckoSession(activeTab?.id)
                    if (gs != null && gs.isOpen) { GeckoViewScreen(geckoSession = gs, onTitleChanged = viewModel::onTitleChanged, onUrlChanged = viewModel::onUrlChanged, onLoadingChanged = viewModel::onLoadingChanged, onCanGoBackChanged = viewModel::onCanGoBack, onCanGoForwardChanged = viewModel::onCanGoForward, modifier = Modifier.fillMaxSize()) }
                    else { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
                }
                BottomBar(onTabsClick = { showTabs = true }, onSessionsClick = { showSessions = true }, onMenuClick = { showScript = true })
            }
            if (showTabs) { TabSwitcherOverlay(tabs = tabs.filter { it.sessionId == activeSession?.id }, activeTabId = activeTab?.id, onSwitch = viewModel::switchTab, onClose = viewModel::closeTab, onNewTab = { viewModel.createTab(); showTabs = false }, onDismiss = { showTabs = false }) }
            if (showSessions) { SessionSwitcherOverlay(sessions = sessions, activeSessionId = activeSession?.id, onSwitch = viewModel::switchSession, onNewSession = { viewModel.createSession("Session ${sessions.size + 1}"); showSessions = false }, onRename = viewModel::renameSession, onDelete = viewModel::deleteSession, onDismiss = { showSessions = false }) }
            ScriptRunnerDialog(isOpen = showScript, onDismiss = { showScript = false }, onRunUrl = { input -> if (input.startsWith("javascript:", true)) viewModel.runJs(input) else viewModel.submitUrl(input); showScript = false }, onRunJs = { js -> viewModel.runJs(js); showScript = false })
        }
    }
}

@Composable private fun Splash() { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("MultiSection", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(16.dp)); CircularProgressIndicator() } } }

@Composable
fun CrashScreen(error: Throwable, onRetry: () -> Unit) {
    val errorText = error.stackTraceToString()
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(Modifier.fillMaxSize().background(Color(0xFF1A1A1A)).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(imageVector = Icons.Filled.Error, contentDescription = "Error", tint = Color.Red, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(text = "App Crashed", style = MaterialTheme.typography.headlineMedium, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(text = error.message ?: error.toString(), style = MaterialTheme.typography.bodyLarge, color = Color(0xFFCCCCCC), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(300.dp).background(Color(0xFF2A2A2A)).padding(16.dp)) { Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { Text(text = error.stackTraceToString(), style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace), color = Color(0xFF88CC88)) } }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; clipboard.setPrimaryClip(ClipData.newPlainText("Crash Log", error.stackTraceToString())) }, modifier = Modifier.weight(1f)) { Text("Copy Log") }
                Button(onClick = onRetry, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)), modifier = Modifier.weight(1f)) { Text("Retry", color = Color.White) }
            }
        }
    }
}
