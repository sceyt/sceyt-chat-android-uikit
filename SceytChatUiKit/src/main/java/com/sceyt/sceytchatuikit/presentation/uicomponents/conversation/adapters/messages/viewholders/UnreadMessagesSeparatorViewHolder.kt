package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import com.sceyt.sceytchatuikit.databinding.SceytItemUnreadMessagesSeparatorBinding
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder

class UnreadMessagesSeparatorViewHolder(
        binding: SceytItemUnreadMessagesSeparatorBinding
) : BaseMsgViewHolder(view = binding.root) {

    init {
        binding.setMessageItemStyle()
    }

    override val enableReply = false

    private fun SceytItemUnreadMessagesSeparatorBinding.setMessageItemStyle() {
        with(context) {
            /* messageDay.apply {
                 background = getCompatDrawable(MessagesStyle.dateSeparatorItemBackground)
                 setTextColor(getCompatColor(MessagesStyle.dateSeparatorItemTextColor))
                 val dateTypeface = if (MessagesStyle.dateSeparatorTextFont != -1)
                     ResourcesCompat.getFont(this@with, MessagesStyle.dateSeparatorTextFont) else typeface
                 setTypeface(dateTypeface, MessagesStyle.dateSeparatorTextStyle)
             }*/
        }
    }
}