package com.sceyt.chatuikit.presentation.components.channel.input.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.databinding.SceytFragmentLinkPreviewBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getScope
import com.sceyt.chatuikit.extensions.glideRequestListener
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.MessageInputClickListeners.CancelLinkPreviewClickListener
import com.sceyt.chatuikit.shared.utils.ViewUtil
import com.sceyt.chatuikit.styles.input.MessageInputStyle
import androidx.core.graphics.drawable.toDrawable

@Suppress("MemberVisibilityCanBePrivate", "JoinDeclarationAndAssignment")
class LinkPreviewView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var binding: SceytFragmentLinkPreviewBinding
    private var clickListeners: CancelLinkPreviewClickListener? = null
    private val debounceHelper by lazy { DebounceHelper(300, getScope()) }
    private var showHideAnimator: ValueAnimator? = null
    private lateinit var inputStyle: MessageInputStyle

    init {
        binding = SceytFragmentLinkPreviewBinding.inflate(LayoutInflater.from(context), this)
        initViews()
    }

    private fun initViews() {
        binding.icClose.setOnClickListener {
            clickListeners?.onCancelLinkPreviewClick(it)
        }
    }

    fun showLinkDetails(data: LinkPreviewDetails) {
        debounceHelper.cancelLastDebounce()
        showHideAnimator?.cancel()
        with(binding) {
            if (!root.isVisible || root.height != root.measuredHeight || root.measuredHeight in (0..1)) {
                root.isVisible = true
                showHideAnimator = ViewUtil.expandHeight(root, 1, 200)
            }
            setLinkInfo(data)
        }
    }

    fun hideLinkDetailsWithTimeout() {
        debounceHelper.submit {
            hideLinkDetails()
        }
    }

    fun hideLinkDetails(readyCb: (() -> Unit?)? = null) {
        debounceHelper.cancelLastDebounce()
        showHideAnimator?.cancel()
        with(binding) {
            if (!root.isVisible && root.height == 0) {
                readyCb?.invoke()
                return
            }
            showHideAnimator = ViewUtil.collapseHeight(root, to = 1, duration = 200) {
                root.isVisible = false
                readyCb?.invoke()
            }
        }
    }

    fun setClickListener(clickListeners: CancelLinkPreviewClickListener) {
        this.clickListeners = clickListeners
    }

    private fun setLinkInfo(data: LinkPreviewDetails) {
        with(binding) {
            tvLinkUrl.text = data.link

            tvLinkDescription.apply {
                text = data.description?.trim()
                isVisible = data.description.isNullOrBlank().not()
            }
            setDefaultStateLinkImage()
            val linkUrl = data.imageUrl
            if (!linkUrl.isNullOrBlank()) {
                Glide.with(root.context)
                    .load(linkUrl)
                    .placeholder(defaultImage)
                    .listener(glideRequestListener { success ->
                        if (success) {
                            icLinkImage.background = Color.TRANSPARENT.toDrawable()
                        } else {
                            setDefaultStateLinkImage()
                        }
                    })
                    .into(icLinkImage)
            }
        }
    }

    private fun setDefaultStateLinkImage() {
        binding.icLinkImage.apply {
            setImageDrawable(defaultImage)
            setBackgroundColor(context.getCompatColor(R.color.sceyt_color_background))
        }
    }

    private val defaultImage: Drawable?
        get() = inputStyle.linkPreviewStyle.placeHolder

    internal fun setStyle(inputStyle: MessageInputStyle) {
        this.inputStyle = inputStyle
        binding.applyStyle(inputStyle)
    }

    private fun SceytFragmentLinkPreviewBinding.applyStyle(inputStyle: MessageInputStyle) {
        val style = inputStyle.linkPreviewStyle
        root.setBackgroundColor(style.backgroundColor)
        icClose.setImageDrawable(inputStyle.closeIcon)
        icLinkImage.setImageDrawable(defaultImage)
        style.titleStyle.apply(tvLinkUrl)
        style.descriptionStyle.apply(tvLinkDescription)
        viewTopLinkPreview.setBackgroundColor(style.dividerColor)
    }
}