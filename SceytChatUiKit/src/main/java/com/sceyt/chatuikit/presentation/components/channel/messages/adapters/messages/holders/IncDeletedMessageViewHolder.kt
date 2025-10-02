package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import com.sceyt.chatuikit.databinding.SceytItemIncDeletedMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class IncDeletedMessageViewHolder(
        private val binding: SceytItemIncDeletedMessageBinding,
        private val style: MessageItemStyle,
        displayedListener: ((MessageListItem) -> Unit)?,
        private val messageListeners: MessageClickListeners.ClickListeners?,
) : BaseMessageViewHolder(binding.root, style, displayedListener = displayedListener) {

    init {
        binding.setMessageItemStyle()
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.showAvatarAndNameChanged)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)

                if (item.message.shouldShowAvatarAndName)
                    avatar.setOnClickListener {
                        messageListeners?.onAvatarClick(it, item)
                    }
            }
        }
    }

    override val enableReply = false

    override val selectMessageView get() = binding.selectView

    override val incoming: Boolean
        get() = true

    private fun SceytItemIncDeletedMessageBinding.setMessageItemStyle() {
        style.incomingBubbleBackgroundStyle.apply(layoutDetails)
        style.deletedMessageTextStyle.apply(messageBody)
        style.senderNameTextStyle.apply(tvUserName)
        style.selectionCheckboxStyle.apply(selectView)
        style.avatarStyle.apply(avatar)
        messageBody.text = style.deletedStateText
    }
}