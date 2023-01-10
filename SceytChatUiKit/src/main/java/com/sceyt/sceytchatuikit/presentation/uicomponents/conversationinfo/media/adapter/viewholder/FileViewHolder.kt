package com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.viewholder

import com.sceyt.sceytchatuikit.databinding.ItemChannelFileBinding
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferUpdateObserver
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.BaseFileViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversationinfo.media.adapter.listeners.AttachmentClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import java.util.*

class FileViewHolder(private val binding: ItemChannelFileBinding,
                     private val clickListeners: AttachmentClickListenersImpl,
                     private val needMediaDataCallback: (NeedMediaInfoData) -> Unit) : BaseFileViewHolder(binding.root) {

    init {
        binding.setupStyle()
        binding.root.setOnClickListener {
            clickListeners.onAttachmentClick(it, fileItem)
        }
    }

    override fun bind(item: FileListItem) {
        super.bind(item)
        listenerKey = getKey()
        val file = (item as? FileListItem.File)?.file ?: return
        setListener()

        with(binding) {
            tvFileName.text = file.name
            val date = DateTimeUtil.convertDateToString(Date(item.message.createdAt), "dd/MM/yyyy")
            val sizeAndDate = "${file.fileSize.toPrettySize()}, $date"
            tvFileSizeAndDate.text = sizeAndDate
        }

        transferData?.let {
            updateState(it)
            if (it.filePath.isNullOrBlank() && it.state != TransferState.PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
        }
    }

    private fun updateState(data: TransferData) {
        if (isFileItemInitialized.not() || (data.messageTid != fileItem.sceytMessage.tid)) return
        transferData = data
        when (data.state) {
            TransferState.PendingUpload, TransferState.PauseUpload -> {
                binding.icFile.setImageResource(0)
            }
            TransferState.PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem))
            }
            TransferState.Downloading, TransferState.Uploading -> {
                binding.icFile.setImageResource(0)
            }
            TransferState.Uploaded, TransferState.Downloaded -> {
                binding.icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
            }
            TransferState.ErrorUpload, TransferState.ErrorDownload, TransferState.PauseDownload -> {
                binding.icFile.setImageResource(0)
            }
            TransferState.FilePathChanged, TransferState.ThumbLoaded -> return
        }
    }

    private fun setListener() {
        TransferUpdateObserver.setListener(listenerKey, ::updateState)
    }

    private fun ItemChannelFileBinding.setupStyle() {
        icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
    }
}