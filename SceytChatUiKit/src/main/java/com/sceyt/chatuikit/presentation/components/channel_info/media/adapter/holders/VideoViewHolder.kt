package com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.holders

import android.util.Size
import androidx.core.view.isVisible
import com.sceyt.chatuikit.databinding.SceytItemChannelVideoBinding
import com.sceyt.chatuikit.extensions.setDrawableStart
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
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.components.channel_info.ChannelFileItem
import com.sceyt.chatuikit.presentation.components.channel_info.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.chatuikit.styles.ChannelInfoMediaStyle
import com.sceyt.chatuikit.shared.utils.DateTimeUtil

class VideoViewHolder(
        private val binding: SceytItemChannelVideoBinding,
        private val style: ChannelInfoMediaStyle,
        private val clickListeners: AttachmentClickListenersImpl,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseFileViewHolder<ChannelFileItem>(binding.root, needMediaDataCallback) {

    init {
        binding.applyStyle()

        binding.root.setOnClickListener {
            clickListeners.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: ChannelFileItem) {
        super.bind(item)
        setVideoDuration()
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)

        when (data.state) {
            PendingUpload, ErrorUpload, PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(binding.image, ::requestThumb)
            }

            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
                viewHolderHelper.loadBlurThumb(imageView = binding.image)
            }

            Downloading -> {
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = binding.image)
            }

            Uploading -> {
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(binding.image, ::requestThumb)
            }

            Downloaded, Uploaded -> {
                viewHolderHelper.drawThumbOrRequest(binding.image, ::requestThumb)
            }

            PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.image)
            }

            ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.image)
            }

            FilePathChanged -> {
                if (fileItem.thumbPath.isNullOrBlank())
                    requestThumb()
            }

            ThumbLoaded -> {
                if (isValidThumb(data.thumbData))
                    viewHolderHelper.drawImageWithBlurredThumb(fileItem.thumbPath, binding.image)
            }

            Preparing, WaitingToUpload -> Unit
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


    override fun getThumbSize() = Size(binding.root.width, binding.root.height)

    override fun needThumbFor() = ThumbFor.ConversationInfo

    private fun SceytItemChannelVideoBinding.applyStyle() {
        tvDuration.setDrawableStart(style.videoDurationIcon)
    }
}