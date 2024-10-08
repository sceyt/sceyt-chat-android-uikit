package com.sceyt.chatuikit.logger

enum class Priority(
        val level: Int
) {
    Verbose(1),
    Debug(2),
    Info(3),
    Warning(4),
    Error(5),
    Assert(6)
}
