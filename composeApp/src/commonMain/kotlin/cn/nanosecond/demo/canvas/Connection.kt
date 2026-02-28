package cn.nanosecond.demo.canvas

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
