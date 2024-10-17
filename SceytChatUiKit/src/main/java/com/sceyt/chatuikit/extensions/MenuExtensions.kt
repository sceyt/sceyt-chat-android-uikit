package com.sceyt.chatuikit.extensions

import android.content.Context
import android.content.res.ColorStateList
import android.view.Menu
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.view.MenuItemCompat
import androidx.core.view.forEach

fun Menu.setIconsTint(@ColorInt color: Int) {
    forEach {
        MenuItemCompat.setIconTintList(it, ColorStateList.valueOf(color))
    }
}

fun Menu.setIconsTintColorRes(context: Context, @ColorRes colorRes: Int) {
    forEach {
        MenuItemCompat.setIconTintList(it, ColorStateList.valueOf(context.getCompatColor(colorRes)))
    }
}