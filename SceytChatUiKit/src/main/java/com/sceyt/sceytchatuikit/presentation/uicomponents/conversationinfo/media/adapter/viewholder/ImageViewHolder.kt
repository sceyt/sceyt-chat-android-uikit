package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.databinding.ItemChannelImageBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl

class ImageViewHolder(private val binding: ItemChannelImageBinding,
                      private val clickListeners: AttachmentClickListenersImpl) : BaseFileViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            clickListeners.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: FileListItem) {
        binding.icImage.setImageResource(R.color.sceyt_color_gray)
        super.bind(item)
    }
}