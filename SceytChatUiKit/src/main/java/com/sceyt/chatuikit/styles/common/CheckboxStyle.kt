package com.sceyt.chatuikit.styles.common

import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.widget.CheckBox
import androidx.annotation.StyleableRes

data class CheckboxStyle(
        val checkedIcon: Drawable? = null,
        val uncheckedIcon: Drawable? = null,
        val pressedIcon: Drawable? = null,
) {

    fun apply(checkbox: CheckBox) {
        checkbox.background = null
        checkbox.buttonDrawable = createButtonDrawable()
    }

    private fun createButtonDrawable(): Drawable {
        val pressedState = android.R.attr.state_pressed
        val checkedState = android.R.attr.state_checked
        return StateListDrawable().apply {
            // Checked state
            addState(intArrayOf(checkedState), checkedIcon)
            // Pressed state
            addState(intArrayOf(pressedState), pressedIcon)
            // Unchecked state
            addState(intArrayOf(), uncheckedIcon)
        }
    }

    internal class Builder(private val typedArray: TypedArray) {
        private var checkedIcon: Drawable? = null
        private var uncheckedIcon: Drawable? = null
        private var pressedIcon: Drawable? = null

        fun checkedIcon(@StyleableRes index: Int, defValue: Drawable? = checkedIcon) = apply {
            checkedIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun uncheckedIcon(@StyleableRes index: Int, defValue: Drawable? = uncheckedIcon) = apply {
            uncheckedIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun pressedIcon(@StyleableRes index: Int, defValue: Drawable? = pressedIcon) = apply {
            pressedIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun build(): CheckboxStyle {
            return CheckboxStyle(
                checkedIcon = checkedIcon,
                uncheckedIcon = uncheckedIcon,
                pressedIcon = pressedIcon,
            )
        }
    }
}