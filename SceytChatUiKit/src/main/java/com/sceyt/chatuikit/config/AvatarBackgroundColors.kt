package com.sceyt.chatuikit.config

import android.content.Context

fun interface AvatarBackgroundColors {
    fun getColors(context: Context): List<Int>
}