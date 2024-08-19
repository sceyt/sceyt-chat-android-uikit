package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders

import android.util.Size
import androidx.core.view.isVisible
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytMessageVideoItemBinding
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.filetransfer.ThumbFor
import com.sceyt.chatuikit.persistence.filetransfer.TransferData
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.filetransfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle
import com.sceyt.chatuikit.shared.utils.DateTimeUtil


class MessageVideoViewHolder(
        private val binding: SceytMessageVideoItemBinding,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseMessageFileViewHolder<FileListItem>(binding.root, needMediaDataCallback) {

    init {
        binding.applyStyle()

        binding.root.setOnClickListener {
            messageListeners?.onAttachmentClick(it, fileItem, message)
        }

        binding.root.setOnLongClickListener {
            messageListeners?.onAttachmentLongClick(it, fileItem, message)
            return@setOnLongClickListener true
        }

        binding.loadProgress.setOnClickListener {
            messageListeners?.onAttachmentLoaderClick(it, fileItem, message)
        }
    }

    override fun bind(item: FileListItem, message: SceytMessage) {
        super.bind(item, message)
        /* binding.parentLayout.clipToOutline = true
         binding.videoView.isVisible = false*/
        setVideoDuration()
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        val imageView = binding.imageThumb

        when (data.state) {
            Downloaded, Uploaded -> {
                binding.playPauseItem.isVisible = true
                viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
            }

            PendingUpload, ErrorUpload, PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
                binding.playPauseItem.isVisible = false
            }

            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
                binding.playPauseItem.isVisible = false
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }

            Downloading -> {
                binding.playPauseItem.isVisible = false
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = imageView)
            }

            Uploading, Preparing -> {
                binding.playPauseItem.isVisible = false
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
            }

            WaitingToUpload -> {
                binding.playPauseItem.isVisible = false
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(imageView, ::requestThumb)
            }

            PauseDownload -> {
                binding.playPauseItem.isVisible = false
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }

            ErrorDownload -> {
                binding.playPauseItem.isVisible = false
                viewHolderHelper.loadBlurThumb(imageView = imageView)
            }

            FilePathChanged -> {
                if (fileItem.thumbPath.isNullOrBlank())
                    requestThumb()
            }

            ThumbLoaded -> {
                if (isValidThumb(data.thumbData))
                    viewHolderHelper.drawImageWithBlurredThumb(fileItem.thumbPath, imageView)
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

    override fun needThumbFor() = ThumbFor.MessagesLisView

    override val loadingProgressView: SceytCircularProgressView
        get() = binding.loadProgress

    /* private fun initializePlayer(mediaPath: String?) {
          binding.videoViewController.setPlayerViewAndPath(binding.videoView, mediaPath)
          (bindingAdapter as? MessageFilesAdapter)?.videoControllersList?.add(binding.videoViewController)
     }*/

    private fun SceytMessageVideoItemBinding.applyStyle() {
        loadProgress.setProgressColor(style.mediaLoaderColor)
    }
}