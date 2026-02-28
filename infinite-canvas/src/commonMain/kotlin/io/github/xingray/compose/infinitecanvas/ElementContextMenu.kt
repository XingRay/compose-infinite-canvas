package io.github.xingray.compose.infinitecanvas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ELEMENT_MENU_WIDTH = 180.dp

@Composable
fun ElementContextMenu(
    state: ContextMenuState,
    canvasSize: IntSize,
    onDismiss: () -> Unit,
    onRightClick: (Offset) -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
) {
    // 点击外部关闭: 左键关闭，右键直接打开新菜单
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            if (event.buttons.isSecondaryPressed) {
                                val pos = event.changes.firstOrNull()?.position
                                if (pos != null) onRightClick(pos)
                            } else {
                                onDismiss()
                            }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
    )

    // 菜单本体
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .menuPosition(state.screenPosition, canvasSize)
            .width(ELEMENT_MENU_WIDTH)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(Color.White)
            .padding(vertical = 4.dp),
    ) {
        ElementMenuItem(label = "复制", shortcut = "Ctrl+C", onClick = { onCopy(); onDismiss() })
        ElementMenuItem(label = "剪切", shortcut = "Ctrl+X", onClick = { onCut(); onDismiss() })
        ElementMenuItem(label = "复制副本", shortcut = "Ctrl+D", onClick = { onDuplicate(); onDismiss() })

        ElementMenuDivider()

        ElementMenuItem(label = "置于顶层", onClick = { onBringToFront(); onDismiss() })
        ElementMenuItem(label = "置于底层", onClick = { onSendToBack(); onDismiss() })

        ElementMenuDivider()

        ElementMenuItem(
            label = "删除",
            shortcut = "Del",
            color = Color(0xFFE53935),
            onClick = { onDelete(); onDismiss() },
        )
    }
}

@Composable
private fun ElementMenuItem(
    label: String,
    shortcut: String? = null,
    color: Color = Color(0xFF333333),
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor = if (isHovered) Color(0xFFF0F0F0) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 13.sp, color = color)
        if (shortcut != null) {
            Text(text = shortcut, fontSize = 11.sp, color = Color(0xFFAAAAAA))
        }
    }
}

@Composable
private fun ElementMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp),
        thickness = 1.dp,
        color = Color(0xFFEEEEEE),
    )
}
