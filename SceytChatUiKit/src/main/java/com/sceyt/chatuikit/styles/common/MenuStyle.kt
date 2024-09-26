package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import androidx.annotation.StyleRes
import androidx.annotation.StyleableRes
import androidx.appcompat.widget.Toolbar
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_STYLE

data class MenuStyle(
        @StyleRes val style: Int = UNSET_STYLE,
        @StyleRes val titleAppearance: Int = UNSET_STYLE
) {

    fun apply(toolbar: Toolbar){
        if (style != UNSET_STYLE) {
            toolbar.popupTheme = style
        }
        if (titleAppearance != UNSET_STYLE) {
            toolbar.setTitleTextAppearance(toolbar.context, titleAppearance)
        }
    }

    internal class Builder(private val typedArray: TypedArray) {
        @StyleRes
        private var style: Int = UNSET_STYLE

        @StyleRes
        private var titleAppearance: Int = UNSET_STYLE

        fun style(@StyleableRes index: Int, @StyleRes defValue: Int = style) = apply {
            this.style = typedArray.getResourceId(index, defValue)
        }

        fun titleAppearance(
                @StyleableRes index: Int,
                @StyleRes defValue: Int = titleAppearance
        ) = apply {
            this.titleAppearance = typedArray.getResourceId(index, defValue)
        }

        fun build() = MenuStyle(
            style = style,
            titleAppearance = titleAppearance
        )
    }
}