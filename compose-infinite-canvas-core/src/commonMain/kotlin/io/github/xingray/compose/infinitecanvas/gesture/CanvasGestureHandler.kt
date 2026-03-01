package io.github.xingray.compose.infinitecanvas.gesture

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import io.github.xingray.compose.infinitecanvas.CanvasMode
import io.github.xingray.compose.infinitecanvas.ContextMenuState
import io.github.xingray.compose.infinitecanvas.DragMode
import io.github.xingray.compose.infinitecanvas.InfiniteCanvasState
import kotlin.math.sqrt

internal fun Modifier.canvasGestures(state: InfiniteCanvasState): Modifier = this
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed
                    && state.effectiveMode == CanvasMode.Select) {
                    val pos = event.changes.firstOrNull()?.position ?: continue
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
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
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
                        isMultiTouch = true
                        prevDistance = distance
                        prevCentroid = centroid
                        when (state.dragMode) {
                            DragMode.BoxSelect -> state.cancelBoxSelect()
                            DragMode.Connection -> state.cancelDraggingConnection()
                            else -> {}
                        }
                        state.dragMode = DragMode.None
                    } else {
                        if (prevDistance > 1f && distance > 1f) {
                            val zoomFactor = distance / prevDistance
                            state.viewport.zoomBy(zoomFactor, centroid)
                        }
                        val panDelta = centroid - prevCentroid
                        state.viewport.panBy(panDelta)

                        prevDistance = distance
                        prevCentroid = centroid
                    }

                    event.changes.forEach { it.consume() }
                } else {
                    if (isMultiTouch) {
                        isMultiTouch = false
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    }
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
                            state.viewport.zoomBy(zoomFactor, change.position)
                        }
                    } else if (isShift) {
                        val panAmount = -scrollDelta.y * 50f
                        state.viewport.panBy(Offset(panAmount, 0f))
                    } else {
                        val panAmount = -scrollDelta.y * 50f
                        state.viewport.panBy(Offset(0f, panAmount))
                    }
                    change.consume()
                }
            }
        }
    }
    .pointerInput(Unit) {
        detectTapGestures { screenPos ->
            if (state.effectiveMode == CanvasMode.Pan) return@detectTapGestures

            val hitConn = state.hitTestConnection(screenPos)
            if (hitConn != null) {
                state.removeConnection(hitConn.id)
                return@detectTapGestures
            }
            val hit = state.hitTest(screenPos)
            state.selectNode(hit?.id)
        }
    }
    .pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { screenPos ->
                if (state.effectiveMode == CanvasMode.Pan) {
                    state.dragMode = DragMode.Pan
                    return@detectDragGestures
                }

                val anchorHit = state.hitTestAnchor(screenPos)
                if (anchorHit != null) {
                    val (nodeId, anchor) = anchorHit
                    val worldPos = state.viewport.screenToWorld(screenPos)
                    state.startDraggingConnection(nodeId, anchor, worldPos)
                    return@detectDragGestures
                }

                val hit = state.hitTest(screenPos)
                if (hit != null) {
                    if (!state.isSelected(hit.id)) {
                        state.selectNode(hit.id)
                    }
                    state.dragMode = DragMode.MoveElement
                } else {
                    state.startBoxSelect(screenPos)
                }
            },
            onDrag = { change, dragAmount ->
                when (state.dragMode) {
                    DragMode.Connection -> {
                        val worldPos = state.viewport.screenToWorld(change.position)
                        state.updateDraggingConnection(worldPos)
                    }
                    DragMode.MoveElement -> {
                        val worldDelta = dragAmount / state.viewport.scale
                        state.moveSelectedNodes(worldDelta)
                    }
                    DragMode.Pan -> {
                        state.viewport.panBy(dragAmount)
                    }
                    DragMode.BoxSelect -> {
                        state.updateBoxSelect(change.position)
                    }
                    DragMode.None -> {}
                }
                change.consume()
            },
            onDragEnd = {
                when (state.dragMode) {
                    DragMode.Connection -> {
                        val drag = state.draggingConnection
                        if (drag != null) {
                            val screenPos = state.viewport.worldToScreen(drag.currentWorldPos)
                            state.finishDraggingConnection(screenPos)
                        }
                    }
                    DragMode.BoxSelect -> {
                        state.finishBoxSelect()
                    }
                    else -> {}
                }
                state.dragMode = DragMode.None
            },
            onDragCancel = {
                when (state.dragMode) {
                    DragMode.Connection -> state.cancelDraggingConnection()
                    DragMode.BoxSelect -> state.cancelBoxSelect()
                    else -> {}
                }
                state.dragMode = DragMode.None
            }
        )
    }

private fun dist(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}
