package com.sceyt.chatuikit.presentation.components.media.adapter.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.databinding.SceytMediaItemImageBinding
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
import com.sceyt.chatuikit.presentation.components.media.adapter.MediaItem
import com.sceyt.chatuikit.styles.MediaPreviewStyle

class MediaImageViewHolder(
        private val binding: SceytMediaItemImageBinding,
        private val style: MediaPreviewStyle,
        private val clickListeners: (MediaItem) -> Unit,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseFileViewHolder<MediaItem>(binding.root, needMediaDataCallback) {

    init {
        binding.applyStyle()

        binding.imageView.setOnClickListener {
            clickListeners.invoke(fileItem)
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        binding.progress.isVisible = data.state == Downloading || data.state == PendingDownload

        when (data.state) {
            PendingUpload, ErrorUpload, PauseUpload -> {
                viewHolderHelper.drawOriginalFile(binding.imageView)
            }

            Uploading -> {
                if (isOnBind)
                    viewHolderHelper.drawOriginalFile(binding.imageView)
            }

            Uploaded -> {
                viewHolderHelper.drawOriginalFile(binding.imageView)
            }

            PendingDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.imageView)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
            }

            Downloading -> {
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = binding.imageView)

                binding.progress.setProgress(data.progressPercent)
            }

            Downloaded -> {
                viewHolderHelper.drawOriginalFile(binding.imageView)
            }

            PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.imageView)
            }

            ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.imageView)
            }

            FilePathChanged -> {
                viewHolderHelper.drawOriginalFile(binding.imageView)
            }

            ThumbLoaded, Preparing, WaitingToUpload -> Unit
        }
    }

    override fun needThumbFor() = ThumbFor.MediaPreview

    private fun SceytMediaItemImageBinding.applyStyle() {
        style.mediaLoaderStyle.apply(progress)
    }
}