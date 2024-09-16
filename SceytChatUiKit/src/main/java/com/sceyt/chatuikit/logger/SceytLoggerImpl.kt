package com.sceyt.chatuikit.logger

import android.util.Log

class SceytLoggerImpl : SceytLogger {

    override fun log(priority: Priority, tag: String?, message: String?, throwable: Throwable?) {
        when (priority) {
            Priority.Verbose -> Log.v(tag, message, throwable)
            Priority.Debug -> Log.d(tag, message, throwable)
            Priority.Info -> Log.i(tag, message, throwable)
            Priority.Warning -> Log.w(tag, message, throwable)
            Priority.Error, Priority.Assert -> Log.e(tag, message, throwable)
        }
    }
}