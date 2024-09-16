package com.sceyt.chatuikit.styles

import android.content.Context

fun interface StyleCustomizer<S> {
    fun apply(context: Context, style: S): S
}