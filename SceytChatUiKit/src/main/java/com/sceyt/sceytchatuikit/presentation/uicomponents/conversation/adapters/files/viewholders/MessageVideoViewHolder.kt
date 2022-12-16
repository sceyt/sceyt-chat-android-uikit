package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Log
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.databinding.SceytMessageVideoItemBinding
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.glideCustomTarget
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle


class MessageVideoViewHolder(
        private val binding: SceytMessageVideoItemBinding,
        private val messageListeners: MessageClickListenersImpl?,
        private val needDownloadCallback: (FileListItem) -> Unit
) : BaseFileViewHolder(binding.root) {

    init {
        binding.setupStyle()

        binding.root.setOnClickListener {
            messageListeners?.onAttachmentClick(it, fileItem)
        }

        binding.root.setOnLongClickListener {
            messageListeners?.onAttachmentLongClick(it, fileItem)
            return@setOnLongClickListener true
        }

        binding.loadProgress.setOnClickListener {
            messageListeners?.onAttachmentLoaderClick(it, fileItem)
        }
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        listenerKey = getKey()
        binding.parentLayout.clipToOutline = true
        binding.videoView.isVisible = false
        binding.loadProgress.release()

        setListener()

        transferData?.let {
            updateState(it)
            if (it.state == Downloading)
                needDownloadCallback.invoke(fileItem)
        }
    }

    private fun updateState(data: TransferData) {
        Log.i(TAG, "$data  $isFileItemInitialized")
        if (isFileItemInitialized.not()) return
        transferData = data
        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        when (data.state) {
            PendingUpload -> {
                loadImage(fileItem.file.filePath)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            PendingDownload -> {
                needDownloadCallback.invoke(fileItem)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            Downloading -> {
                binding.videoViewController.setImageThumb(null)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            Uploading -> {
                loadImage(fileItem.file.filePath)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            Uploaded, Downloaded -> {
                binding.videoViewController.showPlayPauseButtons(true)
                initializePlayer(fileItem.file.filePath)
                loadImage(fileItem.file.filePath)
            }
        }
    }

    private fun setListener() {
        MessageFilesAdapter.setListener(listenerKey, ::updateState)
    }

    private fun loadImage(path: String?) {
        with(binding) {
            Glide.with(itemView.context.applicationContext)
                .load(path)
                .override(videoView.width, videoView.height)
                .into(glideCustomTarget {
                    if (it != null) {
                        videoViewController.setImageThumb(it)
                    }
                })
        }
    }

    private fun initializePlayer(mediaPath: String?) {
        binding.videoViewController.setPlayerViewAndPath(binding.videoView, mediaPath)
        (bindingAdapter as? MessageFilesAdapter)?.videoControllersList?.add(binding.videoViewController)
    }

    private fun SceytMessageVideoItemBinding.setupStyle() {
        loadProgress.setProgressColor(context.getCompatColor(MessagesStyle.mediaLoaderColor))
    }
}