package com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.databinding.SceytItemIncDeletedMessageBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chatuikit.sceytstyles.MessageItemStyle

class IncDeletedMsgViewHolder(
        private val binding: SceytItemIncDeletedMessageBinding,
        private val style: MessageItemStyle,
        userNameBuilder: ((User) -> String)?,
        displayedListener: ((MessageListItem) -> Unit)?,
        private val messageListeners: MessageClickListeners.ClickListeners?,
) : BaseMsgViewHolder(binding.root, style, userNameBuilder = userNameBuilder, displayedListener = displayedListener) {

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