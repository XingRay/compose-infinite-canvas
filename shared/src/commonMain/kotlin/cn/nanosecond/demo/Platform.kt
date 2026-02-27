package cn.nanosecond.demo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform