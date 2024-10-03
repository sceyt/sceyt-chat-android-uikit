package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.View.OnClickListener
import androidx.annotation.ColorInt
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setBackgroundTint

class CustomFloatingActonButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private var isEnabledClick: Boolean = isClickable
    private var localClickListener: OnClickListener? = null
    private var buttonColor: Int = context.getCompatColor(SceytChatUIKit.theme.colors.accentColor)

    init {
        if (!isEnabledClick)
            setEnabledOrNot(false)
    }

    fun setEnabledOrNot(enabled: Boolean) {
        isEnabledClick = enabled
        setBackgroundTint(if (enabled)
            buttonColor else context.getCompatColor(SceytChatUIKit.theme.colors.iconInactiveColor))
    }

    private fun initClickListener() {
        if (localClickListener != null) return
        val clickListener = OnClickListener {
            if (!isEnabledClick) return@OnClickListener
            else localClickListener?.onClick(it)
        }
        super.setOnClickListener(clickListener)
    }

    fun setButtonColor(@ColorInt color: Int){
        buttonColor = color
    }

    override fun setOnClickListener(l: OnClickListener?) {
        localClickListener = null
        initClickListener()
        localClickListener = l
    }
}