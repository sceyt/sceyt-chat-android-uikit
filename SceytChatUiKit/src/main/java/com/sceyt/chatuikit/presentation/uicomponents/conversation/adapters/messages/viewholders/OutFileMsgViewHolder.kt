package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.databinding.SceytItemOutFileMessageBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setTextAndDrawableByColor
import com.sceyt.chatuikit.extensions.toPrettySize
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
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
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle


class OutFileMsgViewHolder(
        private val binding: SceytItemOutFileMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        userNameFormatter: UserNameFormatter?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMediaMessageViewHolder(binding.root, style, messageListeners, userNameFormatter = userNameFormatter, needMediaDataCallback = needMediaDataCallback) {

    init {
        with(binding) {
            setMessageItemStyle()

            root.setOnClickListener {
                messageListeners?.onMessageClick(it, messageListItem as MessageListItem.MessageItem)
            }

            root.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, messageListItem as MessageListItem.MessageItem)
                return@setOnLongClickListener true
            }

            messageBody.doOnLongClick {
                messageListeners?.onMessageLongClick(it, messageListItem as MessageListItem.MessageItem)
            }

            messageBody.doOnClickWhenNoLink {
                messageListeners?.onMessageClick(it, messageListItem as MessageListItem.MessageItem)
            }

            viewHandleClick.setOnClickListener {
                messageListeners?.onAttachmentClick(it, fileItem)
            }

            viewHandleClick.setOnLongClickListener {
                messageListeners?.onAttachmentLongClick(it, fileItem)
                return@setOnLongClickListener true
            }

            loadProgress.setOnClickListener {
                messageListeners?.onAttachmentLoaderClick(it, fileItem)
            }
        }
    }


    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)
        setFileDetails(fileItem.file)

        with(binding) {
            val message = (item as MessageListItem.MessageItem).message
            tvForwarded.isVisible = message.isForwarded

            val body = message.body.trim()
            if (body.isNotBlank()) {
                messageBody.isVisible = true
                setMessageBody(messageBody, message)
            } else messageBody.isVisible = false

            if (!diff.hasDifference()) return

            if (diff.edited || diff.statusChanged)
                setMessageStatusAndDateText(message, messageDate)

            if (diff.replyCountChanged)
                setReplyCount(tvReplyCount, toReplyLine, item)

            if (diff.filesChanged)
                initAttachment()

            if (diff.reactionsChanged || diff.edited)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions)

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, binding.viewReply, false)
        }
    }

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, false)

    private fun setFileDetails(file: SceytAttachment) {
        with(binding) {
            tvFileName.text = file.name
            tvFileSize.text = file.fileSize.toPrettySize()
        }
    }

    private fun setProgress(data: TransferData) {
        if (!data.isCalculatedLoadedSize()) return
        val text = "${data.fileLoadedSize} • ${data.fileTotalSize}"
        binding.tvFileSize.text = text
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        when (data.state) {
            Uploaded, Downloaded -> {
                binding.icFile.setImageDrawable(style.fileAttachmentIcon)
                binding.tvFileSize.text = data.fileTotalSize
                        ?: fileItem.file.fileSize.toPrettySize()
            }

            PendingUpload -> {
                binding.icFile.setImageResource(0)
            }

            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
            }

            Downloading, Uploading, Preparing, WaitingToUpload -> {
                binding.icFile.setImageResource(0)
                setProgress(data)
            }

            ErrorUpload, ErrorDownload, PauseDownload, PauseUpload -> {
                binding.icFile.setImageResource(0)
            }

            FilePathChanged, ThumbLoaded -> return
        }
    }

    override val loadingProgressView: SceytCircularProgressView
        get() = binding.loadProgress

    override val selectMessageView get() = binding.selectView

    override fun setMaxWidth() {
        binding.layoutDetails.layoutParams.width = bubbleMaxWidth
    }

    private fun SceytItemOutFileMessageBinding.setMessageItemStyle() {
        val accentColor = context.getCompatColor(SceytChatUIKit.theme.accentColor)
        layoutDetails.backgroundTintList = ColorStateList.valueOf(style.outBubbleColor)
        tvForwarded.setTextAndDrawableByColor(accentColor)
        icFile.setImageDrawable(style.fileAttachmentIcon)
        icFile.backgroundTintList = ColorStateList.valueOf(accentColor)
        loadProgress.setBackgroundColor(accentColor)
        messageBody.applyStyle(style)
        tvFileSize.setTextColor(context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))
        tvFileName.setTextColor(context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))
    }
}