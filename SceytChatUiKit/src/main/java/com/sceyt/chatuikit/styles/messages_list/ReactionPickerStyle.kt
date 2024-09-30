package com.sceyt.chatuikit.styles.messages_list

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes

data class ReactionPickerStyle(
        @ColorInt val backgroundColor: Int,
        @ColorInt val moreBackgroundColor: Int,
        @ColorInt val selectedBackgroundColor: Int,
        val moreIcon: Drawable?,
) {
    internal class Builder(
            private val typedArray: TypedArray
    ) {
        @ColorInt
        private var backgroundColor: Int = 0

        @ColorInt
        private var moreBackgroundColor: Int = 0

        @ColorInt
        private var selectedBackgroundColor: Int = 0
        private var moreIcon: Drawable? = null

        fun backgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = backgroundColor) = apply {
            this.backgroundColor = typedArray.getColor(index, defValue)
        }

        fun moreBackgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = moreBackgroundColor) = apply {
            this.moreBackgroundColor = typedArray.getColor(index, defValue)
        }

        fun selectedBackgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = selectedBackgroundColor) = apply {
            this.selectedBackgroundColor = typedArray.getColor(index, defValue)
        }

        fun moreIcon(@StyleableRes index: Int, defValue: Drawable? = moreIcon) = apply {
            this.moreIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun build() = ReactionPickerStyle(
            backgroundColor = backgroundColor,
            moreBackgroundColor = moreBackgroundColor,
            selectedBackgroundColor = selectedBackgroundColor,
            moreIcon = moreIcon
        )
    }
}