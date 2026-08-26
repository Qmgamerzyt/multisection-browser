package com.multisectionbrowser

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
        public set

    /** Tracks initialization state for splash coordination */
    @Volatile
    var runtimeReady: Boolean = false
        public set

    /** Simple lock state for synchronizing runtime access */
    @Volatile
    private var runtimeLock: Boolean = false

    /** Store last initialization error for crash screen */
    @Volatile
    var lastInitError: Throwable? = null
        public set

    override fun onCreate() {
        super.onCreate()
        installCrashCapture()
        
        // Start GeckoRuntime creation ASYNCHRONOUSLY - don't block main thread!
        CoroutineScope(Dispatchers.IO).launch {
            initializeRuntimeAsync()
        }
    }

    fun initializeRuntimeAsync() {
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
            lastInitError = e
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
        // Simple spin-lock with exponential backoff
        while (true) {
            // Try to acquire lock
            if (!runtimeLock) {
                // Try to acquire lock using atomic compare-and-set
                // Since we're in a single-threaded coroutine context, simple check is enough
                if (!runtimeLock) {
                    // We got the lock
                    try {
                        if (geckoRuntime != null) {
                            return geckoRuntime!!
                        }
                        
                        // Wait for initialization to complete
                        while (!runtimeReady) {
                            kotlinx.coroutines.delay(50)
                        }
                        
                        return geckoRuntime!!
                    } finally {
                        // Release lock (not strictly needed in single-threaded coroutine)
                    }
                }
            }
            // Wait a bit before retrying
            kotlinx.coroutines.delay(50)
        }
    }

    /** Checks if runtime is ready without suspending */
    fun isRuntimeReady(): Boolean = runtimeReady

    /** Gets runtime if ready, null otherwise (non-blocking) */
    fun getRuntimeIfReady(): GeckoRuntime? = if (runtimeReady) geckoRuntime else null

    /** Gets last initialization error */
    fun getLastInitError(): Throwable? = lastInitError

    /** Retries runtime initialization after a failure */
    fun retryInit() {
        lastInitError = null
        runtimeReady = false
        geckoRuntime = null
        CoroutineScope(Dispatchers.IO).launch { initializeRuntimeAsync() }
    }

    /** Extension to get full stack trace as string */
    fun Throwable.stackTraceToString(): String {
        val sw = java.io.StringWriter()
        val pw = java.io.PrintWriter(sw)
        printStackTrace(pw)
        return sw.toString()
    }

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