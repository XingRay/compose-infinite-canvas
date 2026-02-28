package cn.nanosecond.compose.infinitecanvas.element

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.nanosecond.compose.infinitecanvas.AnchorPosition
import cn.nanosecond.compose.infinitecanvas.CardElement
import cn.nanosecond.compose.infinitecanvas.ViewportState

@Composable
fun CardElementView(
    card: CardElement,
    viewport: ViewportState,
    isSelected: Boolean,
) {
    val density = LocalDensity.current
    val screenPos = viewport.worldToScreen(
        Offset(card.position.x, card.position.y)
    )
    val screenWidth = card.size.width * viewport.scale
    val screenHeight = card.size.height * viewport.scale

    val xDp = with(density) { screenPos.x.toDp() }
    val yDp = with(density) { screenPos.y.toDp() }
    val widthDp = with(density) { screenWidth.toDp() }
    val heightDp = with(density) { screenHeight.toDp() }

    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isSelected) Color(0xFF2196F3) else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp

    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .size(width = widthDp, height = heightDp)
    ) {
        // 卡片本体
        Column(
            modifier = Modifier
                .fillMaxSize()
                .shadow(4.dp, shape)
                .clip(shape)
                .background(Color.White)
                .border(borderWidth, borderColor, shape)
                .padding(12.dp)
        ) {
            if (card.title.isNotBlank()) {
                Text(
                    text = card.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = (14 * viewport.scale).sp.takeIf { viewport.scale > 0.3f } ?: 4.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF212121),
                )
                Spacer(Modifier.height(4.dp))
            }

            if (card.content.isNotBlank()) {
                Text(
                    text = card.content,
                    fontSize = (12 * viewport.scale).sp.takeIf { viewport.scale > 0.3f } ?: 3.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF666666),
                    lineHeight = (16 * viewport.scale).sp,
                )
            }

            if (card.imageUrl != null) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Image",
                        color = Color(0xFF90CAF9),
                        fontSize = (10 * viewport.scale).sp,
                    )
                }
            }
        }

        // 选中时显示锚点
        if (isSelected) {
            AnchorDot(Modifier.align(Alignment.TopCenter), AnchorPosition.Top)
            AnchorDot(Modifier.align(Alignment.BottomCenter), AnchorPosition.Bottom)
            AnchorDot(Modifier.align(Alignment.CenterStart), AnchorPosition.Left)
            AnchorDot(Modifier.align(Alignment.CenterEnd), AnchorPosition.Right)
        }
    }
}

/** 锚点圆点 (纯视觉，手势在画布层统一处理) */
@Composable
private fun AnchorDot(modifier: Modifier, anchor: AnchorPosition) {
    Box(
        modifier = modifier
            .size(14.dp)
            .offset(
                x = when (anchor) {
                    AnchorPosition.Left -> (-7).dp
                    AnchorPosition.Right -> 7.dp
                    else -> 0.dp
                },
                y = when (anchor) {
                    AnchorPosition.Top -> (-7).dp
                    AnchorPosition.Bottom -> 7.dp
                    else -> 0.dp
                },
            )
            .clip(CircleShape)
            .background(Color(0xFF2196F3))
            .border(2.dp, Color.White, CircleShape)
    )
}
