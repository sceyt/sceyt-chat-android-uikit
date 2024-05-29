package com.sceyt.chatuikit.sceytstyles

import android.content.Context

fun interface StyleCustomizer<S> {
    fun apply(context: Context, style: S): S
}