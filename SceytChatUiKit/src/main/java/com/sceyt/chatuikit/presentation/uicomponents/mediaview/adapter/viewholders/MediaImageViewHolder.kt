package com.sceyt.chatuikit.presentation.uicomponents.mediaview.adapter.viewholders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.databinding.SceytMediaItemImageBinding
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
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.mediaview.adapter.MediaItem

class MediaImageViewHolder(private val binding: SceytMediaItemImageBinding,
                           private val clickListeners: (MediaItem) -> Unit,
                           private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseFileViewHolder<MediaItem>(binding.root, needMediaDataCallback) {

    init {
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
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
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
}