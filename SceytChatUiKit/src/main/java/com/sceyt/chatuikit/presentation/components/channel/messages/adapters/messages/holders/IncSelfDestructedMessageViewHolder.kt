package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.databinding.SceytItemIncSelfDestructedMessageBinding
import com.sceyt.chatuikit.extensions.setDrawableStart
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class IncSelfDestructedMessageViewHolder(
    private val binding: SceytItemIncSelfDestructedMessageBinding,
    private val viewPool: RecyclerView.RecycledViewPool,
    private val style: MessageItemStyle,
    private val messageListeners: MessageClickListeners.ClickListeners?,
    displayedListener: ((MessageListItem) -> Unit)?,
) : BaseMessageViewHolder(binding.root, style, messageListeners, displayedListener) {

    init {
        with(binding) {
            setMessageItemStyle()

            root.setOnClickListener {
                messageListeners?.onMessageClick(it, messageListItem as MessageListItem.MessageItem)
            }

            root.setOnLongClickListener {
                messageListeners?.onMessageLongClick(
                    it,
                    messageListItem as MessageListItem.MessageItem
                )
                return@setOnLongClickListener true
            }

            messageBody.doOnLongClick {
                messageListeners?.onMessageLongClick(
                    it,
                    messageListItem as MessageListItem.MessageItem
                )
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

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.edited || diff.bodyChanged) {
                    val body = style.selfDestructedMessageItemStyle.bodyFormatter.format(context, message)
                    messageBody.setText(body, TextView.BufferType.SPANNABLE)
                    setBodyTextPosition(messageBody, messageDate, layoutDetails)
                }

                if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, viewReply)

                if (diff.reactionsChanged || diff.edited)
                    setOrUpdateReactions(item, rvReactions, viewPool)

                if (item.message.shouldShowAvatarAndName)
                    avatar.setOnClickListener {
                        messageListeners?.onAvatarClick(it, item)
                    }
            }
        }
    }

    override val selectMessageView get() = binding.selectView

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, false)

    override val incoming: Boolean
        get() = true

    private fun SceytItemIncSelfDestructedMessageBinding.setMessageItemStyle() {
        val selfDestructedStyle = style.selfDestructedMessageItemStyle

        applyCommonStyle(
            layoutDetails = layoutDetails,
            tvForwarded = null,
            messageBody = null,
            tvThreadReplyCount = tvReplyCount,
            toReplyLine = toReplyLine,
            tvSenderName = tvUserName,
            avatarView = avatar
        )

        messageBody.setDrawableStart(
            drawable = selfDestructedStyle.drawable,
            tint = selfDestructedStyle.iconColor
        )
    }
}