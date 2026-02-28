package cn.nanosecond.demo.canvas.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import cn.nanosecond.demo.canvas.CanvasMode
import cn.nanosecond.demo.canvas.CanvasViewModel
import cn.nanosecond.demo.canvas.ContextMenuState
import cn.nanosecond.demo.canvas.DragMode
import kotlin.math.sqrt

/**
 * 画布手势处理 (根据 CanvasMode 分发):
 *
 * Select 模式:
 * - 点击元素 → 选中
 * - 点击连接线 → 删除
 * - 拖拽锚点 → 创建连接线
 * - 拖拽元素 → 移动选中元素
 * - 拖拽空白 → 框选
 *
 * Pan 模式:
 * - 所有拖拽 → 平移画布
 *
 * 通用:
 * - Ctrl+滚轮 → 缩放
 * - 滚轮 → 上下滑动画布
 * - Shift+滚轮 → 左右滑动画布
 * - 双指捏合 → 缩放 + 平移 (触屏)
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.canvasGestures(viewModel: CanvasViewModel): Modifier = this
    // 右键菜单检测
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed
                    && viewModel.effectiveMode == CanvasMode.Select) {
                    val pos = event.changes.firstOrNull()?.position ?: continue
                    val hit = viewModel.hitTest(pos)
                    if (hit != null) {
                        // 右键点击卡片 → 卡片菜单
                        if (!viewModel.isSelected(hit.id)) {
                            viewModel.selectElement(hit.id)
                        }
                        viewModel.contextMenuState = ContextMenuState(
                            screenPosition = pos,
                            targetElementId = hit.id,
                        )
                    } else {
                        // 右键点击空白 → 画布菜单
                        viewModel.contextMenuState = ContextMenuState(screenPosition = pos)
                    }
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
    // 多指触控: 捏合缩放 + 双指平移
    .pointerInput(Unit) {
        awaitPointerEventScope {
            var prevDistance = 0f
            var prevCentroid = Offset.Zero
            var isMultiTouch = false

            while (true) {
                val event = awaitPointerEvent()
                val pressed = event.changes.filter { it.pressed }

                if (pressed.size >= 2) {
                    val p1 = pressed[0].position
                    val p2 = pressed[1].position
                    val distance = dist(p1, p2)
                    val centroid = Offset((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f)

                    if (!isMultiTouch) {
                        // 刚进入多指模式
                        isMultiTouch = true
                        prevDistance = distance
                        prevCentroid = centroid
                        // 取消当前可能正在进行的单指操作
                        when (viewModel.dragMode) {
                            DragMode.BoxSelect -> viewModel.cancelBoxSelect()
                            DragMode.Connection -> viewModel.cancelDraggingConnection()
                            else -> {}
                        }
                        viewModel.dragMode = DragMode.None
                    } else {
                        // 计算缩放
                        if (prevDistance > 1f && distance > 1f) {
                            val zoomFactor = distance / prevDistance
                            viewModel.viewport.zoomBy(zoomFactor, centroid)
                        }
                        // 计算平移
                        val panDelta = centroid - prevCentroid
                        viewModel.viewport.panBy(panDelta)

                        prevDistance = distance
                        prevCentroid = centroid
                    }

                    // 消费所有变化，防止传递给单指手势
                    event.changes.forEach { it.consume() }
                } else {
                    if (isMultiTouch) {
                        // 退出多指模式
                        isMultiTouch = false
                        // 消费剩余的 change，防止触发单指手势
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    }
    // 滚轮: Ctrl+缩放 / Shift+水平滑动 / 普通上下滑动
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val change = event.changes.firstOrNull() ?: continue
                    val scrollDelta = change.scrollDelta
                    val isCtrl = event.keyboardModifiers.isCtrlPressed
                    val isShift = event.keyboardModifiers.isShiftPressed

                    if (isCtrl) {
                        if (scrollDelta.y != 0f) {
                            val zoomFactor = if (scrollDelta.y < 0) 1.1f else 1f / 1.1f
                            viewModel.viewport.zoomBy(zoomFactor, change.position)
                        }
                    } else if (isShift) {
                        val panAmount = -scrollDelta.y * 50f
                        viewModel.viewport.panBy(Offset(panAmount, 0f))
                    } else {
                        val panAmount = -scrollDelta.y * 50f
                        viewModel.viewport.panBy(Offset(0f, panAmount))
                    }
                    change.consume()
                }
            }
        }
    }
    // 单指点击
    .pointerInput(Unit) {
        detectTapGestures { screenPos ->
            if (viewModel.effectiveMode == CanvasMode.Pan) return@detectTapGestures

            val hitConn = viewModel.hitTestConnection(screenPos)
            if (hitConn != null) {
                viewModel.removeConnection(hitConn.id)
                return@detectTapGestures
            }
            val hit = viewModel.hitTest(screenPos)
            viewModel.selectElement(hit?.id)
        }
    }
    // 单指拖拽
    .pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { screenPos ->
                if (viewModel.effectiveMode == CanvasMode.Pan) {
                    viewModel.dragMode = DragMode.Pan
                    return@detectDragGestures
                }

                val anchorHit = viewModel.hitTestAnchor(screenPos)
                if (anchorHit != null) {
                    val (elementId, anchor) = anchorHit
                    val worldPos = viewModel.viewport.screenToWorld(screenPos)
                    viewModel.startDraggingConnection(elementId, anchor, worldPos)
                    return@detectDragGestures
                }

                val hit = viewModel.hitTest(screenPos)
                if (hit != null) {
                    if (!viewModel.isSelected(hit.id)) {
                        viewModel.selectElement(hit.id)
                    }
                    viewModel.dragMode = DragMode.MoveElement
                } else {
                    viewModel.startBoxSelect(screenPos)
                }
            },
            onDrag = { change, dragAmount ->
                when (viewModel.dragMode) {
                    DragMode.Connection -> {
                        val worldPos = viewModel.viewport.screenToWorld(change.position)
                        viewModel.updateDraggingConnection(worldPos)
                    }

                    DragMode.MoveElement -> {
                        val worldDelta = dragAmount / viewModel.viewport.scale
                        viewModel.moveSelectedElements(worldDelta)
                    }

                    DragMode.Pan -> {
                        viewModel.viewport.panBy(dragAmount)
                    }

                    DragMode.BoxSelect -> {
                        viewModel.updateBoxSelect(change.position)
                    }

                    DragMode.None -> {}
                }
                change.consume()
            },
            onDragEnd = {
                when (viewModel.dragMode) {
                    DragMode.Connection -> {
                        val drag = viewModel.draggingConnection
                        if (drag != null) {
                            val screenPos = viewModel.viewport.worldToScreen(drag.currentWorldPos)
                            viewModel.finishDraggingConnection(screenPos)
                        }
                    }

                    DragMode.BoxSelect -> {
                        viewModel.finishBoxSelect()
                    }

                    else -> {}
                }
                viewModel.dragMode = DragMode.None
            },
            onDragCancel = {
                when (viewModel.dragMode) {
                    DragMode.Connection -> viewModel.cancelDraggingConnection()
                    DragMode.BoxSelect -> viewModel.cancelBoxSelect()
                    else -> {}
                }
                viewModel.dragMode = DragMode.None
            }
        )
    }

private fun dist(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}
