package com.multisectionbrowser

import android.app.Application
import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.File

class MultiSessionBrowserApp : Application() {

    var geckoRuntime: GeckoRuntime? = null
        private set

    override fun onCreate() {
        super.onCreate()
        installCrashCapture()

        val settings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(true)
            .build()
        // ONE runtime for the whole app — never per tab/session.
        geckoRuntime = GeckoRuntime.create(this, settings)
    }

    /** Writes every uncaught stack trace to a readable file so phone-only
     *  debugging is possible: Android/data/com.multisectionbrowser/files/last_crash.txt */
    private fun installCrashCapture() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = getExternalFilesDir(null) ?: filesDir
                File(dir, "last_crash.txt").writeText(
                    "Thread: ${thread.name}\n\n" +
                        Log.getStackTraceString(throwable)
                )
            } catch (_: Exception) { }
            previous?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun appendCrashLog(context: android.content.Context, tag: String, message: String) {
            try {
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                java.io.File(dir, "last_crash.txt")
                    .appendText("\n--- $tag ---\n$message\n")
            } catch (_: Exception) { }
        }
    }
}