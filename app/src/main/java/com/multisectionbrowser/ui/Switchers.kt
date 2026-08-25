package com.multisectionbrowser.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.multisectionbrowser.engine.BrowserSession
import com.multisectionbrowser.engine.BrowserTab

/* ============================== TAB SWITCHER ============================== */

/** Chrome-style 2-column grid of open tabs for the CURRENT session only. */
@Composable
fun TabSwitcherOverlay(
    tabs: List<BrowserTab>,
    activeTabId: String?,
    onSwitch: (String) -> Unit,
    onClose: (String) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit
) {
    OverlayScaffold(title = "Tabs", dismissLabel = "Done", onDismiss = onDismiss) {
        FilledTonalButton(onClick = { onNewTab(); }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp)); Text("New tab")
        }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()) {
            items(tabs, key = { it.id }) { tab ->
                val active = tab.id == activeTabId
                Card(shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(if (active) 4.dp else 1.dp),
                    colors = CardDefaults.cardColors(containerColor =
                        if (active) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .aspectRatio(0.85f)
                        .clickable {
                            onSwitch(tab.id); onDismiss()
                        }) {
                    Column(Modifier.fillMaxSize().padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Public, null,
                                 tint = MaterialTheme.colorScheme.primary,
                                 modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(tab.title.ifBlank { "New Tab" },
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f))
                            IconButton(onClick = { onClose(tab.id) },
                                       modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, "Close tab",
                                     modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(hostOf(tab.url), style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

/* ============================ SESSION SWITCHER ============================ */

/** Sessions list; long-press (or pencil) to rename, trash icon to delete. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SessionSwitcherOverlay(
    sessions: List<BrowserSession>,
    activeSessionId: String?,
    onSwitch: (String) -> Unit,
    onNewSession: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var renameTarget by remember { mutableStateOf<BrowserSession?>(null) }

    renameTarget?.let { target ->
        RenameDialog(initial = target.name, onDismiss = { renameTarget = null }) { newName ->
            onRename(target.id, newName); renameTarget = null
        }
    }

    OverlayScaffold(title = "Sessions", dismissLabel = "Done", onDismiss = onDismiss) {
        FilledTonalButton(onClick = { onNewSession() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp)); Text("New session")
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sessions, key = { it.id }) { s ->
                val active = s.id == activeSessionId
                Card(shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor =
                        if (active) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(onClick = { onSwitch(s.id); onDismiss() },
                                           onLongClick = { renameTarget = s })) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(CircleShape)
                                 .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, null, tint = Color.White,
                                 modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium,
                                 fontWeight = FontWeight.SemiBold, maxLines = 1,
                                 overflow = TextOverflow.Ellipsis)
                            if (active) Text("Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { renameTarget = s }) {
                            Icon(Icons.Filled.Edit, "Rename", Modifier.size(20.dp))
                        }
                        IconButton(onClick = { onDelete(s.id) }) {
                            Icon(Icons.Filled.Delete, "Delete",
                                 tint = MaterialTheme.colorScheme.error,
                                 modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Rename session") },
        text = { androidx.compose.material3.TextField(value = value, onValueChange = { value = it },
                   singleLine = true) },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

/* ================================ shared ================================== */

@Composable
private fun OverlayScaffold(
    title: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Box(Modifier.fillMaxSize()
             .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
             .clickable(onClick = onDismiss)) {
        Card(shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(520.dp)
                .clickable(enabled = false) {}) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleLarge,
                         fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(dismissLabel) }
                }
                content()
            }
        }
    }
}

private fun hostOf(url: String): String =
    try { java.net.URI(url).host ?: url } catch (_: Exception) { url }