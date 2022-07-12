package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.databinding.SceytItemOutLinkMessageBinding
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle
import com.sceyt.chat.ui.shared.helpers.LinkPreviewHelper

class OutLinkMsgViewHolder(
        private val binding: SceytItemOutLinkMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
        linkPreview: LinkPreviewHelper,
        private val messageListeners: MessageClickListenersImpl?,
) : BaseLinkMsgViewHolder(linkPreview, binding.root, messageListeners) {

    private lateinit var messageItem: MessageListItem.MessageItem

    init {
        binding.setMessageItemStyle()

        binding.layoutDetails.setOnLongClickListener {
            messageListeners?.onMessageLongClick(it, messageItem)
            return@setOnLongClickListener true
        }

        binding.layoutDetails.setOnClickListener {
            messageListeners?.onLinkClick(it, messageItem)
        }
    }

    override fun bind(item: MessageListItem, diff: MessageItemPayloadDiff) {
        if (item is MessageListItem.MessageItem) {
            messageItem = item

            with(binding) {
                val message = item.message

                if (diff.edited) {
                    val space = if (message.state == MessageState.Edited) MessagesStyle.OUT_EDITED_SPACE else MessagesStyle.OUT_DEFAULT_SPACE
                    messageBody.text = HtmlCompat.fromHtml("${message.body} $space", HtmlCompat.FROM_HTML_MODE_LEGACY)
                }

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.replayCountChanged)
                    setReplayCount(tvReplayCount, toReplayLine, item)

                if (diff.reactionsChanged)
                    setOrUpdateReactions(item, rvReactions, viewPool)

                if (diff.replayContainerChanged)
                    setReplayedMessageContainer(message, viewReplay)

                loadLinkPreview(messageItem, layoutLinkPreview, messageBody)
            }
        }
    }

    private fun SceytItemOutLinkMessageBinding.setMessageItemStyle() {
        with(root.context) {
            layoutDetails.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(MessagesStyle.outBubbleColor))
        }
    }
}