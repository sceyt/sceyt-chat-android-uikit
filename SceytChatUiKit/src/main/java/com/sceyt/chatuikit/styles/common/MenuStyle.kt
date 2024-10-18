package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.view.Menu
import androidx.annotation.MenuRes
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.appcompat.widget.Toolbar
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_RESOURCE
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_STYLE

data class MenuStyle(
        @StyleRes val popupTheme: Int = UNSET_STYLE,
        @StyleRes val titleAppearance: Int = UNSET_STYLE,
        @MenuRes val menuRes: Int = UNSET_RESOURCE,
        val overFlowIcon: Drawable? = null,
        val menuCustomizer: Menu.() -> Unit = {},
) {

    fun apply(toolbar: Toolbar) {
        if (popupTheme != UNSET_STYLE) {
            toolbar.popupTheme = popupTheme
        }
        if (titleAppearance != UNSET_STYLE) {
            toolbar.setTitleTextAppearance(toolbar.context, titleAppearance)
        }

        if (menuRes != UNSET_RESOURCE) {
            toolbar.inflateMenu(menuRes)
            toolbar.getMenu().menuCustomizer()
        }

        if (overFlowIcon != null) {
            toolbar.overflowIcon = overFlowIcon
        }
    }

    internal class Builder(private val typedArray: TypedArray) {
        @StyleRes
        private var popupTheme: Int = UNSET_STYLE

        @StyleRes
        private var titleAppearance: Int = UNSET_STYLE

        @MenuRes
        private var menuRes: Int = UNSET_RESOURCE

        private var overFlowIcon: Drawable? = null

        private var menuCustomizer: Menu.() -> Unit = {}

        fun popupTheme(@StyleableRes index: Int, @StyleRes defValue: Int = popupTheme) = apply {
            this.popupTheme = typedArray.getResourceId(index, defValue)
        }

        fun titleAppearance(
                @StyleableRes index: Int,
                @StyleRes defValue: Int = titleAppearance,
        ) = apply {
            this.titleAppearance = typedArray.getResourceId(index, defValue)
        }

        fun menuRes(@StyleableRes index: Int, @MenuRes defValue: Int = menuRes) = apply {
            this.menuRes = typedArray.getResourceId(index, defValue)
        }

        fun overFlowIcon(@StyleableRes index: Int, defValue: Drawable? = overFlowIcon) = apply {
            this.overFlowIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun menuCustomizer(menuCustomizer: Menu.() -> Unit) = apply {
            this.menuCustomizer = menuCustomizer
        }

        fun build() = MenuStyle(
            popupTheme = popupTheme,
            titleAppearance = titleAppearance,
            menuRes = menuRes,
            overFlowIcon = overFlowIcon,
            menuCustomizer = menuCustomizer
        )
    }
}