package cn.nanosecond.demo.canvas

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

private val MENU_WIDTH = 200.dp

@Composable
fun CanvasContextMenu(
    state: ContextMenuState,
    canvasSize: IntSize,
    hasClipboard: Boolean,
    onDismiss: () -> Unit,
    onRightClick: (Offset) -> Unit,
    onPaste: () -> Unit,
    onSelectAll: () -> Unit,
    onExportPng: () -> Unit,
    onExportSvg: () -> Unit,
    onCopyPng: () -> Unit,
    onCopySvg: () -> Unit,
    onSubmenuToggle: (String?) -> Unit,
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
            .width(MENU_WIDTH)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(Color.White)
            .padding(vertical = 4.dp),
    ) {
        MenuItem(
            label = "粘贴",
            shortcut = "Ctrl+V",
            enabled = hasClipboard,
            onClick = { onPaste(); onDismiss() },
        )

        MenuDivider()

        MenuItem(
            label = "选中全部",
            shortcut = "Ctrl+A",
            onClick = { onSelectAll(); onDismiss() },
        )

        MenuDivider()

        ExpandableMenuItem(
            label = "导出为",
            isExpanded = state.expandedSubmenu == "export",
            onToggle = {
                onSubmenuToggle(if (state.expandedSubmenu == "export") null else "export")
            },
        ) {
            MenuItem(label = "PNG", indented = true, onClick = { onExportPng(); onDismiss() })
            MenuItem(label = "SVG", indented = true, onClick = { onExportSvg(); onDismiss() })
        }

        ExpandableMenuItem(
            label = "复制为",
            isExpanded = state.expandedSubmenu == "copy",
            onToggle = {
                onSubmenuToggle(if (state.expandedSubmenu == "copy") null else "copy")
            },
        ) {
            MenuItem(label = "PNG", indented = true, onClick = { onCopyPng(); onDismiss() })
            MenuItem(label = "SVG", indented = true, onClick = { onCopySvg(); onDismiss() })
        }
    }
}

@Composable
private fun MenuItem(
    label: String,
    shortcut: String? = null,
    enabled: Boolean = true,
    indented: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor = when {
        !enabled -> Color.Transparent
        isHovered -> Color(0xFFF0F0F0)
        else -> Color.Transparent
    }
    val textColor = if (enabled) Color(0xFF333333) else Color(0xFFBBBBBB)
    val startPadding = if (indented) 28.dp else 12.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .background(bgColor)
            .padding(start = startPadding, end = 12.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 13.sp, color = textColor)
        if (shortcut != null) {
            Text(text = shortcut, fontSize = 11.sp, color = Color(0xFFAAAAAA))
        }
    }
}

@Composable
private fun ExpandableMenuItem(
    label: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor = if (isHovered || isExpanded) Color(0xFFF0F0F0) else Color.Transparent
    val arrow = if (isExpanded) "▾" else "▸"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(onClick = onToggle)
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF333333))
        Text(text = arrow, fontSize = 11.sp, color = Color(0xFF999999))
    }

    if (isExpanded) {
        content()
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp),
        thickness = 1.dp,
        color = Color(0xFFEEEEEE),
    )
}
