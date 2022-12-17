package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.data.models.messages.FileLoadData
import com.sceyt.sceytchatuikit.databinding.ItemChannelVideoBinding
import com.sceyt.sceytchatuikit.extensions.glideCustomTarget
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import java.io.File

class VideoViewHolder(private val binding: ItemChannelVideoBinding,
                      private val clickListeners: AttachmentClickListenersImpl) : BaseFileViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            clickListeners.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: FileListItem) {
        with(binding) {
            videoViewController.setBitmapImageThumb(null)
            parentLayout.clipToOutline = true
            videoView.isVisible = false
           // binding.videoViewController.showPlayPauseButtons(!item.fileLoadData.loading)
        }
        super.bind(item)
    }

    private fun ItemChannelVideoBinding.updateDownloadState(data: FileLoadData, file: File?) {
        groupLoading.isVisible = data.loading
        binding.videoViewController.showPlayPauseButtons(!data.loading)
        if (data.loading) {
            loadProgress.progress = data.progressPercent.toInt()
            videoViewController.setBitmapImageThumb(null)
        }
        if (file != null) {
            val mediaPath = file.path
            initializePlayer(mediaPath)

            with(binding) {
                Glide.with(itemView.context)
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

    private fun initializePlayer(mediaPath: String) {
        binding.videoViewController.setPlayerViewAndPath(binding.videoView, mediaPath)
        (bindingAdapter as? MessageFilesAdapter)?.videoControllersList?.add(binding.videoViewController)
    }
}