package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.ScrollToBottomViewBinding
import com.sceyt.sceytchatuikit.extensions.animationListener
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle

class ScrollToDownView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {
    private var binding: ScrollToBottomViewBinding
    private var changeVisibilityWithAnim = true

    init {
        binding = ScrollToBottomViewBinding.inflate(LayoutInflater.from(context), this, true)
        if (!isInEditMode) {
            isVisible = false
            binding.initWithStyle()
        }
        setGravity()
    }

    private fun ScrollToBottomViewBinding.initWithStyle() {
        unreadCount.backgroundTintList = ColorStateList.valueOf(context.getCompatColor(MessagesStyle.downScrollerUnreadCountColor))
    }

    private fun setGravity() {
        val params = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.END
        }
        layoutParams = params
    }

    private fun setVisibilityWithAnim(visible: Boolean) {
        if (isVisible == visible) return
        if (isAttachedToWindow) {
            if (animation == null || !animation.hasStarted() || animation.hasEnded()) {
                if (visible) {
                    if (!isVisible) {
                        super.setVisibility(VISIBLE)
                        val anim = AnimationUtils.loadAnimation(context, R.anim.sceyt_anim_show_scale_with_alpha)
                        startAnimation(anim)
                    }
                } else {
                    if (isVisible) {
                        val anim = AnimationUtils.loadAnimation(context, R.anim.sceyt_anim_hide_scale_with_alpha)
                        anim.setAnimationListener(animationListener {
                            super.setVisibility(GONE)
                        })
                        startAnimation(anim)
                    }
                }
            }
        } else isVisible = visible
    }

    override fun setVisibility(visibility: Int) {
        if (changeVisibilityWithAnim && isAttachedToWindow) {
            setVisibilityWithAnim(visibility == VISIBLE)
        } else
            super.setVisibility(visibility)
    }

    fun setUnreadCount(count: Int) {
        binding.unreadCount.isVisible = count > 0
        binding.unreadCount.text = if (count > 99) {
            "99+"
        } else count.toString()
    }
}