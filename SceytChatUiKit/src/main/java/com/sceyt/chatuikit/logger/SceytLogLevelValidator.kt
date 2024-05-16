package com.sceyt.chatuikit.logger

import com.sceyt.chatuikit.logger.SceytLogLevel.ALL
import com.sceyt.chatuikit.logger.SceytLogLevel.DEBUG
import com.sceyt.chatuikit.logger.SceytLogLevel.ERROR
import com.sceyt.chatuikit.logger.SceytLogLevel.NONE
import com.sceyt.chatuikit.logger.SceytLogLevel.WARNING

internal class SceytLogLevelValidator(private val logLevel: SceytLogLevel) : LogLevelValidator {

    override fun isLoggable(priority: Priority): Boolean {
        return when (logLevel) {
            NONE -> false
            ALL -> true
            DEBUG -> priority.level >= Priority.DEBUG.level
            WARNING -> priority.level >= Priority.WARNING.level
            ERROR -> priority.level >= Priority.ERROR.level
        }
    }
}

