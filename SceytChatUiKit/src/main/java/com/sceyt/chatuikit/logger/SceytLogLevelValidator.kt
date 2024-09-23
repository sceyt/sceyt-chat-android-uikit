package com.sceyt.chatuikit.logger

internal class SceytLogLevelValidator(
        private val logLevel: SceytLogLevel
) : LogLevelValidator {

    override fun isLoggable(priority: Priority): Boolean {
        return when (logLevel) {
            SceytLogLevel.None -> false
            SceytLogLevel.Verbose -> true
            SceytLogLevel.Debug -> priority.level >= Priority.Debug.level
            SceytLogLevel.Info -> priority.level >= Priority.Info.level
            SceytLogLevel.Warning -> priority.level >= Priority.Warning.level
            SceytLogLevel.Error -> priority.level >= Priority.Error.level
            SceytLogLevel.Fatal -> priority.level >= Priority.Assert.level
        }
    }
}

