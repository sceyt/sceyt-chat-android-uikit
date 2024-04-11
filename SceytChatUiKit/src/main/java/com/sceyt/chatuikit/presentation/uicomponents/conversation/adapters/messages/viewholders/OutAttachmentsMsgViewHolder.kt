package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.databinding.SceytItemOutAttachmentsMessageBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.setTextAndDrawableColor
import com.sceyt.chatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.files.viewholders.FilesViewHolderFactory
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.sceytstyles.MessagesStyle
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig

class OutAttachmentsMsgViewHolder(
        private val binding: SceytItemOutAttachmentsMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        private val viewPoolFiles: RecyclerView.RecycledViewPool,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        userNameBuilder: ((User) -> String)?,
        private val needMediaDataCallback: (NeedMediaInfoData) -> Unit,
) : BaseMsgViewHolder(binding.root, messageListeners, userNameBuilder = userNameBuilder) {
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

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPoolReactions)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, binding.viewReply, false)
            }
        }
    }

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, false)

    override val selectMessageView get() = binding.selectView

    private fun setFilesAdapter(message: SceytMessage) {
        val attachments = ArrayList(message.files ?: return)

        initFilesRecyclerView(message, binding.rvFiles)

        if (filedAdapter == null) {
            with(binding.rvFiles) {
                setHasFixedSize(true)

                setRecycledViewPool(viewPoolFiles)
                itemAnimator = null
                adapter = MessageFilesAdapter(attachments, FilesViewHolderFactory(context = context, messageListeners, needMediaDataCallback)).also {
                    filedAdapter = it
                }
            }
        } else filedAdapter?.notifyUpdate(attachments)
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        filedAdapter?.onItemDetached()
    }

    private fun SceytItemOutAttachmentsMessageBinding.setMessageItemStyle() {
        with(context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColor(MessagesStyle.outBubbleColor))
            tvForwarded.setTextAndDrawableColor(SceytKitConfig.sceytColorAccent)
        }
    }

    override fun setMaxWidth() {
        binding.layoutDetails.layoutParams.width = bubbleMaxWidth
    }
}