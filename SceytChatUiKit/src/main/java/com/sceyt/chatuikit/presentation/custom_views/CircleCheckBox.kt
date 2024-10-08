package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import com.sceyt.chatuikit.styles.common.CheckboxStyle

class CircleCheckBox @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatCheckBox(context, attrs, defStyleAttr) {

    fun applyStyle(checkboxStyle: CheckboxStyle) {
        val pressedState = android.R.attr.state_pressed
        val checkedState = android.R.attr.state_checked
        buttonDrawable = StateListDrawable().apply {
            // Checked state
            addState(intArrayOf(checkedState), checkboxStyle.checkedIcon)
            // Pressed state
            addState(intArrayOf(pressedState), checkboxStyle.pressedIcon)
            // Unchecked state
            addState(intArrayOf(-pressedState), checkboxStyle.uncheckedIcon)
        }
    }
}