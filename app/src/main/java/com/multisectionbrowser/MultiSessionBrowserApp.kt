package com.multisectionbrowser

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.Mutex
import kotlinx.coroutines.suspendCancellableCoroutine
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.File

class MultiSessionBrowserApp : Application() {

    /** 
     * Volatile for safe publication across threads.
     * Initialized lazily on first access via getOrCreateRuntime().
     */
    @Volatile
    var geckoRuntime: GeckoRuntime? = null
        private set

    /** Tracks initialization state for splash coordination */
    @Volatile
    var runtimeReady: Boolean = false
        private set

    /** Mutex for synchronizing runtime access */
    private val runtimeMutex = Mutex()

    override fun onCreate() {
        super.onCreate()
        installCrashCapture()
        
        // Start GeckoRuntime creation ASYNCHRONOUSLY - don't block main thread!
        CoroutineScope(Dispatchers.IO).launch {
            initializeRuntimeAsync()
        }
    }

    private fun initializeRuntimeAsync() {
        try {
            val settings = GeckoRuntimeSettings.Builder()
                .javaScriptEnabled(true)
                .build()
            
            // This is the heavy call - run on IO thread
            val runtime = GeckoRuntime.create(this, settings)
            
            // Publish safely
            geckoRuntime = runtime
            runtimeReady = true
            
            Log.d("MultiSessionBrowser", "GeckoRuntime initialized successfully")
        } catch (e: Exception) {
            Log.e("MultiSessionBrowser", "GeckoRuntime init failed", e)
            // Still mark ready to avoid deadlock
            runtimeReady = true
        }
    }

    /**
     * Gets the GeckoRuntime, initializing if needed.
     * If runtime isn't ready yet, suspends until it's ready.
     */
    suspend fun getOrCreateRuntime(): GeckoRuntime {
        // Fast path - already ready
        if (geckoRuntime != null) {
            return geckoRuntime!!
        }
        
        // Slow path - wait for initialization to complete
        return runtimeMutex.withLock {
            if (geckoRuntime != null) {
                return@withLock geckoRuntime!!
            }
            
            // Wait for initialization to complete
            while (!runtimeReady) {
                kotlinx.coroutines.delay(50)
            }
            
            // Should be ready now
            geckoRuntime!!
        }
    }

    /** Checks if runtime is ready without suspending */
    fun isRuntimeReady(): Boolean = runtimeReady

    /** Gets runtime if ready, null otherwise (non-blocking) */
    fun getRuntimeIfReady(): GeckoRuntime? = if (runtimeReady) geckoRuntime else null

    /** Writes every uncaught stack trace to a readable file for phone-only debugging */
    private fun installCrashCapture() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val dir = getExternalFilesDir(null) ?: filesDir
                File(dir, "last_crash.txt").writeText(
                    "Thread: ${thread.name}\n\n" +
                        android.util.Log.getStackTraceString(throwable)
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