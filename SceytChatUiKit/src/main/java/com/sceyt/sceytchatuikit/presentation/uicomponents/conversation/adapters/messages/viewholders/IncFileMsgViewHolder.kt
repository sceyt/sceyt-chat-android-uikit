package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.databinding.SceytItemIncFileMessageBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.extensions.setTextAndDrawableColor
import com.sceyt.sceytchatuikit.extensions.toPrettySize
import com.sceyt.sceytchatuikit.persistence.differs.MessageDiff
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Downloading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ErrorUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.FilePathChanged
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingDownload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Preparing
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.WaitingToUpload
import com.sceyt.sceytchatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle

class IncFileMsgViewHolder(
        private val binding: SceytItemIncFileMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)?,
        userNameBuilder: ((User) -> String)?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMediaMessageViewHolder(binding.root, messageListeners, displayedListener, userNameBuilder, needMediaDataCallback) {

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

            if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                setMessageUserAvatarAndName(avatar, tvUserName, message)

            if (diff.replyCountChanged)
                setReplyCount(tvReplyCount, toReplyLine, item)

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, binding.viewReply, false)

            if (diff.filesChanged)
                initAttachment()

            if (diff.reactionsChanged)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions)

            if (item.message.shouldShowAvatarAndName)
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
                binding.icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
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

    override fun setMaxWidth() {
        binding.layoutDetails.layoutParams.width = bubbleMaxWidth
    }

    private fun SceytItemIncFileMessageBinding.setMessageItemStyle() {
        with(context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColor(MessagesStyle.incBubbleColor))
            tvUserName.setTextColor(getCompatColor(MessagesStyle.senderNameTextColor))
            tvForwarded.setTextAndDrawableColor(SceytKitConfig.sceytColorAccent)
            loadProgress.setBackgroundColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
            icFile.setImageResource(MessagesStyle.fileAttachmentIcon)
            icFile.backgroundTintList = ColorStateList.valueOf(getCompatColor(SceytKitConfig.sceytColorAccent))
        }
    }
}