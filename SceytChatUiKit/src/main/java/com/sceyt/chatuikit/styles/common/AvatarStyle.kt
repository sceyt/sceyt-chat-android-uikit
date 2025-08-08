package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

data class AvatarStyle(
        @param:ColorInt val avatarBackgroundColor: Int = UNSET_COLOR,
        val textStyle: TextStyle = TextStyle(),
        val shape: Shape = Shape.Circle
) {

    internal class Builder(private val typedArray: TypedArray) {
        @ColorInt
        private var avatarBackgroundColor: Int = UNSET_COLOR
        private var textStyle: TextStyle = TextStyle()
        private var shape: Shape = Shape.Circle


        fun avatarBackgroundColor(@StyleableRes index: Int, @ColorInt defValue: Int = avatarBackgroundColor) = apply {
            avatarBackgroundColor = typedArray.getColor(index, defValue)
        }

        fun textStyle(textStyle: TextStyle) = apply {
            this.textStyle = textStyle
        }

        fun shape(shape: Shape) = apply {
            this.shape = shape
        }

        fun build(): AvatarStyle {
            return AvatarStyle(
                avatarBackgroundColor = avatarBackgroundColor,
                textStyle = textStyle,
                shape = shape
            )
        }
    }

    fun apply(avatarView: AvatarView) {
        avatarView.applyStyle(this)
    }
}