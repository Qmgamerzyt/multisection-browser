package com.multisectionbrowser.engine.extensions

import android.content.Context
import android.net.Uri
import android.util.Log
import com.multisectionbrowser.data.db.ExtensionEntity
import com.multisectionbrowser.data.db.SessionExtensionSettingsEntity
import com.multisectionbrowser.data.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtensionController
import java.io.File
import java.io.FileOutputStream
import java.net.URL

class ExtensionManager(private val context: Context) {

    private val repository = BrowserRepository.getInstance(context)
    private val geckoRuntime: GeckoRuntime? = (context.applicationContext as MultiSessionBrowserApp).getGeckoRuntime()

    companion object {
        private const val TAG = "ExtensionManager"
        const val INSTALL_METHOD_AMO = 0
        const val INSTALL_METHOD_XPI = 1
    }

    // Install extension from AMO (addons.mozilla.org)
    suspend fun installFromAMO(extensionId: String, version: String = "latest"): ExtensionEntity? {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch extension metadata from AMO API
                val amoApiUrl = "https://addons.mozilla.org/api/v5/addons/$extensionId/"
                val response = URL(amoApiUrl).openStream().bufferedReader().readText()
                
                // Parse JSON response (simplified - in production use a JSON library)
                val name = extractJsonField(response, "name", "en-US") ?: extensionId
                val description = extractJsonField(response, "description", "en-US") ?: ""
                val currentVersion = extractJsonField(response, "current_version", "version") ?: version
                val author = extractJsonField(response, "authors", 0, "name") ?: "Unknown"
                
                // Get XPI download URL
                val xpiUrl = getXpiDownloadUrl(response, version)
                
                var localXpiPath: String? = null
                if (xpiUrl != null) {
                    localXpiPath = downloadXpi(xpiUrl, extensionId)
                }

                val extEntity = ExtensionEntity(
                    id = extensionId,
                    name = name,
                    description = description,
                    version = currentVersion,
                    author = author,
                    sourceUrl = amoApiUrl,
                    xpiPath = localXpiPath,
                    isInstalled = false,
                    installMethod = INSTALL_METHOD_AMO
                )
                repository.insertExtension(extEntity)
                Log.d(TAG, "Registered AMO extension: $name ($extensionId)")
                extEntity
            } catch (e: Exception) {
                Log.e(TAG, "Failed to install from AMO: $extensionId", e)
                null
            }
        }
    }

    // Install extension from local XPI file
    suspend fun installFromXPI(xpiPath: String): ExtensionEntity? {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(xpiPath)
                if (!file.exists()) {
                    Log.e(TAG, "XPI file not found: $xpiPath")
                    return@withContext null
                }

                // Extract extension ID from XPI (manifest.json)
                val extId = extractExtensionIdFromXPI(xpiPath)
                
                val extEntity = ExtensionEntity(
                    id = extId,
                    name = extId,
                    description = "Manually installed extension",
                    version = "1.0",
                    author = "Manual Install",
                    sourceUrl = "file://$xpiPath",
                    xpiPath = xpiPath,
                    isInstalled = false,
                    installMethod = INSTALL_METHOD_XPI
                )
                repository.insertExtension(extEntity)
                Log.d(TAG, "Registered manual XPI extension: $extId")
                extEntity
            } catch (e: Exception) {
                Log.e(TAG, "Failed to install from XPI: $xpiPath", e)
                null
            }
        }
    }

    private fun extractExtensionIdFromXPI(xpiPath: String): String {
        // In a real implementation, extract from manifest.json inside XPI using ZipFile
        // For now, use filename without extension
        return File(xpiPath).nameWithoutExtension
    }

    private fun getXpiDownloadUrl(jsonResponse: String, version: String): String? {
        // Parse JSON to find the XPI download URL for the specified version
        // Simplified implementation - in production use proper JSON parsing
        return null
    }

    private fun downloadXpi(xpiUrl: String, extensionId: String): String? {
        try {
            val url = URL(xpiUrl)
            val tempDir = File(context.cacheDir, "extensions")
            tempDir.mkdirs()
            val outputFile = File(tempDir, "$extensionId.xpi")
            
            url.openStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }
            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download XPI: $xpiUrl", e)
            return null
        }
    }

    private fun extractJsonField(json: String, vararg path: String): String? {
        // Simplified JSON field extraction - in production use kotlinx.serialization or Gson
        return null
    }

    // Enable/disable extension for a specific session
    suspend fun setExtensionEnabled(sessionId: String, extensionId: String, enabled: Boolean) {
        repository.setExtensionEnabled(sessionId, extensionId, enabled)
        
        if (enabled) {
            installExtensionForSession(sessionId, extensionId)
        } else {
            uninstallExtensionForSession(sessionId, extensionId)
        }
    }

    // Set trigger mode for extension in a session
    suspend fun setExtensionTriggerMode(sessionId: String, extensionId: String, mode: Int) {
        repository.setExtensionTriggerMode(sessionId, extensionId, mode)
    }

    // Install extension into a GeckoSession for a specific session
    private fun installExtensionForSession(sessionId: String, extensionId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val ext = repository.getExtension(extensionId)
            val session = repository.getSession(sessionId)
            if (ext != null && session != null && geckoRuntime != null) {
                // In a real implementation, this would use WebExtensionController
                // to install the extension into the session's profile
                Log.d(TAG, "Installing extension $extensionId for session $sessionId")
            }
        }
    }

    // Uninstall extension from a session
    private fun uninstallExtensionForSession(sessionId: String, extensionId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            Log.d(TAG, "Uninstalling extension $extensionId for session $sessionId")
        }
    }

    // Get all extensions
    suspend fun getAllExtensions(): List<ExtensionEntity> {
        return repository.getAllExtensionsSync()
    }

    // Get enabled extensions for a session
    suspend fun getEnabledExtensionsForSession(sessionId: String): List<SessionExtensionSettingsEntity> {
        return repository.getEnabledExtensionsForSession(sessionId)
    }

    // Get extensions by trigger mode for a session
    suspend fun getExtensionsForSessionByMode(sessionId: String, mode: Int): List<SessionExtensionSettingsEntity> {
        return repository.getExtensionsForSessionByMode(sessionId, mode)
    }

    // Initialize extension settings for a new session
    suspend fun initializeSessionExtensions(sessionId: String) {
        repository.initializeSessionExtensionSettings(sessionId)
    }

    // Install extension into a GeckoSession using WebExtensionController
    suspend fun installExtensionIntoSession(geckoSession: GeckoSession, extension: ExtensionEntity): Boolean {
        return withContext(Dispatchers.IO) {
            val controller = geckoSession.extensionController
            if (controller == null) {
                Log.e(TAG, "Extension controller not available")
                return@withContext false
            }

            val xpiPath = extension.xpiPath
            if (xpiPath == null || !File(xpiPath).exists()) {
                Log.e(TAG, "XPI file not found for extension: ${extension.id}")
                return@withContext false
            }

            try {
                // Install the extension
                val installResult = controller.install(
                    File(xpiPath),
                    WebExtensionController.INSTALLATION_METHOD_TEMPORARY
                )
                
                // Handle installation result
                // In a real implementation, we'd wait for the install to complete
                // and handle the prompt delegate if needed
                
                Log.d(TAG, "Extension install initiated for: ${extension.id}")
                repository.updateExtension(extension.copy(isInstalled = true))
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to install extension: ${extension.id}", e)
                false
            }
        }
    }

    // Uninstall extension from a GeckoSession
    suspend fun uninstallExtensionFromSession(geckoSession: GeckoSession, extensionId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val controller = geckoSession.extensionController
            if (controller == null) {
                return@withContext false
            }

            try {
                controller.uninstall(extensionId)
                val ext = repository.getExtension(extensionId)
                ext?.let { repository.updateExtension(it.copy(isInstalled = false)) }
                Log.d(TAG, "Extension uninstalled: $extensionId")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to uninstall extension: $extensionId", e)
                false
            }
        }
    }

    // Apply extensions to a GeckoSession based on trigger mode
    suspend fun applyExtensionsToSession(geckoSession: GeckoSession, sessionId: String) {
        withContext(Dispatchers.IO) {
            val autoExtensions = repository.getExtensionsForSessionByMode(sessionId, SessionExtensionSettingsEntity.TRIGGER_AUTO)
            
            // Auto extensions are installed automatically
            for (setting in autoExtensions) {
                val ext = repository.getExtension(setting.extensionId)
                ext?.let { installExtensionIntoSession(geckoSession, it) }
            }
        }
    }
}