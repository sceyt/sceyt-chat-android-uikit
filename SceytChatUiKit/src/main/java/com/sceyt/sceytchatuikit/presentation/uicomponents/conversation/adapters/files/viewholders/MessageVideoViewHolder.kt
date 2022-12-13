package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.sceyt.sceytchatuikit.databinding.SceytMessageVideoItemBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.glideCustomTarget
import com.sceyt.sceytchatuikit.extensions.isNotNullOrBlank
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
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
        super.onViewDetachedFromWindow()
        if (isFileItemInitialized)
            fileItem.file.removeListener()
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        listenerKey = getKey()
        binding.parentLayout.clipToOutline = true
        binding.videoView.isVisible = false
        binding.loadProgress.isVisible = true

        setListener()
        val transferData = item.file.fileTransferData

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
        updateState(transferData)
    }

    private fun updateState(transferData: TransferData) {
        //Log.i("sdfsdf22", "$transferData  $isFileItemInitialized")

        if (isFileItemInitialized.not()) return
        fileItem.file.fileTransferData = transferData
        when (transferData.state) {
            TransferState.PendingUpload -> {
                binding.loadProgress.release()
                loadImage(fileItem.file.filePath)
                binding.loadProgress.isVisible = true
                binding.videoViewController.showPlayPauseButtons(false)
            }
            TransferState.PendingDownload -> {
                needDownloadCallback.invoke(fileItem)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            TransferState.Downloading -> {
                binding.videoViewController.setImageThumb(null)
                binding.videoViewController.showPlayPauseButtons(false)
                binding.loadProgress.isVisible = true
                binding.loadProgress.setProgress(transferData.progressPercent)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            TransferState.Uploading -> {
                loadImage(fileItem.file.filePath)
                binding.loadProgress.isVisible = true
                binding.loadProgress.setProgress(transferData.progressPercent)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            TransferState.Uploaded -> {
                binding.loadProgress.isVisible = false
                val path = fileItem.file.fileTransferData?.filePath
                binding.videoViewController.showPlayPauseButtons(true)
                initializePlayer(path)

                loadImage(path)
            }
            TransferState.Downloaded -> {
                binding.loadProgress.isVisible = false
                binding.videoViewController.showPlayPauseButtons(true)
                initializePlayer(fileItem.file.fileTransferData?.filePath)
                loadImage(fileItem.file.fileTransferData?.filePath)
            }
            TransferState.Error -> {

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