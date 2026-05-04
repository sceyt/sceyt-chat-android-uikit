package com.sceyt.chatuikit.formatters

import android.content.Context

fun interface TypedFormatter<T, R> {
    fun format(context: Context, from: T): R
}
