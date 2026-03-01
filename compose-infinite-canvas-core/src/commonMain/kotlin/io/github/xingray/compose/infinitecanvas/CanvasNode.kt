package io.github.xingray.compose.infinitecanvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CanvasNode(
    val id: String = generateId(),
    val modifier: Modifier = Modifier,
    val state: CanvasNodeState,
    val content: @Composable () -> Unit,
    val menu: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
)

@OptIn(ExperimentalUuidApi::class)
fun generateId(): String = Uuid.random().toString()
