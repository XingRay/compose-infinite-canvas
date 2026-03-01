package io.github.xingray.compose.infinitecanvas.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.xingray.compose.infinitecanvas.*

@Composable
fun App() {
    MaterialTheme {
        val canvasState = rememberInfiniteCanvasState()

        val node1State = rememberCanvasNodeState(x = 100f, y = 100f)
        val node2State = rememberCanvasNodeState(x = 500f, y = 80f)
        val node3State = rememberCanvasNodeState(x = 200f, y = 420f)
        val node4State = rememberCanvasNodeState(x = 600f, y = 400f)
        val node5State = rememberCanvasNodeState(x = 350f, y = 650f, fixed = true, pinToFront = true)

        // Dynamic node list: additional notes added via canvas menu
        val dynamicNodes = remember { mutableStateListOf<CanvasNode>() }

        // Counter for generating unique note titles
        var noteCounter by remember { mutableIntStateOf(1) }

        val nodes = remember(node1State, node2State, node3State, node4State, node5State) {
            listOf(
                // 1) Text note card
                CanvasNode(
                    id = "note-1",
                    modifier = Modifier.width(220.dp),
                    state = node1State,
                    content = {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "Project Overview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF212121),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "A Compose Multiplatform infinite canvas library supporting custom node content and context menus.",
                                fontSize = 12.sp,
                                color = Color(0xFF666666),
                                lineHeight = 17.sp,
                            )
                        }
                    },
                    menu = { onDismiss ->
                        MenuPanel {
                            MenuItem("Edit note") { onDismiss() }
                            MenuItem("Change color") { onDismiss() }
                            MenuDivider()
                            MenuItem("Delete", color = Color(0xFFE53935)) { onDismiss() }
                        }
                    },
                ),

                // 2) Image placeholder card
                CanvasNode(
                    id = "card-2",
                    modifier = Modifier.size(240.dp, 200.dp),
                    state = node2State,
                    content = {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "Architecture Diagram",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF212121),
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE3F2FD)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Image Placeholder", color = Color(0xFF90CAF9), fontSize = 11.sp)
                            }
                        }
                    },
                    menu = { onDismiss ->
                        MenuPanel {
                            MenuItem("Replace image") { onDismiss() }
                            MenuItem("Export as PNG") { onDismiss() }
                            MenuDivider()
                            MenuItem("Delete", color = Color(0xFFE53935)) { onDismiss() }
                        }
                    },
                ),

                // 3) Status / tag card
                CanvasNode(
                    id = "status-3",
                    modifier = Modifier.width(200.dp),
                    state = node3State,
                    content = {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "In Progress",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF212121),
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Canvas refactoring task: custom content & menus support.",
                                fontSize = 12.sp,
                                color = Color(0xFF888888),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 17.sp,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                TagChip("Compose", Color(0xFF2196F3))
                                TagChip("KMP", Color(0xFF9C27B0))
                            }
                        }
                    },
                    menu = { onDismiss ->
                        MenuPanel {
                            MenuItem("Mark as Done") { onDismiss() }
                            MenuItem("Change priority") { onDismiss() }
                            MenuDivider()
                            MenuItem("Archive") { onDismiss() }
                        }
                    },
                ),

                // 4) Code snippet card
                CanvasNode(
                    id = "code-4",
                    modifier = Modifier.width(280.dp),
                    state = node4State,
                    content = {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                "Usage Example",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF212121),
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF263238))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "InfiniteCanvas(\n  state = canvasState,\n  nodes = nodes,\n  menu = { onDismiss ->\n    // canvas menu\n  }\n)",
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF80CBC4),
                                    lineHeight = 16.sp,
                                )
                            }
                        }
                    },
                    menu = { onDismiss ->
                        MenuPanel {
                            MenuItem("Copy code") { onDismiss() }
                            MenuItem("Run snippet") { onDismiss() }
                        }
                    },
                ),

                // 5) Fixed legend node (pinToFront + fixed)
                CanvasNode(
                    id = "legend-5",
                    modifier = Modifier.width(160.dp),
                    state = node5State,
                    content = {
                        Column(Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (node5State.fixed) "Legend (fixed)" else "Legend (draggable)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF212121),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LegendRow(Color(0xFF4CAF50), "Active")
                            LegendRow(Color(0xFFFFC107), "Pending")
                            LegendRow(Color(0xFFE53935), "Blocked")
                        }
                    },
                    menu = { onDismiss ->
                        MenuPanel {
                            MenuItem(
                                if (node5State.fixed) "Unlock position" else "Lock position"
                            ) {
                                node5State.fixed = !node5State.fixed
                                onDismiss()
                            }
                        }
                    },
                ),
            )
        }

        // Merge static + dynamic nodes
        val allNodes = nodes + dynamicNodes

        InfiniteCanvas(
            modifier = Modifier.fillMaxSize(),
            state = canvasState,
            nodes = allNodes,
            menu = { onDismiss ->
                MenuPanel {
                    MenuItem("Add note") {
                        val idx = noteCounter++
                        val worldPos = canvasState.viewport.screenToWorld(
                            canvasState.contextMenuState?.screenPosition
                                ?: androidx.compose.ui.geometry.Offset(200f, 200f)
                        )
                        val newState = CanvasNodeState(initialX = worldPos.x, initialY = worldPos.y)
                        dynamicNodes.add(
                            CanvasNode(
                                modifier = Modifier.width(200.dp),
                                state = newState,
                                content = {
                                    Column(Modifier.padding(14.dp)) {
                                        Text(
                                            "Note #$idx",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF212121),
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "New note added from context menu.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF888888),
                                            lineHeight = 17.sp,
                                        )
                                    }
                                },
                                menu = { dismissMenu ->
                                    MenuPanel {
                                        MenuItem("Edit") { dismissMenu() }
                                        MenuDivider()
                                        MenuItem("Delete", color = Color(0xFFE53935)) {
                                            dynamicNodes.removeAll { it.state === newState }
                                            dismissMenu()
                                        }
                                    }
                                },
                            )
                        )
                        onDismiss()
                    }
                    MenuItem("Add image") { onDismiss() }
                    MenuDivider()
                    MenuItem("Select all") {
                        canvasState.selectAll()
                        onDismiss()
                    }
                    MenuItem("Reset zoom") {
                        canvasState.viewport.zoomTo(1f, androidx.compose.ui.geometry.Offset.Zero)
                        onDismiss()
                    }
                }
            },
        )
    }
}

// --- Reusable UI components for the demo ---

@Composable
private fun TagChip(label: String, color: Color) {
    Text(
        text = label,
        fontSize = 10.sp,
        color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = Color(0xFF666666))
    }
}

// --- Menu components ---

@Composable
private fun MenuPanel(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Column(
        modifier = Modifier
            .width(180.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(Color.White)
            .padding(vertical = 4.dp),
        content = content,
    )
}

@Composable
private fun MenuItem(
    label: String,
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
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, fontSize = 13.sp, color = color)
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
