package com.sceyt.chatuikit.logger


object SceytLog {
    private var logLevel = SceytLogLevel.ALL
    private var logger: SceytLogger = SceytLoggerImpl()

    internal fun setLogger(logLevel: SceytLogLevel, logger: SceytLogger) {
        this.logLevel = logLevel
        this.logger = logger
    }

    fun e(tag: String, message: String?, throwable: Throwable? = null) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.ERROR))
            logger.log(Priority.ERROR, tag, message, throwable)
    }

    fun e(tag: String, message: String?) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.ERROR))
            logger.log(Priority.ERROR, tag, message, null)
    }

    fun w(tag: String, message: String?, throwable: Throwable? = null) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.WARNING))
            logger.log(Priority.WARNING, tag, message, throwable)
    }

    fun w(tag: String, message: String?) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.WARNING))
            logger.log(Priority.WARNING, tag, message, null)
    }

    fun d(tag: String, message: String?, throwable: Throwable? = null) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.DEBUG))
            logger.log(Priority.DEBUG, tag, message, throwable)
    }

    fun d(tag: String, message: String?) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.DEBUG))
            logger.log(Priority.DEBUG, tag, message, null)
    }

    fun i(tag: String, message: String?, throwable: Throwable? = null) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.INFO))
            logger.log(Priority.INFO, tag, message, throwable)
    }

    fun i(tag: String, message: String?) {
        if (SceytLogLevelValidator(logLevel).isLoggable(Priority.INFO))
            logger.log(Priority.INFO, tag, message, null)
    }
}