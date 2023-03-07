package com.sceyt.sceytchatuikit.presentation.customviews

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig

class SceytCustomFloatingActonButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FloatingActionButton(context, attrs, defStyleAttr) {

    init {
        setEnabledOrNot(enabled = isClickable)
    }

    fun setEnabledOrNot(enabled: Boolean) {
        isClickable = enabled
        backgroundTintList = if (enabled) {
            ColorStateList.valueOf(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        } else {
            ColorStateList.valueOf(context.getCompatColor(R.color.sceyt_color_disabled))
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        val oldState = isClickable
        super.setOnClickListener(l)
        isClickable = oldState
    }
}