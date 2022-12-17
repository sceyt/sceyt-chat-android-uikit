package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.databinding.SceytItemOutLinkMessageBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle
import com.sceyt.sceytchatuikit.shared.helpers.LinkPreviewHelper

class OutLinkMsgViewHolder(
        private val binding: SceytItemOutLinkMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
        linkPreview: LinkPreviewHelper,
        private val messageListeners: MessageClickListenersImpl?,
        senderNameBuilder: ((User) -> String)?
) : BaseLinkMsgViewHolder(linkPreview, binding.root, messageListeners, senderNameBuilder = senderNameBuilder) {

    init {
        binding.setMessageItemStyle()

        binding.layoutDetails.setOnLongClickListener {
            messageListeners?.onMessageLongClick(it, messageListItem as MessageListItem.MessageItem)
            return@setOnLongClickListener true
        }

        binding.layoutDetails.setOnClickListener {
            messageListeners?.onLinkClick(it, messageListItem as MessageListItem.MessageItem)
        }
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        super.bind(item, diff)

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message

                if (diff.edited || diff.bodyChanged)
                    setMessageBody(messageBody, message)

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPool)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, viewReply)

               // loadLinkPreview(item, layoutLinkPreview, messageBody)
            }
        }
    }

    private fun SceytItemOutLinkMessageBinding.setMessageItemStyle() {
        with(context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.outBubbleColor))
        }
    }
}