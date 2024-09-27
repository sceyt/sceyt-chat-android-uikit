package com.sceyt.chatuikit.providers

import android.content.Context

interface VisualProvider<From, To> {
    fun provide(context: Context, from: From): To
}