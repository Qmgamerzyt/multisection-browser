package com.multisectionbrowser.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multisectionbrowser.data.db.ExtensionEntity
import com.multisectionbrowser.data.db.SessionExtensionSettingsEntity
import com.multisectionbrowser.engine.extensions.ExtensionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsScreen(
    sessionId: String,
    extensionManager: ExtensionManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var extensions by remember { mutableStateOf<List<ExtensionEntity>>(emptyList()) }
    var sessionSettings by remember { mutableStateOf<List<SessionExtensionSettingsEntity>>(emptyList()) }
    var showInstallDialog by remember { mutableStateOf(false) }
    var installUrl by remember { mutableStateOf("") }
    var installMethod by remember { mutableStateOf(0) } // 0 = AMO, 1 = XPI

    // Load extensions
    androidx.compose.runtime.LaunchedEffect(Unit) {
        loadExtensions()
    }

    fun loadExtensions() {
        // In a real implementation, this would use the repository
        // For now, we'll use a placeholder
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Extensions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray
                )
            }
        }

        // Install button
        Button(
            onClick = { showInstallDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                contentDescription = "Add extension"
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Install Extension")
        }

        // Extensions list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (extensions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No extensions installed",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                extensions.forEach { ext ->
                    val setting = sessionSettings.firstOrNull { it.extensionId == ext.id }
                    val isEnabled = if (setting != null) setting.isEnabled else true
                    val triggerMode = if (setting != null) setting.triggerMode else SessionExtensionSettingsEntity.TRIGGER_AUTO

                    ExtensionCard(
                        extension = ext,
                        isEnabled = isEnabled,
                        triggerMode = triggerMode,
                        onEnabledChange = { enabled ->
                            extensionManager.setExtensionEnabled(sessionId, ext.id, enabled)
                            loadExtensions()
                        },
                        onTriggerModeChange = { mode ->
                            extensionManager.setExtensionTriggerMode(sessionId, ext.id, mode)
                            loadExtensions()
                        },
                        onUninstall = {
                            extensionManager.uninstallExtensionFromSession(
                                // Need access to GeckoSession here
                            )
                            extensionManager.setExtensionEnabled(sessionId, ext.id, false)
                            loadExtensions()
                        }
                    )
                }
            }
        }
    }

    // Install dialog
    if (showInstallDialog) {
        InstallExtensionDialog(
            onDismiss = { showInstallDialog = false },
            onInstall = { url, method ->
                if (method == 0) {
                    // AMO
                    extensionManager.installFromAMO(url)
                } else {
                    // XPI file - would need file picker
                }
                showInstallDialog = false
                loadExtensions()
            }
        )
    }
}

@Composable
fun ExtensionCard(
    extension: ExtensionEntity,
    isEnabled: Boolean,
    triggerMode: Int,
    onEnabledChange: (Boolean) -> Unit,
    onTriggerModeChange: (Int) -> Unit,
    onUninstall: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = extension.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "v${extension.version} by ${extension.author}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                androidx.compose.material3.Switch(
                    checked = isEnabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }

            Text(
                text = extension.description,
                fontSize = 14.sp,
                color = Color.DarkGray
            )

            // Trigger mode selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Trigger:", fontSize = 14.sp, color = Color.DarkGray)
                
                val modes = listOf(
                    SessionExtensionSettingsEntity.TRIGGER_AUTO to "Auto",
                    SessionExtensionSettingsEntity.TRIGGER_OFF to "Off",
                    SessionExtensionSettingsEntity.TRIGGER_MANUAL to "Manual"
                )
                
                modes.forEach { (modeValue, modeLabel) ->
                    val isSelected = triggerMode == modeValue
                    androidx.compose.material3.FilterChip(
                        selected = isSelected,
                        onClick = { onTriggerModeChange(modeValue) },
                        label = { Text(text = modeLabel, fontSize = 12.sp) },
                        modifier = Modifier.height(32.dp)
                    )
                }
                
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = onUninstall) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                        contentDescription = "Uninstall",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun InstallExtensionDialog(
    onDismiss: () -> Unit,
    onInstall: (String, Int) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var method by remember { mutableStateOf(0) } // 0 = AMO ID, 1 = XPI file

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(300.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Install Extension",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                // Method selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { method = 0 },
                        modifier = Modifier.weight(1f),
                        colors = if (method == 0) androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4),
                            contentColor = Color.White
                        ) else androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF5F5F5),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(text = "AMO (ID)", fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = { method = 1 },
                        modifier = Modifier.weight(1f),
                        colors = if (method == 1) androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4),
                            contentColor = Color.White
                        ) else androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF5F5F5),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(text = "XPI File", fontWeight = FontWeight.Medium)
                    }
                }

                // Input field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(8.dp))
                ) {
                    androidx.compose.material3.TextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.input.keyboard.KeyboardType.Text
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = Color.Black
                        ),
                        placeholder = {
                            Text(
                                text = if (method == 0) "Extension ID (e.g., ublock-origin)" else "XPI file path",
                                color = Color.Gray
                            )
                        }
                    )
                }

                // Install button
                Button(
                    onClick = {
                        if (url.isNotBlank()) {
                            onInstall(url, method)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6750A4),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Install", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}