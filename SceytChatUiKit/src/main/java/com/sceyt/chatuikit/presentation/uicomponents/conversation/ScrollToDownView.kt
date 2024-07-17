package com.sceyt.chatuikit.presentation.uicomponents.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.ScrollToBottomViewBinding
import com.sceyt.chatuikit.extensions.animationListener
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.extensions.setTintColorRes
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle

class ScrollToDownView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: ScrollToBottomViewBinding
    private var changeVisibilityWithAnim = true

    init {
        binding = ScrollToBottomViewBinding.inflate(LayoutInflater.from(context), this)
    }

    internal fun setStyle(style: MessagesListViewStyle) {
        binding.unreadCount.setBackgroundTint(style.downScrollerUnreadCountColor)
        binding.fabNext.setTintColorRes(SceytChatUIKit.theme.iconSecondaryColor)
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