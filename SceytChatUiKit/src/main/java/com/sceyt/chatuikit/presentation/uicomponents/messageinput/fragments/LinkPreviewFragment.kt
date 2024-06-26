package com.sceyt.chatuikit.presentation.uicomponents.messageinput.fragments

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
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.databinding.SceytFragmentLinkPreviewBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.glideRequestListener
import com.sceyt.chatuikit.extensions.setTextColorRes
import com.sceyt.chatuikit.extensions.setTintColorRes
import com.sceyt.chatuikit.persistence.lazyVar
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.listeners.clicklisteners.MessageInputClickListeners.CancelLinkPreviewClickListener
import com.sceyt.chatuikit.presentation.uicomponents.searchinput.DebounceHelper
import com.sceyt.chatuikit.sceytstyles.MessageInputStyle
import com.sceyt.chatuikit.shared.utils.ViewUtil

open class LinkPreviewFragment : Fragment() {
    protected var binding: SceytFragmentLinkPreviewBinding? = null
    protected var clickListeners: CancelLinkPreviewClickListener? = null
    protected val debounceHelper by lazy { DebounceHelper(300, lifecycleScope) }
    protected var showHideAnimator: ValueAnimator? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return SceytFragmentLinkPreviewBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.applyStyle()
        initViews()
    }

    private fun initViews() {
        binding?.icClose?.setOnClickListener {
            clickListeners?.onCancelLinkPreviewClick(it)
        }
    }

    internal fun setStyle(style: MessageInputStyle) {
        defaultImage = style.linkIcon
        binding?.icLinkImage?.setImageDrawable(defaultImage)
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

    protected open var defaultImage: Drawable? by lazyVar {
        requireContext().getCompatDrawable(R.drawable.sceyt_ic_link)?.apply {
            mutate().setTint(requireContext().getCompatColor(SceytChatUIKit.theme.accentColor))
        }
    }

    private fun SceytFragmentLinkPreviewBinding.applyStyle() {
        root.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.surface1Color))
        icClose.setTintColorRes(SceytChatUIKit.theme.iconSecondaryColor)
        tvLinkDescription.setTextColorRes(SceytChatUIKit.theme.textSecondaryColor)
        viewTop.setBackgroundColor(requireContext().getCompatColor(SceytChatUIKit.theme.borderColor))
    }
}