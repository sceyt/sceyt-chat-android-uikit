package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.databinding.SceytItemIncImageMessageBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.extensions.setTextAndDrawableColor
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.presentation.customviews.SceytCircularProgressView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig


class IncImageMsgViewHolder(
        private val binding: SceytItemIncImageMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)?,
        senderNameBuilder: ((User) -> String)?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMediaMessageViewHolder(binding.root, messageListeners, displayedListener, senderNameBuilder, needMediaDataCallback) {

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


    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
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

            if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                setMessageUserAvatarAndName(avatar, tvUserName, message)

            if (diff.replyCountChanged)
                setReplyCount(tvReplyCount, toReplyLine, item)

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, binding.viewReply)

            if (diff.filesChanged)
                initAttachment()

            if (diff.reactionsChanged)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions, binding.layoutDetails)

            if (diff.bodyChanged && !diff.reactionsChanged && recyclerViewReactions != null)
                initWidthsDependReactions(recyclerViewReactions, layoutDetails)

            if (item.message.canShowAvatarAndName)
                avatar.setOnClickListener {
                    messageListeners?.onAvatarClick(it, item)
                }
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        when (data.state) {
            TransferState.Uploaded, TransferState.Downloaded -> {
                viewHolderHelper.drawThumbOrRequest(fileContainer, ::requestThumb)
            }
            TransferState.PendingUpload, TransferState.ErrorUpload, TransferState.PauseUpload -> {
                viewHolderHelper.drawThumbOrRequest(fileContainer, ::requestThumb)
            }
            TransferState.Uploading -> {
                if (isOnBind)
                    viewHolderHelper.drawThumbOrRequest(fileContainer, ::requestThumb)
            }
            TransferState.PendingDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = fileContainer)
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.file))
            }
            TransferState.Downloading -> {
                if (isOnBind)
                    viewHolderHelper.loadBlurThumb(imageView = fileContainer)
            }
            TransferState.PauseDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = fileContainer)
            }
            TransferState.ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = fileContainer)
            }
            TransferState.FilePathChanged -> {
                requestThumb()
            }
            TransferState.ThumbLoaded -> {
                viewHolderHelper.drawImageWithBlurredThumb(fileItem.thumbPath, fileContainer)
            }
        }
    }

    override val fileContainer: ImageView
        get() = binding.fileImage

    override val loadingProgressView: SceytCircularProgressView
        get() = binding.loadProgress

    override val layoutDetails: ConstraintLayout
        get() = binding.layoutDetails

    private fun SceytItemIncImageMessageBinding.setMessageItemStyle() {
        with(context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.incBubbleColor))
            tvUserName.setTextColor(getCompatColorByTheme(MessagesStyle.senderNameTextColor))
            tvForwarded.setTextAndDrawableColor(SceytKitConfig.sceytColorAccent)
        }
    }
}