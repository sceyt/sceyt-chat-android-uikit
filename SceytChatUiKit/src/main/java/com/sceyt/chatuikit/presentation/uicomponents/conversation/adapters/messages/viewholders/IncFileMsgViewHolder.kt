package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.databinding.SceytItemIncFileMessageBinding
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
import com.sceyt.chatuikit.presentation.customviews.CircularProgressView
import com.sceyt.chatuikit.presentation.uicomponents.conversation.ShowAvatarType
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle

class IncFileMsgViewHolder(
        private val binding: SceytItemIncFileMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)?,
        userNameFormatter: UserNameFormatter?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMediaMessageViewHolder(binding.root, style, messageListeners, displayedListener, userNameFormatter, needMediaDataCallback) {

    init {
        with(binding) {
            setMessageItemStyle()

            root.setOnClickListener {
                messageListeners?.onMessageClick(it, requireMessageItem)
            }

            root.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, requireMessageItem)
                return@setOnLongClickListener true
            }

            messageBody.doOnLongClick {
                messageListeners?.onMessageLongClick(it, requireMessageItem)
            }

            messageBody.doOnClickWhenNoLink {
                messageListeners?.onMessageClick(it, requireMessageItem)
            }

            viewHandleClick.setOnClickListener {
                messageListeners?.onAttachmentClick(it, fileItem, requireMessage)
            }

            viewHandleClick.setOnLongClickListener {
                messageListeners?.onAttachmentLongClick(it, fileItem, requireMessage)
                return@setOnLongClickListener true
            }

            loadProgress.setOnClickListener {
                messageListeners?.onAttachmentLoaderClick(it, fileItem, requireMessage)
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

            if (diff.avatarChanged || diff.showAvatarTypeChanged)
                setMessageUserAvatarAndName(avatar, tvUserName, message)

            if (diff.replyCountChanged)
                setReplyCount(tvReplyCount, toReplyLine, item)

            if (diff.filesChanged)
                initAttachment()

            if (diff.reactionsChanged || diff.edited)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions)

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, binding.viewReply, false)

            if (item.message.showAvatarType in ShowAvatarType.avatarSupport)
                avatar.setOnClickListener {
                    messageListeners?.onAvatarClick(it, item)
                }
        }
    }

    override val selectMessageView get() = binding.selectView

    override val layoutBubbleConfig get() = Pair(binding.root, false)

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

    override val loadingProgressView: CircularProgressView
        get() = binding.loadProgress

    override fun setMaxWidth() {
        binding.layoutDetails.layoutParams.width = bubbleMaxWidth
    }

    private fun SceytItemIncFileMessageBinding.setMessageItemStyle() {
        val accentColor = context.getCompatColor(SceytChatUIKit.theme.accentColor)
        layoutDetails.backgroundTintList = ColorStateList.valueOf(style.incBubbleColor)
        tvUserName.setTextColor(style.senderNameTextColor)
        tvForwarded.setTextAndDrawableByColor(accentColor)
        loadProgress.setBackgroundColor(accentColor)
        icFile.setImageDrawable(style.fileAttachmentIcon)
        icFile.backgroundTintList = ColorStateList.valueOf(accentColor)
        messageBody.applyStyle(style)
        tvFileSize.setTextColor(context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))
        tvFileName.setTextColor(context.getCompatColor(SceytChatUIKit.theme.textPrimaryColor))
    }
}