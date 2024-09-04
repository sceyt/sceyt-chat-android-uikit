package com.sceyt.chatuikit.presentation.customviews

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes

class CustomFloatingActonButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private var isEnabledClick: Boolean = isClickable
    private var localClickListener: OnClickListener? = null

    init {
        setEnabledOrNot(enabled = isEnabledClick)
    }

    fun setEnabledOrNot(enabled: Boolean) {
        isEnabledClick = enabled
        setBackgroundTintColorRes(if (enabled)
            SceytChatUIKit.theme.accentColor else SceytChatUIKit.theme.iconInactiveColor)
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