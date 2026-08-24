package com.multisectionbrowser

import android.app.Application
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

class MultiSessionBrowserApp : Application() {

    private var geckoRuntime: GeckoRuntime? = null

    override fun onCreate() {
        super.onCreate()
        initializeGeckoRuntime()
    }

    private fun initializeGeckoRuntime() {
        val settings = GeckoRuntimeSettings.Builder(this)
            .useWebRender(true)
            .build()

        geckoRuntime = GeckoRuntime.create(this, settings)
    }

    fun getGeckoRuntime(): GeckoRuntime? {
        return geckoRuntime
    }
}