package com.sceyt.chatuikit.sceytstyles

fun interface StyleCustomizer<S> {
    fun apply(style: S): S
}