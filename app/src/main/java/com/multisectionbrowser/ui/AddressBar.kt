package com.multisectionbrowser.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** Chrome-style rounded address bar with a thin page-load progress line under it. */
@Composable
fun AddressBar(
    url: String,
    isLoading: Boolean,
    progress: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var text by remember(url) { mutableStateOf(url) }
    val secure = text.startsWith("https://") || text.startsWith("file://")

    Column(Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBack, enabled = canGoBack) {
                Icon(Icons.Filled.ArrowBack, "Back",
                     tint = MaterialTheme.colorScheme.onSurface.copy(alpha =
                         if (canGoBack) 1f else 0.35f))
            }

            TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = { if (text.isNotBlank()) onSubmit(text.trim()) }),
                leadingIcon = {
                    if (secure) Icon(
                        Icons.Filled.Lock, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp))
                },
                placeholder = { Text("Search or type URL",
                    style = MaterialTheme.typography.bodyMedium) },
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = if (isLoading) onReload else onReload) {
                Icon(Icons.Filled.Refresh, "Reload")
            }
            IconButton(onClick = onForward, enabled = canGoForward) {
                Icon(Icons.Filled.KeyboardArrowRight, "Forward",
                     tint = MaterialTheme.colorScheme.onSurface.copy(alpha =
                         if (canGoForward) 1f else 0.35f))
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                progress = (progress.coerceIn(0, 100) / 100f),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
        }
    }
}
