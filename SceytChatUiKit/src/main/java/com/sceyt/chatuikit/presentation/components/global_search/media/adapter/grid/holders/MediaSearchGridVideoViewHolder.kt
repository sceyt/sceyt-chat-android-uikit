package com.sceyt.chatuikit.presentation.components.global_search.media.adapter.grid.holders

import android.util.Size
import androidx.core.view.isVisible
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
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
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.styles.channel_info.media.ChannelInfoMediaItemStyle

class MediaSearchGridVideoViewHolder(
    private val binding: SceytItemChannelVideoBinding,
    private val style: ChannelInfoMediaItemStyle,
    private val mediaDataCallback: (NeedMediaInfoData) -> Unit,
    private val onItemClick: (GlobalSearchAttachmentResult) -> Unit,
) : BaseFileViewHolder<GlobalSearchListItem.AttachmentItem>(binding.root, mediaDataCallback) {

    init {
        with(binding.tvDuration) {
            setDrawableStart(style.videoDurationIcon)
            style.videoDurationTextStyle.apply(this)
        }
        binding.root.setOnClickListener { onItemClick(fileItem.result) }
    }

    override fun bind(item: GlobalSearchListItem.AttachmentItem) {
        super.bind(item)
        setVideoDuration()
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        when (data.state) {
            PendingDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.image)
                mediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
            }

            Downloading -> {
                if (isOnBind) viewHolderHelper.loadBlurThumb(imageView = binding.image)
            }

            Downloaded, Uploaded -> {
                viewHolderHelper.drawThumbOrRequest(binding.image, ::requestThumb)
            }

            PendingUpload, ErrorUpload, PauseUpload, Uploading -> {
                if (isOnBind) viewHolderHelper.drawThumbOrRequest(binding.image, ::requestThumb)
            }

            PauseDownload, ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.image)
            }

            FilePathChanged -> {
                if (fileItem.thumbPath.isNullOrBlank()) requestThumb()
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
                text = style.durationFormatter.format(context, it)
                isVisible = true
            } ?: run { isVisible = false }
        }
    }

    override fun getThumbSize() = Size(binding.root.width, binding.root.height)

    override fun needThumbFor() = ThumbFor.GlobalSearch
}
