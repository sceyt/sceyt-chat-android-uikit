package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import com.sceyt.chat.ui.databinding.SceytItemIncDeletedMessageBinding
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle

class IncDeletedMsgViewHolder(
        private val binding: SceytItemIncDeletedMessageBinding
) : BaseMsgViewHolder(binding.root) {

    init {
        binding.setMessageItemStyle()
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.showAvatarAndNameChanged)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)
            }
        }
    }

    private fun SceytItemIncDeletedMessageBinding.setMessageItemStyle() {
        with(root.context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.incBubbleColor))
        }
    }
}