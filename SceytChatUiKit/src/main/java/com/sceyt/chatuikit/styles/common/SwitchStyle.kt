package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StyleableRes
import androidx.appcompat.widget.SwitchCompat
import com.sceyt.chatuikit.extensions.setColors
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_CORNER_RADIUS
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_SIZE
import com.sceyt.chatuikit.styles.common.Shape.RoundedCornerShape

data class SwitchStyle(
        val backgroundStyle: BackgroundStyle = BackgroundStyle(),
        val textStyle: TextStyle = TextStyle(),
        @param:ColorInt val checkedColor: Int,
        @param:ColorInt val thumbUncheckedColor: Int,
        @param:ColorInt val trackUncheckedColor: Int,
) {

    fun apply(switch: SwitchCompat) {
        backgroundStyle.apply(switch)
        textStyle.apply(switch)
        switch.setColors(
            checkedColor = checkedColor,
            thumbUncheckedColor = thumbUncheckedColor,
            trackUncheckedColor = trackUncheckedColor
        )
    }

    internal class Builder(private val typedArray: TypedArray) {
        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var borderColor: Int = UNSET_COLOR

        @Px
        private var borderWidth: Int = UNSET_SIZE

        @Px
        private var cornerRadius: Float = UNSET_CORNER_RADIUS

        private var textStyle: TextStyle = TextStyle()

        @ColorInt
        private var checkedColor: Int = UNSET_COLOR

        @ColorInt
        private var thumbUncheckedColor: Int = UNSET_COLOR

        @ColorInt
        private var trackUncheckedColor: Int = UNSET_COLOR

        fun setBackgroundColor(@StyleableRes index: Int, defValue: Int = backgroundColor) = apply {
            backgroundColor = typedArray.getColor(index, defValue)
        }

        fun setBorderColor(@StyleableRes index: Int, defValue: Int = borderColor) = apply {
            borderColor = typedArray.getColor(index, defValue)
        }

        fun setBorderWidth(@StyleableRes index: Int, defValue: Int = borderWidth) = apply {
            borderWidth = typedArray.getDimensionPixelSize(index, defValue)
        }

        fun setCornerRadius(@StyleableRes index: Int, defValue: Float = cornerRadius) = apply {
            cornerRadius = typedArray.getDimension(index, defValue)
        }

        fun setTextStyle(textStyle: TextStyle) = apply {
            this.textStyle = textStyle
        }

        fun setCheckedColor(@StyleableRes index: Int, defValue: Int = checkedColor) = apply {
            checkedColor = typedArray.getColor(index, defValue)
        }

        fun setThumbUncheckedColor(@StyleableRes index: Int, defValue: Int = thumbUncheckedColor) = apply {
            thumbUncheckedColor = typedArray.getColor(index, defValue)
        }

        fun setTrackUncheckedColor(@StyleableRes index: Int, defValue: Int = trackUncheckedColor) = apply {
            trackUncheckedColor = typedArray.getColor(index, defValue)
        }

        fun build() = SwitchStyle(
            backgroundStyle = BackgroundStyle(
                backgroundColor = backgroundColor,
                borderColor = borderColor,
                borderWidth = borderWidth,
                shape = if (cornerRadius != UNSET_CORNER_RADIUS)
                    RoundedCornerShape(cornerRadius) else Shape.UnsetShape,
            ),
            textStyle = textStyle,
            checkedColor = checkedColor,
            thumbUncheckedColor = thumbUncheckedColor,
            trackUncheckedColor = trackUncheckedColor
        )
    }
}
