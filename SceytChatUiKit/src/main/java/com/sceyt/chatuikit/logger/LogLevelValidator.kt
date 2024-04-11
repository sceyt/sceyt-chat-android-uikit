package com.sceyt.chatuikit.logger

internal interface LogLevelValidator {
    fun isLoggable(priority: Priority): Boolean
}