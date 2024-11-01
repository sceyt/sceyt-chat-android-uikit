package com.sceyt.chatuikit.styles.common

import android.content.Context
import android.content.res.ColorStateList
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
        val buttonTint: ColorStateList? = null,
        val textStyle: TextStyle? = null,
) {

    fun apply(checkbox: CheckBox) {
        if (shouldApplyButtonDrawable())
            checkbox.buttonDrawable = createButtonDrawable().mutate()

        if (buttonTint != null)
            checkbox.buttonTintList = buttonTint

        textStyle?.apply(checkbox)
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
            addState(intArrayOf(-pressedState), uncheckedIcon)
        }
    }

    private fun shouldApplyButtonDrawable() = checkedIcon != null
            || uncheckedIcon != null || pressedIcon != null

    internal class Builder(private val typedArray: TypedArray) {
        private var checkedIcon: Drawable? = null
        private var uncheckedIcon: Drawable? = null
        private var pressedIcon: Drawable? = null
        private var buttonTint: ColorStateList? = null
        private var textStyle: TextStyle? = null

        fun checkedIcon(@StyleableRes index: Int, defValue: Drawable? = checkedIcon) = apply {
            checkedIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun uncheckedIcon(@StyleableRes index: Int, defValue: Drawable? = uncheckedIcon) = apply {
            uncheckedIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun pressedIcon(@StyleableRes index: Int, defValue: Drawable? = pressedIcon) = apply {
            pressedIcon = typedArray.getDrawable(index) ?: defValue
        }

        fun buttonTint(@StyleableRes index: Int, defValue: ColorStateList? = buttonTint) = apply {
            buttonTint = typedArray.getColorStateList(index) ?: defValue
        }

        fun textStyle(textStyle: TextStyle) = apply {
            this.textStyle = textStyle
        }

        fun build(): CheckboxStyle {
            return CheckboxStyle(
                checkedIcon = checkedIcon,
                uncheckedIcon = uncheckedIcon,
                pressedIcon = pressedIcon,
                buttonTint = buttonTint,
                textStyle = textStyle,
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