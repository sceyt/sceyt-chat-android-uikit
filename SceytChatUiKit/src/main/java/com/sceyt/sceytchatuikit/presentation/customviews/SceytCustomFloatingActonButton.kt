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

    private var isEnabledClick: Boolean = isClickable
    private var localClickListener: OnClickListener? = null

    init {
        setEnabledOrNot(enabled = isEnabledClick)
    }

    fun setEnabledOrNot(enabled: Boolean) {
        isEnabledClick = enabled
        backgroundTintList = if (enabled) {
            ColorStateList.valueOf(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        } else {
            ColorStateList.valueOf(context.getCompatColor(R.color.sceyt_color_disabled))
        }
    }

    private fun initClickListener() {
        if (localClickListener != null) return
        val clickListener = OnClickListener {
            if (!isEnabledClick) return@OnClickListener
            else localClickListener?.onClick(it)
        }
        super.setOnClickListener(clickListener)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        localClickListener = null
        initClickListener()
        localClickListener = l
    }
}