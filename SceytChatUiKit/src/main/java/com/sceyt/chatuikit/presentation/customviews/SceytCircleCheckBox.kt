package com.sceyt.chatuikit.presentation.customviews

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatCheckBox
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig

class SceytCircleCheckBox @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : AppCompatCheckBox(context, attrs, defStyleAttr) {

    init {
        buttonDrawable = createStateListDrawable()
    }

    private fun createStateListDrawable(): Drawable {
        val pressedState = android.R.attr.state_pressed
        val checkedState = android.R.attr.state_checked
        val colorCheckBox = getCompatColor(SceytKitConfig.sceytColorAccent)
        val stateListDrawable = StateListDrawable()
        // Checked state
        val circleDrawable = ShapeDrawable(OvalShape())
        circleDrawable.paint.color = colorCheckBox

        val checkmarkDrawable = context.getCompatDrawable(R.drawable.sceyt_ic_checkmark)
        val layers = arrayOf(circleDrawable, checkmarkDrawable)
        val layerDrawable = LayerDrawable(layers)
        stateListDrawable.addState(intArrayOf(checkedState), layerDrawable)

        // Pressed state
        val pressedDrawable = context.getCompatDrawable(R.drawable.sceyt_radio_button_pressed_state)
        pressedDrawable?.setTint(colorCheckBox)
        stateListDrawable.addState(intArrayOf(pressedState), pressedDrawable)

        // Unchecked state
        val uncheckedDrawable = context.getCompatDrawable(R.drawable.sceyt_radio_button_unchecked_state)
        // An empty array {} means that this state has no state, it's the default
        stateListDrawable.addState(intArrayOf(-pressedState), uncheckedDrawable)

        return stateListDrawable
    }
}