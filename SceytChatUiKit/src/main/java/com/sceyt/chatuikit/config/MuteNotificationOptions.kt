package com.sceyt.chatuikit.config

import android.content.Context

fun interface MuteNotificationOptions {
    fun getOptions(context: Context): List<IntervalOption>
}