package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import com.bumptech.glide.Glide
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.databinding.ItemChannelImageBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder

class ImageViewHolder(private val binding: ItemChannelImageBinding) : BaseFileViewHolder(binding.root) {

    override fun bind(item: FileListItem) {

        Glide.with(itemView.context)
            .load(item.file.url)
            .override(binding.icImage.width)
            .placeholder(R.color.sceyt_color_gray)
            .into(binding.icImage)
    }
}