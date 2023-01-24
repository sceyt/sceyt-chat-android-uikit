package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelLinkBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.getCompatDrawable
import com.sceyt.sceytchatuikit.extensions.glideRequestListener
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseChannelFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.ChannelFileItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper


class LinkViewHolder(private var binding: SceytItemChannelLinkBinding,
                     private val linkPreview: LinkPreviewHelper?,
                     private val clickListener: AttachmentClickListenersImpl)
    : BaseChannelFileViewHolder(binding.root, {}) {


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

            if ((fileItem as ChannelFileItem.Link).linkPreviewMetaData == null) {

                setLinkInfo(null)

                linkPreview?.getPreview(attachment.messageTid, attachment.url.toString(), successListener = {
                    if (it.messageId == fileItem.file.messageTid) {
                        (fileItem as ChannelFileItem.Link).linkPreviewMetaData = it

                        setLinkInfo(it)
                    }
                })
            } else setLinkInfo((fileItem as ChannelFileItem.Link).linkPreviewMetaData)
        }
    }

    private fun SceytItemChannelLinkBinding.setLinkInfo(data: LinkPreviewHelper.PreviewMetaData?) {
        if (data == null || viewHolderHelper.isFileItemInitialized.not() || data.messageId != fileItem.file.messageTid) {
            tvLinkName.text = ""
            tvLinkName.isVisible = false
            setDefaultStateLinkImage()
        } else {
            val title = if (data.siteName.isNullOrBlank()) data.title else data.siteName
            tvLinkName.text = title
            tvLinkName.isVisible = title.isNullOrBlank().not()

            Glide.with(root.context)
                .load(if (data.favicon.isNullOrBlank().not()) data.favicon else data.imageUrl)
                .placeholder(R.drawable.sceyt_ic_link_with_background)
                .override(icLinkImage.width)
                .listener(glideRequestListener { sucess ->
                    if (sucess) {
                        icLinkImage.background = ColorDrawable(Color.TRANSPARENT)
                    } else {
                        icLinkImage.setImageResource(R.drawable.sceyt_ic_link_with_background)
                        setDefaultStateLinkImage()
                    }
                })
                .into(icLinkImage)
        }
    }

    private fun setDefaultStateLinkImage() {
        binding.icLinkImage.setImageResource(R.drawable.sceyt_ic_link_with_background)
        binding.icLinkImage.background = context.getCompatDrawable(R.drawable.sceyt_bg_corners_8)?.apply {
            setTint(context.getCompatColor(SceytKitConfig.sceytColorAccent))
        }
    }

    private fun SceytItemChannelLinkBinding.setupStyle() {
        icLinkImage.backgroundTintList = ColorStateList.valueOf(itemView.context.getCompatColor(SceytKitConfig.sceytColorAccent))
    }
}