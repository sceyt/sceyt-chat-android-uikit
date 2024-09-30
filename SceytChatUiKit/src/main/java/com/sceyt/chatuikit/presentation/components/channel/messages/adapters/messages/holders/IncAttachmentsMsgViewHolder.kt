package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemIncAttachmentsMessageBinding
import com.sceyt.chatuikit.extensions.setTextAndDrawableByColorId
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.MessageFilesAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.FilesViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.formatters.UserNameFormatter
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class IncAttachmentsMsgViewHolder(
        private val binding: SceytItemIncAttachmentsMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val viewPoolFiles: RecyclerView.RecycledViewPool,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)?,
        userNameFormatter: UserNameFormatter?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit
) : BaseMsgViewHolder(binding.root, style, messageListeners, displayedListener, userNameFormatter) {
    private var filedAdapter: MessageFilesAdapter? = null

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
        }
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message
                tvForwarded.isVisible = message.isForwarded

                val body = message.body.trim()
                if (body.isNotBlank()) {
                    messageBody.isVisible = true
                    setMessageBody(messageBody, message)
                } else messageBody.isVisible = false

                if (!diff.hasDifference()) return

                if (diff.edited || diff.statusChanged) {
                    setMessageDateDependAttachments(messageDate, message.files)
                    setMessageStatusAndDateText(message, messageDate)
                }

                if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.reactionsChanged || diff.edited)
                    setOrUpdateReactions(item, rvReactions, viewPoolReactions)

                if (diff.filesChanged)
                    setFilesAdapter(message)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, binding.viewReply, false)

                if (item.message.shouldShowAvatarAndName)
                    avatar.setOnClickListener {
                        messageListeners?.onAvatarClick(it, item)
                    }
            }
        }
    }

    override val selectMessageView get() = binding.selectView

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, false)

    private fun setFilesAdapter(message: SceytMessage) {
        val attachments = message.files ?: return

        initFilesRecyclerView(message, binding.rvFiles)

        if (filedAdapter == null) {
            with(binding.rvFiles) {
                setHasFixedSize(true)

                setRecycledViewPool(viewPoolFiles)
                itemAnimator = null
                adapter = MessageFilesAdapter(message, attachments,
                    FilesViewHolderFactory(context = context, messageListeners, needMediaDataCallback).apply {
                        setStyle(style)
                    }).also { filedAdapter = it }
            }
        } else filedAdapter?.notifyUpdate(attachments)

    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        filedAdapter?.onItemDetached()
    }

    override fun setMaxWidth() {
        binding.layoutDetails.layoutParams.width = bubbleMaxWidth
    }

    private fun SceytItemIncAttachmentsMessageBinding.setMessageItemStyle() {
        layoutDetails.backgroundTintList = ColorStateList.valueOf(style.incomingBubbleColor)
        tvForwarded.setTextAndDrawableByColorId(SceytChatUIKit.theme.colors.accentColor)
        messageBody.applyStyle(style)
        style.senderNameTextStyle.apply(tvUserName)
    }
}