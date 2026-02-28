package io.github.xingray.compose.infinitecanvas

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class CanvasOffset(val x: Float, val y: Float) {
    companion object {
        val Zero = CanvasOffset(0f, 0f)
    }

    operator fun plus(other: CanvasOffset) = CanvasOffset(x + other.x, y + other.y)
    operator fun minus(other: CanvasOffset) = CanvasOffset(x - other.x, y - other.y)
}

data class CanvasSize(val width: Float, val height: Float) {
    companion object {
        val Zero = CanvasSize(0f, 0f)
    }
}

@OptIn(ExperimentalUuidApi::class)
fun generateId(): String = Uuid.random().toString()

sealed class CanvasElement {
    abstract val id: String
    abstract val position: CanvasOffset
    abstract val size: CanvasSize

    abstract fun copyWithPosition(position: CanvasOffset): CanvasElement
}

data class CardElement(
    override val id: String = generateId(),
    override val position: CanvasOffset = CanvasOffset.Zero,
    override val size: CanvasSize = CanvasSize(240f, 180f),
    val title: String = "",
    val content: String = "",
    val imageUrl: String? = null,
) : CanvasElement() {
    override fun copyWithPosition(position: CanvasOffset) = copy(position = position)
}
