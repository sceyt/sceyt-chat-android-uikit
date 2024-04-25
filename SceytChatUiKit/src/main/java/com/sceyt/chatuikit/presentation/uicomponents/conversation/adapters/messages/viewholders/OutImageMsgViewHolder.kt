package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemOutImageMessageBinding
import com.sceyt.chatuikit.extensions.setTextAndDrawableByColorId
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
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle


class OutImageMsgViewHolder(
        private val binding: SceytItemOutImageMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        userNameBuilder: ((User) -> String)?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMediaMessageViewHolder(binding.root, style, messageListeners, userNameBuilder = userNameBuilder, needMediaDataCallback = needMediaDataCallback) {

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

            fileImage.setOnClickListener {
                messageListeners?.onAttachmentClick(it, fileItem)
            }

            fileImage.setOnLongClickListener {
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

            if (diff.reactionsChanged)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions)

            if (diff.bodyChanged && !diff.reactionsChanged && recyclerViewReactions != null)
                initWidthsDependReactions(recyclerViewReactions, layoutDetails)

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, binding.viewReply)
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        when (data.state) {
            Downloaded, Uploaded -> {
                viewHolderHelper.drawThumbOrRequest(fileContainer, ::requestThumb)
            }

            PendingUpload, ErrorUpload, PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(fileContainer, ::requestThumb)
            }

            Uploading, Preparing, WaitingToUpload -> {
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(fileContainer, ::requestThumb)
            }

            PendingDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = fileContainer)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
            }

            Downloading -> {
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = fileContainer)
            }

            PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = fileContainer)
            }

            ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = fileContainer)
            }

            FilePathChanged -> {
                if (fileItem.thumbPath.isNullOrBlank())
                    requestThumb()
            }

            ThumbLoaded -> {
                if (isValidThumb(data.thumbData))
                    viewHolderHelper.drawImageWithBlurredThumb(fileItem.thumbPath, fileContainer)
            }
        }
    }

    override val fileContainer: ImageView
        get() = binding.fileImage

    override val loadingProgressView: SceytCircularProgressView
        get() = binding.loadProgress

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, true)

    override val selectMessageView get() = binding.selectView

    private fun SceytItemOutImageMessageBinding.setMessageItemStyle() {
        layoutDetails.backgroundTintList = ColorStateList.valueOf(style.outBubbleColor)
        tvForwarded.setTextAndDrawableByColorId(SceytChatUIKit.theme.accentColor)
        messageBody.applyStyle(style)
    }
}