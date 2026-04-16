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
import com.sceyt.chatuikit.styles.preview.MediaPreviewStyle

class MediaImageViewHolder(
    private val binding: SceytMediaItemImageBinding,
    private val style: MediaPreviewStyle,
    private val clickListeners: (MediaItem) -> Unit,
    private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseFileViewHolder<MediaItem>(binding.root, needMediaDataCallback),
    SharedTransitionViewProvider {
    private var pendingReadyCallback: (() -> Unit)? = null
    private var isOriginalImageReady = false

    init {
        binding.applyStyle()

        binding.imageView.setOnClickListener {
            clickListeners.invoke(fileItem)
        }
    }

    override fun bind(item: MediaItem) {
        super.bind(item)
        pendingReadyCallback = null
        isOriginalImageReady = false
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        binding.progress.isVisible = data.state == Downloading || data.state == PendingDownload

        when (data.state) {
            PendingUpload, ErrorUpload, PauseUpload -> {
                drawOriginalFile()
            }

            Uploading -> {
                if (isOnBind)
                    drawOriginalFile()
            }

            Uploaded -> {
                drawOriginalFile()
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
                drawOriginalFile()
            }

            PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.imageView)
            }

            ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = binding.imageView)
            }

            FilePathChanged -> {
                drawOriginalFile()
            }

            ThumbLoaded, Preparing, WaitingToUpload -> Unit
        }
    }

    override fun needThumbFor() = ThumbFor.MediaPreview

    override fun provide() = binding.imageView

    override fun awaitReadyForSharedTransition(onReady: () -> Unit) {
        if (isOriginalImageReady) {
            onReady()
        } else {
            pendingReadyCallback = onReady
        }
    }

    private fun drawOriginalFile() {
        val filePath = fileItem.attachment.filePath
        isOriginalImageReady = false
        viewHolderHelper.drawOriginalFile(binding.imageView) ready@{
            if (filePath.isNullOrBlank() || filePath != fileItem.attachment.filePath) {
                return@ready
            }
            isOriginalImageReady = true
            pendingReadyCallback?.invoke()
            pendingReadyCallback = null
        }
    }

    private fun SceytMediaItemImageBinding.applyStyle() {
        style.mediaLoaderStyle.apply(progress)
    }
}