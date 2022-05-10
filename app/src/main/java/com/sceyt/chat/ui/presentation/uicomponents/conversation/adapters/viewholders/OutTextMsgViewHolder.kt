package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.viewholders

import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.fromHtml
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.databinding.SceytUiItemOutTextMessage2Binding
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.MessageReactionsAdapter
import com.sceyt.chat.ui.utils.DateTimeUtil.getDateTimeString
import com.sceyt.chat.ui.utils.RecyclerItemOffsetDecoration
import kotlin.math.min

class OutTextMsgViewHolder(
        private val binding: SceytUiItemOutTextMessage2Binding,
        private val viewPool: RecyclerView.RecycledViewPool,
) : BaseMessageViewHolder(binding.root) {
    private var reactionsAdapter: MessageReactionsAdapter? = null

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

                    if (message.replyCount > 0)
                        setReplayCountLineMargins(layoutDetails, tvReplayCount, toReplayLine)

                    if (!message.reactionScores.isNullOrEmpty())
                        setOrUpdateReactionsAdapter(message)
                    else binding.rvReactions.isVisible = false
                }
            }
            MessageListItem.LoadingMoreItem -> Unit
        }
    }

    private fun setOrUpdateReactionsAdapter(message: SceytUiMessage) {
        val reactions = ArrayList(message.reactionScores!!.sortedByDescending { it.score }.toList()).apply {
            add(ReactionScore("", -1))
        }

        val spanCount = min(4, message.reactionScores!!.size)

        if (reactionsAdapter == null) {
            reactionsAdapter = MessageReactionsAdapter(
                message.reactionScores!!.toList(),
                /*   onAddNewReactionCb = {
                   getMessageByBindingPosition()?.let {
                       reactionsListener?.onAddNewReactionClick(it, bindingAdapterPosition)
                   }
               }, onAddReactionCb = {
                   getMessageByBindingPosition()?.let { message ->
                       reactionsListener?.onAddReactionClick(message, it)
                   }
               }, onReduceReactionCb = {
                   getMessageByBindingPosition()?.let { message ->
                       reactionsListener?.onReduceReaction(message, it)
                   }
               }, onDeleteReactionCb = {
                   getMessageByBindingPosition()?.let { message ->
                       reactionsListener?.onDeleteReaction(message, it)
                   }
               }*/
            )

            with(binding.rvReactions) {
                setRecycledViewPool(viewPool)
                if (itemDecorationCount == 0)
                    addItemDecoration(RecyclerItemOffsetDecoration(0, 4, 8, 4))

                layoutManager = GridLayoutManager(itemView.context, spanCount)
                adapter = reactionsAdapter
            }
        } else binding.rvReactions.layoutManager = GridLayoutManager(itemView.context, spanCount)

        binding.rvReactions.isVisible = true

        reactionsAdapter?.submitData(reactions)
    }

    private fun SceytUiItemOutTextMessage2Binding.setStyle() {
        /* with(root.context) {
             channelTitle.setTextColor(getCompatColorByTheme(ChannelStyle.titleColor))
             lastMessage.setTextColor(getCompatColorByTheme(ChannelStyle.lastMessageTextColor))
             messageCount.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(ChannelStyle.unreadCountColor))
         }*/
    }
}