package com.sceyt.sceytchatuikit.logger

internal interface LogLevelValidator {
    fun isLoggable(priority: Priority): Boolean
}