package com.sceyt.chatuikit.presentation.custom_views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.databinding.ScrollerViewBinding
import com.sceyt.chatuikit.extensions.animationListener
import com.sceyt.chatuikit.extensions.setBackgroundTint
import com.sceyt.chatuikit.styles.StyleConstants.UNSET_COLOR
import com.sceyt.chatuikit.styles.common.TextStyle
import com.sceyt.chatuikit.styles.messages_list.ScrollButtonStyle

class ScrollerView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: ScrollerViewBinding
    private var changeVisibilityWithAnim = true
    private lateinit var style: ScrollButtonStyle
    private var unreadCount = 0L

    init {
        binding = ScrollerViewBinding.inflate(LayoutInflater.from(context), this)
        context.obtainStyledAttributes(attrs, R.styleable.ScrollerView).use { array ->
            changeVisibilityWithAnim = array.getBoolean(R.styleable.ScrollerView_sceytUiScrollerChangeVisibilityWithAnim, changeVisibilityWithAnim)
            unreadCount = array.getInt(R.styleable.ScrollerView_sceytUiScrollerUnreadCount, unreadCount.toInt()).toLong()
            style = ScrollButtonStyle.Builder(context, array)
                .unreadCountTextStyle(
                    TextStyle.Builder(array)
                        .setBackgroundColor(R.styleable.ScrollerView_sceytUiScrollerTextBackgroundColor)
                        .setSize(R.styleable.ScrollerView_sceytUiScrollerTextSize)
                        .setColor(R.styleable.ScrollerView_sceytUiScrollerTextColor)
                        .setFont(R.styleable.ScrollerView_sceytUiScrollerTextFont)
                        .setStyle(R.styleable.ScrollerView_sceytUiScrollerTextStyle)
                        .build()
                )
                .icon(R.styleable.ScrollerView_sceytUiScrollerIcon)
                .backgroundColor(R.styleable.ScrollerView_sceytUiScrollerBackgroundColor)
                .build()

            setStyle(style)
            setUnreadCount(unreadCount)
        }
    }

    internal fun setStyle(style: ScrollButtonStyle) {
        this.style = style
        style.unreadCountTextStyle.apply(binding.unreadCount)

        binding.icScrollTo.setImageDrawable(style.icon)

        if (style.backgroundColor != UNSET_COLOR)
            binding.icScrollTo.setBackgroundTint(style.backgroundColor)
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
        unreadCount = count
        binding.unreadCount.isVisible = count > 0
        binding.unreadCount.text = style.unreadCountFormatter.format(context, count)
    }
}