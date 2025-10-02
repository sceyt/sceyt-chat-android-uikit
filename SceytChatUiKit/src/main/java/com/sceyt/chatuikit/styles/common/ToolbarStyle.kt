package com.sceyt.chatuikit.styles.common

import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import com.sceyt.chatuikit.presentation.custom_views.CustomToolbar
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

data class ToolbarStyle(
        @param:ColorInt val backgroundColor: Int = UNSET_COLOR,
        @param:ColorInt val underlineColor: Int = UNSET_COLOR,
        val navigationIcon: Drawable? = null,
        val menuStyle: MenuStyle = MenuStyle(),
        val titleTextStyle: TextStyle = TextStyle(),
        val subtitleTextStyle: TextStyle = TextStyle(),
) {
    /**
     * Applies the style to the given [toolbar].
     * Note: This method does not apply the underline color and subtitle text style to the toolbar.
     * */
    fun apply(toolbar: Toolbar) {
        if (backgroundColor != UNSET_COLOR)
            toolbar.setBackgroundColor(backgroundColor)

        if (navigationIcon != null)
            toolbar.setNavigationIcon(navigationIcon)

        menuStyle.apply(toolbar)
        toolbar.setTitleTextColor(titleTextStyle.color)
    }

    /**
     * Applies the style to the given [toolbar].
     * */
    fun apply(toolbar: CustomToolbar) {
        if (backgroundColor != UNSET_COLOR)
            toolbar.setBackgroundColor(backgroundColor)

        if (underlineColor != UNSET_COLOR)
            toolbar.setBorderColor(underlineColor)

        if (navigationIcon != null)
            toolbar.setNavigationIcon(navigationIcon)

        menuStyle.apply(toolbar.getToolbar())
        toolbar.setTitleTextStyle(titleTextStyle)
        toolbar.setSubtitleTextStyle(subtitleTextStyle)
    }
}