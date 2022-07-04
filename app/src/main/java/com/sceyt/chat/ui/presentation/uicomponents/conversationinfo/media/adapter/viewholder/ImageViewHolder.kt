package com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.FileLoadData
import com.sceyt.chat.ui.databinding.ItemChannelImageBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import java.io.File

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

    private fun ItemChannelImageBinding.updateDownloadState(data: FileLoadData, file: File?) {
        groupLoading.isVisible = data.loading
        loadProgress.progress = data.progressPercent.toInt()
        if (file != null) {
            Glide.with(itemView.context)
                .load(file)
                .placeholder(R.color.sceyt_color_gray)
                .transition(DrawableTransitionOptions.withCrossFade())
                .override(root.width, root.height)
                .into(icImage)
        }
    }

    private fun ItemChannelImageBinding.updateUploadState(data: FileLoadData) {
        groupLoading.isVisible = data.loading
        if (data.loading)
            loadProgress.progress = data.progressPercent.toInt()
    }

    override fun updateUploadingState(data: FileLoadData) {
        binding.updateUploadState(data)
    }

    override fun updateDownloadingState(data: FileLoadData, file: File?) {
        binding.updateDownloadState(data, file)
    }
}