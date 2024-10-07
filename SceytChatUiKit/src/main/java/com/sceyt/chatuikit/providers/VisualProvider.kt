package com.sceyt.chatuikit.providers

import android.content.Context

fun interface VisualProvider<From, To> {
    fun provide(context: Context, from: From): To
}