package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.sceyt.chat.ui.data.models.messages.FileLoadData
import com.sceyt.chat.ui.databinding.SceytMessageImageItemBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import java.io.File


class MessageImageViewHolder(
        private val binding: SceytMessageImageItemBinding,
        private val messageListeners: MessageClickListenersImpl?) : BaseFileViewHolder(binding.root) {

    private lateinit var fileItem: FileListItem

    init {
        binding.root.setOnClickListener {
            messageListeners?.onAttachmentClick(it, fileItem)
        }

        binding.root.setOnLongClickListener {
            messageListeners?.onAttachmentLongClick(it, fileItem)
            return@setOnLongClickListener true
        }
    }

    override fun bind(item: FileListItem) {
        binding.fileImage.setImageBitmap(null)
        super.bind(item)

        fileItem = item
    }

    override fun updateUploadingState(load: FileLoadData, finish: Boolean) {
        binding.updateLoadState(load, finish)
    }

    override fun updateDownloadingState(load: FileLoadData) {
        binding.updateLoadState(load, false)
    }

    override fun downloadFinish(load: FileLoadData, file: File?) {
        with(binding) {
            updateLoadState(load, true)
            Glide.with(itemView.context)
                .load(file)
                .transition(DrawableTransitionOptions.withCrossFade())
                .override(root.width, root.height)
                .into(fileImage)
        }
    }

    private fun SceytMessageImageItemBinding.updateLoadState(data: FileLoadData, finish: Boolean) {
        if (finish) {
            groupLoading.isVisible = false
        } else {
            groupLoading.isVisible = data.loading
            if (data.loading)
                loadProgress.progress = data.progressPercent
        }
    }
}