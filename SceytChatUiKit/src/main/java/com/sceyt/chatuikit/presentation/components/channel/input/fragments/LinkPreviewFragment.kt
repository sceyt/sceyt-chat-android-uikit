package com.sceyt.chatuikit.presentation.components.channel.input.fragments

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.databinding.SceytFragmentLinkPreviewBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.glideRequestListener
import com.sceyt.chatuikit.presentation.common.DebounceHelper
import com.sceyt.chatuikit.presentation.components.channel.input.listeners.click.MessageInputClickListeners.CancelLinkPreviewClickListener
import com.sceyt.chatuikit.shared.utils.ViewUtil
import com.sceyt.chatuikit.styles.input.MessageInputStyle

@Suppress("MemberVisibilityCanBePrivate")
open class LinkPreviewFragment : Fragment() {
    protected var binding: SceytFragmentLinkPreviewBinding? = null
    protected var clickListeners: CancelLinkPreviewClickListener? = null
    protected val debounceHelper by lazy { DebounceHelper(300, lifecycleScope) }
    protected var showHideAnimator: ValueAnimator? = null
    protected var inputStyle: MessageInputStyle? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentLinkPreviewBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inputStyle?.let {
            binding?.applyStyle(it)
        }
        initViews()
    }

    private fun initViews() {
        binding?.icClose?.setOnClickListener {
            clickListeners?.onCancelLinkPreviewClick(it)
        }
    }

    internal fun setStyle(inputStyle: MessageInputStyle) {
        this.inputStyle = inputStyle
    }

    open fun showLinkDetails(data: LinkPreviewDetails) {
        if (!isAdded) return
        debounceHelper.cancelLastDebounce()
        showHideAnimator?.cancel()
        with(binding ?: return) {
            if (!root.isVisible || root.height != root.measuredHeight) {
                root.isVisible = true
                showHideAnimator = ViewUtil.expandHeight(root, 0, 200)
            }
            setLinkInfo(data)
        }
    }

    open fun hideLinkDetailsWithTimeout() {
        if (!isAdded) return
        debounceHelper.submit {
            hideLinkDetails()
        }
    }

    open fun hideLinkDetails(readyCb: (() -> Unit?)? = null) {
        if (!isAdded) return
        debounceHelper.cancelLastDebounce()
        showHideAnimator?.cancel()
        with(binding ?: return) {
            if (!root.isVisible && root.height == 0) {
                readyCb?.invoke()
                return
            }
            showHideAnimator = ViewUtil.collapseHeight(root, to = 0, duration = 200) {
                root.isVisible = false
                readyCb?.invoke()
            }
        }
    }

    open fun setClickListener(clickListeners: CancelLinkPreviewClickListener) {
        this.clickListeners = clickListeners
    }

    protected open fun setLinkInfo(data: LinkPreviewDetails) {
        with(binding ?: return) {
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
                            icLinkImage.background = ColorDrawable(Color.TRANSPARENT)
                        } else {
                            setDefaultStateLinkImage()
                        }
                    })
                    .into(icLinkImage)
            }
        }
    }

    protected open fun setDefaultStateLinkImage() {
        binding?.icLinkImage?.apply {
            setImageDrawable(defaultImage)
            setBackgroundColor(context.getCompatColor(R.color.sceyt_color_background))
        }
    }

    protected open val defaultImage: Drawable?
        get() = inputStyle?.linkPreviewStyle?.placeHolder

    private fun SceytFragmentLinkPreviewBinding.applyStyle(inputStyle: MessageInputStyle) {
        val style = inputStyle.linkPreviewStyle
        root.setBackgroundColor(style.backgroundColor)
        icClose.setImageDrawable(inputStyle.closeIcon)
        icLinkImage.setImageDrawable(defaultImage)
        style.titleStyle.apply(tvLinkUrl)
        style.descriptionStyle.apply(tvLinkDescription)
        viewTop.setBackgroundColor(style.dividerColor)
    }
}