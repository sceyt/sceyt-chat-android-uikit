package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.data.models.messages.FileLoadData
import com.sceyt.sceytchatuikit.databinding.SceytMessageVideoItemBinding
import com.sceyt.sceytchatuikit.extensions.glideCustomTarget
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import java.io.File


class MessageVideoViewHolder(
        private val binding: SceytMessageVideoItemBinding,
        private val messageListeners: MessageClickListenersImpl?,
) : BaseFileViewHolder(binding.root) {

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
        with(binding) {
            videoViewController.setImageThumb(null)
            parentLayout.clipToOutline = true
            videoView.isVisible = false
            binding.videoViewController.showPlayPauseButtons(!item.fileLoadData.loading)
        }
        super.bind(item)
    }

    private fun SceytMessageVideoItemBinding.updateDownloadState(data: FileLoadData, file: File?) {
        groupLoading.isVisible = data.loading
        binding.videoViewController.showPlayPauseButtons(!data.loading)
        if (data.loading) {
            loadProgress.progress = data.progressPercent.toInt()
            videoViewController.setImageThumb(null)
        }
        if (file != null) {
            val mediaPath = file.path
            initializePlayer(mediaPath)

            with(binding) {
                Glide.with(itemView.context.applicationContext)
                    .load(file)
                    .override(videoView.width, videoView.height)
                    .into(glideCustomTarget {
                        if (it != null) {
                            videoViewController.setImageThumb(it)
                        }
                    })
            }
        }
    }

    private fun SceytMessageVideoItemBinding.updateUploadState(data: FileLoadData) {
        groupLoading.isVisible = data.loading
        binding.videoViewController.showPlayPauseButtons(!data.loading)
        if (data.loading)
            loadProgress.progress = data.progressPercent.toInt()
    }

    private fun initializePlayer(mediaPath: String) {
        binding.videoViewController.setPlayerViewAndPath(binding.videoView, mediaPath)
        (bindingAdapter as? MessageFilesAdapter)?.videoControllersList?.add(binding.videoViewController)
    }

    override fun updateUploadingState(data: FileLoadData) {
        binding.updateUploadState(data)
    }

    override fun updateDownloadingState(data: FileLoadData, file: File?) {
        binding.updateDownloadState(data, file)
    }
}