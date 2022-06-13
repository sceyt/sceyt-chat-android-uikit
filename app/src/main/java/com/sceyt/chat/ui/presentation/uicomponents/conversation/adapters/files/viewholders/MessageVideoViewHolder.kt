package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.chat.ui.data.models.messages.FileLoadData
import com.sceyt.chat.ui.databinding.SceytMessageVideoItemBinding
import com.sceyt.chat.ui.extensions.glideCustomTarget
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import java.io.File


class MessageVideoViewHolder(
        private val binding: SceytMessageVideoItemBinding,
        private val messageListeners: MessageClickListenersImpl?,
) : BaseFileViewHolder(binding.root) {

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
        super.bind(item)
        fileItem = item

        with(binding) {
            parentLayout.clipToOutline = true
            videoView.isVisible = false
        }
    }

    override fun updateUploadingState(load: FileLoadData, finish: Boolean) {
        binding.updateLoadState(load, finish)
    }

    override fun updateDownloadingState(load: FileLoadData) {
        binding.updateLoadState(load, false)
    }

    override fun downloadFinish(load: FileLoadData, file: File?) {
        binding.updateLoadState(load, true)
        val mediaPath = (file ?: return).path
        initializePlayer(mediaPath)

        with(binding) {
            Glide.with(itemView.context)
                .load(mediaPath)
                .override(videoView.width, videoView.height)
                .into(glideCustomTarget {
                    if (it != null) {
                        videoViewController.setImageThumb(it)
                    }
                })
        }
    }

    private fun initializePlayer(mediaPath: String) {
        binding.videoViewController.setPlayerViewAndPath(binding.videoView, mediaPath)
        (bindingAdapter as? MessageFilesAdapter)?.videoControllersList?.add(binding.videoViewController)
    }

    private fun SceytMessageVideoItemBinding.updateLoadState(data: FileLoadData, finish: Boolean) {
        if (finish) {
            groupLoading.isVisible = false
            binding.videoViewController.showPlayPauseButtons(false)
        } else {
            groupLoading.isVisible = data.loading
            binding.videoViewController.showPlayPauseButtons(!data.loading)
            if (data.loading)
                loadProgress.progress = data.progressPercent
        }
    }
}