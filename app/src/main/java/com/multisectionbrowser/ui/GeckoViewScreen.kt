package com.multisectionbrowser.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

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
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { context ->
            GeckoView(context).apply { setBackgroundColor(0xFF101010.toInt()) }
        },
        update = { view ->
            if (geckoSession != null && view.session !== geckoSession) {
                view.setSession(geckoSession)
            }
        },
        modifier = modifier.fillMaxSize()
    )

    // Keep the GeckoSession alive/focused with the Compose lifecycle.
    DisposableEffect(lifecycleOwner, geckoSession) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME ->
                    geckoSession?.setActive(true)
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE ->
                    geckoSession?.setActive(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        geckoSession?.let { session ->
            // Delegates are plain interfaces in GeckoView 129 — override only what we need.
            session.progressDelegate = object : GeckoSession.ProgressDelegate {
                override fun onProgressChange(session: GeckoSession, progress: Int) {
                    onLoadingChanged(progress in 1..99)
                }
            }
            session.navigationDelegate = object : GeckoSession.NavigationDelegate {
                // GV129 signature: (session, url?, perms, hasUserGesture)
                override fun onLocationChange(
                    session: GeckoSession,
                    url: String?,
                    perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                    hasUserGesture: Boolean
                ) {
                    onUrlChanged(url ?: "")
                }

                override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                    onCanGoBackChanged(canGoBack)
                }

                override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                    onCanGoForwardChanged(canGoForward)
                }
            }
            // Title callbacks live in ContentDelegate in GV129.
            session.contentDelegate = object : GeckoSession.ContentDelegate {
                override fun onTitleChange(session: GeckoSession, title: String?) {
                    onTitleChanged(title ?: "")
                }
            }
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            geckoSession?.apply {
                progressDelegate = null
                navigationDelegate = null
                contentDelegate = null
            }
        }
    }
}