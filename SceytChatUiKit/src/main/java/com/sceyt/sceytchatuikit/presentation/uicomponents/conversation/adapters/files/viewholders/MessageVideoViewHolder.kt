package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.databinding.SceytMessageVideoItemBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.glideCustomTarget
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
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
    }


    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        if (isFileItemInitialized)
            setListener()
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()/*
        if (isFileItemInitialized)
            fileItem.file.removeListener()*/
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        listenerKey = getKey()
        binding.parentLayout.clipToOutline = true
        binding.videoView.isVisible = false

        setListener()

        if (transferData == null) {
            //  Log.i("sdfsdf", "filePath ${item.file.filePath}  url ${item.file.url}")
            if (item.file.filePath.isNullOrBlank() && item.file.url.isNotNullOrBlank()) {
                //  Log.i("sdfsdf", "needDownloadCallback")

                binding.videoViewController.setImageThumb(null)
                binding.videoViewController.showPlayPauseButtons(false)
                needDownloadCallback.invoke(item)
            }
            return
        }
        transferData?.let {
            updateState(it)
        }
    }

    private fun updateState(data: TransferData) {
        //Log.i("sdfsdf22", "$transferData  $isFileItemInitialized")

        if (isFileItemInitialized.not()) return
        // fileItem.file.fileTransferData = transferData
        transferData = data
        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        when (data.state) {
            PendingUpload -> {
                // binding.loadProgress.release()
                loadImage(fileItem.file.filePath)
                // binding.loadProgress.isVisible = true
                binding.videoViewController.showPlayPauseButtons(false)
            }
            PendingDownload -> {
                needDownloadCallback.invoke(fileItem)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            Downloading -> {
                binding.videoViewController.setImageThumb(null)
                binding.videoViewController.showPlayPauseButtons(false)
                /*binding.loadProgress.apply {
                    isVisible = true
                    setTransferring(true)
                    setProgress(transferData.progressPercent)
                }*/
                binding.videoViewController.showPlayPauseButtons(false)
            }
            Uploading -> {
                loadImage(fileItem.file.filePath)
                /*  binding.loadProgress.apply {
                      isVisible = true
                      setTransferring(true)
                  }
                  binding.loadProgress.setProgress(transferData.progressPercent)*/
                binding.videoViewController.showPlayPauseButtons(false)
            }
            Uploaded -> {
                // binding.loadProgress.isVisible = false
                val path = fileItem.file.filePath
                binding.videoViewController.showPlayPauseButtons(true)
                initializePlayer(path)

                loadImage(path)
            }
            Downloaded -> {
                //binding.loadProgress.isVisible = false
                binding.videoViewController.showPlayPauseButtons(true)
                initializePlayer(fileItem.file.filePath)
                loadImage(fileItem.file.filePath)
            }
            ErrorDownload -> {
                //binding.videoViewController.showPlayPauseButtons(false)
                /* binding.loadProgress.apply {
                     isVisible = true
                     setTransferring(false)
                 }*/
            }
            ErrorUpload -> {
                /*  binding.loadProgress.apply {
                      isVisible = true
                      setTransferring(false)
                  }*/
                binding.videoViewController.showPlayPauseButtons(false)
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