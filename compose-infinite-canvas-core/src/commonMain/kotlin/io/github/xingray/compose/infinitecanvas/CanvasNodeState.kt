package io.github.xingray.compose.infinitecanvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
class CanvasNodeState(
    initialX: Float = 0f,
    initialY: Float = 0f,
    fixed: Boolean = false,
    pinToFront: Boolean = false,
) {
    var x by mutableFloatStateOf(initialX)
    var y by mutableFloatStateOf(initialY)
    var fixed by mutableStateOf(fixed)
    var pinToFront by mutableStateOf(pinToFront)

    internal var measuredWidth by mutableFloatStateOf(0f)
    internal var measuredHeight by mutableFloatStateOf(0f)
}

@Composable
fun rememberCanvasNodeState(
    x: Float = 0f,
    y: Float = 0f,
    fixed: Boolean = false,
    pinToFront: Boolean = false,
): CanvasNodeState = remember { CanvasNodeState(x, y, fixed, pinToFront) }
