package io.github.xingray.compose.infinitecanvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

@Stable
class InfiniteCanvasState {
    val viewport = ViewportState()

    var canvasMode by mutableStateOf(CanvasMode.Select)
        private set

    var isTempPanMode by mutableStateOf(false)
        private set

    val effectiveMode: CanvasMode
        get() = if (isTempPanMode) CanvasMode.Pan else canvasMode

    var selectedNodeIds by mutableStateOf(setOf<String>())

    private val _connections = mutableStateListOf<Connection>()
    val connections: List<Connection> get() = _connections

    internal var dragMode by mutableStateOf(DragMode.None)
    internal var draggingConnection by mutableStateOf<DraggingConnection?>(null)
    internal var selectionRect by mutableStateOf<SelectionRect?>(null)
    var contextMenuState by mutableStateOf<ContextMenuState?>(null)
        internal set
    internal var canvasSize by mutableStateOf(IntSize.Zero)
    internal var currentNodes: List<CanvasNode> = emptyList()

    // --- Public API ---

    fun switchMode(mode: CanvasMode) {
        canvasMode = mode
        if (mode == CanvasMode.Pan && isTempPanMode) {
            isTempPanMode = false
        }
    }

    fun selectNode(id: String?) {
        selectedNodeIds = if (id != null) setOf(id) else emptySet()
    }

    fun selectAll() {
        selectedNodeIds = currentNodes.map { it.id }.toSet()
    }

    fun isSelected(id: String): Boolean = id in selectedNodeIds

    fun addConnection(connection: Connection) {
        val exists = _connections.any {
            (it.fromElementId == connection.fromElementId && it.toElementId == connection.toElementId) ||
                (it.fromElementId == connection.toElementId && it.toElementId == connection.fromElementId)
        }
        if (!exists && connection.fromElementId != connection.toElementId) {
            _connections.add(connection)
        }
    }

    fun removeConnection(id: String) {
        _connections.removeAll { it.id == id }
    }

    // --- Internal ---

    internal fun onSpaceDown() {
        if (canvasMode == CanvasMode.Select && !isTempPanMode) {
            isTempPanMode = true
        }
    }

    internal fun onSpaceUp() {
        if (isTempPanMode) {
            isTempPanMode = false
        }
    }

    internal fun renderOrderedNodes(): List<CanvasNode> {
        val normal = currentNodes.filter { !it.state.pinToFront }
        val pinned = currentNodes.filter { it.state.pinToFront }
        return normal + pinned
    }

    internal fun hitTest(screenPos: Offset): CanvasNode? {
        val worldPos = viewport.screenToWorld(screenPos)
        return renderOrderedNodes().lastOrNull { node ->
            val ns = node.state
            worldPos.x >= ns.x && worldPos.x <= ns.x + ns.measuredWidth &&
                worldPos.y >= ns.y && worldPos.y <= ns.y + ns.measuredHeight
        }
    }

    internal fun hitTestAnchor(screenPos: Offset, hitRadius: Float = 20f): Pair<String, AnchorPosition>? {
        for (selectedId in selectedNodeIds) {
            val node = currentNodes.find { it.id == selectedId } ?: continue
            for (anchor in AnchorPosition.entries) {
                val anchorWorld = getAnchorWorldPosition(node, anchor)
                val anchorScreen = viewport.worldToScreen(anchorWorld)
                val dist = (screenPos - anchorScreen).getDistance()
                if (dist <= hitRadius) {
                    return node.id to anchor
                }
            }
        }
        return null
    }

    internal fun hitTestConnection(screenPos: Offset, hitRadius: Float = 12f): Connection? {
        val worldPos = viewport.screenToWorld(screenPos)
        return _connections.firstOrNull { conn ->
            val from = getAnchorWorldPositionById(conn.fromElementId, conn.fromAnchor) ?: return@firstOrNull false
            val to = getAnchorWorldPositionById(conn.toElementId, conn.toAnchor) ?: return@firstOrNull false
            isPointNearBezier(worldPos, from, to, conn.fromAnchor, conn.toAnchor, hitRadius / viewport.scale)
        }
    }

    internal fun getAnchorWorldPositionById(nodeId: String, anchor: AnchorPosition): Offset? {
        val node = currentNodes.find { it.id == nodeId } ?: return null
        return getAnchorWorldPosition(node, anchor)
    }

    private fun getAnchorWorldPosition(node: CanvasNode, anchor: AnchorPosition): Offset {
        val ns = node.state
        return when (anchor) {
            AnchorPosition.Top -> Offset(ns.x + ns.measuredWidth / 2, ns.y)
            AnchorPosition.Bottom -> Offset(ns.x + ns.measuredWidth / 2, ns.y + ns.measuredHeight)
            AnchorPosition.Left -> Offset(ns.x, ns.y + ns.measuredHeight / 2)
            AnchorPosition.Right -> Offset(ns.x + ns.measuredWidth, ns.y + ns.measuredHeight / 2)
        }
    }

    internal fun findBestAnchor(nodeId: String, worldPos: Offset): AnchorPosition {
        val node = currentNodes.find { it.id == nodeId } ?: return AnchorPosition.Right
        val ns = node.state
        val cx = ns.x + ns.measuredWidth / 2
        val cy = ns.y + ns.measuredHeight / 2
        val dx = worldPos.x - cx
        val dy = worldPos.y - cy
        return if (kotlin.math.abs(dx) * ns.measuredHeight > kotlin.math.abs(dy) * ns.measuredWidth) {
            if (dx > 0) AnchorPosition.Right else AnchorPosition.Left
        } else {
            if (dy > 0) AnchorPosition.Bottom else AnchorPosition.Top
        }
    }

    internal fun moveNode(id: String, worldDelta: Offset) {
        val node = currentNodes.find { it.id == id && !it.state.fixed } ?: return
        node.state.x += worldDelta.x
        node.state.y += worldDelta.y
    }

    internal fun moveSelectedNodes(worldDelta: Offset) {
        selectedNodeIds.forEach { id -> moveNode(id, worldDelta) }
    }

    // --- Box select ---

    internal fun startBoxSelect(screenPos: Offset) {
        selectionRect = SelectionRect(screenPos, screenPos)
        dragMode = DragMode.BoxSelect
    }

    internal fun updateBoxSelect(currentScreenPos: Offset) {
        val rect = selectionRect ?: return
        selectionRect = rect.copy(end = currentScreenPos)
    }

    internal fun finishBoxSelect() {
        val rect = selectionRect ?: return
        val worldRect = rect.toWorldRect(viewport)

        val selected = currentNodes.filter { node ->
            val ns = node.state
            val nodeRect = Rect(ns.x, ns.y, ns.x + ns.measuredWidth, ns.y + ns.measuredHeight)
            worldRect.overlaps(nodeRect)
        }.map { it.id }.toSet()

        selectedNodeIds = selected
        selectionRect = null
        dragMode = DragMode.None
    }

    internal fun cancelBoxSelect() {
        selectionRect = null
        dragMode = DragMode.None
    }

    // --- Connection dragging ---

    internal fun startDraggingConnection(fromNodeId: String, fromAnchor: AnchorPosition, startWorldPos: Offset) {
        draggingConnection = DraggingConnection(fromNodeId, fromAnchor, startWorldPos)
        dragMode = DragMode.Connection
    }

    internal fun updateDraggingConnection(currentWorldPos: Offset) {
        draggingConnection = draggingConnection?.copy(currentWorldPos = currentWorldPos)
    }

    internal fun finishDraggingConnection(screenPos: Offset) {
        val drag = draggingConnection ?: return
        val targetNode = hitTest(screenPos)
        val targetId = targetNode?.id
        if (targetId != null && targetId != drag.fromNodeId) {
            val toAnchor = findBestAnchor(targetId, drag.currentWorldPos)
            addConnection(
                Connection(
                    fromElementId = drag.fromNodeId,
                    fromAnchor = drag.fromAnchor,
                    toElementId = targetId,
                    toAnchor = toAnchor,
                )
            )
        }
        draggingConnection = null
        dragMode = DragMode.None
    }

    internal fun cancelDraggingConnection() {
        draggingConnection = null
        dragMode = DragMode.None
    }
}

@Composable
fun rememberInfiniteCanvasState(): InfiniteCanvasState = remember { InfiniteCanvasState() }

// --- Supporting types ---

data class SelectionRect(val start: Offset, val end: Offset) {
    fun toWorldRect(viewport: ViewportState): Rect {
        val worldStart = viewport.screenToWorld(start)
        val worldEnd = viewport.screenToWorld(end)
        return Rect(
            left = minOf(worldStart.x, worldEnd.x),
            top = minOf(worldStart.y, worldEnd.y),
            right = maxOf(worldStart.x, worldEnd.x),
            bottom = maxOf(worldStart.y, worldEnd.y),
        )
    }

    fun toScreenRect(): Rect = Rect(
        left = minOf(start.x, end.x),
        top = minOf(start.y, end.y),
        right = maxOf(start.x, end.x),
        bottom = maxOf(start.y, end.y),
    )
}

enum class DragMode {
    None, Pan, MoveElement, Connection, BoxSelect
}

enum class CanvasMode {
    Select, Pan
}

data class ContextMenuState(
    val screenPosition: Offset,
    val targetNodeId: String? = null,
)

data class DraggingConnection(
    val fromNodeId: String,
    val fromAnchor: AnchorPosition,
    val currentWorldPos: Offset,
)
