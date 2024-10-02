package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.presentation.custom_views.CustomToolbar

data class ToolbarStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val borderColor: Int,
        val navigationIcon: Drawable?,
        val titleTextStyle: TextStyle,
) {

    fun apply(toolbar: CustomToolbar) {
        toolbar.setBackgroundColor(backgroundColor)
        toolbar.setTitleTextStyle(titleTextStyle)
        toolbar.setBorderColor(borderColor)
        toolbar.setNavigationIcon(navigationIcon)
    }
}