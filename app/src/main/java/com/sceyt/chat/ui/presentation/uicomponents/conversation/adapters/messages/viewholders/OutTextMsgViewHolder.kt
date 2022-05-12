package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.fromHtml
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.databinding.SceytUiItemOutTextMessageBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle.OUT_DEFAULT_SPACE
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle.OUT_EDITED_SPACE

class OutTextMsgViewHolder(
        private val binding: SceytUiItemOutTextMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
) : BaseMsgViewHolder(binding.root) {

    override fun bindViews(item: MessageListItem) {
        when (item) {
            is MessageListItem.MessageItem -> {
                with(binding) {
                    val message = item.message
                    this.message = message

                    val space = if (message.state == MessageState.Edited) OUT_EDITED_SPACE else OUT_DEFAULT_SPACE
                    messageBody.text = fromHtml("${message.body} $space", HtmlCompat.FROM_HTML_MODE_LEGACY)

                    setReplayCount(tvReplayCount, toReplayLine, message.replyCount)
                    setOrUpdateReactions(message.reactionScores, rvReactions, viewPool)
                    setMessageDay(message.createdAt, message.showDate, binding.messageDay)
                    setMessageDateText(message.createdAt, messageDate, message.state == MessageState.Edited)
                    setReplayedMessageContainer(message, binding.viewReplay)
                }
            }
            MessageListItem.LoadingMoreItem -> return
        }
    }
}