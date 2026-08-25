package com.multisectionbrowser.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleEventObserver
import com.multisectionbrowser.MultiSessionBrowserApp
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/**
 * Renders one already-OPENED GeckoSession.
 *
 * Guarantees:
 *  - the session was opened on the shared runtime by TabManager (checked here too)
 *  - setSession() is called at most once per GeckoView instance (update{} guard)
 *  - releaseSession() on dispose so switching tabs never double-attaches
 */
@Composable
fun GeckoViewScreen(
    geckoSession: GeckoSession?,
    modifier: Modifier = Modifier,
    onTitleChanged: (String) -> Unit = {},
    onUrlChanged: (String) -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {},
    onCanGoBackChanged: (Boolean) -> Unit = {},
    onCanGoForwardChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewRef = remember { mutableStateOf<GeckoView?>(null) }

    AndroidView(
        factory = { ctx ->
            GeckoView(ctx).apply {
                setBackgroundColor(0xFFFFFFFF.toInt())
                viewRef.value = this
            }
        },
        update = { view ->
            val session = geckoSession
            if (session != null && view.session !== session) {
                try {
                    if (!session.isOpen) {
                        // Safety net — should never happen (TabManager opens it),
                        // but an unopened attach is an instant crash, so guard hard.
                        MultiSessionBrowserApp.appendCrashLog(
                            context, "GeckoViewScreen",
                            "session not open — refusing setSession"
                        )
                        return@AndroidView
                    }
                    view.setSession(session)
                } catch (t: Throwable) {
                    MultiSessionBrowserApp.appendCrashLog(context, "setSession", t.stackTraceToString())
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(lifecycleOwner, geckoSession) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME ->
                    geckoSession?.takeIf { it.isOpen }?.setActive(true)
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE ->
                    geckoSession?.takeIf { it.isOpen }?.setActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try { geckoSession?.takeIf { it.isOpen }?.setActive(false) } catch (_: Exception) {}
        }
    }
}