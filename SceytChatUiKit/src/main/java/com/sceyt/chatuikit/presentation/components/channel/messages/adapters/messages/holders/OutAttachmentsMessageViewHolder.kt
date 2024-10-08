package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemOutAttachmentsMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.MessageFilesAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.holders.FilesViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class OutAttachmentsMessageViewHolder(
        private val binding: SceytItemOutAttachmentsMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val viewPoolFiles: RecyclerView.RecycledViewPool,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMessageViewHolder(binding.root, style, messageListeners) {
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

                if (diff.filesChanged)
                    setFilesAdapter(message)

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.reactionsChanged || diff.edited)
                    setOrUpdateReactions(item, rvReactions, viewPoolReactions)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, binding.viewReply, false)
            }
        }
    }

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, false)

    override val selectMessageView get() = binding.selectView

    override val incoming: Boolean
        get() = false

    private fun setFilesAdapter(message: SceytMessage) {
        val attachments = ArrayList(message.files ?: return)

        initFilesRecyclerView(message, binding.rvFiles)

        if (filedAdapter == null) {
            with(binding.rvFiles) {
                setHasFixedSize(true)

                setRecycledViewPool(viewPoolFiles)
                itemAnimator = null
                adapter = MessageFilesAdapter(
                    message = message,
                    files = attachments,
                    viewHolderFactory = FilesViewHolderFactory(context = context, messageListeners, needMediaDataCallback).apply {
                        setStyle(style)
                    }).also { filedAdapter = it }
            }
        } else filedAdapter?.notifyUpdate(attachments)
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        filedAdapter?.onItemDetached()
    }

    private fun SceytItemOutAttachmentsMessageBinding.setMessageItemStyle() {
        applyCommonStyle(
            layoutDetails = layoutDetails, tvForwarded = tvForwarded,
            messageBody = messageBody,
            tvThreadReplyCount = tvReplyCount,
            toReplyLine = toReplyLine
        )
    }

    override fun setMaxWidth() {
        binding.layoutDetails.layoutParams.width = bubbleMaxWidth
    }
}