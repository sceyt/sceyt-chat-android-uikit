package com.sceyt.chatuikit.styles.common

import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import androidx.annotation.Px
import androidx.annotation.StyleableRes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_SIZE

data class BackgroundStyle(
        val background: Drawable? = null,
        @param:ColorInt val backgroundColor: Int = UNSET_COLOR,
        @param:ColorInt val borderColor: Int = UNSET_COLOR,
        @param:Px val borderWidth: Int = UNSET_SIZE,
        val shape: Shape = Shape.UnsetShape,
) {

    fun apply(view: View) {
        if (!shouldApplyStyle) return

        if (background != null) {
            view.background = background
            return
        }

        val background = GradientDrawable().apply {
            this@BackgroundStyle.shape.applyTo(this)

            if (borderWidth != UNSET_SIZE)
                setStroke(borderWidth, borderColor)

            if (backgroundColor != UNSET_COLOR) {
                view.backgroundTintList = null
                setColor(backgroundColor)
            }
        }
        view.background = background.mutate()
    }

    fun apply(view: Window?) {
        view ?: return
        if (!shouldApplyStyle) return

        if (background != null) {
            view.setBackgroundDrawable(background)
            return
        }

        val background = GradientDrawable().apply {
            this@BackgroundStyle.shape.applyTo(this)

            if (borderWidth != UNSET_SIZE)
                setStroke(borderWidth, borderColor)

            if (backgroundColor != UNSET_COLOR) {
                setColor(backgroundColor)
            }
        }
        view.setBackgroundDrawable(background.mutate())
    }

    fun apply(button: FloatingActionButton) {
        if (!shouldApplyStyle) return

        if (background != null) {
            button.background = background
            return
        }

        shape.applyTo(button)

        if (backgroundColor != UNSET_COLOR)
            button.backgroundTintList = ColorStateList.valueOf(backgroundColor)
    }

    private val shouldApplyStyle: Boolean
        get() = backgroundColor != UNSET_COLOR ||
                shape != Shape.UnsetShape || borderWidth != UNSET_SIZE ||
                background != null

    @Suppress("unused")
    internal class Builder(private val typedArray: TypedArray) {
        private var background: Drawable? = null

        @ColorInt
        private var backgroundColor: Int = UNSET_COLOR

        @ColorInt
        private var borderColor: Int = UNSET_COLOR

        @Px
        private var borderWidth: Int = UNSET_SIZE

        private var shape: Shape = Shape.UnsetShape

        fun setBackground(@StyleableRes index: Int, defValue: Drawable? = background) = apply {
            background = typedArray.getDrawable(index) ?: defValue
        }

        fun setBackgroundColor(@StyleableRes index: Int, defValue: Int = backgroundColor) = apply {
            backgroundColor = typedArray.getColor(index, defValue)
        }

        fun setBorderColor(@StyleableRes index: Int, defValue: Int = borderColor) = apply {
            borderColor = typedArray.getColor(index, defValue)
        }

        fun setBorderWidth(@StyleableRes index: Int, defValue: Int = borderWidth) = apply {
            borderWidth = typedArray.getDimensionPixelSize(index, defValue)
        }

        fun setShape(shape: Shape) = apply {
            this.shape = shape
        }

        fun build(): BackgroundStyle {
            return BackgroundStyle(
                background = background,
                backgroundColor = backgroundColor,
                borderColor = borderColor,
                borderWidth = borderWidth,
                shape = shape
            )
        }
    }
}