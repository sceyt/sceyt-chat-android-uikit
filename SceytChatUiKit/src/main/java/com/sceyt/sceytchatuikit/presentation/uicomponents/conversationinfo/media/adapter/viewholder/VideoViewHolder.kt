package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import android.util.Size
import androidx.core.view.isVisible
import com.sceyt.sceytchatuikit.databinding.ItemChannelVideoBinding
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferUpdateObserver
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil

class VideoViewHolder(private val binding: ItemChannelVideoBinding,
                      private val clickListeners: AttachmentClickListenersImpl,
                      private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseFileViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            clickListeners.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        listenerKey = getKey()
        setVideoDuration()

        setListener()

        transferData?.let {
            updateState(it, true)
            if (it.filePath.isNullOrBlank())
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
        }

        if (fileItem.thumbPath.isNullOrBlank())
            requestThumb()
    }

    private fun updateState(data: TransferData, isOnBind: Boolean = false) {
        if (isFileItemInitialized.not() || (data.messageTid != fileItem.sceytMessage.tid)) return
        transferData = data
        when (data.state) {
            TransferState.PendingUpload, TransferState.ErrorUpload, TransferState.PauseUpload -> {
                drawThumbOrRequest(binding.image, ::requestThumb)
            }
            TransferState.PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
                loadBlurThumb(blurredThumb, binding.image)
            }
            TransferState.Downloading -> {
                if (isOnBind)
                    loadBlurThumb(blurredThumb, binding.image)
            }
            TransferState.Uploading -> {
                if (isOnBind)
                    drawThumbOrRequest(binding.image, ::requestThumb)
            }
            TransferState.Downloaded, TransferState.Uploaded -> {
                drawThumbOrRequest(binding.image, ::requestThumb)
            }
            TransferState.PauseDownload -> {
                loadBlurThumb(blurredThumb, binding.image)
            }
            TransferState.ErrorDownload -> {
                loadBlurThumb(blurredThumb, binding.image)
            }
            TransferState.FilePathChanged -> {
                requestThumb()
            }
            TransferState.ThumbLoaded -> {
                loadThumb(data.filePath, binding.image)
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

    override fun getThumbSize() = Size(binding.root.width, binding.root.height)

    private fun setListener() {
        TransferUpdateObserver.setListener(listenerKey, ::updateState)
    }
}