package cn.nanosecond.demo.canvas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

@Stable
class ViewportState(
    initialOffset: Offset = Offset.Zero,
    initialScale: Float = 1f,
) {
    var offset by mutableStateOf(initialOffset)
    var scale by mutableFloatStateOf(initialScale)

    companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 5.0f
    }

    /** 屏幕坐标 → 世界坐标 */
    fun screenToWorld(screenPos: Offset): Offset {
        return (screenPos - offset) / scale
    }

    /** 世界坐标 → 屏幕坐标 */
    fun worldToScreen(worldPos: Offset): Offset {
        return worldPos * scale + offset
    }

    /** 以 focalPoint (屏幕坐标) 为中心进行缩放 */
    fun zoomBy(zoomFactor: Float, focalPoint: Offset) {
        val newScale = (scale * zoomFactor).coerceIn(MIN_SCALE, MAX_SCALE)
        val actualFactor = newScale / scale
        // 保持 focalPoint 对应的世界坐标不变
        offset = focalPoint - (focalPoint - offset) * actualFactor
        scale = newScale
    }

    /** 平移画布 */
    fun panBy(delta: Offset) {
        offset += delta
    }

    /** 设置缩放到指定值 (以视口中心为焦点) */
    fun zoomTo(targetScale: Float, viewportCenter: Offset) {
        val clamped = targetScale.coerceIn(MIN_SCALE, MAX_SCALE)
        val factor = clamped / scale
        offset = viewportCenter - (viewportCenter - offset) * factor
        scale = clamped
    }

    /** 缩放百分比 */
    val scalePercent: Int get() = (scale * 100).toInt()
}
