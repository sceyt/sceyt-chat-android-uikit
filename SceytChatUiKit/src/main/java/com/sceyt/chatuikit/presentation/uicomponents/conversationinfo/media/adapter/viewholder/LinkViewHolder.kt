package com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.databinding.SceytItemChannelLinkBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getCompatDrawable
import com.sceyt.chatuikit.extensions.glideRequestListener
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.chatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListeners


class LinkViewHolder(
        private val binding: SceytItemChannelLinkBinding,
        private val clickListener: AttachmentClickListeners.AttachmentClickListener,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseFileViewHolder<ChannelFileItem>(binding.root, {}) {

    init {
        binding.applyStyle()
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
            val previewDetails  =attachment.linkPreviewDetails
            if (previewDetails == null) {
                setLinkInfo(null, attachment)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedLinkPreview(attachment, false))
            } else {
                setLinkInfo(previewDetails, attachment)
                if (previewDetails.imageUrl != null && previewDetails.imageWidth == null)
                    needMediaDataCallback(NeedMediaInfoData.NeedLinkPreview(attachment, true))
            }
        }
    }

    private fun SceytItemChannelLinkBinding.setLinkInfo(data: LinkPreviewDetails?, attachment: SceytAttachment) {
        if (data == null || viewHolderHelper.isFileItemInitialized.not() || data.link != attachment.url || data.hideDetails) {
            tvLinkName.text = null
            tvLinkName.isVisible = false
            tvLinkDescription.isVisible = false
            setDefaultStateLinkImage()
        } else {
            fileItem.file = attachment.copy(linkPreviewDetails = data)
            tvLinkName.apply {
                text = data.title?.trim()
                isVisible = data.title.isNullOrBlank().not()
            }

            tvLinkDescription.apply {
                text = data.description?.trim()
                isVisible = data.description.isNullOrBlank().not()
            }

            val linkUrl = data.imageUrl
            setDefaultStateLinkImage()
            if (!linkUrl.isNullOrBlank()) {
                Glide.with(root.context)
                    .load(linkUrl)
                    .override(icLinkImage.width)
                    .placeholder(defaultImage)
                    .listener(glideRequestListener { success ->
                        if (success) {
                            icLinkImage.background = ColorDrawable(Color.TRANSPARENT)
                        } else
                            setDefaultStateLinkImage()
                    })
                    .into(icLinkImage)
            }
        }
    }

    private fun setDefaultStateLinkImage() {
        binding.icLinkImage.setImageDrawable(defaultImage)
        binding.icLinkImage.setBackgroundColor(context.getCompatColor(R.color.sceyt_color_gray))
    }

    private val defaultImage by lazy {
        context.getCompatDrawable(R.drawable.sceyt_ic_link)?.apply {
            setTint(context.getCompatColor(SceytChatUIKit.theme.accentColor))
        }
    }

    private fun SceytItemChannelLinkBinding.applyStyle() {
        root.setBackgroundColor(context.getCompatColor(SceytChatUIKit.theme.backgroundColorSections))
    }
}