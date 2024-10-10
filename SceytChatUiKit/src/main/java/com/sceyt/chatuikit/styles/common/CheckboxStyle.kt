package com.sceyt.chatuikit.styles.common

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.widget.CheckBox
import androidx.annotation.StyleableRes
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.applyTint
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable

data class CheckboxStyle(
        val checkedIcon: Drawable? = null,
        val uncheckedIcon: Drawable? = null,
        val pressedIcon: Drawable? = null,
) {

    fun apply(checkbox: CheckBox) {
        checkbox.buttonDrawable = createButtonDrawable().mutate()
    }

    private fun createButtonDrawable(): StateListDrawable {
        val pressedState = android.R.attr.state_pressed
        val checkedState = android.R.attr.state_checked
        return StateListDrawable().apply {
            // Checked state
            addState(intArrayOf(checkedState), checkedIcon)
            // Pressed state
            addState(intArrayOf(pressedState), pressedIcon)
            // Unchecked state
            addState(intArrayOf(-pressedState), uncheckedIcon)
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

    companion object {
        internal fun default(context: Context) = CheckboxStyle(
            checkedIcon = context.getCompatDrawable(R.drawable.sceyt_ic_checked_state).applyTint(
                context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
            ),
            uncheckedIcon = context.getCompatDrawable(R.drawable.sceyt_ic_unchecked_state).applyTint(
                context.getCompatColor(SceytChatUIKit.theme.colors.iconSecondaryColor)
            ),
            pressedIcon = context.getCompatDrawable(R.drawable.sceyt_ic_pressed_state).applyTint(
                context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)
            ),
        )
    }
}