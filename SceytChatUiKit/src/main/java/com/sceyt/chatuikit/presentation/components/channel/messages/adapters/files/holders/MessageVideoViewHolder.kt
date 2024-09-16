package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders

import android.util.Size
import androidx.core.view.isVisible
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytMessageVideoItemBinding
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ErrorUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.FilePathChanged
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.presentation.customviews.CircularProgressView
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.MessageItemStyle
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

    override val loadingProgressView: CircularProgressView
        get() = binding.loadProgress

    /* private fun initializePlayer(mediaPath: String?) {
          binding.videoViewController.setPlayerViewAndPath(binding.videoView, mediaPath)
          (bindingAdapter as? MessageFilesAdapter)?.videoControllersList?.add(binding.videoViewController)
     }*/

    private fun SceytMessageVideoItemBinding.applyStyle() {
        loadProgress.setProgressColor(style.mediaLoaderColor)
    }
}