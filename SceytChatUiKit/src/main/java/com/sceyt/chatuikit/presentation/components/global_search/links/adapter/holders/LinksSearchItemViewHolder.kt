package com.sceyt.chatuikit.presentation.components.global_search.links.adapter.holders

import android.graphics.Color
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.databinding.SceytItemChannelLinkBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.glideRequestListener
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.styles.channel_info.link.ChannelInfoLinkItemStyle

open class LinksSearchItemViewHolder(
    private val style: ChannelInfoLinkItemStyle,
    private val binding: SceytItemChannelLinkBinding,
    needMediaDataCallback: (NeedMediaInfoData) -> Unit,
    private val onAttachmentClickListener: ((GlobalSearchListItem.AttachmentItem) -> Unit)?,
) : BaseFileViewHolder<GlobalSearchListItem.AttachmentItem>(binding.root, needMediaDataCallback) {

    init {
        binding.applyStyle()
        binding.root.setOnClickListener {
            onAttachmentClickListener?.invoke(fileItem)
        }
    }

    override fun bind(item: GlobalSearchListItem.AttachmentItem) {
        super.bind(item)
        val attachment = item.attachment

        with(binding) {
            root.layoutTransition?.setAnimateParentHierarchy(false)
            tvLinkUrl.text = attachment.url
            setLinkInfo(attachment.linkPreviewDetails, attachment)
        }
    }

    private fun SceytItemChannelLinkBinding.setLinkInfo(
        data: LinkPreviewDetails?,
        attachment: SceytAttachment,
    ) {
        if (data == null || !viewHolderHelper.isFileItemInitialized || data.link != attachment.url || data.hideDetails) {
            tvLinkName.text = null
            tvLinkName.isVisible = false
            tvLinkDescription.isVisible = false
            setDefaultLinkImage()
        } else {
            fileItem.updateAttachment(attachment.copy(linkPreviewDetails = data))
            tvLinkName.apply {
                text = data.title?.trim()
                isVisible = !data.title.isNullOrBlank()
            }
            tvLinkDescription.apply {
                text = data.description?.trim()
                isVisible = !data.description.isNullOrBlank()
            }

            val imageUrl = data.imageUrl
            setDefaultLinkImage()
            if (!imageUrl.isNullOrBlank()) {
                Glide.with(root.context)
                    .load(imageUrl)
                    .override(icLinkImage.width)
                    .placeholder(style.linkPreviewStyle.placeHolder)
                    .listener(glideRequestListener { success ->
                        if (success) {
                            icLinkImage.background = Color.TRANSPARENT.toDrawable()
                        } else {
                            setDefaultLinkImage()
                        }
                    })
                    .into(icLinkImage)
            }
        }
    }

    private fun setDefaultLinkImage() {
        binding.icLinkImage.setImageDrawable(style.linkPreviewStyle.placeHolder)
        binding.icLinkImage.setBackgroundColor(context.getCompatColor(SceytChatUIKit.theme.colors.surface2Color))
    }

    private fun SceytItemChannelLinkBinding.applyStyle() {
        root.setBackgroundColor(style.backgroundColor)
        icLinkImage.setBackgroundColor(context.getCompatColor(SceytChatUIKit.theme.colors.surface2Color))
        style.linkTextStyle.apply(tvLinkUrl)
        with(style.linkPreviewStyle) {
            titleStyle.apply(tvLinkName)
            descriptionStyle.apply(tvLinkDescription)
        }
    }
}
