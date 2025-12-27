package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemIncSelfDestructingMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Downloaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingDownload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMediaMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.custom_views.CircularProgressView
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle


class IncSelfDestructingMessageViewHolder(
    private val binding: SceytItemIncSelfDestructingMessageBinding,
    private val viewPoolReactions: RecyclerView.RecycledViewPool,
    private val style: MessageItemStyle,
    private val messageListeners: MessageClickListeners.ClickListeners?,
    displayedListener: ((MessageListItem) -> Unit)?,
    private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMediaMessageViewHolder(
    binding.root,
    style,
    messageListeners,
    displayedListener,
    needMediaDataCallback
) {

    private var isMediaReady = false

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

            fileImage.setOnClickListener {
                if (isMediaReady) {
                    messageListeners?.onAttachmentClick(it, fileItem, requireMessage)
                }
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

            if (!diff.hasDifference()) return

            if (diff.edited || diff.statusChanged)
                setMessageStatusAndDateText(message, messageDate)

            setImageSize(fileContainer, ignoreBody = true)

            if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                setMessageUserAvatarAndName(avatar, tvUserName, message)

            if (diff.replyCountChanged)
                setReplyCount(tvReplyCount, toReplyLine, item)

            if (diff.filesChanged) {
                initAttachment()
                setImageTopCorners(fileImage, ignoreBody = true)
            }

            if (diff.replyContainerChanged)
                setReplyMessageContainer(message, binding.viewReply)

            if (diff.reactionsChanged || diff.edited)
                setOrUpdateReactions(item, rvReactions, viewPoolReactions)

            if (item.message.shouldShowAvatarAndName)
                avatar.setOnClickListener {
                    messageListeners?.onAvatarClick(it, item)
                }

            viewHolderHelper.loadBlurThumb(imageView = fileContainer)
        }
    }

    override fun updateState(data: TransferData, isOnBind: Boolean) {
        super.updateState(data, isOnBind)
        when (data.state) {
            Downloaded, Uploaded, ThumbLoaded -> {
                isMediaReady = true
                showSelfDestructIcon()
            }

            PendingDownload -> {
                needMediaDataCallback.invoke(NeedMediaInfoData.NeedDownload(fileItem.attachment))
                hideSelfDestructIcon()
            }

            else -> hideSelfDestructIcon()
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
            messageBody = null,
            tvThreadReplyCount = tvReplyCount,
            toReplyLine = toReplyLine,
            tvSenderName = tvUserName,
            avatarView = avatar
        )
    }
}