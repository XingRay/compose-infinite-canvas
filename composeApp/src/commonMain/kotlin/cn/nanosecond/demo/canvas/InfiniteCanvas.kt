package cn.nanosecond.demo.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import cn.nanosecond.demo.canvas.connection.drawBezierConnection
import cn.nanosecond.demo.canvas.element.CardElementView
import cn.nanosecond.demo.canvas.gesture.canvasGestures

@Composable
fun InfiniteCanvas(
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier,
) {
    val viewport = viewModel.viewport
    val isPanMode = viewModel.effectiveMode == CanvasMode.Pan
    val cursorIcon = if (isPanMode) PointerIcon.Hand else PointerIcon.Default

    val focusRequester = remember { FocusRequester() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // 自动获取焦点以接收键盘事件
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 画布区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Spacebar) {
                        when (event.type) {
                            KeyEventType.KeyDown -> {
                                viewModel.onSpaceDown()
                                true
                            }
                            KeyEventType.KeyUp -> {
                                viewModel.onSpaceUp()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                }
                .pointerHoverIcon(cursorIcon)
                .canvasGestures(viewModel)
                .onSizeChanged { canvasSize = it }
        ) {
            // 底层: 网格 + 连接线 + 框选矩形
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawGrid(viewport)
                drawConnections(viewModel, viewport)
                drawDraggingConnection(viewModel, viewport)
                drawSelectionRect(viewModel)
            }

            // 上层: 元素
            viewModel.elements.forEach { element ->
                when (element) {
                    is CardElement -> {
                        CardElementView(
                            card = element,
                            viewport = viewport,
                            isSelected = viewModel.isSelected(element.id),
                        )
                    }
                }
            }
        }

        // 左下角控制面板
        BottomControls(
            viewModel = viewModel,
            canvasSize = canvasSize,
            onInteraction = { focusRequester.requestFocus() },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
                .zIndex(10f),
        )

        // 右键上下文菜单
        val contextMenu = viewModel.contextMenuState
        if (contextMenu != null) {
            val dismissMenu: () -> Unit = {
                viewModel.contextMenuState = null
                focusRequester.requestFocus()
            }

            if (contextMenu.targetElementId != null) {
                // 卡片菜单
                val elementId = contextMenu.targetElementId
                ElementContextMenu(
                    state = contextMenu,
                    canvasSize = canvasSize,
                    onDismiss = dismissMenu,
                    onCopy = { /* TODO */ },
                    onCut = { /* TODO */ },
                    onDelete = { viewModel.removeElement(elementId) },
                    onDuplicate = { viewModel.duplicateElement(elementId) },
                    onBringToFront = { viewModel.bringToFront(elementId) },
                    onSendToBack = { viewModel.sendToBack(elementId) },
                )
            } else {
                // 画布菜单
                CanvasContextMenu(
                    state = contextMenu,
                    canvasSize = canvasSize,
                    hasClipboard = false, // TODO: 接入剪贴板检测
                    onDismiss = dismissMenu,
                    onPaste = { /* TODO */ },
                    onSelectAll = { viewModel.selectAll() },
                    onExportPng = { /* TODO */ },
                    onExportSvg = { /* TODO */ },
                    onCopyPng = { /* TODO */ },
                    onCopySvg = { /* TODO */ },
                    onSubmenuToggle = { submenu ->
                        viewModel.contextMenuState = contextMenu.copy(expandedSubmenu = submenu)
                    },
                )
            }
        }
    }
}

// --- 左下角控制面板 ---

@Composable
private fun BottomControls(
    viewModel: CanvasViewModel,
    canvasSize: IntSize,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 模式切换
        ModeSwitch(
            currentMode = viewModel.canvasMode,
            onModeChange = {
                viewModel.switchMode(it)
                onInteraction()
            },
        )

        // 缩放控件
        ZoomControls(
            scalePercent = viewModel.viewport.scalePercent,
            onZoomIn = {
                val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                viewModel.viewport.zoomBy(1.25f, center)
                onInteraction()
            },
            onZoomOut = {
                val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                viewModel.viewport.zoomBy(0.8f, center)
                onInteraction()
            },
            onResetZoom = {
                val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                viewModel.viewport.zoomTo(1f, center)
                onInteraction()
            },
        )
    }
}

// --- 模式切换 ---

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

// --- 缩放控件 ---

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
        // 缩小按钮
        ZoomButton(icon = MinusIcon, contentDescription = "Zoom out", onClick = onZoomOut)

        // 百分比显示 (点击重置为 100%)
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

        // 放大按钮
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
        name = "Arrow",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(7f, 2f)
            lineTo(7f, 20f)
            lineTo(11.5f, 15.5f)
            lineTo(16f, 22f)
            lineTo(18f, 21f)
            lineTo(13.5f, 14.5f)
            lineTo(19f, 14f)
            close()
        }
    }.build()
}

private val HandIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Hand",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(12.8f, 2f, 13.5f, 2.7f, 13.5f, 3.5f)
            lineTo(13.5f, 10f)
            lineTo(15f, 10f)
            lineTo(15f, 5.5f)
            curveTo(15f, 4.7f, 15.7f, 4f, 16.5f, 4f)
            curveTo(17.3f, 4f, 18f, 4.7f, 18f, 5.5f)
            lineTo(18f, 10f)
            lineTo(19.5f, 10.5f)
            curveTo(19.5f, 9.7f, 20.2f, 9f, 21f, 9f)
            curveTo(21.8f, 9f, 22f, 9.7f, 22f, 10.5f)
            lineTo(22f, 16f)
            curveTo(22f, 19.3f, 19.3f, 22f, 16f, 22f)
            lineTo(13f, 22f)
            curveTo(10.8f, 22f, 8.8f, 20.9f, 7.6f, 19.2f)
            lineTo(4f, 13.5f)
            curveTo(3.5f, 12.8f, 3.7f, 11.8f, 4.4f, 11.3f)
            curveTo(5.1f, 10.8f, 6.1f, 11f, 6.6f, 11.7f)
            lineTo(8.5f, 14.5f)
            lineTo(8.5f, 14f)
            lineTo(10.5f, 14f)
            lineTo(10.5f, 3.5f)
            curveTo(10.5f, 2.7f, 11.2f, 2f, 12f, 2f)
            close()
        }
    }.build()
}

private val MinusIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Minus",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(5f, 11f)
            lineTo(19f, 11f)
            lineTo(19f, 13f)
            lineTo(5f, 13f)
            close()
        }
    }.build()
}

private val PlusIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Plus",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
            moveTo(11f, 5f)
            lineTo(13f, 5f)
            lineTo(13f, 11f)
            lineTo(19f, 11f)
            lineTo(19f, 13f)
            lineTo(13f, 13f)
            lineTo(13f, 19f)
            lineTo(11f, 19f)
            lineTo(11f, 13f)
            lineTo(5f, 13f)
            lineTo(5f, 11f)
            lineTo(11f, 11f)
            close()
        }
    }.build()
}

// --- Canvas drawing ---

private fun DrawScope.drawGrid(viewport: ViewportState) {
    val gridColor = Color(0xFFE0E0E0)
    val gridSize = 50f * viewport.scale

    if (gridSize < 5f) return

    val offsetX = viewport.offset.x % gridSize
    val offsetY = viewport.offset.y % gridSize

    var x = offsetX
    while (x < size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += gridSize
    }
    var y = offsetY
    while (y < size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += gridSize
    }
}

private fun DrawScope.drawConnections(viewModel: CanvasViewModel, viewport: ViewportState) {
    val lineColor = Color(0xFF666666)
    viewModel.connections.forEach { conn ->
        val from = viewModel.getAnchorWorldPosition(conn.fromElementId, conn.fromAnchor) ?: return@forEach
        val to = viewModel.getAnchorWorldPosition(conn.toElementId, conn.toAnchor) ?: return@forEach
        val screenFrom = viewport.worldToScreen(from)
        val screenTo = viewport.worldToScreen(to)
        drawBezierConnection(screenFrom, screenTo, conn.fromAnchor, conn.toAnchor, lineColor, viewport.scale)
    }
}

private fun DrawScope.drawDraggingConnection(viewModel: CanvasViewModel, viewport: ViewportState) {
    val drag = viewModel.draggingConnection ?: return
    val from = viewModel.getAnchorWorldPosition(drag.fromElementId, drag.fromAnchor) ?: return
    val screenFrom = viewport.worldToScreen(from)
    val screenTo = viewport.worldToScreen(drag.currentWorldPos)
    drawBezierConnection(screenFrom, screenTo, drag.fromAnchor, null, Color(0xFF2196F3), viewport.scale)
}

private fun DrawScope.drawSelectionRect(viewModel: CanvasViewModel) {
    val rect = viewModel.selectionRect ?: return
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
