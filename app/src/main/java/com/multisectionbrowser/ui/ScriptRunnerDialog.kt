package com.multisectionbrowser.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScriptRunnerDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onRunUrl: (String) -> Unit,
    onRunJs: (String) -> Unit
) {
    if (!isOpen) return

    var scriptText by remember { mutableStateOf("") }
    var isUrlMode by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(340.dp)
                .clickable(enabled = false) { /* swallow clicks */ }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Run Script",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isUrlMode = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUrlMode) Color(0xFF6750A4) else Color(0xFFECE6F0),
                            contentColor = if (isUrlMode) Color.White else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("URL", fontWeight = FontWeight.Medium) }

                    Button(
                        onClick = { isUrlMode = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isUrlMode) Color(0xFF6750A4) else Color(0xFFECE6F0),
                            contentColor = if (!isUrlMode) Color.White else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) { Text("JavaScript", fontWeight = FontWeight.Medium) }
                }

                TextField(
                    value = scriptText,
                    onValueChange = { scriptText = it },
                    textStyle = TextStyle(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Black
                    ),
                    placeholder = {
                        Text(
                            text = if (isUrlMode)
                                "https://example.com or javascript:alert('hi')"
                            else "document.title",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                Button(
                    onClick = {
                        val input = scriptText.trim()
                        if (input.isNotBlank()) {
                            if (isUrlMode) onRunUrl(input) else onRunJs(input)
                            scriptText = ""
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6750A4),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("Run", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}