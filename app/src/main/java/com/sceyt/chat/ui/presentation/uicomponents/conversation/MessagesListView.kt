package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.extensions.*
import com.sceyt.chat.ui.presentation.root.PageState
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.openFile
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.diff
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders.MessageViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.dialogs.DeleteMessageDialog
import com.sceyt.chat.ui.presentation.uicomponents.conversation.events.MessageEvent
import com.sceyt.chat.ui.presentation.uicomponents.conversation.events.ReactionEvent
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.*
import com.sceyt.chat.ui.presentation.uicomponents.conversation.popups.PopupMenuMessage
import com.sceyt.chat.ui.sceytconfigs.MessagesStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MessagesListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), MessageClickListeners.ClickListeners,
        MessagePopupClickListeners.PopupClickListeners, ReactionPopupClickListeners.PopupClickListeners {

    private var messagesRV: MessagesRV
    private var pageStateView: PageStateView? = null
    private lateinit var clickListeners: MessageClickListenersImpl
    private lateinit var messagePopupClickListeners: MessagePopupClickListenersImpl
    private lateinit var reactionClickListeners: ReactionPopupClickListenersImpl
    private var reactionEventListener: ((ReactionEvent) -> Unit)? = null
    private var messageEventListener: ((MessageEvent) -> Unit)? = null
    private var enabledClickActions = true

    init {
        setBackgroundColor(context.getCompatColor(R.color.sceyt_color_bg))

        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.MessagesListView)
            MessagesStyle.updateWithAttributes(a)
            a.recycle()
        }

        messagesRV = MessagesRV(context)
        messagesRV.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        messagesRV.clipToPadding = clipToPadding
        setPadding(0, 0, 0, 0)

        addView(messagesRV, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        if (!isInEditMode)
            addView(PageStateView(context).also {
                pageStateView = it
                it.setLoadingStateView(MessagesStyle.loadingState)
                it.setEmptyStateView(MessagesStyle.emptyState)
            })

        initClickListeners()
    }

    private fun initClickListeners() {
        clickListeners = MessageClickListenersImpl(this)
        messagePopupClickListeners = MessagePopupClickListenersImpl(this)
        reactionClickListeners = ReactionPopupClickListenersImpl(this)

        messagesRV.setMessageListener(object : MessageClickListeners.ClickListeners {
            override fun onMessageLongClick(view: View, item: MessageListItem.MessageItem) {
                if (enabledClickActions)
                    clickListeners.onMessageLongClick(view, item)
            }

            override fun onAvatarClick(view: View, item: MessageListItem.MessageItem) {
                if (enabledClickActions)
                    clickListeners.onAvatarClick(view, item)
            }

            override fun onReplayCountClick(view: View, item: MessageListItem.MessageItem) {
                if (enabledClickActions)
                    clickListeners.onReplayCountClick(view, item)
            }

            override fun onAddReactionClick(view: View, message: SceytMessage) {
                if (enabledClickActions)
                    clickListeners.onAddReactionClick(view, message)
            }

            override fun onReactionClick(view: View, item: ReactionItem.Reaction) {
                if (enabledClickActions)
                    clickListeners.onReactionClick(view, item)
            }

            override fun onReactionLongClick(view: View, item: ReactionItem.Reaction) {
                if (enabledClickActions)
                    clickListeners.onReactionLongClick(view, item)
            }

            override fun onAttachmentClick(view: View, item: FileListItem) {
                if (enabledClickActions)
                    clickListeners.onAttachmentClick(view, item)
            }

            override fun onAttachmentLongClick(view: View, item: FileListItem) {
                if (enabledClickActions)
                    clickListeners.onAttachmentLongClick(view, item)
            }

            override fun onLinkClick(view: View, item: MessageListItem.MessageItem) {
                if (enabledClickActions)
                    clickListeners.onLinkClick(view, item)
            }
        })
    }

    private fun showReactionActionsPopup(view: View, reaction: ReactionItem.Reaction) {
        val popup = PopupMenu(ContextThemeWrapper(context, R.style.SceytPopupMenuStyle), view)
        popup.inflate(R.menu.sceyt_menu_popup_reacton)
        val containsSelf = reaction.reaction.containsSelf
        popup.menu.findItem(R.id.sceyt_add).isVisible = !containsSelf
        popup.menu.findItem(R.id.sceyt_remove).isVisible = containsSelf

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sceyt_add -> reactionClickListeners.onAddReaction(reaction.message, reaction.reaction.key)
                R.id.sceyt_remove -> reactionClickListeners.onDeleteReaction(reaction)
            }
            false
        }
        popup.show()
    }

    private fun showMessageActionsPopup(view: View, message: SceytMessage) {
        val popup = PopupMenuMessage(ContextThemeWrapper(context, R.style.SceytPopupMenuStyle), view, message.incoming)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sceyt_edit_message -> messagePopupClickListeners.onEditMessageClick(message)
                R.id.sceyt_react -> messagePopupClickListeners.onReactMessageClick(view, message)
                R.id.sceyt_replay -> messagePopupClickListeners.onReplayMessageClick(message)
                R.id.sceyt_replay_in_thread -> messagePopupClickListeners.onReplayInThreadMessageClick(message)
                R.id.sceyt_copy_message -> messagePopupClickListeners.onCopyMessageClick(message)
                R.id.sceyt_delete_message -> messagePopupClickListeners.onDeleteMessageClick(message)
            }
            false
        }
        popup.show()
    }

    private fun addReaction(message: SceytMessage, key: String) {
        reactionEventListener?.invoke(ReactionEvent.AddReaction(message, key))
    }

    private fun onReduceReaction(reaction: ReactionItem.Reaction) {

    }

    private fun onReactionClick(reaction: ReactionItem.Reaction) {
        val containsSelf = reaction.reaction.containsSelf
        if (containsSelf)
            reactionClickListeners.onDeleteReaction(reaction)
        else
            reactionClickListeners.onAddReaction(reaction.message, reaction.reaction.key)

    }

    private fun showAddEmojiDialog(message: SceytMessage) {
        context.getFragmentManager()?.let {
            BottomSheetEmojisFragment(emojiListener = { emoji ->
                onAddReaction(message, emoji.unicode)
            }).show(it, null)
        }
    }

    private suspend fun addNewServerMessagesAndSort(newMessages: ArrayList<MessageListItem>,
                                                    currentMessages: List<MessageListItem>, hasLoadingItem: Boolean,
                                                    mappingCb: (List<MessageListItem>, Boolean) -> List<MessageListItem>) {
        if (newMessages.isNotEmpty()) {
            val messages2 = ArrayList(currentMessages)
            messages2.addAll(newMessages)
            messages2.sortBy {
                when (it) {
                    is MessageListItem.MessageItem -> it.message.createdAt
                    is MessageListItem.DateSeparatorItem -> it.createdAt
                    is MessageListItem.LoadingMoreItem -> 0
                }
            }
            val initList = mappingCb.invoke(messages2, hasLoadingItem)

            withContext(Dispatchers.Main) {
                messagesRV.awaitAnimationEnd {
                    if (messages2.isNotEmpty()) {
                        val isLastItemDisplaying = messagesRV.isLastItemDisplaying()
                        messagesRV.setData(initList)

                        if (!hasLoadingItem)
                            messagesRV.hideLoadingItem()

                        if (isLastItemDisplaying)
                            messagesRV.scrollToPosition(initList.size - 1)
                    }
                }
            }
        }
    }

    internal fun updateMessagesWithServerData(data: List<MessageListItem>, offset: Int, lifecycleOwner: LifecycleOwner,
                                              mapingCb: (List<MessageListItem>, Boolean) -> List<MessageListItem>) {
        val currentMessages = messagesRV.getData() ?: arrayListOf()
        if (currentMessages.isEmpty() || data.isEmpty() && offset == 0) {
            messagesRV.setData(data)
            return
        } else if (data.isEmpty() && offset > 0)
            messagesRV.hideLoadingItem()

        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
            var dataHasLoadingItem = false
            val newMessages: ArrayList<MessageListItem> = arrayListOf()
            // Update UI messages if exist and have diff, or add new messages
            data.forEach { dataItem ->
                if (!dataHasLoadingItem)
                    dataHasLoadingItem = dataItem is MessageListItem.LoadingMoreItem

                if (dataItem is MessageListItem.MessageItem) {
                    currentMessages.findIndexed {
                        it is MessageListItem.MessageItem && it.message.id == dataItem.message.id
                    }?.let {
                        val oldItem = currentMessages[it.first] as MessageListItem.MessageItem
                        val diff = oldItem.message.diff(dataItem.message)

                        if (diff.hasDifference()) {
                            currentMessages[it.first] = dataItem
                            messagesRV.notifyItemChangedSafety(it.first, diff)
                            Log.i(TAG, "update: changed item ${dataItem.message.body} $diff")
                        }
                    } ?: run {
                        Log.i(TAG, "update: addNewMessage ${dataItem.message.body}")
                        newMessages.add(dataItem)
                    }
                } else if (!currentMessages.contains(dataItem)) {
                    newMessages.add(dataItem)
                }
            }
            // Add new messages and sort list
            addNewServerMessagesAndSort(newMessages, currentMessages, dataHasLoadingItem, mapingCb)
        }
    }

    internal fun getLastMessage() = messagesRV.getLastMsg()

    internal fun setMessagesList(data: List<MessageListItem>) {
        messagesRV.setData(data)
    }

    internal fun addNextPageMessages(data: List<MessageListItem>) {
        messagesRV.addNextPageMessages(data)
    }

    internal fun addNewMessages(vararg data: MessageListItem) {
        messagesRV.addNewMessages(*data)
    }

    internal fun updateMessage(message: SceytMessage) {
        for ((index, item) in messagesRV.getData()?.withIndex() ?: return) {
            if (item is MessageListItem.MessageItem && (item.message.id == message.id ||
                            (item.message.id == 0L && item.message.tid == message.tid))) {
                val oldMessage = item.message.clone()

                item.message.apply {
                    updateMessage(message)
                    deliveryStatus = message.deliveryStatus
                }
                messagesRV.adapter?.notifyItemChanged(index, oldMessage.diff(item.message))
                break
            }
        }
    }

    internal fun messageEditedOrDeleted(updateMessage: SceytMessage) {
        messagesRV.getData()?.findIndexed { it is MessageListItem.MessageItem && it.message.id == updateMessage.id }?.let {
            val message = (it.second as MessageListItem.MessageItem).message
            val oldMessage = message.clone()
            message.updateMessage(updateMessage)
            if (message.state == MessageState.Deleted)
                messagesRV.adapter?.notifyItemChanged(it.first)
            else {
                messagesRV.adapter?.notifyItemChanged(it.first, oldMessage.diff(message))
            }
        }
    }

    internal fun updateReaction(data: SceytMessage) {
        messagesRV.getData()?.findIndexed { it is MessageListItem.MessageItem && it.message.id == data.id }?.let {
            val message = (it.second as MessageListItem.MessageItem).message
            val oldMessage = message.clone()
            message.updateMessage(data)
            message.messageReactions = data.messageReactions
            messagesRV.adapter?.notifyItemChanged(it.first, oldMessage.diff(message))
        }
    }

    internal fun updateViewState(state: PageState) {
        pageStateView?.updateState(state, messagesRV.isEmpty())
    }

    internal fun updateMessagesStatus(status: DeliveryStatus, ids: MutableList<Long>) {
        ids.forEach { id ->
            for ((index: Int, item: MessageListItem) in (messagesRV.getData()
                    ?: return).withIndex()) {
                if (item is MessageListItem.MessageItem) {
                    val oldMessage = item.message.clone()
                    if (item.message.id == id) {
                        if (item.message.deliveryStatus < status)
                            item.message.deliveryStatus = status
                        messagesRV.adapter?.notifyItemChanged(index, oldMessage.diff(item.message))
                        break
                    } else {
                        if (item.message.deliveryStatus < status && item.message.deliveryStatus != DeliveryStatus.Pending) {
                            item.message.deliveryStatus = status
                            messagesRV.adapter?.notifyItemChanged(index, oldMessage.diff(item.message))
                        }
                    }
                }
            }
        }
    }

    internal fun messageSendFailed(tid: Long) {
        messagesRV.getData()?.findIndexed { it is MessageListItem.MessageItem && it.message.tid == tid }?.let {
            val message = (it.second as MessageListItem.MessageItem).message
            val oldMessage = message.clone()
            message.deliveryStatus = DeliveryStatus.Failed
            messagesRV.adapter?.notifyItemChanged(it.first, oldMessage.diff(message))
        }
    }

    internal fun updateReplayCount(replayedMessage: SceytMessage?) {
        messagesRV.getData()?.findIndexed {
            it is MessageListItem.MessageItem && it.message.id == replayedMessage?.parent?.id
        }?.let {
            val message = (it.second as MessageListItem.MessageItem).message
            val oldMessage = message.clone()
            message.replyCount++
            messagesRV.adapter?.notifyItemChanged(it.first, oldMessage.diff(message))
        }
    }

    internal fun newReplayMessage(messageId: Long?) {
        messagesRV.getData()?.findIndexed {
            it is MessageListItem.MessageItem && it.message.id == messageId
        }?.let {
            val message = (it.second as MessageListItem.MessageItem).message
            val oldMessage = message.clone()
            message.replyCount++
            messagesRV.adapter?.notifyItemChanged(it.first, oldMessage.diff(message))
        }
    }

    internal fun setNeedLoadMoreMessagesListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        messagesRV.setNeedLoadMoreMessagesListener(listener)
    }

    internal fun setReachToStartListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        messagesRV.setRichToStartListener(listener)
    }

    internal fun setRichToPrefetchDistanceListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        messagesRV.setRichToPrefetchDistanceListener(listener)
    }

    internal fun setMessageReactionsEventListener(listener: (ReactionEvent) -> Unit) {
        reactionEventListener = listener
    }

    internal fun setMessageEventListener(listener: (MessageEvent) -> Unit) {
        messageEventListener = listener
    }

    internal fun clearData() {
        messagesRV.clearData()
        updateViewState(PageState.StateEmpty())
    }

    fun setViewHolderFactory(factory: MessageViewHolderFactory) {
        messagesRV.setViewHolderFactory(factory.also {
            it.setMessageListener(clickListeners)
        })
    }

    // Click listeners
    fun setMessageClickListener(listener: MessageClickListeners) {
        clickListeners.setListener(listener)
    }

    fun setMessagePopupClickListener(listener: MessagePopupClickListeners) {
        messagePopupClickListeners.setListener(listener)
    }

    fun setCustomMessageClickListener(listener: MessageClickListenersImpl) {
        clickListeners = listener
    }

    fun setCustomMessagePopupClickListener(listener: MessagePopupClickListenersImpl) {
        messagePopupClickListeners = listener
    }

    fun enableDisableClickActions(enabled: Boolean) {
        enabledClickActions = enabled
    }

    // Click events
    override fun onMessageLongClick(view: View, item: MessageListItem.MessageItem) {
        showMessageActionsPopup(view, item.message)
    }

    override fun onAvatarClick(view: View, item: MessageListItem.MessageItem) {

    }

    override fun onReplayCountClick(view: View, item: MessageListItem.MessageItem) {
        onReplayInThreadMessageClick(item.message)
    }

    override fun onAddReactionClick(view: View, message: SceytMessage) {
        showAddEmojiDialog(message)
    }

    override fun onReactionClick(view: View, item: ReactionItem.Reaction) {
        onReactionClick(item)
    }

    override fun onReactionLongClick(view: View, item: ReactionItem.Reaction) {
        showReactionActionsPopup(view, item)
    }

    override fun onAttachmentClick(view: View, item: FileListItem) {
        item.openFile(context)
    }

    override fun onAttachmentLongClick(view: View, item: FileListItem) {
        showMessageActionsPopup(view, item.sceytMessage)
    }

    override fun onLinkClick(view: View, item: MessageListItem.MessageItem) {
        context.openLink(item.message.body)
    }


    // Message popup events
    override fun onCopyMessageClick(message: SceytMessage) {
        context.setClipboard(message.body)
    }

    override fun onDeleteMessageClick(message: SceytMessage) {
        DeleteMessageDialog(context, positiveClickListener = {
            messageEventListener?.invoke(MessageEvent.DeleteMessage(message))
        }).show()
    }

    override fun onEditMessageClick(message: SceytMessage) {
        messageEventListener?.invoke(MessageEvent.EditMessage(message))
    }

    override fun onReactMessageClick(view: View, message: SceytMessage) {
        onAddReactionClick(view, message)
    }

    override fun onReplayMessageClick(message: SceytMessage) {
        messageEventListener?.invoke(MessageEvent.Replay(message))
    }

    override fun onReplayInThreadMessageClick(message: SceytMessage) {
        messageEventListener?.invoke(MessageEvent.ReplayInThread(message, context))
    }


    //Reaction popup events
    override fun onAddReaction(message: SceytMessage, key: String) {
        addReaction(message, key)
    }

    override fun onRemoveReaction(reactionItem: ReactionItem.Reaction) {
        onReduceReaction(reactionItem)
    }

    override fun onDeleteReaction(reactionItem: ReactionItem.Reaction) {
        reactionEventListener?.invoke(ReactionEvent.DeleteReaction(reactionItem.message, reactionItem.reaction.key))
    }
}