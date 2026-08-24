package com.multisectionbrowser.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewFactory
import androidx.compose.ui.platform.androidView
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
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
    onCanGoForwardChanged: (Boolean) -> Unit = {},
    onFaviconChanged: (String?) -> Unit = {}
) {
    var geckoView by remember { mutableStateOf<GeckoView?>(null) }
    var lifecycleObserver by remember { mutableStateOf<LifecycleEventObserver?>(null) }

    androidView(
        factory = ViewFactory { context ->
            val view = GeckoView(context)
            view.setBackgroundColor(0xFFFFFFFF.toInt())
            geckoView = view
            view
        },
        update = { view ->
            if (geckoSession != null && view.geckoSession !== geckoSession) {
                view.geckoSession = geckoSession
            }
        },
        modifier = modifier.fillMaxSize()
    )

    // Observe GeckoSession events
    androidx.compose.runtime.DisposableEffect(geckoSession) {
        if (geckoSession != null) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    androidx.lifecycle.Lifecycle.Event.ON_START -> {
                        geckoSession.resume()
                    }
                    androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                        geckoSession.pause()
                    }
                    else -> {}
                }
            }
            lifecycleObserver = observer
            (geckoView?.context as? LifecycleOwner)?.lifecycle?.addObserver(observer)
        }
        onDispose {
            lifecycleObserver?.let {
                (geckoView?.context as? LifecycleOwner)?.lifecycle?.removeObserver(it)
            }
        }
    }

    // Set up delegates
    androidx.compose.runtime.DisposableEffect(geckoSession) {
        geckoSession?.apply {
            progressDelegate = object : GeckoSession.ProgressDelegate() {
                override fun onProgressChange(session: GeckoSession, progress: Int) {
                    onLoadingChanged(progress < 100)
                }
            }

            navigationDelegate = object : GeckoSession.NavigationDelegate() {
                override fun onLocationChange(session: GeckoSession, uri: String) {
                    onUrlChanged(uri)
                }

                override fun onTitleChange(session: GeckoSession, title: String) {
                    onTitleChanged(title)
                }

                override fun onCanGoBackChange(session: GeckoSession, canGoBack: Boolean) {
                    onCanGoBackChanged(canGoBack)
                }

                override fun onCanGoForwardChange(session: GeckoSession, canGoForward: Boolean) {
                    onCanGoForwardChanged(canGoForward)
                }
            }

            contentDelegate = object : GeckoSession.ContentDelegate() {
                override fun onFavicon(session: GeckoSession, icon: Bitmap?) {
                    // Favicon handling could be added here
                }
            }
        }
        onDispose {
            geckoSession?.apply {
                progressDelegate = null
                navigationDelegate = null
                contentDelegate = null
            }
        }
    }
}