package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.Drawable
import android.view.Menu
import androidx.annotation.ColorInt
import androidx.annotation.MenuRes
import com.sceyt.chatuikit.presentation.custom_views.CustomToolbar
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_RESOURCE

data class ToolbarStyle(
        @ColorInt val backgroundColor: Int = UNSET_COLOR,
        @ColorInt val underlineColor: Int = UNSET_COLOR,
        @MenuRes val menuRes: Int = UNSET_RESOURCE,
        val navigationIcon: Drawable? = null,
        val titleTextStyle: TextStyle = TextStyle(),
        val subtitleTextStyle: TextStyle = TextStyle(),
        val menuCustomizer: Menu.() -> Unit = {}
) {

    fun apply(toolbar: CustomToolbar) {
        if (backgroundColor != UNSET_COLOR)
            toolbar.setBackgroundColor(backgroundColor)

        if (underlineColor != UNSET_COLOR)
            toolbar.setBorderColor(underlineColor)

        if (navigationIcon != null)
            toolbar.setNavigationIcon(navigationIcon)

        if (menuRes != UNSET_RESOURCE) {
            toolbar.inflateMenu(menuRes)
            toolbar.getMenu().menuCustomizer()
        }

        toolbar.setTitleTextStyle(titleTextStyle)
        toolbar.setSubtitleTextStyle(subtitleTextStyle)
    }
}