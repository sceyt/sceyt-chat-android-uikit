package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemIncTextMessageBinding
import com.sceyt.chatuikit.extensions.setTextAndDrawableByColorId
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.sceytconfigs.UserNameFormatter
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle

class IncTextMsgViewHolder(
        private val binding: SceytItemIncTextMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
        private val style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)?,
        userNameFormatter: UserNameFormatter?
) : BaseMsgViewHolder(binding.root, style, messageListeners, displayedListener, userNameFormatter) {

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

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.edited || diff.bodyChanged) {
                    setMessageBody(messageBody, message, false)
                    setBodyTextPosition(messageBody, messageDate, layoutDetails)
                }

                if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.reactionsChanged || diff.edited)
                    setOrUpdateReactions(item, rvReactions, viewPool)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, viewReply)

                if (item.message.shouldShowAvatarAndName)
                    avatar.setOnClickListener {
                        messageListeners?.onAvatarClick(it, item)
                    }
            }
        }
    }

    override val selectMessageView get() = binding.selectView

    private fun SceytItemIncTextMessageBinding.setMessageItemStyle() {
        layoutDetails.backgroundTintList = ColorStateList.valueOf(style.incBubbleColor)
        tvUserName.setTextColor(style.senderNameTextColor)
        tvForwarded.setTextAndDrawableByColorId(SceytChatUIKit.theme.accentColor)
        messageBody.applyStyle(style)
    }

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, true)
}