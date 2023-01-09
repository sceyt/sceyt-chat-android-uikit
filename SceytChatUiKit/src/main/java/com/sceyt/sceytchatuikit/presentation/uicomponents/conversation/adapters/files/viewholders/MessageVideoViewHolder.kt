package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Size
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.databinding.SceytMessageVideoItemBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferUpdateObserver
import com.sceyt.sceytchatuikit.persistence.filetransfer.getProgressWithState
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil


class MessageVideoViewHolder(
        private val binding: SceytMessageVideoItemBinding,
        private val messageListeners: MessageClickListenersImpl?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
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
        binding.loadProgress.release(item.file.progressPercent)
        setVideoDuration()

        setListener()

        transferData?.let {
            updateState(it, true)
            if (it.filePath == null)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
        }

        if (fileItem.thumbPath.isNullOrBlank())
            requestThumb()
    }

    private fun updateState(data: TransferData, isOnBind: Boolean = false) {
        if (isFileItemInitialized.not() || (data.messageTid != fileItem.sceytMessage.tid)) return
        transferData = data
        binding.loadProgress.getProgressWithState(data.state, data.progressPercent)
        val imageView = binding.videoViewController.getImageView()
        when (data.state) {
            PendingUpload, ErrorUpload, PauseUpload -> {
                drawThumbOrRequest(imageView, ::requestThumb)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
                binding.videoViewController.showPlayPauseButtons(false)
                loadBlurThumb(blurredThumb, imageView)
            }
            Downloading -> {
                binding.videoViewController.showPlayPauseButtons(false)
                if (isOnBind)
                    loadBlurThumb(blurredThumb, imageView)
            }
            Uploading -> {
                if (isOnBind)
                    drawThumbOrRequest(imageView, ::requestThumb)
                binding.videoViewController.showPlayPauseButtons(false)
            }
            Downloaded -> {
                binding.videoViewController.showPlayPauseButtons(true)
                initializePlayer(fileItem.file.filePath)
                drawThumbOrRequest(imageView, ::requestThumb)
            }
            Uploaded -> {
                binding.videoViewController.showPlayPauseButtons(true)
                initializePlayer(fileItem.file.filePath)
                drawThumbOrRequest(imageView, ::requestThumb)
            }
            PauseDownload -> {
                binding.videoViewController.showPlayPauseButtons(false)
                loadBlurThumb(blurredThumb, imageView)
            }
            ErrorDownload -> {
                binding.videoViewController.showPlayPauseButtons(false)
                loadBlurThumb(blurredThumb, imageView)
            }
            FilePathChanged -> {
                requestThumb()
            }
            ThumbLoaded -> {
                loadThumb(fileItem.thumbPath, imageView)
            }
        }
    }

    private fun setVideoDuration() {
        with(binding.tvDuration) {
            (fileItem as? FileListItem.Video)?.videoDuration?.let {
                text = DateTimeUtil.secondsToTime(it)
                isVisible = true
            } ?: run { isVisible = false }
        }
    }

    private fun requestThumb() {
        itemView.post {
            needMediaDataCallback.invoke(NeedMediaInfoData.NeedThumb(fileItem, getThumbSize()))
        }
    }

    override fun getThumbSize() = Size(binding.videoView.width, binding.videoView.height)

    private fun setListener() {
        TransferUpdateObserver.setListener(listenerKey, ::updateState)
    }

    private fun initializePlayer(mediaPath: String?) {
        binding.videoViewController.setPlayerViewAndPath(binding.videoView, mediaPath)
        (bindingAdapter as? MessageFilesAdapter)?.videoControllersList?.add(binding.videoViewController)
    }

    private fun SceytMessageVideoItemBinding.setupStyle() {
        loadProgress.setProgressColor(context.getCompatColor(MessagesStyle.mediaLoaderColor))
    }
}