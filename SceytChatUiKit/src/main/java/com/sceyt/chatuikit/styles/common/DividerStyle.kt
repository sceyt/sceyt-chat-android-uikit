package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.view.View
import androidx.annotation.ColorInt

data class DividerStyle(
        @param:ColorInt val color: Int,
        val height: Int
) {
    fun apply(view: View) {
        view.setBackgroundColor(color)
        view.layoutParams = view.layoutParams?.apply {
            this.height = this@DividerStyle.height
        }
    }

    class Builder(private val typedArray: TypedArray) {
        private var color: Int = 0
        private var height: Int = 0

        fun setColor(index: Int, defValue: Int): Builder {
            color = typedArray.getColor(index, defValue)
            return this
        }

        fun setHeight(index: Int, defValue: Int): Builder {
            height = typedArray.getDimensionPixelSize(index, defValue)
            return this
        }

        fun build(): DividerStyle {
            return DividerStyle(
                color = color,
                height = height
            )
        }
    }
}