package com.sceyt.chatuikit.formatters

import android.content.Context

fun interface Formatter<T> {
    fun format(context: Context, from: T): CharSequence
}

val NoFormatter = Formatter<Nothing> { _, _ -> "" }