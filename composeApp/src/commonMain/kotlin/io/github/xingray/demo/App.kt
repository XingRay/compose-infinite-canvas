package io.github.xingray.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.xingray.compose.infinitecanvas.CanvasViewModel
import io.github.xingray.compose.infinitecanvas.CardElement
import io.github.xingray.compose.infinitecanvas.CanvasOffset
import io.github.xingray.compose.infinitecanvas.CanvasSize
import io.github.xingray.compose.infinitecanvas.InfiniteCanvas

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel = remember {
            CanvasViewModel().apply {
                // 示例卡片
                addElement(
                    CardElement(
                        id = "card-1",
                        position = CanvasOffset(100f, 100f),
                        size = CanvasSize(240f, 180f),
                        title = "Project Overview",
                        content = "This is a Kotlin Multiplatform project with Compose Multiplatform for cross-platform UI.",
                    )
                )
                addElement(
                    CardElement(
                        id = "card-2",
                        position = CanvasOffset(450f, 80f),
                        size = CanvasSize(240f, 180f),
                        title = "Shared Module",
                        content = "Contains common business logic shared across all platforms.",
                        imageUrl = "placeholder",
                    )
                )
                addElement(
                    CardElement(
                        id = "card-3",
                        position = CanvasOffset(200f, 380f),
                        size = CanvasSize(240f, 160f),
                        title = "Compose App",
                        content = "UI layer targeting Android, iOS, Desktop, and Web.",
                    )
                )
                addElement(
                    CardElement(
                        id = "card-4",
                        position = CanvasOffset(550f, 380f),
                        size = CanvasSize(240f, 160f),
                        title = "Ktor Server",
                        content = "Backend server running on JVM with Netty engine.",
                    )
                )
            }
        }

        InfiniteCanvas(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
