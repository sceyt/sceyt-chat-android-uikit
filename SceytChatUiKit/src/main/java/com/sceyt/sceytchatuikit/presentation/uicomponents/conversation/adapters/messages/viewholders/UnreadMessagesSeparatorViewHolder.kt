package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import com.sceyt.sceytchatuikit.databinding.SceytItemUnreadMessagesSeparatorBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColor
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle

class UnreadMessagesSeparatorViewHolder(
        binding: SceytItemUnreadMessagesSeparatorBinding
) : BaseMsgViewHolder(view = binding.root) {

    init {
        binding.setMessageItemStyle()
    }

    override val enableReply = false

    private fun SceytItemUnreadMessagesSeparatorBinding.setMessageItemStyle() {
        with(tvUnreadMessagesSeparator) {
            setTextColor(context.getCompatColor(MessagesStyle.unreadMessagesTextColor))
            setBackgroundColor(context.getCompatColor(MessagesStyle.unreadMessagesBackendColor))
            setTypeface(typeface, MessagesStyle.unreadMessagesSeparatorTextStyle)
        }
    }
}