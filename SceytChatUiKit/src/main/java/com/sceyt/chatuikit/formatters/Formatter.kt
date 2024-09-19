package com.sceyt.chatuikit.formatters

import android.content.Context

interface Formatter<T> {
    fun format(context: Context, from: T): String
}