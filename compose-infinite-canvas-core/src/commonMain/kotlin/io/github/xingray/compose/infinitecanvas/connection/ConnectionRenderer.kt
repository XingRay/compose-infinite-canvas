package io.github.xingray.compose.infinitecanvas.connection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.xingray.compose.infinitecanvas.AnchorPosition
import kotlin.math.abs

/**
 * 绘制贝塞尔曲线连接线
 * @param from 起点 (屏幕坐标)
 * @param to 终点 (屏幕坐标)
 * @param fromAnchor 起点锚点方向
 * @param toAnchor 终点锚点方向 (null 表示正在拖拽，自动推断)
 */
fun DrawScope.drawBezierConnection(
    from: Offset,
    to: Offset,
    fromAnchor: AnchorPosition,
    toAnchor: AnchorPosition?,
    color: Color,
    scale: Float,
) {
    val distance = abs(to.x - from.x) + abs(to.y - from.y)
    val controlOffset = (distance * 0.4f).coerceIn(30f * scale, 200f * scale)

    val cp1 = controlPointForAnchor(from, fromAnchor, controlOffset)
    val cp2 = if (toAnchor != null) {
        controlPointForAnchor(to, toAnchor, controlOffset)
    } else {
        // 拖拽中: 终点控制点向起点方向偏移
        val reverseAnchor = when (fromAnchor) {
            AnchorPosition.Top -> AnchorPosition.Bottom
            AnchorPosition.Bottom -> AnchorPosition.Top
            AnchorPosition.Left -> AnchorPosition.Right
            AnchorPosition.Right -> AnchorPosition.Left
        }
        controlPointForAnchor(to, reverseAnchor, controlOffset)
    }

    val path = Path().apply {
        moveTo(from.x, from.y)
        cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, to.x, to.y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = 2.5f * scale,
            cap = StrokeCap.Round,
        )
    )

    // 终点箭头
    drawArrowHead(to, cp2, color, scale)
}

/** 根据锚点方向计算控制点 */
private fun controlPointForAnchor(point: Offset, anchor: AnchorPosition, offset: Float): Offset {
    return when (anchor) {
        AnchorPosition.Top -> Offset(point.x, point.y - offset)
        AnchorPosition.Bottom -> Offset(point.x, point.y + offset)
        AnchorPosition.Left -> Offset(point.x - offset, point.y)
        AnchorPosition.Right -> Offset(point.x + offset, point.y)
    }
}

/** 在终点画箭头 */
private fun DrawScope.drawArrowHead(tip: Offset, controlPoint: Offset, color: Color, scale: Float) {
    val arrowSize = 8f * scale
    // 箭头方向: 从控制点指向终点
    val dx = tip.x - controlPoint.x
    val dy = tip.y - controlPoint.y
    val len = kotlin.math.sqrt(dx * dx + dy * dy)
    if (len < 1f) return

    val ux = dx / len
    val uy = dy / len

    // 箭头两翼
    val wing1 = Offset(
        tip.x - arrowSize * ux + arrowSize * 0.5f * uy,
        tip.y - arrowSize * uy - arrowSize * 0.5f * ux,
    )
    val wing2 = Offset(
        tip.x - arrowSize * ux - arrowSize * 0.5f * uy,
        tip.y - arrowSize * uy + arrowSize * 0.5f * ux,
    )

    val arrowPath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(wing1.x, wing1.y)
        lineTo(wing2.x, wing2.y)
        close()
    }
    drawPath(arrowPath, color)
}
