package cn.nanosecond.compose.infinitecanvas

enum class AnchorPosition {
    Top, Bottom, Left, Right
}

data class Connection(
    val id: String = generateId(),
    val fromElementId: String,
    val fromAnchor: AnchorPosition,
    val toElementId: String,
    val toAnchor: AnchorPosition,
)
