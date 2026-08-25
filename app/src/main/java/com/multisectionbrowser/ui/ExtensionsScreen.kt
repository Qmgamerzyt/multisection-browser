package com.multisectionbrowser.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multisectionbrowser.data.db.ExtensionEntity
import com.multisectionbrowser.data.db.SessionExtensionSettingsEntity
import com.multisectionbrowser.engine.extensions.ExtensionManager

@Composable
fun ExtensionsScreen(
    sessionId: String,
    extensions: List<ExtensionEntity>,
    sessionSettings: List<SessionExtensionSettingsEntity>,
    extensionManager: ExtensionManager,
    onDismiss: () -> Unit
) {
    var showInstallDialog by remember { mutableStateOf(false) }
    var installInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Extensions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.Gray
                )
            }
        }

        Button(
            onClick = { showInstallDialog = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Install Extension")
        }

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
                        fontSize = 15.sp,
                        color = Color.Gray
                    )
                }
            } else {
                extensions.forEach { ext ->
                    val setting = sessionSettings.firstOrNull { it.extensionId == ext.id }
                    val isEnabled = setting?.isEnabled ?: true
                    val triggerMode = setting?.triggerMode
                        ?: SessionExtensionSettingsEntity.TRIGGER_AUTO

                    ExtensionCard(
                        extension = ext,
                        isEnabled = isEnabled,
                        triggerMode = triggerMode,
                        onEnabledChange = { enabled ->
                            extensionManager.setExtensionEnabled(sessionId, ext.id, enabled)
                        },
                        onTriggerModeChange = { mode ->
                            extensionManager.setExtensionTriggerMode(sessionId, ext.id, mode)
                        },
                        onUninstall = {
                            extensionManager.setExtensionEnabled(sessionId, ext.id, false)
                        }
                    )
                }
            }
        }
    }

    if (showInstallDialog) {
        InstallExtensionDialog(
            input = installInput,
            onInputChange = { installInput = it },
            onDismiss = { showInstallDialog = false },
            onInstall = { id ->
                extensionManager.installFromAMO(id)
                showInstallDialog = false
                installInput = ""
            }
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = extension.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "v${extension.version} by ${extension.author}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                Switch(checked = isEnabled, onCheckedChange = onEnabledChange)
            }

            if (extension.description.isNotBlank()) {
                Text(
                    text = extension.description,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    maxLines = 2
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    SessionExtensionSettingsEntity.TRIGGER_AUTO to "Auto",
                    SessionExtensionSettingsEntity.TRIGGER_OFF to "Off",
                    SessionExtensionSettingsEntity.TRIGGER_MANUAL to "Manual"
                ).forEach { (modeValue, label) ->
                    FilterChip(
                        selected = triggerMode == modeValue,
                        onClick = { onTriggerModeChange(modeValue) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = onUninstall) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Uninstall",
                        tint = Color(0xFFB3261E)
                    )
                }
            }
        }
    }
}

@Composable
fun InstallExtensionDialog(
    input: String,
    onInputChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Install from AMO",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                TextField(
                    value = input,
                    onValueChange = onInputChange,
                    singleLine = true,
                    placeholder = { Text("Extension id e.g. ublock-origin", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFECE6F0),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text("Cancel") }

                    Button(
                        onClick = { if (input.isNotBlank()) onInstall(input.trim()) },
                        enabled = input.isNotBlank()
                    ) { Text("Install") }
                }
            }
        }
    }
}