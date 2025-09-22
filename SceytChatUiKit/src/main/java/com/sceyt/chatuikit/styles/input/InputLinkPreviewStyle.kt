package com.sceyt.chatuikit.styles.input

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleCustomizer
import com.sceyt.chatuikit.styles.common.TextStyle

data class InputLinkPreviewStyle(
        @param:ColorInt val backgroundColor: Int,
        @param:ColorInt val dividerColor: Int,
        val titleStyle: TextStyle,
        val descriptionStyle: TextStyle,
        val placeHolder: Drawable?
) {
    companion object {
        var styleCustomizer = StyleCustomizer<InputLinkPreviewStyle> { _, style -> style }
    }

    internal class Builder(
            private val context: Context,
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var dividerColor: Int = UNSET_COLOR
        private var titleStyle: TextStyle = TextStyle()
        private var descriptionStyle: TextStyle = TextStyle()
        private var placeHolder: Drawable? = null

        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun dividerColor(@StyleableRes index: Int, @ColorInt defValue: Int = dividerColor) = apply {
            this.dividerColor = typedArray.getColor(index, defValue)
        }

        fun titleStyle(titleStyle: TextStyle) = apply {
            this.titleStyle = titleStyle
        }

        fun descriptionStyle(descriptionStyle: TextStyle) = apply {
            this.descriptionStyle = descriptionStyle
        }

        fun placeHolder(@StyleableRes index: Int, defValue: Drawable? = placeHolder) = apply {
            this.placeHolder = typedArray.getDrawable(index) ?: defValue
        }

        fun build() = InputLinkPreviewStyle(
            backgroundColor = backgroundColor,
            dividerColor = dividerColor,
            titleStyle = titleStyle,
            descriptionStyle = descriptionStyle,
            placeHolder = placeHolder
        ).let { styleCustomizer.apply(context, it) }
    }
}