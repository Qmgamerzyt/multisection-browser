package com.multisectionbrowser.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Omnibox(
    url: String,
    isLoading: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onUrlSubmit: (String) -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onRefresh: () -> Unit,
    onStop: () -> Unit,
    onShowScriptDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(url) { mutableStateOf(url) }
    val isSecure = text.startsWith("https://") || text.startsWith("file://")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 2.dp)
    ) {
        IconButton(onClick = onGoBack, enabled = canGoBack) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else Color.Gray
            )
        }

        TextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = { if (text.isNotBlank()) onUrlSubmit(text.trim()) }
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            placeholder = { Text("Search or enter URL", fontSize = 13.sp) },
            leadingIcon = {
                if (isSecure) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                }
            },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reload",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        )

        IconButton(onClick = onGoForward, enabled = canGoForward) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "Forward",
                tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else Color.Gray
            )
        }

        IconButton(onClick = onShowScriptDialog) {
            Icon(
                imageVector = Icons.Filled.Build,
                contentDescription = "Run script",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onStop) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Stop",
                tint = Color.Gray
            )
        }
    }
}