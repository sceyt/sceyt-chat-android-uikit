package com.sceyt.sceytchatuikit.logger

import android.util.Log

class SceytLoggerImpl : SceytLogger {

    override fun log(priority: Priority, tag: String?, message: String?, throwable: Throwable?) {
        when (priority) {
            Priority.VERBOSE -> Log.v(tag, message, throwable)
            Priority.DEBUG -> Log.d(tag, message, throwable)
            Priority.INFO -> Log.i(tag, message, throwable)
            Priority.WARNING -> Log.w(tag, message, throwable)
            Priority.ERROR, Priority.ASSERT -> Log.e(tag, message, throwable)
        }
    }
}