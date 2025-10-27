package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_BORDER_WIDTH
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR

data class AvatarStyle(
        @param:ColorInt val avatarBackgroundColor: Int = UNSET_COLOR,
        @param:ColorInt val borderColor: Int = UNSET_COLOR,
        val borderWidth: Float = UNSET_BORDER_WIDTH,
        val shape: Shape = Shape.Circle,
        val textStyle: TextStyle = TextStyle(),
) {

    internal class Builder(private val typedArray: TypedArray) {
        @ColorInt
        private var avatarBackgroundColor: Int = UNSET_COLOR
          @ColorInt
        private var borderColor: Int = 0
        private var borderWidth: Float = 0f
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

        fun borderWidth(@StyleableRes index: Int, defValue: Float = borderWidth) = apply {
            borderWidth = typedArray.getDimension(index, defValue)
        }

        fun borderColor(@StyleableRes index: Int, @ColorInt defValue: Int = borderColor) = apply {
            borderColor = typedArray.getColor(index, defValue)
        }

        fun build(): AvatarStyle {
            return AvatarStyle(
                avatarBackgroundColor = avatarBackgroundColor,
                textStyle = textStyle,
                shape = shape,
                borderWidth = borderWidth,
                borderColor = borderColor
            )
        }
    }

    fun apply(avatarView: AvatarView) {
        avatarView.applyStyle(this)
    }
}