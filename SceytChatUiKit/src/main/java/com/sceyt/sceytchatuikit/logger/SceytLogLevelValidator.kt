package com.sceyt.sceytchatuikit.logger

import com.sceyt.sceytchatuikit.logger.SceytLogLevel.ALL
import com.sceyt.sceytchatuikit.logger.SceytLogLevel.DEBUG
import com.sceyt.sceytchatuikit.logger.SceytLogLevel.ERROR
import com.sceyt.sceytchatuikit.logger.SceytLogLevel.NONE
import com.sceyt.sceytchatuikit.logger.SceytLogLevel.WARNING

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

