package com.multisectionbrowser.ui.components

import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FloatingBall(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var position by remember { mutableStateOf(Offset(0f, 0f)) }
    var isDragging by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isDragging) 1.1f else 1f, spring(dampingRatio = 0.8f, stiffness = 200f))

    Box(
        modifier = modifier
            .size(56.dp)
            .offset { position }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDrag = { change, dragAmount ->
                        position = Offset(
                            position.x + dragAmount.x,
                            position.y + dragAmount.y
                        )
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onTap = { onClick() }
                )
            }
            .background(Color(0xFF6750A4), CircleShape)
            .graphicsLayer { this.scaleX = scale; this.scaleY = scale }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Menu,
            contentDescription = "Toggle header",
            tint = Color.White
        )
    }
}