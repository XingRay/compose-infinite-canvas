package io.github.xingray.compose.infinitecanvas.element

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.xingray.compose.infinitecanvas.AnchorPosition
import io.github.xingray.compose.infinitecanvas.CanvasNode
import io.github.xingray.compose.infinitecanvas.ViewportState

@Composable
internal fun NodeChrome(
    node: CanvasNode,
    viewport: ViewportState,
    isSelected: Boolean,
) {
    val density = LocalDensity.current
    val screenPos = viewport.worldToScreen(
        Offset(node.state.x, node.state.y)
    )

    val xDp = with(density) { screenPos.x.toDp() }
    val yDp = with(density) { screenPos.y.toDp() }

    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .graphicsLayer {
                scaleX = viewport.scale
                scaleY = viewport.scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
    ) {
        val shape = RoundedCornerShape(12.dp)
        val borderColor = if (isSelected) Color(0xFF2196F3) else Color.Transparent
        val borderWidth = if (isSelected) 2.dp else 0.dp

        Box(
            modifier = node.modifier
                .shadow(4.dp, shape)
                .clip(shape)
                .background(Color.White)
                .border(borderWidth, borderColor, shape)
                .onSizeChanged { size ->
                    node.state.measuredWidth = size.width.toFloat()
                    node.state.measuredHeight = size.height.toFloat()
                }
        ) {
            node.content()
        }

        if (isSelected) {
            AnchorDot(Modifier.align(Alignment.TopCenter), AnchorPosition.Top)
            AnchorDot(Modifier.align(Alignment.BottomCenter), AnchorPosition.Bottom)
            AnchorDot(Modifier.align(Alignment.CenterStart), AnchorPosition.Left)
            AnchorDot(Modifier.align(Alignment.CenterEnd), AnchorPosition.Right)
        }
    }
}

@Composable
internal fun AnchorDot(modifier: Modifier, anchor: AnchorPosition) {
    Box(
        modifier = modifier
            .size(14.dp)
            .offset(
                x = when (anchor) {
                    AnchorPosition.Left -> (-7).dp
                    AnchorPosition.Right -> 7.dp
                    else -> 0.dp
                },
                y = when (anchor) {
                    AnchorPosition.Top -> (-7).dp
                    AnchorPosition.Bottom -> 7.dp
                    else -> 0.dp
                },
            )
            .clip(CircleShape)
            .background(Color(0xFF2196F3))
            .border(2.dp, Color.White, CircleShape)
    )
}
