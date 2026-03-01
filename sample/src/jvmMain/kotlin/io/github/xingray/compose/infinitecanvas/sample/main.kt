package io.github.xingray.compose.infinitecanvas.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "InfiniteCanvas Sample",
    ) {
        App()
    }
}
