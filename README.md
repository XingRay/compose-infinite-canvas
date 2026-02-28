# Compose Infinite Canvas

A Compose Multiplatform infinite canvas library for building node-based editors, whiteboards, and diagram tools.

[中文文档](#中文文档)

## Features

- **Infinite Canvas** — Pan and zoom with no boundaries
- **Card Elements** — Draggable cards with title, content, and image placeholder
- **Connections** — Bezier curve connections between element anchor points
- **Gestures** — Full gesture support:
  - Click to select, drag to move elements
  - Drag from anchor to create connections
  - Click connection to delete
  - Box select (drag on empty area)
  - Ctrl+Scroll to zoom, Scroll to pan
  - Pinch to zoom on touch devices
  - Spacebar for temporary pan mode
- **Context Menus** — Right-click menus for canvas and elements
- **Multiplatform** — Android, iOS, Desktop (JVM), Web (JS/WASM)

## Installation

### Maven Central (Recommended)

All platforms including iOS and macOS are available from Maven Central.

Add the dependency to your `build.gradle.kts`:

```kotlin
implementation("io.github.xingray:compose-infinite-canvas:0.1.2")
```

### JitPack

Alternatively, you can use JitPack. Add the repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Add the dependency:

```kotlin
implementation("com.github.XingRay:compose-infinite-canvas:0.1.2")
```

> **Note:** JitPack builds on Linux, so iOS/macOS targets are not available via JitPack.

## Usage

```kotlin
import io.github.xingray.compose.infinitecanvas.*

@Composable
fun MyCanvas() {
    val viewModel = remember {
        CanvasViewModel().apply {
            addElement(
                CardElement(
                    position = CanvasOffset(100f, 100f),
                    title = "Hello",
                    content = "This is a card on the infinite canvas.",
                )
            )
            addElement(
                CardElement(
                    position = CanvasOffset(450f, 100f),
                    title = "World",
                    content = "Connect me to the other card!",
                )
            )
        }
    }

    InfiniteCanvas(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize(),
    )
}
```

## Project Structure

```
compose-infinite-canvas/
├── infinite-canvas/          # Library module (publishable)
│   └── src/commonMain/
│       └── io.github.xingray.compose.infinitecanvas/
│           ├── CanvasElement.kt        # Element data models
│           ├── Connection.kt           # Connection data model
│           ├── ViewportState.kt        # Pan & zoom state
│           ├── CanvasViewModel.kt      # Canvas state management
│           ├── InfiniteCanvas.kt       # Main composable
│           ├── CanvasContextMenu.kt    # Canvas right-click menu
│           ├── ElementContextMenu.kt   # Element right-click menu
│           ├── MenuPositionUtils.kt    # Menu positioning
│           ├── connection/
│           │   └── ConnectionRenderer.kt   # Bezier curve rendering
│           ├── element/
│           │   └── CardElementView.kt      # Card element UI
│           └── gesture/
│               └── CanvasGestureHandler.kt # Gesture handling
├── composeApp/               # Demo application
└── jitpack.yml               # JitPack build config
```

## Build from Source

```bash
# Build the library
./gradlew :infinite-canvas:build

# Run the demo app (Desktop)
./gradlew :composeApp:run
```

## License

This project is open source. See the repository for license details.

---

# 中文文档

一个基于 Compose Multiplatform 的无限画布库，可用于构建节点编辑器、白板和图表工具。

## 功能特性

- **无限画布** — 无边界的平移和缩放
- **卡片元素** — 可拖拽的卡片，支持标题、内容和图片占位
- **连接线** — 基于贝塞尔曲线的锚点连接
- **手势操作** — 完整的手势支持：
  - 点击选中，拖拽移动元素
  - 从锚点拖拽创建连接线
  - 点击连接线删除
  - 框选（在空白区域拖拽）
  - Ctrl+滚轮缩放，滚轮平移
  - 触屏双指缩放
  - 空格键临时切换平移模式
- **右键菜单** — 画布和元素的上下文菜单
- **多平台** — Android、iOS、桌面端 (JVM)、Web (JS/WASM)

## 安装

### Maven Central（推荐）

所有平台（包括 iOS 和 macOS）均可从 Maven Central 获取。

在 `build.gradle.kts` 中添加依赖：

```kotlin
implementation("io.github.xingray:compose-infinite-canvas:0.1.2")
```

### JitPack

也可以使用 JitPack。在 `settings.gradle.kts` 中添加仓库：

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

添加依赖：

```kotlin
implementation("com.github.XingRay:compose-infinite-canvas:0.1.2")
```

> **注意：** JitPack 在 Linux 上构建，无法编译 iOS/macOS 目标。

## 使用示例

```kotlin
import io.github.xingray.compose.infinitecanvas.*

@Composable
fun MyCanvas() {
    val viewModel = remember {
        CanvasViewModel().apply {
            addElement(
                CardElement(
                    position = CanvasOffset(100f, 100f),
                    title = "你好",
                    content = "这是无限画布上的一张卡片。",
                )
            )
            addElement(
                CardElement(
                    position = CanvasOffset(450f, 100f),
                    title = "世界",
                    content = "把我和另一张卡片连接起来！",
                )
            )
        }
    }

    InfiniteCanvas(
        viewModel = viewModel,
        modifier = Modifier.fillMaxSize(),
    )
}
```

## 从源码构建

```bash
# 构建库
./gradlew :infinite-canvas:build

# 运行示例应用（桌面端）
./gradlew :composeApp:run
```
