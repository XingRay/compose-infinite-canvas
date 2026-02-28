package cn.nanosecond.demo.canvas

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * 将菜单定位在 screenPosition 处，并自动调整确保不超出 canvasSize 边界。
 * 使用 layout 修饰符在测量阶段获取菜单实际尺寸，避免估算高度不准的问题。
 */
fun Modifier.menuPosition(
    screenPosition: Offset,
    canvasSize: IntSize,
    marginPx: Int = 24,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)

    var x = screenPosition.x.roundToInt()
    var y = screenPosition.y.roundToInt()

    // 右边界
    if (x + placeable.width + marginPx > canvasSize.width) {
        x = (canvasSize.width - placeable.width - marginPx).coerceAtLeast(marginPx)
    }
    // 下边界
    if (y + placeable.height + marginPx > canvasSize.height) {
        y = (canvasSize.height - placeable.height - marginPx).coerceAtLeast(marginPx)
    }

    layout(placeable.width, placeable.height) {
        placeable.place(x, y)
    }
}
