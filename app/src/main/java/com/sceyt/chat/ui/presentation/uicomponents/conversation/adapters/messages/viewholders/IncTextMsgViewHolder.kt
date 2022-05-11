package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.content.res.ColorStateList
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.databinding.SceytUiItemChannelBinding
import com.sceyt.chat.ui.databinding.SceytUiItemIncTextMessage2Binding
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle.INC_DEFAULT_SPACE
import com.sceyt.chat.ui.utils.DateTimeUtil

class IncTextMsgViewHolder(
        private val binding: SceytUiItemIncTextMessage2Binding,
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

                    messageDate.setDateText(DateTimeUtil.getDateTimeString(message.createdAt))
                    messageBody.text = HtmlCompat.fromHtml("${message.body} $INC_DEFAULT_SPACE", HtmlCompat.FROM_HTML_MODE_LEGACY)

                    setReplayCount(viewBg, tvReplayCount, toReplayLine, message.replyCount)
                    setOrUpdateReactions(message.reactionScores, rvReactions, viewPool)
                    setDate(message.createdAt, message.showDate, binding.messageDay)
                }
            }
            MessageListItem.LoadingMoreItem -> Unit
        }
    }

    private fun SceytUiItemChannelBinding.setChannelItemStyle() {
        with(root.context) {
            channelTitle.setTextColor(getCompatColorByTheme(ChannelStyle.titleColor))
            lastMessage.setTextColor(getCompatColorByTheme(ChannelStyle.lastMessageTextColor))
            messageCount.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(ChannelStyle.unreadCountColor))
        }
    }
}