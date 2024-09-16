package com.sceyt.chatuikit.logger

import com.sceyt.chatuikit.logger.SceytLogLevel.All
import com.sceyt.chatuikit.logger.SceytLogLevel.Debug
import com.sceyt.chatuikit.logger.SceytLogLevel.Error
import com.sceyt.chatuikit.logger.SceytLogLevel.None
import com.sceyt.chatuikit.logger.SceytLogLevel.Warning

internal class SceytLogLevelValidator(private val logLevel: SceytLogLevel) : LogLevelValidator {

    override fun isLoggable(priority: Priority): Boolean {
        return when (logLevel) {
            None -> false
            All -> true
            Debug -> priority.level >= Priority.Debug.level
            Warning -> priority.level >= Priority.Warning.level
            Error -> priority.level >= Priority.Error.level
        }
    }
}

