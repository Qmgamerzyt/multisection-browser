package com.multisectionbrowser.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.keyboard.KeyboardOptions
import androidx.compose.ui.input.keyboard.KeyboardType
import androidx.compose.ui.input.keyboard.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardAction
import androidx.compose.ui.text.input.KeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Omnibox(
    url: String,
    isLoading: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onUrlChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onRefresh: () -> Unit,
    onStop: () -> Unit,
    onShowScriptDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(url) }

    val isUrl = text.startsWith("http://") || text.startsWith("https://") || text.startsWith("file://") || text.contains("://")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.White)
            .padding(horizontal = 12.dp)
    ) {
        // Back button
        IconButton(onClick = onGoBack, enabled = canGoBack) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                contentDescription = "Go back",
                tint = if (canGoBack) Color.Black else Color.Gray
            )
        }

        // Forward button
        IconButton(onClick = onGoForward, enabled = canGoForward) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.ArrowForward,
                contentDescription = "Go forward",
                tint = if (canGoForward) Color.Black else Color.Gray
            )
        }

        // URL field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .background(
                    Color.White,
                    RoundedCornerShape(24.dp)
                )
        ) {
            androidx.compose.material3.TextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    onUrlChange(newText)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Url,
                    imeAction = ImeAction.Go
                ),
                visualTransformation = VisualTransformation { textFieldValue ->
                    textFieldValue
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 48.dp, top = 8.dp, bottom = 8.dp),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                placeholder = { Text(text = "Search or enter URL", color = Color.Gray) },
                trailingIcon = {
                    if (isLoading) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Blue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onStop) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                leadingIcon = {
                    if (isUrl) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                            contentDescription = "Secure",
                            tint = Color.Green
                        )
                    }
                }
            )
        }

        // Menu button (three dots) - for script runner
        IconButton(onClick = onShowScriptDialog) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = Color.Black
            )
        }
    }
}