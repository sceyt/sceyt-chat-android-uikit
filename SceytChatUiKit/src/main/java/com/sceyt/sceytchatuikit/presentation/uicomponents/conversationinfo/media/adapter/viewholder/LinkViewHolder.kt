package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelLinkBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.extensions.glideRequestListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper


class LinkViewHolder(private var binding: SceytItemChannelLinkBinding,
                     private val linkPreview: LinkPreviewHelper?,
                     private val clickListener: AttachmentClickListenersImpl
) : BaseFileViewHolder<ChannelFileItem>(binding.root, {}) {

    init {
        binding.setupStyle()

        binding.root.setOnClickListener {
            clickListener.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: ChannelFileItem) {
        super.bind(item)
        val attachment = item.file

        with(binding) {
            root.layoutTransition?.setAnimateParentHierarchy(false)

            tvLinkUrl.text = attachment.url

            if ((attachment).linkPreviewDetails == null) {
                setLinkInfo(null, attachment)
                linkPreview?.getPreview(attachment, successListener = {
                    setLinkInfo(it, attachment)
                })
            } else setLinkInfo(attachment.linkPreviewDetails, attachment)
        }
    }

    private fun SceytItemChannelLinkBinding.setLinkInfo(data: LinkPreviewDetails?, attachment: SceytAttachment) {
        if (data == null || viewHolderHelper.isFileItemInitialized.not() || data.link != attachment.url) {
            tvLinkName.text = ""
            tvLinkName.isVisible = false
            tvLinkDescription.isVisible = false
            setDefaultStateLinkImage()
        } else {
            attachment.linkPreviewDetails = data
            tvLinkName.apply {
                text = data.siteName?.trim()
                isVisible = data.siteName.isNullOrBlank().not()
            }

            tvLinkDescription.apply {
                text = data.description?.trim()
                isVisible = data.description.isNullOrBlank().not()
            }

            Glide.with(root.context)
                .load(if (data.faviconUrl.isNullOrBlank().not()) data.faviconUrl else data.imageUrl)
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

    private fun setDefaultStateLinkImage() {
        binding.icLinkImage.setImageDrawable(defaultImage)
    }

    private val defaultImage by lazy {
        binding.root.context.getCompatDrawable(R.drawable.sceyt_ic_link)?.apply {
            setTint(binding.root.context.getCompatColor(SceytKitConfig.sceytColorAccent))
        }
    }

    private fun SceytItemChannelLinkBinding.setupStyle() {
        tvLinkUrl.setTextColor(itemView.context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}