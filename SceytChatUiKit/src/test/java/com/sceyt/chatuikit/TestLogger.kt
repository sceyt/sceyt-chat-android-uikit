package com.sceyt.chatuikit

import com.sceyt.chatuikit.logger.Priority
import com.sceyt.chatuikit.logger.SceytLogger

/**
 * A simple test logger for unit tests
 */
class TestLogger : SceytLogger {
    override fun log(priority: Priority, tag: String?, message: String?, throwable: Throwable?) {
        println("[$priority] $tag: $message ${throwable?.message ?: ""}")
    }
}