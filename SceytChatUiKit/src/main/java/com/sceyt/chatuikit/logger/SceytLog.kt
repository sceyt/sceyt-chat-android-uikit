package com.sceyt.chatuikit.logger


object SceytLog {
    private var logLevel = SceytLogLevel.All
    private var logger: SceytLogger = SceytLoggerImpl()

    internal fun setLogger(logLevel: SceytLogLevel, logger: SceytLogger) {
        this.logLevel = logLevel
        this.logger = logger
    }

    fun e(tag: String, message: String?, throwable: Throwable? = null) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.Error))
            logger.log(Priority.Error, tag, message, throwable)
    }

    fun e(tag: String, message: String?) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.Error))
            logger.log(Priority.Error, tag, message, null)
    }

    fun w(tag: String, message: String?, throwable: Throwable? = null) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.Warning))
            logger.log(Priority.Warning, tag, message, throwable)
    }

    fun w(tag: String, message: String?) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.Warning))
            logger.log(Priority.Warning, tag, message, null)
    }

    fun d(tag: String, message: String?, throwable: Throwable? = null) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.Debug))
            logger.log(Priority.Debug, tag, message, throwable)
    }

    fun d(tag: String, message: String?) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.Debug))
            logger.log(Priority.Debug, tag, message, null)
    }

    fun i(tag: String, message: String?, throwable: Throwable? = null) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.Info))
            logger.log(Priority.Info, tag, message, throwable)
    }

    fun i(tag: String, message: String?) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.Info))
            logger.log(Priority.Info, tag, message, null)
    }
}