package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.appcompat.widget.Toolbar
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_STYLE

data class MenuStyle(
        @StyleRes val popupTheme: Int = UNSET_STYLE,
        @StyleRes val titleAppearance: Int = UNSET_STYLE
) {

    fun apply(toolbar: Toolbar){
        if (popupTheme != UNSET_STYLE) {
            toolbar.popupTheme = popupTheme
        }
        if (titleAppearance != UNSET_STYLE) {
            toolbar.setTitleTextAppearance(toolbar.context, titleAppearance)
        }
    }

    internal class Builder(private val typedArray: TypedArray) {
        @StyleRes
        private var popupTheme: Int = UNSET_STYLE

        @StyleRes
        private var titleAppearance: Int = UNSET_STYLE

        fun popupTheme(@StyleableRes index: Int, @StyleRes defValue: Int = popupTheme) = apply {
            this.popupTheme = typedArray.getResourceId(index, defValue)
        }

        fun titleAppearance(
                @StyleableRes index: Int,
                @StyleRes defValue: Int = titleAppearance
        ) = apply {
            this.titleAppearance = typedArray.getResourceId(index, defValue)
        }

        fun build() = MenuStyle(
            popupTheme = popupTheme,
            titleAppearance = titleAppearance
        )
    }
}