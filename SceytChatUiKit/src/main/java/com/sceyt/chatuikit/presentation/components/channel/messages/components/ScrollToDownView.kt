package com.sceyt.chatuikit.presentation.components.channel.messages.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.ScrollToBottomViewBinding
import com.sceyt.chatuikit.extensions.animationListener
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.messages_list.ScrollDownButtonStyle

class ScrollToDownView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: ScrollToBottomViewBinding
    private var changeVisibilityWithAnim = true
    private lateinit var style: ScrollDownButtonStyle

    init {
        binding = ScrollToBottomViewBinding.inflate(LayoutInflater.from(context), this)
    }

    internal fun setStyle(style: ScrollDownButtonStyle) {
        this.style = style
        style.unreadCountTextStyle.apply(binding.unreadCount)

        binding.icScrollToBottom.setImageDrawable(style.icon)

        if (style.backgroundColor != UNSET_COLOR)
            binding.icScrollToBottom.setBackgroundTint(style.backgroundColor)
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

    fun setUnreadCount(count: Long) {
        binding.unreadCount.isVisible = count > 0
        binding.unreadCount.text = style.unreadCountFormatter.format(context, count)
    }
}