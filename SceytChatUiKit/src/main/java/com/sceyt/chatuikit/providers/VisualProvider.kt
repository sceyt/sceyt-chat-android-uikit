 package com.sceyt.chatuikit.providers

interface VisualProvider<From, To> {
    fun provide(from: From): To
}