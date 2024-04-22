package com.sceyt.chatuikit.presentation.customviews

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View.OnClickListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor

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
            ColorStateList.valueOf(context.getCompatColor(SceytChatUIKit.theme.accentColor))
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