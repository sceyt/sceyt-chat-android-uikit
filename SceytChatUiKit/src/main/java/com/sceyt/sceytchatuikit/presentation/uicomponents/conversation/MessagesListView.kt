package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation

import android.content.Context
import android.util.AttributeSet
import android.util.Log
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
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState
import com.sceyt.sceytchatuikit.presentation.common.diff
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.root.PageStateView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.MessageFilesAdapter
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MessagesListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), MessageClickListeners.ClickListeners,
        MessagePopupClickListeners.PopupClickListeners, ReactionPopupClickListeners.PopupClickListeners {

    private var messagesRV: MessagesRV
    private var scrollDownIcon: ScrollToDownView
    private var pageStateView: PageStateView? = null
    private lateinit var defaultClickListeners: MessageClickListenersImpl
    private lateinit var clickListeners: MessageClickListenersImpl
    private lateinit var messagePopupClickListeners: MessagePopupClickListenersImpl
    private lateinit var reactionClickListeners: ReactionPopupClickListenersImpl
    private var reactionEventListener: ((ReactionEvent) -> Unit)? = null
    private var messageCommandEventListener: ((MessageCommandEvent) -> Unit)? = null
    private var enabledClickActions = true

    init {
        MessageFilesAdapter.clearListeners()
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

        defaultClickListeners = object : MessageClickListenersImpl() {
            override fun onMessageLongClick(view: View, item: MessageItem) {
                if (enabledClickActions)
                    clickListeners.onMessageLongClick(view, item)
            }

            override fun onAvatarClick(view: View, item: MessageItem) {
                if (enabledClickActions)
                    clickListeners.onAvatarClick(view, item)
            }

            override fun onReplyMessageContainerClick(view: View, item: MessageItem) {
                clickListeners.onReplyMessageContainerClick(view, item)
            }

            override fun onReplyCountClick(view: View, item: MessageItem) {
                if (enabledClickActions)
                    clickListeners.onReplyCountClick(view, item)
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

            override fun onAttachmentLoaderClick(view: View, item: FileListItem) {
                if (enabledClickActions)
                    clickListeners.onAttachmentLoaderClick(view, item)
            }

            override fun onLinkClick(view: View, item: MessageItem) {
                if (enabledClickActions)
                    clickListeners.onLinkClick(view, item)
            }

            override fun onScrollToDownClick(view: ScrollToDownView) {
                clickListeners.onScrollToDownClick(view)
            }
        }
        messagesRV.setMessageListener(defaultClickListeners)

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
                R.id.sceyt_reply -> messagePopupClickListeners.onReplyMessageClick(message)
                R.id.sceyt_reply_in_thread -> messagePopupClickListeners.onReplyMessageInThreadClick(message)
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

    private fun onReplyMessageContainerClick(item: MessageItem) {
        messageCommandEventListener?.invoke(MessageCommandEvent.ScrollToReplyMessage(item.message))
    }

    private fun onReactionClick(reaction: ReactionItem.Reaction) {
        val containsSelf = reaction.reaction.containsSelf
        if (containsSelf)
            reactionClickListeners.onRemoveReaction(reaction)
        else
            reactionClickListeners.onAddReaction(reaction.message, reaction.reaction.key)

    }

    private fun onAttachmentLoaderClick(item: FileListItem) {
        messageCommandEventListener?.invoke(MessageCommandEvent.AttachmentLoaderClick(item))
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
                ?: run { messagesRV.adapter?.notifyItemChanged(index, diff) }
    }

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
            if (item is MessageItem && ((message.id != 0L && item.message.id == message.id) ||
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

    internal suspend fun updateProgress(data: TransferData) {
        Log.i(TAG, data.toString())
        messagesRV.getData()?.findIndexed { item -> item is MessageItem && item.message.tid == data.messageTid }?.let {
            val predicate: (SceytAttachment) -> Boolean = when (data.state) {
                TransferState.Uploading, TransferState.PendingUpload, TransferState.Uploaded -> { attachment ->
                    attachment.tid == data.attachmentTid
                }
                else -> { attachment ->
                    attachment.url == data.url
                }
            }

            val foundAttachment = (it.second as MessageItem).message.attachments?.find(predicate)
            Log.i(TAG, "foundAttachment " + foundAttachment?.tid.toString() + " $data")

            foundAttachment?.let { attachment ->
              /*  (it.second as MessageItem).message.files?.find { fileItem ->
                    fileItem.file == attachment
                }*/

                attachment.filePath = data.filePath
                attachment.url = data.url
                attachment.transferState = data.state
                attachment.progressPercent = data.progressPercent

                withContext(Dispatchers.Main) {
                    MessageFilesAdapter.update(data)
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

    internal fun updateReplyCount(replyMessage: SceytMessage?) {
        messagesRV.getData()?.findIndexed {
            it is MessageItem && it.message.id == replyMessage?.parent?.id
        }?.let {
            val message = (it.second as MessageItem).message
            val oldMessage = message.clone()
            message.replyCount++
            updateItem(it.first, it.second, oldMessage.diff(message))
        }
    }

    internal fun newReplyMessage(messageId: Long?) {
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
            it.setMessageListener(defaultClickListeners)
        })
    }

    fun setUserNameBuilder(builder: (User) -> String) {
        messagesRV.getViewHolderFactory().setUserNameBuilder(builder)
    }

    fun setNeedDownloadListener(callBack: (FileListItem) -> Unit) {
        messagesRV.getViewHolderFactory().setNeedDownloadCallback(callBack)
    }

    fun scrollToMessage(msgId: Long, highlight: Boolean) {
        MessagesAdapter.awaitUpdating {
            messagesRV.awaitAnimationEnd {
                messagesRV.getData()?.findIndexed { it is MessageItem && it.message.id == msgId }?.let {
                    if (highlight)
                        it.second.highlighted = true
                    messagesRV.scrollToPosition(it.first)
                }
            }
        }
    }

    fun scrollToPositionAndHighlight(position: Int, highlight: Boolean) {
        MessagesAdapter.awaitUpdating {
            messagesRV.awaitAnimationEnd {
                messagesRV.scrollToPosition(position)
                if (highlight) {
                    messagesRV.awaitToScrollFinish(position, callback = {
                        (messagesRV.findViewHolderForAdapterPosition(position) as? BaseMsgViewHolder)?.highlight()
                    })
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


    fun getFirstMessage() = messagesRV.getFirstMsg()

    fun getLastMessage() = messagesRV.getLastMsg()

    fun getFirstMessageBy(predicate: (MessageListItem) -> Boolean) = messagesRV.getFirstMessageBy(predicate)

    fun getLastMessageBy(predicate: (MessageListItem) -> Boolean) = messagesRV.getLastMessageBy(predicate)

    fun getMessagesRecyclerView() = messagesRV

    fun isLastCompletelyItemDisplaying() = messagesRV.isLastCompletelyItemDisplaying()

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
    override fun onMessageLongClick(view: View, item: MessageItem) {
        showMessageActionsPopup(view, item.message)
    }

    override fun onAvatarClick(view: View, item: MessageItem) {

    }

    override fun onReplyMessageContainerClick(view: View, item: MessageItem) {
        onReplyMessageContainerClick(item)
    }

    override fun onReplyCountClick(view: View, item: MessageItem) {
        onReplyMessageInThreadClick(item.message)
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
        clickListeners.onMessageLongClick(view, MessageItem(item.sceytMessage))
    }

    override fun onAttachmentLoaderClick(view: View, item: FileListItem) {
        onAttachmentLoaderClick(item)
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

    override fun onReplyMessageClick(message: SceytMessage) {
        messageCommandEventListener?.invoke(MessageCommandEvent.Reply(message))
    }

    override fun onReplyMessageInThreadClick(message: SceytMessage) {
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