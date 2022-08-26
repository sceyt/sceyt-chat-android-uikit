package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.adapters.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.links.adapters.LinkItem
import com.sceyt.sceytchatuikit.databinding.ItemChannelLinkBinding
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper


class LinkViewHolder(private var binding: ItemChannelLinkBinding,
                     private val linkPreview: LinkPreviewHelper,
                     private val clickListener: ChannelLinkViewHolderFactory.LinkClickListener)
    : com.sceyt.sceytchatuikit.presentation.common.BaseViewHolder<LinkItem>(binding.root) {

    private lateinit var listItem: LinkItem.Link

    init {
        binding.root.setOnClickListener {
            clickListener.onItemClick(listItem = listItem)
        }
    }

    override fun bind(item: LinkItem) {
        val message = (item as LinkItem.Link).message
        listItem = item

        with(binding) {
            root.layoutTransition?.setAnimateParentHierarchy(false)

            tvLinkUrl.text = message.body

            if (listItem.linkPreviewMetaData == null) {

                setLinkInfo(null)

                linkPreview.getPreview(message.id, message.body, successListener = {
                    if (it.loadId == listItem.message.id) {
                        listItem.linkPreviewMetaData = it

                        setLinkInfo(it)
                    }
                })
            } else setLinkInfo(listItem.linkPreviewMetaData)
        }
    }

    private fun ItemChannelLinkBinding.setLinkInfo(data: LinkPreviewHelper.PreviewMetaData?) {
        if (data == null || ::listItem.isInitialized.not() || data.loadId != listItem.message.id) {
            tvLinkName.text = ""
            tvLinkName.isVisible = false
            icLinkImage.setImageResource(R.drawable.sceyt_ic_link_with_background)
        } else {
            val title = if (data.siteName.isNullOrBlank()) data.title else data.siteName
            tvLinkName.text = title
            tvLinkName.isVisible = title.isNullOrBlank().not()

            Glide.with(root.context)
                .load(if (data.favicon.isNullOrBlank().not()) data.favicon else data.imageUrl)
                .placeholder(R.drawable.sceyt_ic_link_with_background)
                .override(icLinkImage.width)
                .error(R.drawable.sceyt_ic_link_with_background)
                .into(icLinkImage)
        }
    }
}