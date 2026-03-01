package io.github.xingray.compose.infinitecanvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.xingray.compose.infinitecanvas.connection.drawBezierConnection
import io.github.xingray.compose.infinitecanvas.element.NodeChrome
import io.github.xingray.compose.infinitecanvas.gesture.canvasGestures

@Composable
fun InfiniteCanvas(
    modifier: Modifier = Modifier,
    state: InfiniteCanvasState = rememberInfiniteCanvasState(),
    config: InfiniteCanvasConfig = InfiniteCanvasConfig(),
    nodes: List<CanvasNode>,
    menu: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
) {
    SideEffect {
        state.currentNodes = nodes
    }

    val viewport = state.viewport
    val isPanMode = state.effectiveMode == CanvasMode.Pan
    val cursorIcon = if (isPanMode) PointerIcon.Hand else PointerIcon.Default

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(config.backgroundColor)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Spacebar) {
                        when (event.type) {
                            KeyEventType.KeyDown -> {
                                state.onSpaceDown()
                                true
                            }
                            KeyEventType.KeyUp -> {
                                state.onSpaceUp()
                                true
                            }
                            else -> false
                        }
                    } else false
                }
                .pointerHoverIcon(cursorIcon)
                .canvasGestures(state)
                .onSizeChanged { state.canvasSize = it }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (config.showGrid) {
                    drawGrid(viewport, config)
                }
                drawConnections(state, viewport)
                drawDraggingConnection(state, viewport)
                drawSelectionRect(state)
            }

            // Render nodes: non-pinned first, then pinned (for z-ordering)
            val normalNodes = nodes.filter { !it.state.pinToFront }
            val pinnedNodes = nodes.filter { it.state.pinToFront }
            (normalNodes + pinnedNodes).forEach { node ->
                NodeChrome(
                    node = node,
                    viewport = viewport,
                    isSelected = state.isSelected(node.id),
                )
            }
        }

        if (config.showBottomControls) {
            BottomControls(
                state = state,
                onInteraction = { focusRequester.requestFocus() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp)
                    .zIndex(10f),
            )
        }

        // Context menu
        val contextMenu = state.contextMenuState
        if (contextMenu != null) {
            val dismissMenu: () -> Unit = {
                state.contextMenuState = null
                focusRequester.requestFocus()
            }

            // Backdrop for dismiss
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(20f)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    if (event.buttons.isSecondaryPressed) {
                                        val pos = event.changes.firstOrNull()?.position
                                        if (pos != null) {
                                            handleRightClick(state, pos)
                                        }
                                    } else {
                                        dismissMenu()
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
            )

            val targetNodeId = contextMenu.targetNodeId
            if (targetNodeId != null) {
                val targetNode = nodes.find { it.id == targetNodeId }
                val nodeMenu = targetNode?.menu
                if (nodeMenu != null) {
                    Box(
                        modifier = Modifier
                            .zIndex(21f)
                            .menuPosition(contextMenu.screenPosition, state.canvasSize)
                    ) {
                        nodeMenu(dismissMenu)
                    }
                }
            } else {
                if (menu != null) {
                    Box(
                        modifier = Modifier
                            .zIndex(21f)
                            .menuPosition(contextMenu.screenPosition, state.canvasSize)
                    ) {
                        menu(dismissMenu)
                    }
                }
            }
        }
    }
}

private fun handleRightClick(state: InfiniteCanvasState, pos: Offset) {
    if (state.effectiveMode == CanvasMode.Select) {
        val hit = state.hitTest(pos)
        if (hit != null) {
            if (!state.isSelected(hit.id)) {
                state.selectNode(hit.id)
            }
            state.contextMenuState = ContextMenuState(
                screenPosition = pos,
                targetNodeId = hit.id,
            )
        } else {
            state.contextMenuState = ContextMenuState(screenPosition = pos)
        }
    }
}

// --- Bottom Controls ---

@Composable
private fun BottomControls(
    state: InfiniteCanvasState,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ModeSwitch(
            currentMode = state.canvasMode,
            onModeChange = {
                state.switchMode(it)
                onInteraction()
            },
        )
        ZoomControls(
            scalePercent = state.viewport.scalePercent,
            onZoomIn = {
                val center = Offset(state.canvasSize.width / 2f, state.canvasSize.height / 2f)
                state.viewport.zoomBy(1.25f, center)
                onInteraction()
            },
            onZoomOut = {
                val center = Offset(state.canvasSize.width / 2f, state.canvasSize.height / 2f)
                state.viewport.zoomBy(0.8f, center)
                onInteraction()
            },
            onResetZoom = {
                val center = Offset(state.canvasSize.width / 2f, state.canvasSize.height / 2f)
                state.viewport.zoomTo(1f, center)
                onInteraction()
            },
        )
    }
}

@Composable
private fun ModeSwitch(
    currentMode: CanvasMode,
    onModeChange: (CanvasMode) -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .shadow(4.dp, shape)
            .clip(shape)
            .background(Color.White)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ModeButton(
            icon = ArrowIcon,
            label = "V",
            isActive = currentMode == CanvasMode.Select,
            onClick = { onModeChange(CanvasMode.Select) },
        )
        ModeButton(
            icon = HandIcon,
            label = "H",
            isActive = currentMode == CanvasMode.Pan,
            onClick = { onModeChange(CanvasMode.Pan) },
        )
    }
}

@Composable
private fun ModeButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isActive) Color(0xFF2196F3) else Color.Transparent
    val contentColor = if (isActive) Color.White else Color(0xFF666666)
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ZoomControls(
    scalePercent: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .shadow(4.dp, shape)
            .clip(shape)
            .background(Color.White)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        ZoomButton(icon = MinusIcon, contentDescription = "Zoom out", onClick = onZoomOut)
        Box(
            modifier = Modifier
                .width(56.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onResetZoom)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${scalePercent}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center,
            )
        }
        ZoomButton(icon = PlusIcon, contentDescription = "Zoom in", onClick = onZoomIn)
    }
}

@Composable
private fun ZoomButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF666666),
            modifier = Modifier.size(16.dp),
        )
    }
}

// --- Vector Icons ---

private val ArrowIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Arrow", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(7f, 2f); lineTo(7f, 20f); lineTo(11.5f, 15.5f)
            lineTo(16f, 22f); lineTo(18f, 21f); lineTo(13.5f, 14.5f)
            lineTo(19f, 14f); close()
        }
    }.build()
}

private val HandIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Hand", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(12.8f, 2f, 13.5f, 2.7f, 13.5f, 3.5f)
            lineTo(13.5f, 10f); lineTo(15f, 10f); lineTo(15f, 5.5f)
            curveTo(15f, 4.7f, 15.7f, 4f, 16.5f, 4f)
            curveTo(17.3f, 4f, 18f, 4.7f, 18f, 5.5f)
            lineTo(18f, 10f); lineTo(19.5f, 10.5f)
            curveTo(19.5f, 9.7f, 20.2f, 9f, 21f, 9f)
            curveTo(21.8f, 9f, 22f, 9.7f, 22f, 10.5f)
            lineTo(22f, 16f)
            curveTo(22f, 19.3f, 19.3f, 22f, 16f, 22f)
            lineTo(13f, 22f)
            curveTo(10.8f, 22f, 8.8f, 20.9f, 7.6f, 19.2f)
            lineTo(4f, 13.5f)
            curveTo(3.5f, 12.8f, 3.7f, 11.8f, 4.4f, 11.3f)
            curveTo(5.1f, 10.8f, 6.1f, 11f, 6.6f, 11.7f)
            lineTo(8.5f, 14.5f); lineTo(8.5f, 14f); lineTo(10.5f, 14f)
            lineTo(10.5f, 3.5f)
            curveTo(10.5f, 2.7f, 11.2f, 2f, 12f, 2f)
            close()
        }
    }.build()
}

private val MinusIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Minus", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(5f, 11f); lineTo(19f, 11f); lineTo(19f, 13f); lineTo(5f, 13f); close()
        }
    }.build()
}

private val PlusIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Plus", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(11f, 5f); lineTo(13f, 5f); lineTo(13f, 11f)
            lineTo(19f, 11f); lineTo(19f, 13f); lineTo(13f, 13f)
            lineTo(13f, 19f); lineTo(11f, 19f); lineTo(11f, 13f)
            lineTo(5f, 13f); lineTo(5f, 11f); lineTo(11f, 11f); close()
        }
    }.build()
}

// --- Canvas drawing ---

private fun DrawScope.drawGrid(viewport: ViewportState, config: InfiniteCanvasConfig) {
    val gridSize = config.gridSize * viewport.scale

    if (gridSize < 5f) return

    val offsetX = viewport.offset.x % gridSize
    val offsetY = viewport.offset.y % gridSize

    var x = offsetX
    while (x < size.width) {
        drawLine(config.gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += gridSize
    }
    var y = offsetY
    while (y < size.height) {
        drawLine(config.gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += gridSize
    }
}

private fun DrawScope.drawConnections(state: InfiniteCanvasState, viewport: ViewportState) {
    val lineColor = Color(0xFF666666)
    state.connections.forEach { conn ->
        val from = state.getAnchorWorldPositionById(conn.fromElementId, conn.fromAnchor) ?: return@forEach
        val to = state.getAnchorWorldPositionById(conn.toElementId, conn.toAnchor) ?: return@forEach
        val screenFrom = viewport.worldToScreen(from)
        val screenTo = viewport.worldToScreen(to)
        drawBezierConnection(screenFrom, screenTo, conn.fromAnchor, conn.toAnchor, lineColor, viewport.scale)
    }
}

private fun DrawScope.drawDraggingConnection(state: InfiniteCanvasState, viewport: ViewportState) {
    val drag = state.draggingConnection ?: return
    val from = state.getAnchorWorldPositionById(drag.fromNodeId, drag.fromAnchor) ?: return
    val screenFrom = viewport.worldToScreen(from)
    val screenTo = viewport.worldToScreen(drag.currentWorldPos)
    drawBezierConnection(screenFrom, screenTo, drag.fromAnchor, null, Color(0xFF2196F3), viewport.scale)
}

private fun DrawScope.drawSelectionRect(state: InfiniteCanvasState) {
    val rect = state.selectionRect ?: return
    val screenRect = rect.toScreenRect()
    drawRect(
        color = Color(0x222196F3),
        topLeft = Offset(screenRect.left, screenRect.top),
        size = androidx.compose.ui.geometry.Size(screenRect.width, screenRect.height),
    )
    drawRect(
        color = Color(0xFF2196F3),
        topLeft = Offset(screenRect.left, screenRect.top),
        size = androidx.compose.ui.geometry.Size(screenRect.width, screenRect.height),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f),
    )
}

// --- Bezier hit testing helpers ---

internal fun isPointNearBezier(
    point: Offset,
    from: Offset,
    to: Offset,
    fromAnchor: AnchorPosition,
    toAnchor: AnchorPosition,
    threshold: Float,
): Boolean {
    val distance = kotlin.math.abs(to.x - from.x) + kotlin.math.abs(to.y - from.y)
    val controlOffset = (distance * 0.4f).coerceIn(30f, 200f)

    val cp1 = controlPointForAnchor(from, fromAnchor, controlOffset)
    val cp2 = controlPointForAnchor(to, toAnchor, controlOffset)

    val steps = 20
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val bx = bezier(t, from.x, cp1.x, cp2.x, to.x)
        val by = bezier(t, from.y, cp1.y, cp2.y, to.y)
        val dx = point.x - bx
        val dy = point.y - by
        if (dx * dx + dy * dy <= threshold * threshold) return true
    }
    return false
}

private fun bezier(t: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
    val u = 1 - t
    return u * u * u * p0 + 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t * p3
}

private fun controlPointForAnchor(point: Offset, anchor: AnchorPosition, offset: Float): Offset {
    return when (anchor) {
        AnchorPosition.Top -> Offset(point.x, point.y - offset)
        AnchorPosition.Bottom -> Offset(point.x, point.y + offset)
        AnchorPosition.Left -> Offset(point.x - offset, point.y)
        AnchorPosition.Right -> Offset(point.x + offset, point.y)
    }
}
