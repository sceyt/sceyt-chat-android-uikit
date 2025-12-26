package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemIncSelfDestructingMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
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
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.components.channel.messages.preview.SelfDestructingMediaPreviewActivity
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle


class IncSelfDestructingMessageViewHolder(
        private val binding: SceytItemIncSelfDestructingMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMediaMessageViewHolder(binding.root, style, messageListeners, displayedListener, needMediaDataCallback) {

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
            fileImage.setOnClickListener {
                SelfDestructingMediaPreviewActivity.launchActivity(
                    context = it.context,
                    message = requireMessage,
                    attachment = fileItem.attachment
                )
            }

            fileImage.setOnLongClickListener {
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

            if (diff.filesChanged) {
                initAttachment()
                setImageTopCorners(fileImage)
            }

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, binding.viewReply)

            if (diff.reactionsChanged || diff.edited)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions)

            if (diff.bodyChanged && !diff.reactionsChanged && recyclerViewReactions != null)
                initWidthsDependReactions(recyclerViewReactions, layoutDetails)

            if (item.message.shouldShowAvatarAndName)
                avatar.setOnClickListener {
                    messageListeners?.onAvatarClick(it, item)
                }
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)  // Updates progress indicator

        when (data.state) {
            Downloaded, Uploaded -> {
                // ALWAYS show blur, NEVER actual image
                viewHolderHelper.loadBlurThumb(imageView = fileContainer)
                showSelfDestructIcon()
            }

            PendingUpload, ErrorUpload, PauseUpload,
            Uploading, Preparing, WaitingToUpload -> {
                viewHolderHelper.loadBlurThumb(imageView = fileContainer)
                hideSelfDestructIcon()
            }

            PendingDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = fileContainer)
                hideSelfDestructIcon()
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
            }

            Downloading, PauseDownload, ErrorDownload -> {
                viewHolderHelper.loadBlurThumb(imageView = fileContainer)
                hideSelfDestructIcon()
            }

            ThumbLoaded -> {
                viewHolderHelper.loadBlurThumb(imageView = fileContainer)
            }

            FilePathChanged -> {
                if (fileItem.thumbPath.isNullOrBlank())
                    requestThumb()
            }
        }
    }

    private fun showSelfDestructIcon() {
        binding.ivSelfDestructIcon.isVisible = true
        binding.loadProgress.isVisible = false
    }

    private fun hideSelfDestructIcon() {
        binding.ivSelfDestructIcon.isVisible = false
    }

    override val selectMessageView
        get() = binding.selectView

    override val fileContainer: ImageView
        get() = binding.fileImage

    override val loadingProgressView: CircularProgressView
        get() = binding.loadProgress

    override val incoming: Boolean
        get() = true

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, true)

    private fun SceytItemIncSelfDestructingMessageBinding.setMessageItemStyle() {
        style.overlayMediaLoaderStyle.apply(loadProgress)
        applyCommonStyle(
            layoutDetails = layoutDetails,
            tvForwarded = tvForwarded,
            messageBody = messageBody,
            tvThreadReplyCount = tvReplyCount,
            toReplyLine = toReplyLine,
            tvSenderName = tvUserName,
            avatarView = avatar
        )
    }
}