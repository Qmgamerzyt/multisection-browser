package com.multisectionbrowser

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class MultiSessionBrowserApp : Application() {

    var geckoRuntime: GeckoRuntime? = null
        private set

    override fun onCreate() {
        super.onCreate()
        initializeGeckoRuntime()
    }

    private fun initializeGeckoRuntime() {
        val settings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .build()
        // One shared runtime for the whole app. Per-session isolation is achieved
        // by giving every session its own profile directory + separate GeckoSessions.
        geckoRuntime = GeckoRuntime.create(this, settings)
    }
}