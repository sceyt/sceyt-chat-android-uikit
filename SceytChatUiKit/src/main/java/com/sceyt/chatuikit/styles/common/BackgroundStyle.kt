package com.sceyt.chatuikit.styles.common

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_SIZE

data class BackgroundStyle(
        val background: Drawable? = null,
        @ColorInt val backgroundColor: Int = UNSET_COLOR,
        @ColorInt val borderColor: Int = UNSET_COLOR,
        @Px val borderWidth: Int = UNSET_SIZE,
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
}