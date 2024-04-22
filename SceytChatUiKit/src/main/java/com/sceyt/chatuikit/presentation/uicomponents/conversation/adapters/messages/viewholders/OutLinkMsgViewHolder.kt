package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemOutLinkMessageBinding
import com.sceyt.chatuikit.extensions.setTextAndDrawableByColorId
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseLinkMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.sceytstyles.MessagesListViewStyle
import com.sceyt.chatuikit.shared.helpers.LinkPreviewHelper

class OutLinkMsgViewHolder(
        private val binding: SceytItemOutLinkMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
        linkPreview: LinkPreviewHelper,
        private val style: MessagesListViewStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        userNameBuilder: ((User) -> String)?
) : BaseLinkMsgViewHolder(linkPreview, binding.root, style, messageListeners, userNameBuilder = userNameBuilder) {

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

        if (!diff.hasDifference()) return

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message
                tvForwarded.isVisible = message.isForwarded
                val linkAttachment = message.attachments?.getOrNull(0)

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.edited || diff.bodyChanged) {
                    setMessageBody(messageBody, message, checkLinks = true, isLinkViewHolder = true)
                    setBodyTextPosition(messageBody, messageDate, layoutDetails)
                }

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPool)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, viewReply)

                loadLinkPreview(message, linkAttachment, layoutLinkPreview)
            }
        }
    }

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, true)

    override val selectMessageView get() = binding.selectView

    private fun SceytItemOutLinkMessageBinding.setMessageItemStyle() {
        layoutDetails.backgroundTintList = ColorStateList.valueOf(style.outBubbleColor)
        tvForwarded.setTextAndDrawableByColorId(SceytChatUIKit.theme.accentColor)
        messageBody.setLinkTextColor(style.autoLinkTextColor)
    }
}