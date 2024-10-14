package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import com.sceyt.chatuikit.presentation.custom_views.CustomToolbar
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

data class ToolbarStyle(
        @ColorInt val backgroundColor: Int = UNSET_COLOR,
        @ColorInt val underlineColor: Int = UNSET_COLOR,
        val navigationIcon: Drawable? = null,
        val titleTextStyle: TextStyle = TextStyle(),
) {

    fun apply(toolbar: CustomToolbar) {
        toolbar.setBackgroundColor(backgroundColor)
        toolbar.setTitleTextStyle(titleTextStyle)
        toolbar.setBorderColor(underlineColor)
        toolbar.setNavigationIcon(navigationIcon)
    }
}