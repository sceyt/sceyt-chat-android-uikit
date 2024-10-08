package com.sceyt.chatuikit.config

import android.content.Context

fun interface AutoDeleteMessagesOptions {
    fun getOptions(context: Context): List<IntervalOption>
}