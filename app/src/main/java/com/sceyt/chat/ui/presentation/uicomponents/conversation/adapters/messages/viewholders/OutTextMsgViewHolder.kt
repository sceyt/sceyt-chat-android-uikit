package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.fromHtml
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.SceytUiItemOutTextMessageBinding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle.OUT_DEFAULT_SPACE
import com.sceyt.chat.ui.utils.DateTimeUtil.getDateTimeString

class OutTextMsgViewHolder(
        private val binding: SceytUiItemOutTextMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
) : BaseMsgViewHolder(binding.root) {

    /*init {
        binding.setChannelItemStyle()
    }
*/
    override fun bindViews(item: MessageListItem) {
        when (item) {
            is MessageListItem.MessageItem -> {
                with(binding) {
                    val message = item.message
                    this.message = message

                    messageDate.setDateText(getDateTimeString(message.createdAt))
                    messageBody.text = fromHtml("${message.body} $OUT_DEFAULT_SPACE", HtmlCompat.FROM_HTML_MODE_LEGACY)

                    setReplayCount(layoutDetails, tvReplayCount, toReplayLine, message.replyCount)
                    setOrUpdateReactions(message.reactionScores, rvReactions, viewPool)
                    setDate(message.createdAt, message.showDate, binding.messageDay)
                }
            }
            MessageListItem.LoadingMoreItem -> Unit
        }
    }


    private fun SceytUiItemOutTextMessageBinding.setStyle() {
        /* with(root.context) {
             channelTitle.setTextColor(getCompatColorByTheme(ChannelStyle.titleColor))
             lastMessage.setTextColor(getCompatColorByTheme(ChannelStyle.lastMessageTextColor))
             messageCount.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(ChannelStyle.unreadCountColor))
         }*/
    }
}