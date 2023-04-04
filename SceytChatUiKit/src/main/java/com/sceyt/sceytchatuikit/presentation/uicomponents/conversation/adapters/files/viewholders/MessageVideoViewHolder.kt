package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Size
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.databinding.SceytMessageVideoItemBinding
import com.sceyt.sceytchatuikit.extensions.asComponentActivity
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.persistence.filetransfer.FileTransferHelper
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil


class MessageVideoViewHolder(
        private val binding: SceytMessageVideoItemBinding,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseFileViewHolder<FileListItem>(binding.root, needMediaDataCallback) {

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

        binding.videoViewController.getPlayPauseImageView().setOnClickListener {
            messageListeners?.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        binding.parentLayout.clipToOutline = true
        binding.videoView.isVisible = false
        binding.loadProgress.release(item.file.progressPercent)
        setVideoDuration()

        setListener()

        viewHolderHelper.transferData?.let {
            updateState(it, true)
            if (it.filePath.isNullOrBlank() && it.state != PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
        }

        if (fileItem.thumbPath.isNullOrBlank())
            requestThumb()
    }

    private fun updateState(data: TransferData, isOnBind: Boolean = false) {
        if (!viewHolderHelper.updateTransferData(data, fileItem)) return

        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        val imageView = binding.videoViewController.getImageView()
        when (data.state) {
            PendingUpload, ErrorUpload, PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
                binding.videoViewController.showPlayPauseButtons(false)
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }
            Downloading -> {
                binding.videoViewController.showPlayPauseButtons(false)
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = imageView)
            }
            Uploading -> {
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            Downloaded -> {
                binding.videoViewController.showPlayPauseButtons(true)
                // initializePlayer(fileItem.file.filePath)
                viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
            }
            Uploaded -> {
                binding.videoViewController.showPlayPauseButtons(true)
                //initializePlayer(fileItem.file.filePath)
                viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
            }
            PauseDownload -> {
                binding.videoViewController.showPlayPauseButtons(false)
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }
            ErrorDownload -> {
                binding.videoViewController.showPlayPauseButtons(false)
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }
            FilePathChanged -> {
                requestThumb()
            }
            ThumbLoaded -> {
                viewHolderHelper.loadThumb(fileItem.thumbPath, imageView)
            }
        }
    }

    private fun setVideoDuration() {
        with(binding.tvDuration) {
            fileItem.duration?.let {
                text = DateTimeUtil.secondsToTime(it)
                isVisible = true
            } ?: run { isVisible = false }
        }
    }

    override fun getThumbSize() = Size(1080, 1080)

    private fun setListener() {
        FileTransferHelper.onTransferUpdatedLiveData.observe(context.asComponentActivity(), ::updateState)
    }

    private fun initializePlayer(mediaPath: String?) {
        binding.videoViewController.setPlayerViewAndPath(binding.videoView, mediaPath)
        (bindingAdapter as? MessageFilesAdapter)?.videoControllersList?.add(binding.videoViewController)
    }

    private fun SceytMessageVideoItemBinding.setupStyle() {
        loadProgress.setProgressColor(context.getCompatColor(MessagesStyle.mediaLoaderColor))
    }
}