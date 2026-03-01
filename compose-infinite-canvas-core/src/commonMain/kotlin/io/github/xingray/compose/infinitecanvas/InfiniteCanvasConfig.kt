package io.github.xingray.compose.infinitecanvas

import androidx.compose.ui.graphics.Color

data class InfiniteCanvasConfig(
    val showGrid: Boolean = true,
    val gridSize: Float = 50f,
    val gridColor: Color = Color(0xFFE0E0E0),
    val backgroundColor: Color = Color(0xFFF5F5F5),
    val showBottomControls: Boolean = true,
)
