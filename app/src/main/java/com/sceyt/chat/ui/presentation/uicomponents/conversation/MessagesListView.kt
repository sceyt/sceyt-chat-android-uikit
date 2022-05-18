package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.get
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.BottomSheetEmojisFragment
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.extensions.getFragmentManager
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.events.ReactionEvent
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle
import com.sceyt.chat.ui.utils.binding.BindingUtil

class MessagesListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr) {

    private var messagesRV: MessagesRV
    private var pageStateView: PageStateView?
    private lateinit var defaultMessageClickListeners: MessageClickListeners.ClickListeners
    private var guestClickListeners: MessageClickListeners? = null
    private var reactionEventListener: ((ReactionEvent) -> Unit)? = null

    init {
        setBackgroundColor(context.getCompatColor(R.color.colorBackground))
        BindingUtil.themedBackgroundColor(this, R.color.colorBackground)

        val attributes = intArrayOf(android.R.attr.paddingLeft, android.R.attr.paddingTop, android.R.attr.paddingBottom, android.R.attr.paddingRight)
        val a = context.obtainStyledAttributes(attrs, attributes)
        a.recycle()

        /* if (attrs != null) {
             val a = context.obtainStyledAttributes(attrs, R.styleable.MessagesListView)
             ChannelStyle.updateWithAttributes(a)
             a.recycle()
         }*/
        messagesRV = MessagesRV(context)
        messagesRV.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        messagesRV.clipToPadding = clipToPadding
        setPadding(0, 0, 0, 0)

        addView(messagesRV, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        addView(PageStateView(context).also {
            pageStateView = it
            it.setLoadingStateView(ChannelStyle.loadingState)
            it.setEmptyStateView(ChannelStyle.emptyState)
            it.setEmptySearchStateView(ChannelStyle.emptySearchState)
        })

        initListeners()
    }

    private fun initListeners() {
        defaultMessageClickListeners = object : MessageClickListeners.ClickListeners {
            override fun onMessageLongClick(item: MessageListItem.MessageItem) {
                (guestClickListeners as? MessageClickListeners.MessageClickLongClickListener)?.onMessageLongClick(item)
            }

            override fun onAvatarClick(item: MessageListItem.MessageItem) {
                (guestClickListeners as? MessageClickListeners.AvatarClickListener)?.onAvatarClick(item)
            }

            override fun onReplayCountClick(item: MessageListItem.MessageItem) {
                (guestClickListeners as? MessageClickListeners.ReplayCountClickListener)?.onReplayCountClick(item)
            }

            override fun onAddReactionClick(item: MessageListItem.MessageItem, position: Int) {
                (guestClickListeners as? MessageClickListeners.AddReactionClickListener)?.onAddReactionClick(item, position)
                onAddEmoji(item.message)
            }

            override fun onReactionLongClick(view: View, item: ReactionItem.Reaction) {
                (guestClickListeners as? MessageClickListeners.ReactionLongClickListener)?.onReactionLongClick(view, item)
            }

            override fun onAttachmentClick(item: MessageListItem.MessageItem) {
                (guestClickListeners as? MessageClickListeners.AttachmentClickListener)?.onAttachmentClick(item)
            }

        }
        messagesRV.setMessageListener(defaultMessageClickListeners)
    }

    private fun onAddEmoji(message: SceytUiMessage) {
        context.getFragmentManager()?.let {
            BottomSheetEmojisFragment(emojiListener = { emoji ->
                val scores = initAddReactionScore(message.reactionScores, emoji.unicode)
                message.reactionScores = scores.first
                messagesRV.updateReaction(message)
                reactionEventListener?.invoke(ReactionEvent.AddReaction(message, scores.second))
            }).show(it, null)
        }
    }

    private fun initAddReactionScore(reactionScores: Array<ReactionScore>?, emoji: String): Pair<Array<ReactionScore>, ReactionScore> {
        val scores = reactionScores?.clone() ?: arrayOf()
        val reaction = scores.find { it.key == emoji }
        var score = reaction?.score?.toInt() ?: 0
        val updateReactionScore: ReactionScore
        return if (score == 0) {
            updateReactionScore = ReactionScore(emoji, 1)
            Pair(scores.plus(updateReactionScore), updateReactionScore)
        } else {
            updateReactionScore = ReactionScore(reaction?.key, (++score).toLong())
            val index = scores.indexOf(reaction)
            scores[index] = updateReactionScore
            Pair(scores, updateReactionScore)
        }
    }

    fun setReachToStartListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        messagesRV.setRichToStartListener(listener)
    }

    fun setMessagesList(data: List<MessageListItem>) {
        messagesRV.setData(data)
    }

    fun addNextPageMessages(data: List<MessageListItem>) {
        messagesRV.addNextPageMessages(data)
    }

    fun addNewMessages(vararg data: MessageListItem.MessageItem) {
        messagesRV.addNewMessages(*data)
    }

    fun updateState(state: BaseViewModel.PageState) {
        pageStateView?.updateState(state, messagesRV.isEmpty())
    }

    fun setMessageClickListener(listener: MessageClickListeners) {
        guestClickListeners = listener
    }

    fun setMessageReactionsEventListener(listener: (ReactionEvent) -> Unit) {
        reactionEventListener = listener
    }

    fun updateReaction(data: SceytUiMessage) {
        messagesRV.updateReaction(data)
    }

    fun getLastMessage() = messagesRV.getLastMsg()

    fun updateMessage(message: SceytUiMessage) {
        (messagesRV.getData().find {
            it is MessageListItem.MessageItem && (it.message.id == message.id || it.message.tid == message.tid)
        } as? MessageListItem.MessageItem)?.message?.updateMessage(message)
    }

    fun updateMessagesStatus(status: DeliveryStatus, ids: MutableList<Long>) {
        ids.forEach { id ->
            val item = messagesRV.getData().find {
                it is MessageListItem.MessageItem && it.message.id == id
            } as? MessageListItem.MessageItem

            item?.let {
                if (it.message.status < status)
                    it.message.status = status
            }
        }
    }
}