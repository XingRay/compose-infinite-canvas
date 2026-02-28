package io.github.xingray.compose.infinitecanvas

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel

class CanvasViewModel : ViewModel() {
    val viewport = ViewportState()

    private val _elements = mutableStateListOf<CanvasElement>()
    val elements: List<CanvasElement> get() = _elements

    private val _connections = mutableStateListOf<Connection>()
    val connections: List<Connection> get() = _connections

    /** 选中的元素 ID 集合 (支持多选) */
    var selectedElementIds by mutableStateOf(setOf<String>())
        private set

    /** 正在拖拽创建的连接线状态 */
    var draggingConnection by mutableStateOf<DraggingConnection?>(null)
        private set

    /** 当前拖拽模式 */
    var dragMode by mutableStateOf(DragMode.None)

    /** 画布交互模式 */
    var canvasMode by mutableStateOf(CanvasMode.Select)
        private set

    /** 是否处于临时小手模式 (仅从 Select 模式按住空格进入) */
    var isTempPanMode by mutableStateOf(false)
        private set

    /** 框选矩形 (屏幕坐标) */
    var selectionRect by mutableStateOf<SelectionRect?>(null)
        private set

    /** 右键菜单状态 */
    var contextMenuState by mutableStateOf<ContextMenuState?>(null)

    /** 当前生效的模式 */
    val effectiveMode: CanvasMode
        get() = if (isTempPanMode) CanvasMode.Pan else canvasMode

    fun switchMode(mode: CanvasMode) {
        canvasMode = mode
        if (mode == CanvasMode.Pan && isTempPanMode) {
            isTempPanMode = false
        }
    }

    fun onSpaceDown() {
        if (canvasMode == CanvasMode.Select && !isTempPanMode) {
            isTempPanMode = true
        }
    }

    fun onSpaceUp() {
        if (isTempPanMode) {
            isTempPanMode = false
        }
    }

    fun addElement(element: CanvasElement) {
        _elements.add(element)
    }

    fun removeElement(id: String) {
        _elements.removeAll { it.id == id }
        _connections.removeAll { it.fromElementId == id || it.toElementId == id }
        selectedElementIds = selectedElementIds - id
    }

    fun removeSelectedElements() {
        selectedElementIds.toList().forEach { removeElement(it) }
    }

    fun duplicateElement(id: String) {
        val el = _elements.find { it.id == id } ?: return
        val copy = el.copyWithPosition(
            CanvasOffset(el.position.x + 20f, el.position.y + 20f)
        )
        val newEl = when (copy) {
            is CardElement -> copy.copy(id = generateId())
        }
        _elements.add(newEl)
        selectedElementIds = setOf(newEl.id)
    }

    fun bringToFront(id: String) {
        val index = _elements.indexOfFirst { it.id == id }
        if (index >= 0 && index < _elements.lastIndex) {
            val el = _elements.removeAt(index)
            _elements.add(el)
        }
    }

    fun sendToBack(id: String) {
        val index = _elements.indexOfFirst { it.id == id }
        if (index > 0) {
            val el = _elements.removeAt(index)
            _elements.add(0, el)
        }
    }

    fun updateElement(element: CanvasElement) {
        val index = _elements.indexOfFirst { it.id == element.id }
        if (index >= 0) _elements[index] = element
    }

    fun moveElement(id: String, worldDelta: Offset) {
        val index = _elements.indexOfFirst { it.id == id }
        if (index >= 0) {
            val el = _elements[index]
            _elements[index] = el.copyWithPosition(
                CanvasOffset(el.position.x + worldDelta.x, el.position.y + worldDelta.y)
            )
        }
    }

    fun moveSelectedElements(worldDelta: Offset) {
        selectedElementIds.forEach { id -> moveElement(id, worldDelta) }
    }

    fun selectElement(id: String?) {
        selectedElementIds = if (id != null) setOf(id) else emptySet()
    }

    fun selectAll() {
        selectedElementIds = _elements.map { it.id }.toSet()
    }

    fun isSelected(id: String): Boolean = id in selectedElementIds

    fun hitTest(screenPos: Offset): CanvasElement? {
        val worldPos = viewport.screenToWorld(screenPos)
        return _elements.lastOrNull { el ->
            worldPos.x >= el.position.x && worldPos.x <= el.position.x + el.size.width &&
                worldPos.y >= el.position.y && worldPos.y <= el.position.y + el.size.height
        }
    }

    fun hitTestAnchor(screenPos: Offset, hitRadius: Float = 20f): Pair<String, AnchorPosition>? {
        for (selectedId in selectedElementIds) {
            val el = _elements.find { it.id == selectedId } ?: continue
            for (anchor in AnchorPosition.entries) {
                val anchorWorld = getAnchorWorldPosition(el.id, anchor) ?: continue
                val anchorScreen = viewport.worldToScreen(anchorWorld)
                val dist = (screenPos - anchorScreen).getDistance()
                if (dist <= hitRadius) {
                    return el.id to anchor
                }
            }
        }
        return null
    }

    fun hitTestConnection(screenPos: Offset, hitRadius: Float = 12f): Connection? {
        val worldPos = viewport.screenToWorld(screenPos)
        return _connections.firstOrNull { conn ->
            val from = getAnchorWorldPosition(conn.fromElementId, conn.fromAnchor) ?: return@firstOrNull false
            val to = getAnchorWorldPosition(conn.toElementId, conn.toAnchor) ?: return@firstOrNull false
            isPointNearBezier(worldPos, from, to, conn.fromAnchor, conn.toAnchor, hitRadius / viewport.scale)
        }
    }

    // --- 框选 ---

    fun startBoxSelect(screenPos: Offset) {
        selectionRect = SelectionRect(screenPos, screenPos)
        dragMode = DragMode.BoxSelect
    }

    fun updateBoxSelect(currentScreenPos: Offset) {
        val rect = selectionRect ?: return
        selectionRect = rect.copy(end = currentScreenPos)
    }

    fun finishBoxSelect() {
        val rect = selectionRect ?: return
        val worldRect = rect.toWorldRect(viewport)

        val selected = _elements.filter { el ->
            val elRect = Rect(
                el.position.x, el.position.y,
                el.position.x + el.size.width, el.position.y + el.size.height,
            )
            worldRect.overlaps(elRect)
        }.map { it.id }.toSet()

        selectedElementIds = selected
        selectionRect = null
        dragMode = DragMode.None
    }

    fun cancelBoxSelect() {
        selectionRect = null
        dragMode = DragMode.None
    }

    // --- 连接线 ---

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

    fun getAnchorWorldPosition(elementId: String, anchor: AnchorPosition): Offset? {
        val el = _elements.find { it.id == elementId } ?: return null
        return when (anchor) {
            AnchorPosition.Top -> Offset(el.position.x + el.size.width / 2, el.position.y)
            AnchorPosition.Bottom -> Offset(el.position.x + el.size.width / 2, el.position.y + el.size.height)
            AnchorPosition.Left -> Offset(el.position.x, el.position.y + el.size.height / 2)
            AnchorPosition.Right -> Offset(el.position.x + el.size.width, el.position.y + el.size.height / 2)
        }
    }

    fun findBestAnchor(elementId: String, worldPos: Offset): AnchorPosition {
        val el = _elements.find { it.id == elementId } ?: return AnchorPosition.Right
        val cx = el.position.x + el.size.width / 2
        val cy = el.position.y + el.size.height / 2
        val dx = worldPos.x - cx
        val dy = worldPos.y - cy
        return if (kotlin.math.abs(dx) * el.size.height > kotlin.math.abs(dy) * el.size.width) {
            if (dx > 0) AnchorPosition.Right else AnchorPosition.Left
        } else {
            if (dy > 0) AnchorPosition.Bottom else AnchorPosition.Top
        }
    }

    // --- 拖拽创建连接 ---

    fun startDraggingConnection(fromElementId: String, fromAnchor: AnchorPosition, startWorldPos: Offset) {
        draggingConnection = DraggingConnection(fromElementId, fromAnchor, startWorldPos)
        dragMode = DragMode.Connection
    }

    fun updateDraggingConnection(currentWorldPos: Offset) {
        draggingConnection = draggingConnection?.copy(currentWorldPos = currentWorldPos)
    }

    fun finishDraggingConnection(screenPos: Offset) {
        val drag = draggingConnection ?: return
        val targetElement = hitTest(screenPos)
        val targetId = targetElement?.id
        if (targetId != null && targetId != drag.fromElementId) {
            val toAnchor = findBestAnchor(targetId, drag.currentWorldPos)
            addConnection(
                Connection(
                    fromElementId = drag.fromElementId,
                    fromAnchor = drag.fromAnchor,
                    toElementId = targetId,
                    toAnchor = toAnchor,
                )
            )
        }
        draggingConnection = null
        dragMode = DragMode.None
    }

    fun cancelDraggingConnection() {
        draggingConnection = null
        dragMode = DragMode.None
    }
}

/** 框选矩形 (屏幕坐标) */
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

private fun isPointNearBezier(
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

data class DraggingConnection(
    val fromElementId: String,
    val fromAnchor: AnchorPosition,
    val currentWorldPos: Offset,
)

enum class DragMode {
    None, Pan, MoveElement, Connection, BoxSelect
}

enum class CanvasMode {
    Select, Pan
}

data class ContextMenuState(
    val screenPosition: Offset,
    val targetElementId: String? = null,
    val expandedSubmenu: String? = null,
)