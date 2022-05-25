package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.PopupMenu
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.BottomSheetEmojisFragment
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.extensions.getFragmentManager
import com.sceyt.chat.ui.extensions.setClipboard
import com.sceyt.chat.ui.presentation.root.BaseViewModel
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.events.MessageEvent
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
    private var messageEventListener: ((MessageEvent) -> Unit)? = null

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
            override fun onMessageLongClick(view: View, item: MessageListItem.MessageItem) {
                (guestClickListeners as? MessageClickListeners.MessageClickLongClickListener)?.onMessageLongClick(view, item)
                showMessageActionsPopup(view, item.message)
            }

            override fun onAvatarClick(view: View, item: MessageListItem.MessageItem) {
                (guestClickListeners as? MessageClickListeners.AvatarClickListener)?.onAvatarClick(view, item)
            }

            override fun onReplayCountClick(view: View, item: MessageListItem.MessageItem) {
                (guestClickListeners as? MessageClickListeners.ReplayCountClickListener)?.onReplayCountClick(view, item)
            }

            override fun onAddReactionClick(view: View, item: MessageListItem.MessageItem) {
                (guestClickListeners as? MessageClickListeners.AddReactionClickListener)?.onAddReactionClick(view, item)
                showAddEmojiDialog(item.message)
            }

            override fun onReactionLongClick(view: View, item: ReactionItem.Reaction) {
                (guestClickListeners as? MessageClickListeners.ReactionLongClickListener)?.onReactionLongClick(view, item)
                showReactionActionsPopup(view, item)
            }

            override fun onAttachmentClick(view: View, item: FileListItem) {
                (guestClickListeners as? MessageClickListeners.AttachmentClickListener)?.onAttachmentClick(view, item)
            }

            override fun onAttachmentLongClick(view: View, item: FileListItem) {
                (guestClickListeners as? MessageClickListeners.AttachmentLongClickListener)?.onAttachmentLongClick(view, item)
                showMessageActionsPopup(view, item.sceytUiMessage)
            }
        }
        messagesRV.setMessageListener(defaultMessageClickListeners)
    }


    private fun showReactionActionsPopup(view: View, reaction: ReactionItem.Reaction) {
        val popup = PopupMenu(view.context, view)
        popup.menu.apply {
            add(0, R.id.add, 0, view.context.getString(R.string.add))
            add(0, R.id.remove, 0, view.context.getString(R.string.remove))
            add(0, R.id.delete, 0, view.context.getString(R.string.delete))
        }
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add -> onAddReaction(reaction.messageItem.message, reaction.reactionScore.key)
                R.id.remove -> onReduceReaction(reaction)
                R.id.delete -> onDeleteReaction(reaction)
            }
            false
        }
        popup.show()
    }

    private fun showMessageActionsPopup(view: View, message: SceytUiMessage) {
        val popup = PopupMenu(view.context, view)
        popup.menu.apply {
            add(0, R.id.copyMessage, 0, view.context.getString(R.string.copy))

            if (!message.incoming) {
                add(0, R.id.delete, 0, view.context.getString(R.string.delete))
                add(0, R.id.editMessage, 0, view.context.getString(R.string.edit))
            }
            add(0, R.id.react, 0, view.context.getString(R.string.react))
            add(0, R.id.replay, 0, view.context.getString(R.string.replay))
            add(0, R.id.replayInThread, 0, view.context.getString(R.string.replay_in_thread))
        }
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.copyMessage -> context.setClipboard(message.body)
                R.id.delete -> onDeleteMessage(message)
                R.id.editMessage -> onEditMessage(message)
                R.id.react -> defaultMessageClickListeners.onAddReactionClick(view, MessageListItem.MessageItem(message))
                R.id.replay -> onReplayMessage(message)
                R.id.replayInThread -> onReplayInThreadMessage(message)
            }
            false
        }
        popup.show()
    }

    private fun onDeleteMessage(message: SceytUiMessage) {
        messageEventListener?.invoke(MessageEvent.DeleteMessage(message))
    }

    private fun onEditMessage(message: SceytUiMessage) {
        messageEventListener?.invoke(MessageEvent.EditMessage(message))
    }

    private fun onReplayMessage(message: SceytUiMessage) {
        messageEventListener?.invoke(MessageEvent.Replay(message))
    }

    private fun onReplayInThreadMessage(message: SceytUiMessage) {
        messageEventListener?.invoke(MessageEvent.ReplayInThread(message))
    }

    private fun onAddReaction(message: SceytUiMessage, score: String) {
        val scores = initAddReactionScore(message.reactionScores, score)
        reactionEventListener?.invoke(ReactionEvent.AddReaction(message, scores.second))
    }

    private fun onReduceReaction(reaction: ReactionItem.Reaction) {
        val reactionScore = reaction.reactionScore
        if (reactionScore.score > 1) {
            val updateScore = ReactionScore(reactionScore.key, reactionScore.score - 1)
            reactionEventListener?.invoke(ReactionEvent.AddReaction(reaction.messageItem.message, updateScore))
        } else
            onDeleteReaction(reaction)
    }

    private fun onDeleteReaction(reaction: ReactionItem.Reaction) {
        reactionEventListener?.invoke(ReactionEvent.DeleteReaction(reaction.messageItem.message, reaction.reactionScore))
    }

    private fun showAddEmojiDialog(message: SceytUiMessage) {
        context.getFragmentManager()?.let {
            BottomSheetEmojisFragment(emojiListener = { emoji ->
                onAddReaction(message, emoji.unicode)
            }).show(it, null)
        }
    }

    private fun initAddReactionScore(reactionScores: Array<ReactionScore>?, emoji: String): Pair<Array<ReactionScore>, ReactionScore> {
        val scores = reactionScores ?: arrayOf()
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

    fun getLastMessage() = messagesRV.getLastMsg()

    fun setMessagesList(data: List<MessageListItem>) {
        messagesRV.setData(data)
    }

    fun addNextPageMessages(data: List<MessageListItem>) {
        messagesRV.addNextPageMessages(data)
    }

    fun addNewMessages(vararg data: MessageListItem.MessageItem) {
        messagesRV.addNewMessages(*data)
    }

    fun updateMessage(message: SceytUiMessage, notifyItemChanged: Boolean) {
        for ((index, item) in messagesRV.getData().withIndex()) {
            if (item is MessageListItem.MessageItem && (item.message.id == message.id ||
                            (item.message.id == 0L && item.message.tid == message.tid))) {

                item.message.apply {
                    updateMessage(message)
                    status = message.status
                }
                if (notifyItemChanged)
                    messagesRV.adapter?.notifyItemChanged(index)
                break
            }
        }
    }

    fun updateReaction(data: SceytUiMessage) {
        messagesRV.updateReaction(data.id, data.reactionScores ?: arrayOf())
    }

    fun updateViewState(state: BaseViewModel.PageState) {
        if (state.isEmpty && !messagesRV.isEmpty())
            return
        pageStateView?.updateState(state, messagesRV.isEmpty())
    }

    fun updateMessagesStatus(status: DeliveryStatus, ids: MutableList<Long>) {
        ids.forEach { id ->
            for (item: MessageListItem in messagesRV.getData()) {
                if (item is MessageListItem.MessageItem) {
                    if (item.message.id == id) {
                        if (item.message.status < status)
                            item.message.status = status
                        break
                    } else {
                        if (item.message.status < status && item.message.status != DeliveryStatus.Pending)
                            item.message.status = status
                    }
                }
            }
        }
    }

    fun messageSendFailed(id: Long) {
        messagesRV.getData().find { it is MessageListItem.MessageItem && it.message.id == id }?.let {
            (it as MessageListItem.MessageItem).message.status = DeliveryStatus.Failed
        }
    }

    fun setReachToStartListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        messagesRV.setRichToStartListener(listener)
    }

    fun setMessageClickListener(listener: MessageClickListeners) {
        guestClickListeners = listener
    }

    fun setMessageReactionsEventListener(listener: (ReactionEvent) -> Unit) {
        reactionEventListener = listener
    }

    internal fun setMessageEventListener(listener: (MessageEvent) -> Unit) {
        messageEventListener = listener
    }

    fun clearData() {
        messagesRV.clearData()
        updateViewState(BaseViewModel.PageState(isEmpty = true))
    }
}