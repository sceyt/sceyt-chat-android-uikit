package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders

import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytMessageFileItemBinding
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setBackgroundTintColorRes
import com.sceyt.chatuikit.extensions.toPrettySize
import com.sceyt.chatuikit.persistence.file_transfer.FileTransferHelper
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
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
import com.sceyt.chatuikit.persistence.file_transfer.getProgressWithState
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class MessageFileViewHolder(
        private val binding: SceytMessageFileItemBinding,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMessageFileViewHolder(binding.root, needMediaDataCallback) {

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
        val attachment = item.attachment
        setListener()

        with(binding) {
            tvFileName.text = attachment.name
            loadProgress.release(attachment.progressPercent)
            tvFileSize.text = attachment.fileSize.toPrettySize()
        }

        fileItem.transferData?.let {
            updateState(it)
            if (it.filePath.isNullOrBlank() && it.state != PendingDownload)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(attachment))
        }
    }

    private fun updateState(data: TransferData) {
        if (!viewHolderHelper.updateTransferData(data, fileItem, ::isValidThumb)) return

        binding.loadProgress.getProgressWithState(data.state, style.mediaLoaderStyle,
            message.deliveryStatus != DeliveryStatus.Pending, data.progressPercent)
        when (data.state) {
            PendingUpload -> {
                binding.icFile.setImageResource(0)
            }

            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
            }

            Downloading, Uploading, Preparing, WaitingToUpload -> {
                binding.icFile.setImageResource(0)
            }

            Uploaded, Downloaded -> {
                val icon = style.attachmentIconProvider.provide(context, fileItem.attachment)
                binding.icFile.setImageDrawable(icon)
            }

            ErrorUpload, ErrorDownload, PauseDownload, PauseUpload -> {
                binding.icFile.setImageResource(0)
            }

            FilePathChanged, ThumbLoaded -> return
        }
    }

    private fun setListener() {
        FileTransferHelper.onTransferUpdatedLiveData.observe(context.asComponentActivity(), ::updateState)
    }

    private fun SceytMessageFileItemBinding.applyStyle() {
        loadProgress.setBackgroundColor(context.getCompatColor(SceytChatUIKit.theme.colors.accentColor))
        icFile.setBackgroundTintColorRes(SceytChatUIKit.theme.colors.accentColor)
    }
}