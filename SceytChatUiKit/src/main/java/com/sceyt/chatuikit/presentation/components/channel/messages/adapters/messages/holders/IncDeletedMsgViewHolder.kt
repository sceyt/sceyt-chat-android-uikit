package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.content.res.ColorStateList
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemIncDeletedMessageBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.config.formatters.UserNameFormatter
import com.sceyt.chatuikit.styles.MessageItemStyle

class IncDeletedMsgViewHolder(
        private val binding: SceytItemIncDeletedMessageBinding,
        private val style: MessageItemStyle,
        userNameFormatter: UserNameFormatter?,
        displayedListener: ((MessageListItem) -> Unit)?,
        private val messageListeners: MessageClickListeners.ClickListeners?,
) : BaseMsgViewHolder(binding.root, style, userNameFormatter = userNameFormatter, displayedListener = displayedListener) {

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

    private fun SceytItemIncDeletedMessageBinding.setMessageItemStyle() {
        layoutDetails.backgroundTintList = ColorStateList.valueOf(style.incBubbleColor)
        tvUserName.setTextColor(style.senderNameTextColor)
        messageBody.setTextColor(context.getCompatColor(SceytChatUIKit.theme.textSecondaryColor))
    }
}