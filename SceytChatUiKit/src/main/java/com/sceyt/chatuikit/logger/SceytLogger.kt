package com.sceyt.chatuikit.logger

fun interface SceytLogger {
    fun log(priority: Priority, tag: String?, message: String?, throwable: Throwable?)
}


