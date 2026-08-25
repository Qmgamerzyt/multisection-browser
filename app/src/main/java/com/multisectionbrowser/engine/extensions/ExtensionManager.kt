package com.multisectionbrowser.engine.extensions

import android.content.Context
import android.net.Uri
import android.util.Log
import com.multisectionbrowser.data.db.ExtensionEntity
import com.multisectionbrowser.data.db.SessionExtensionSettingsEntity
import com.multisectionbrowser.data.repository.BrowserRepository
import com.multisectionbrowser.MultiSessionBrowserApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class ExtensionManager(private val context: Context) {

    private val repository = BrowserRepository.getInstance(context)
    private val geckoRuntime get() = (context.applicationContext as MultiSessionBrowserApp).geckoRuntime

    companion object {
        private const val TAG = "ExtensionManager"
        const val INSTALL_METHOD_AMO = 0
        const val INSTALL_METHOD_XPI = 1
    }

    /** Register an extension sourced from AMO and download its .xpi when possible. */
    suspend fun installFromAMO(extensionId: String): ExtensionEntity? {
        return withContext(Dispatchers.IO) {
            try {
                val amoUrl = "https://addons.mozilla.org/firefox/addon/$extensionId/"
                var xpiPath: String? = findExistingXpi(extensionId)

                val entity = ExtensionEntity(
                    id = extensionId,
                    name = extensionId,
                    description = "Extension from AMO",
                    version = "latest",
                    author = "Unknown",
                    sourceUrl = amoUrl,
                    xpiPath = xpiPath,
                    isInstalled = false,
                    installMethod = INSTALL_METHOD_AMO
                )
                repository.insertExtension(entity)
                Log.d(TAG, "Registered AMO extension: $extensionId")
                entity
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register AMO extension: $extensionId", e)
                null
            }
        }
    }

    /** Register a manually sideloaded .xpi file. */
    suspend fun installFromXPI(xpiPath: String): ExtensionEntity? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(xpiPath)
                if (!file.exists()) {
                    Log.e(TAG, "XPI file not found: $xpiPath")
                    return@withContext null
                }
                val extId = file.nameWithoutExtension
                val entity = ExtensionEntity(
                    id = extId,
                    name = extId,
                    description = "Manually installed extension",
                    version = "1.0",
                    author = "Manual Install",
                    sourceUrl = Uri.fromFile(file).toString(),
                    xpiPath = xpiPath,
                    isInstalled = false,
                    installMethod = INSTALL_METHOD_XPI
                )
                repository.insertExtension(entity)
                Log.d(TAG, "Registered manual XPI extension: $extId")
                entity
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register XPI: $xpiPath", e)
                null
            }
        }
    }

    private fun findExistingXpi(extensionId: String): String? {
        val dir = File(context.cacheDir, "extensions")
        val f = File(dir, "$extensionId.xpi")
        return if (f.exists()) f.absolutePath else null
    }

    private fun downloadXpi(urlStr: String, extensionId: String): String? {
        return try {
            val dir = File(context.cacheDir, "extensions")
            dir.mkdirs()
            val out = File(dir, "$extensionId.xpi")
            URL(urlStr).openStream().use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
            out.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "XPI download failed: $urlStr", e)
            null
        }
    }

    suspend fun setExtensionEnabled(sessionId: String, extensionId: String, enabled: Boolean) {
        repository.setExtensionEnabled(sessionId, extensionId, enabled)
    }

    suspend fun setExtensionTriggerMode(sessionId: String, extensionId: String, mode: Int) {
        repository.setExtensionTriggerMode(sessionId, extensionId, mode)
    }

    suspend fun getAllExtensions(): List<ExtensionEntity> =
        repository.getAllExtensionsSync()

    suspend fun getEnabledExtensionsForSession(sessionId: String): List<SessionExtensionSettingsEntity> =
        repository.getEnabledExtensionsForSession(sessionId)

    suspend fun getExtensionsForSessionByMode(
        sessionId: String,
        mode: Int
    ): List<SessionExtensionSettingsEntity> =
        repository.getExtensionsForSessionByMode(sessionId, mode)

    /**
     * Install into the shared runtime via WebExtensionController.
     * Requires a locally available .xpi (downloaded or sideloaded).
     */
    suspend fun installExtensionIntoSession(extension: ExtensionEntity): Boolean {
        return withContext(Dispatchers.IO) {
            val runtime = geckoRuntime ?: run {
                Log.e(TAG, "Runtime not ready")
                return@withContext false
            }
            val path = extension.xpiPath
            if (path == null || !File(path).exists()) {
                Log.w(TAG, "No local XPI for ${extension.id}; skipping install")
                return@withContext false
            }
            try {
                // GeckoView 129 API: controller.install(Uri)
                runtime.webExtensionController.install(Uri.fromFile(File(path)))
                repository.updateExtension(extension.copy(isInstalled = true))
                Log.d(TAG, "Installed extension ${extension.id}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Install failed for ${extension.id}", e)
                false
            }
        }
    }

    suspend fun uninstallExtensionFromSession(extensionId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val runtime = geckoRuntime ?: return@withContext false
            try {
                runtime.webExtensionController.uninstall(extensionId)
                val ext = repository.getExtension(extensionId)
                ext?.let { repository.updateExtension(it.copy(isInstalled = false)) }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Uninstall failed for $extensionId", e)
                false
            }
        }
    }
}