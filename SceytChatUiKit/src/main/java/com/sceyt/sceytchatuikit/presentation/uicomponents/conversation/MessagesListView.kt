package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.presentation.common.diff
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.root.PageStateView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.openFile
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem.MessageItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessagesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.BaseMsgViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.viewholders.MessageViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.dialogs.DeleteMessageDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.ReactionEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.*
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.popups.PopupMenuMessage
import com.sceyt.sceytchatuikit.sceytconfigs.MessagesStyle

class MessagesListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), MessageClickListeners.ClickListeners,
        MessagePopupClickListeners.PopupClickListeners, ReactionPopupClickListeners.PopupClickListeners {

    private var messagesRV: MessagesRV
    private var scrollDownIcon: ScrollToDownView
    private var pageStateView: PageStateView? = null
    private lateinit var clickListeners: MessageClickListenersImpl
    private lateinit var messagePopupClickListeners: MessagePopupClickListenersImpl
    private lateinit var reactionClickListeners: ReactionPopupClickListenersImpl
    private var reactionEventListener: ((ReactionEvent) -> Unit)? = null
    private var messageCommandEventListener: ((MessageCommandEvent) -> Unit)? = null
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

        addView(ScrollToDownView(context).also { toDownView ->
            scrollDownIcon = toDownView
            messagesRV.setScrollDownControllerListener { show ->
                scrollDownIcon.isVisible = show
            }
        })

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

        val clickListeners = object : MessageClickListeners.ClickListeners {
            override fun onMessageLongClick(view: View, item: MessageItem) {
                if (enabledClickActions)
                    clickListeners.onMessageLongClick(view, item)
            }

            override fun onAvatarClick(view: View, item: MessageItem) {
                if (enabledClickActions)
                    clickListeners.onAvatarClick(view, item)
            }

            override fun onReplayCountClick(view: View, item: MessageItem) {
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

            override fun onLinkClick(view: View, item: MessageItem) {
                if (enabledClickActions)
                    clickListeners.onLinkClick(view, item)
            }

            override fun onScrollToDownClick(view: ScrollToDownView) {
                clickListeners.onScrollToDownClick(view)
            }
        }
        messagesRV.setMessageListener(clickListeners)

        scrollDownIcon.setOnClickListener {
            clickListeners.onScrollToDownClick(it as ScrollToDownView)
        }
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
                R.id.sceyt_remove -> reactionClickListeners.onRemoveReaction(reaction)
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
                R.id.sceyt_replay_in_thread -> messagePopupClickListeners.onReplayMessageInThreadClick(message)
                R.id.sceyt_copy_message -> messagePopupClickListeners.onCopyMessageClick(message)
                R.id.sceyt_delete_message -> messagePopupClickListeners.onDeleteMessageClick(message, false)
            }
            false
        }
        popup.show()
    }

    private fun addReaction(message: SceytMessage, key: String) {
        reactionEventListener?.invoke(ReactionEvent.AddReaction(message, key))
    }

    private fun onReactionClick(reaction: ReactionItem.Reaction) {
        val containsSelf = reaction.reaction.containsSelf
        if (containsSelf)
            reactionClickListeners.onRemoveReaction(reaction)
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

    private fun updateItem(index: Int, message: MessageListItem, diff: MessageItemPayloadDiff) {
        (messagesRV.findViewHolderForAdapterPosition(index) as? BaseMsgViewHolder)?.bind(message, diff)
                ?: run {
                    messagesRV.adapter?.notifyItemChanged(index, diff)
                }
    }

    internal fun getFirstMessage() = messagesRV.getFirstMsg()

    internal fun getLastMessage() = messagesRV.getLastMsg()

    internal fun setMessagesList(data: List<MessageListItem>, force: Boolean = false) {
        messagesRV.setData(data, force)
    }

    internal fun addNextPageMessages(data: List<MessageListItem>) {
        messagesRV.addNextPageMessages(data)
    }

    internal fun addPrevPageMessages(data: List<MessageListItem>) {
        messagesRV.addPrevPageMessages(data)
    }

    internal fun addNewMessages(vararg data: MessageListItem) {
        if (data.isEmpty()) return
        messagesRV.addNewMessages(*data)
    }

    internal fun updateMessage(message: SceytMessage) {
        for ((index, item) in messagesRV.getData()?.withIndex() ?: return) {
            if (item is MessageItem && (item.message.id == message.id ||
                            (item.message.id == 0L && item.message.tid == message.tid))) {
                val oldMessage = item.message.clone()
                item.message.updateMessage(message)
                updateItem(index, item, oldMessage.diff(item.message))
                break
            }
        }
    }

    internal fun getMessageById(messageId: Long): MessageItem? {
        return messagesRV.getData()?.find { it is MessageItem && it.message.id == messageId }?.let {
            (it as MessageItem)
        }
    }

    internal fun getMessageIndexedById(messageId: Long): Pair<Int, MessageListItem>? {
        return messagesRV.getData()?.findIndexed { it is MessageItem && it.message.id == messageId }
    }

    internal fun sortMessages() {
        messagesRV.sortMessages()
    }

    internal fun messageEditedOrDeleted(updateMessage: SceytMessage) {
        if (updateMessage.deliveryStatus == DeliveryStatus.Pending && updateMessage.state == MessageState.Deleted) {
            messagesRV.deleteMessageByTid(updateMessage.tid)
            return
        }
        messagesRV.getData()?.findIndexed { it is MessageItem && it.message.id == updateMessage.id }?.let {
            val message = (it.second as MessageItem).message
            val oldMessage = message.clone()
            message.updateMessage(updateMessage)
            if (message.state == MessageState.Deleted)
                messagesRV.adapter?.notifyItemChanged(it.first)
            else
                updateItem(it.first, it.second, oldMessage.diff(message))
        }
    }

    internal fun updateReaction(data: SceytMessage) {
        messagesRV.getData()?.findIndexed { it is MessageItem && it.message.id == data.id }?.let {
            val message = (it.second as MessageItem).message
            val oldMessage = message.clone()
            message.reactionScores = data.reactionScores
            message.lastReactions = data.lastReactions
            message.selfReactions = data.selfReactions
            message.messageReactions = data.messageReactions
            updateItem(it.first, it.second, oldMessage.diff(message))
        }
    }

    internal fun updateViewState(state: PageState, enableErrorSnackBar: Boolean = true) {
        pageStateView?.updateState(state, messagesRV.isEmpty(), enableErrorSnackBar = enableErrorSnackBar)
    }

    internal fun updateMessagesStatus(status: DeliveryStatus, ids: MutableList<Long>) {
        ids.forEach { id ->
            for ((index: Int, item: MessageListItem) in (messagesRV.getData()
                    ?: return).withIndex()) {
                if (item is MessageItem) {
                    val oldMessage = item.message.clone()
                    if (item.message.id == id) {
                        if (item.message.deliveryStatus < status)
                            item.message.deliveryStatus = status
                        updateItem(index, item, oldMessage.diff(item.message))
                        break
                    }
                }
            }
        }
    }

    internal fun messageSendFailed(tid: Long) {
        messagesRV.getData()?.findIndexed { it is MessageItem && it.message.tid == tid }?.let {
            val message = (it.second as MessageItem).message
            val oldMessage = message.clone()
            message.deliveryStatus = DeliveryStatus.Failed
            updateItem(it.first, it.second, oldMessage.diff(message))
        }
    }

    internal fun updateReplayCount(replayedMessage: SceytMessage?) {
        messagesRV.getData()?.findIndexed {
            it is MessageItem && it.message.id == replayedMessage?.parent?.id
        }?.let {
            val message = (it.second as MessageItem).message
            val oldMessage = message.clone()
            message.replyCount++
            updateItem(it.first, it.second, oldMessage.diff(message))
        }
    }

    internal fun newReplayMessage(messageId: Long?) {
        messagesRV.getData()?.findIndexed {
            it is MessageItem && it.message.id == messageId
        }?.let {
            val message = (it.second as MessageItem).message
            val oldMessage = message.clone()
            message.replyCount++
            updateItem(it.first, it.second, oldMessage.diff(message))
        }
    }

    internal fun setMessageDisplayedListener(listener: (MessageListItem) -> Unit) {
        messagesRV.setMessageDisplayedListener(listener)
    }

    internal fun setNeedLoadPrevMessagesListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        messagesRV.setNeedLoadPrevMessagesListener(listener)
    }

    internal fun setNeedLoadNextMessagesListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        messagesRV.setNeedLoadNextMessagesListener(listener)
    }

    internal fun setReachToStartListener(listener: (offset: Int, message: MessageListItem?) -> Unit) {
        messagesRV.setRichToStartListener(listener)
    }

    internal fun setMessageReactionsEventListener(listener: (ReactionEvent) -> Unit) {
        reactionEventListener = listener
    }

    internal fun setMessageCommandEventListener(listener: (MessageCommandEvent) -> Unit) {
        messageCommandEventListener = listener
    }

    internal fun clearData() {
        messagesRV.clearData()
        updateViewState(PageState.StateEmpty())
    }

    internal fun setUnreadCount(unreadCount: Int) {
        scrollDownIcon.setUnreadCount(unreadCount)
    }

    fun hideLoadingPrev() {
        messagesRV.hideLoadingPrevItem()
    }

    fun hideLoadingNext() {
        messagesRV.hideLoadingNextItem()
    }

    fun setViewHolderFactory(factory: MessageViewHolderFactory) {
        messagesRV.setViewHolderFactory(factory.also {
            it.setMessageListener(clickListeners)
        })
    }

    fun setUserNameBuilder(builder: (User) -> String) {
        messagesRV.getViewHolderFactory().setUserNameBuilder(builder)
    }


    fun scrollToMessage(msgId: Long) {
        MessagesAdapter.awaitUpdating {
            messagesRV.awaitAnimationEnd {
                messagesRV.getData()?.findIndexed { it is MessageItem && it.message.id == msgId }?.let {
                    messagesRV.scrollToPosition(it.first)
                }
            }
        }
    }

    fun scrollToUnReadMessage() {
        MessagesAdapter.awaitUpdating {
            messagesRV.awaitAnimationEnd {
                messagesRV.getData()?.findIndexed { it is MessageListItem.UnreadMessagesSeparatorItem }?.let {
                    messagesRV.scrollToPosition(it.first)
                }
            }
        }
    }

    fun scrollToLastMessage() {
        MessagesAdapter.awaitUpdating {
            messagesRV.awaitAnimationEnd {
                messagesRV.scrollToPosition((messagesRV.getData()
                        ?: return@awaitAnimationEnd).size - 1)
            }
        }
    }

    fun getData() = messagesRV.getData()

    fun getMessagesRecyclerView() = messagesRV

    fun isLastItemDisplaying() = messagesRV.isLastItemDisplaying()

    // Click listeners
    fun setMessageClickListener(listener: MessageClickListeners) {
        clickListeners.setListener(listener)
    }

    fun setMessagePopupClickListener(listener: MessagePopupClickListeners) {
        messagePopupClickListeners.setListener(listener)
    }

    fun setCustomMessageClickListener(listener: MessageClickListenersImpl) {
        clickListeners = listener
        messagesRV.getViewHolderFactory().setMessageListener(listener)
    }

    fun setCustomMessagePopupClickListener(listener: MessagePopupClickListenersImpl) {
        messagePopupClickListeners = listener
    }

    fun enableDisableClickActions(enabled: Boolean) {
        enabledClickActions = enabled
    }


    // Click events
    override fun onMessageLongClick(view: View, item: MessageItem) {
        showMessageActionsPopup(view, item.message)
    }

    override fun onAvatarClick(view: View, item: MessageItem) {

    }

    override fun onReplayCountClick(view: View, item: MessageItem) {
        onReplayMessageInThreadClick(item.message)
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

    override fun onLinkClick(view: View, item: MessageItem) {
        context.openLink(item.message.body)
    }

    override fun onScrollToDownClick(view: ScrollToDownView) {
        messageCommandEventListener?.invoke(MessageCommandEvent.ScrollToDown(view))
    }


    // Message popup events
    override fun onCopyMessageClick(message: SceytMessage) {
        context.setClipboard(message.body)
    }

    override fun onDeleteMessageClick(message: SceytMessage, onlyForMe: Boolean) {
        DeleteMessageDialog(context, positiveClickListener = {
            messageCommandEventListener?.invoke(MessageCommandEvent.DeleteMessage(message, onlyForMe))
        }).show()
    }

    override fun onEditMessageClick(message: SceytMessage) {
        messageCommandEventListener?.invoke(MessageCommandEvent.EditMessage(message))
    }

    override fun onReactMessageClick(view: View, message: SceytMessage) {
        onAddReactionClick(view, message)
    }

    override fun onReplayMessageClick(message: SceytMessage) {
        messageCommandEventListener?.invoke(MessageCommandEvent.Replay(message))
    }

    override fun onReplayMessageInThreadClick(message: SceytMessage) {
        // Override and add your logic if you
    }


    //Reaction popup events
    override fun onAddReaction(message: SceytMessage, key: String) {
        addReaction(message, key)
    }

    override fun onRemoveReaction(reactionItem: ReactionItem.Reaction) {
        reactionEventListener?.invoke(ReactionEvent.RemoveReaction(reactionItem.message, reactionItem.reaction.key))
    }
}