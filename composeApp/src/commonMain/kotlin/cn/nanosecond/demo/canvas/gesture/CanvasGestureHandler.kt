package cn.nanosecond.demo.canvas.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import cn.nanosecond.demo.canvas.CanvasMode
import cn.nanosecond.demo.canvas.CanvasViewModel
import cn.nanosecond.demo.canvas.DragMode

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
 */
fun Modifier.canvasGestures(viewModel: CanvasViewModel): Modifier = this
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
                        // Ctrl+滚轮: 缩放
                        if (scrollDelta.y != 0f) {
                            val zoomFactor = if (scrollDelta.y < 0) 1.1f else 1f / 1.1f
                            viewModel.viewport.zoomBy(zoomFactor, change.position)
                        }
                    } else if (isShift) {
                        // Shift+滚轮: 水平滑动
                        val panAmount = -scrollDelta.y * 50f
                        viewModel.viewport.panBy(
                            androidx.compose.ui.geometry.Offset(panAmount, 0f)
                        )
                    } else {
                        // 普通滚轮: 上下滑动
                        val panAmount = -scrollDelta.y * 50f
                        viewModel.viewport.panBy(
                            androidx.compose.ui.geometry.Offset(0f, panAmount)
                        )
                    }
                    change.consume()
                }
            }
        }
    }
    .pointerInput(Unit) {
        detectTapGestures { screenPos ->
            // Pan 模式下不处理点击
            if (viewModel.effectiveMode == CanvasMode.Pan) return@detectTapGestures

            // Select 模式: 检查连接线点击
            val hitConn = viewModel.hitTestConnection(screenPos)
            if (hitConn != null) {
                viewModel.removeConnection(hitConn.id)
                return@detectTapGestures
            }
            // 元素选中
            val hit = viewModel.hitTest(screenPos)
            viewModel.selectElement(hit?.id)
        }
    }
    .pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { screenPos ->
                // Pan 模式: 强制平移
                if (viewModel.effectiveMode == CanvasMode.Pan) {
                    viewModel.dragMode = DragMode.Pan
                    return@detectDragGestures
                }

                // Select 模式: 优先检查锚点
                val anchorHit = viewModel.hitTestAnchor(screenPos)
                if (anchorHit != null) {
                    val (elementId, anchor) = anchorHit
                    val worldPos = viewModel.viewport.screenToWorld(screenPos)
                    viewModel.startDraggingConnection(elementId, anchor, worldPos)
                    return@detectDragGestures
                }

                // 然后检查元素
                val hit = viewModel.hitTest(screenPos)
                if (hit != null) {
                    // 如果拖拽的元素不在已选中集合中，单选它
                    if (!viewModel.isSelected(hit.id)) {
                        viewModel.selectElement(hit.id)
                    }
                    viewModel.dragMode = DragMode.MoveElement
                } else {
                    // 空白区域: 框选
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
