package com.sceyt.sceytchatuikit.presentation.uicomponents.conversation

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.core.util.Predicate
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.models.messages.SceytAttachment
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.extensions.asActivity
import com.sceyt.sceytchatuikit.extensions.awaitAnimationEnd
import com.sceyt.sceytchatuikit.extensions.awaitToScrollFinish
import com.sceyt.sceytchatuikit.extensions.findIndexed
import com.sceyt.sceytchatuikit.extensions.getFragmentManager
import com.sceyt.sceytchatuikit.extensions.isLastCompletelyItemDisplaying
import com.sceyt.sceytchatuikit.extensions.maybeComponentActivity
import com.sceyt.sceytchatuikit.extensions.openLink
import com.sceyt.sceytchatuikit.extensions.setClipboard
import com.sceyt.sceytchatuikit.logger.SceytLog
import com.sceyt.sceytchatuikit.media.audio.AudioPlayerHelper
import com.sceyt.sceytchatuikit.persistence.differs.MessageDiff
import com.sceyt.sceytchatuikit.persistence.differs.diff
import com.sceyt.sceytchatuikit.persistence.extensions.toArrayList
import com.sceyt.sceytchatuikit.persistence.filetransfer.NeedMediaInfoData
import com.sceyt.sceytchatuikit.persistence.filetransfer.ThumbFor
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferData
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PauseUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.PendingUpload
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Preparing
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.ThumbLoaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploaded
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.Uploading
import com.sceyt.sceytchatuikit.persistence.filetransfer.TransferState.WaitingToUpload
import com.sceyt.sceytchatuikit.presentation.common.KeyboardEventListener
import com.sceyt.sceytchatuikit.presentation.root.PageState
import com.sceyt.sceytchatuikit.presentation.root.PageStateView
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.files.openFile
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageListItem.MessageItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessageViewHolderFactory
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.MessagesAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.messages.root.BaseMsgViewHolder
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.dialogs.DeleteMessageDialog
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.MessageCommandEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.events.ReactionEvent
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.fragments.BottomSheetReactionsInfoFragment
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageActionsViewClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageActionsViewClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.ReactionPopupClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.listeners.ReactionPopupClickListenersImpl
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.popups.PopupMenuMessage
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.popups.PopupReactions
import com.sceyt.sceytchatuikit.presentation.uicomponents.conversation.popups.PopupReactionsAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.forward.SceytForwardActivity
import com.sceyt.sceytchatuikit.presentation.uicomponents.mediaview.SceytMediaActivity
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.MAX_SELF_REACTIONS_SIZE
import com.sceyt.sceytchatuikit.sceytstyles.MessagesStyle

class MessagesListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), MessageClickListeners.ClickListeners,
        MessageActionsViewClickListeners.ActionsViewClickListeners, ReactionPopupClickListeners.PopupClickListeners {

    private var messagesRV: MessagesRV
    private var scrollDownIcon: ScrollToDownView
    private var pageStateView: PageStateView? = null
    private lateinit var defaultClickListeners: MessageClickListenersImpl
    private lateinit var clickListeners: MessageClickListenersImpl
    internal lateinit var messageActionsViewClickListeners: MessageActionsViewClickListenersImpl
    private lateinit var reactionClickListeners: ReactionPopupClickListenersImpl
    private var messageCommandEventListener: ((MessageCommandEvent) -> Unit)? = null
    private var reactionEventListener: ((ReactionEvent) -> Unit)? = null
    private var reactionsPopupWindow: PopupWindow? = null
    private var onWindowFocusChangeListener: ((Boolean) -> Unit)? = null
    private var multiselectDestination: Map<Long, SceytMessage>? = null
    private var forceDisabledActions = false
    var enabledActions = true
        private set

    init {
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

        messagesRV.setSwipeToReplyListener { item ->
            (item as? MessageItem)?.message?.let { message ->
                messageCommandEventListener?.invoke(MessageCommandEvent.Reply(message))
            }
        }

        if (!isInEditMode)
            addView(PageStateView(context).also {
                pageStateView = it
                it.setLoadingStateView(MessagesStyle.loadingState)
                it.setEmptyStateView(MessagesStyle.emptyState)
            })

        initClickListeners()
        addKeyBoardListener()
    }

    private fun initClickListeners() {
        clickListeners = MessageClickListenersImpl(this)
        messageActionsViewClickListeners = MessageActionsViewClickListenersImpl(this)
        reactionClickListeners = ReactionPopupClickListenersImpl(this)

        defaultClickListeners = object : MessageClickListenersImpl() {
            override fun onMessageClick(view: View, item: MessageItem) {
                checkMaybeInMultiSelectMode(view, item.message) {
                    clickListeners.onMessageClick(view, item)
                }
            }

            override fun onMessageLongClick(view: View, item: MessageItem) {
                checkMaybeInMultiSelectMode(view, item.message) {
                    clickListeners.onMessageLongClick(view, item)
                }
            }

            override fun onAvatarClick(view: View, item: MessageItem) {
                checkMaybeInMultiSelectMode(view, item.message) {
                    clickListeners.onAvatarClick(view, item)
                }
            }

            override fun onReplyMessageContainerClick(view: View, item: MessageItem) {
                checkMaybeInMultiSelectMode(view, item.message) {
                    clickListeners.onReplyMessageContainerClick(view, item)
                }
            }

            override fun onReplyCountClick(view: View, item: MessageItem) {
                checkMaybeInMultiSelectMode(view, item.message) {
                    clickListeners.onReplyCountClick(view, item)
                }
            }

            override fun onAddReactionClick(view: View, message: SceytMessage) {
                checkMaybeInMultiSelectMode(view, message) {
                    clickListeners.onAddReactionClick(view, message)
                }
            }

            override fun onReactionClick(view: View, item: ReactionItem.Reaction) {
                checkMaybeInMultiSelectMode(view, item.message) {
                    clickListeners.onReactionClick(view, item)
                }
            }

            override fun onReactionLongClick(view: View, item: ReactionItem.Reaction) {
                checkMaybeInMultiSelectMode(view, item.message) {
                    clickListeners.onReactionLongClick(view, item)
                }
            }

            override fun onAttachmentClick(view: View, item: FileListItem) {
                checkMaybeInMultiSelectMode(view, item.sceytMessage) {
                    clickListeners.onAttachmentClick(view, item)
                }
            }

            override fun onAttachmentLongClick(view: View, item: FileListItem) {
                checkMaybeInMultiSelectMode(view, item.sceytMessage) {
                    clickListeners.onAttachmentLongClick(view, item)
                }
            }

            override fun onMentionClick(view: View, userId: String) {
                clickListeners.onMentionClick(view, userId)
            }

            override fun onAttachmentLoaderClick(view: View, item: FileListItem) {
                checkMaybeInMultiSelectMode(view, item.sceytMessage) {
                    clickListeners.onAttachmentLoaderClick(view, item)
                }
            }

            override fun onLinkClick(view: View, item: MessageItem) {
                checkMaybeInMultiSelectMode(view, item.message) {
                    clickListeners.onLinkClick(view, item)
                }
            }

            override fun onLinkDetailsClick(view: View, item: MessageItem) {
                checkMaybeInMultiSelectMode(view, item.message) {
                    clickListeners.onLinkDetailsClick(view, item)
                }
            }

            override fun onMultiSelectClick(view: View, message: SceytMessage) {
                clickListeners.onMultiSelectClick(view, message)
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

    fun setMultiSelectableMode() {
        (messagesRV.getMessagesAdapter())?.setMultiSelectableMode(true)
        messagesRV.enableDisableSwipeToReply(false)
        for (i in 0 until messagesRV.childCount) {
            messagesRV.getChildAt(i)?.let {
                val holder = messagesRV.getChildViewHolder(it)
                (holder as? BaseMsgViewHolder)?.setSelectableState()
            }
        }
    }

    private fun checkMaybeInMultiSelectMode(view: View, message: SceytMessage, action: () -> Unit) {
        if (multiselectDestination.isNullOrEmpty()) {
            action.invoke()
        } else clickListeners.onMultiSelectClick(view, message)
    }

    private fun addKeyBoardListener() {
        context.maybeComponentActivity()?.let {
            KeyboardEventListener(it) { isOpen ->
                if (!isOpen) {
                    reactionsPopupWindow?.dismiss()
                    reactionsPopupWindow = null
                }
            }
        }
    }

    private fun showModifyReactionsPopup(view: View, message: SceytMessage): PopupReactions? {
        if (message.deliveryStatus == DeliveryStatus.Pending) return null
        val reactions = message.messageReactions?.map { it.reaction.key }?.toArrayList()
                ?: arrayListOf()
        if (reactions.size < MAX_SELF_REACTIONS_SIZE)
            reactions.addAll(SceytKitConfig.defaultReactions.minus(reactions.toSet()).take(MAX_SELF_REACTIONS_SIZE - reactions.size))

        return PopupReactions(context).showPopup(view, message, reactions, object : PopupReactionsAdapter.OnItemClickListener {
            override fun onReactionClick(reaction: ReactionItem.Reaction) {
                this@MessagesListView.onAddOrRemoveReaction(reaction)
            }

            override fun onAddClick() {
                onAddReactionClick(view, message)
            }
        }).also {
            reactionsPopupWindow = it
            it.setOnDismissListener {
                Handler(Looper.getMainLooper()).postDelayed({ reactionsPopupWindow = null }, 100)
            }
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
        val popup = PopupMenuMessage(ContextThemeWrapper(context, R.style.SceytPopupMenuStyle), view, message)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sceyt_edit_message -> messageActionsViewClickListeners.onEditMessageClick(message)
                R.id.sceyt_forward -> messageActionsViewClickListeners.onForwardMessageClick(message)
                R.id.sceyt_react -> messageActionsViewClickListeners.onReactMessageClick(message)
                R.id.sceyt_reply -> messageActionsViewClickListeners.onReplyMessageClick(message)
                R.id.sceyt_reply_in_thread -> messageActionsViewClickListeners.onReplyMessageInThreadClick(message)
                R.id.sceyt_copy_message -> messageActionsViewClickListeners.onCopyMessagesClick(message)
                R.id.sceyt_delete_message -> messageActionsViewClickListeners.onDeleteMessageClick(message, requireForMe = false, actionFinish = {})
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

    private fun onAddOrRemoveReaction(reaction: ReactionItem.Reaction) {
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
            BottomSheetEmojisFragment().also { fragment ->
                fragment.setEmojiListener { emoji ->
                    val containsSelf = message.userReactions?.find { reaction -> reaction.key == emoji } != null
                    onAddOrRemoveReaction(ReactionItem.Reaction(SceytReactionTotal(emoji, containsSelf = containsSelf), message, true))
                }
            }.show(it, null)
        }
    }

    private fun updateItem(index: Int, item: MessageListItem, diff: MessageDiff) {
        val message = (item as? MessageItem)?.message ?: return
        (messagesRV.findViewHolderForItemId(item.getItemId()) as? BaseMsgViewHolder)?.let {
            SceytLog.i("StatusIssueTag", "updateItem: found by itemId: ${item.getItemId()}, msgId-> ${message.id}, diff ${diff.statusChanged}")
            it.bind(item, diff)
        } ?: run {
            SceytLog.i("StatusIssueTag", "updateItem: notifyItemChanged by index $index, diff ${diff.statusChanged}, msgId-> ${message.id}")
            messagesRV.adapter?.notifyItemChanged(index, diff)
        }
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
        messagesRV.awaitAnimationEnd {
            messagesRV.addNewMessages(*data)
        }
    }

    internal fun updateMessage(message: SceytMessage): Boolean {
        SceytLog.i(TAG, "Message updated: id ${message.id}, tid ${message.tid}," +
                " body ${message.body}, deliveryStatus ${message.deliveryStatus}")
        var found = false

        for ((index, item) in messagesRV.getData()?.withIndex() ?: return false) {
            if (item is MessageItem && item.message.tid == message.tid) {
                val oldMessage = item.message.clone()
                Log.i(TAG, "${oldMessage.deliveryStatus}  ${message.deliveryStatus}")
                item.message.updateMessage(message)
                val diff = oldMessage.diff(item.message)
                SceytLog.i(TAG, "Found to update: id ${item.message.id}, tid ${item.message.tid}," +
                        " diff ${diff.statusChanged}, newStatus ${message.deliveryStatus}, index $index, size ${messagesRV.getData()?.size}")
                updateItem(index, item, diff)
                found = true
                break
            }
        }
        return found
    }

    internal fun updateMessageSelection(message: SceytMessage) {
        for ((index, item) in messagesRV.getData()?.withIndex() ?: return) {
            if (item is MessageItem && item.message.tid == message.tid) {
                item.message.isSelected = message.isSelected
                updateItem(index, item, MessageDiff.DEFAULT_FALSE.copy(selectionChanged = true))
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

    internal fun messageEditedOrDeleted(updateMessage: SceytMessage): Boolean {
        var found = false
        if (updateMessage.deliveryStatus == DeliveryStatus.Pending && updateMessage.state == MessageState.Deleted) {
            messagesRV.deleteMessageByTid(updateMessage.tid)
            if (messagesRV.isEmpty())
                pageStateView?.updateState(PageState.StateEmpty())
            return true
        }
        val data = messagesRV.getData() ?: return false
        data.findIndexed { it is MessageItem && it.message.id == updateMessage.id }?.let {
            val message = (it.second as MessageItem).message
            val oldMessage = message.clone()
            message.updateMessage(updateMessage)
            if (message.state == MessageState.Deleted && oldMessage.state != MessageState.Deleted)
                messagesRV.adapter?.notifyItemChanged(it.first)
            else
                updateItem(it.first, it.second, oldMessage.diff(message))
            found = true
        }

        // Check reply message to update
        data.filter { it is MessageItem && it.message.parentMessage?.id == updateMessage.id }.forEach { item ->
            data.findIndexed { it is MessageItem && it.message.id == (item as MessageItem).message.id }?.let {
                val message = (it.second as MessageItem).message
                val oldMessage = message.clone()
                message.parentMessage?.updateMessage(updateMessage)
                updateItem(it.first, it.second, oldMessage.diff(message))
            }
        }
        return found
    }

    internal fun forceDeleteMessageByTid(tid: Long) {
        messagesRV.deleteMessageByTid(tid)
        if (messagesRV.isEmpty())
            pageStateView?.updateState(PageState.StateEmpty())
    }

    internal fun updateReaction(data: SceytMessage) {
        messagesRV.getData()?.findIndexed { it is MessageItem && it.message.id == data.id }?.let {
            val message = (it.second as MessageItem).message
            val oldMessage = message.clone()
            message.reactionTotals = data.reactionTotals
            message.userReactions = data.userReactions
            message.messageReactions = data.messageReactions
            updateItem(it.first, it.second, oldMessage.diff(message))
        }
    }

    internal fun updateViewState(state: PageState, enableErrorSnackBar: Boolean = true) {
        pageStateView?.updateState(state, messagesRV.isEmpty(), enableErrorSnackBar = enableErrorSnackBar)
    }

    internal fun updateMessagesStatus(status: DeliveryStatus, ids: MutableList<Long>) {
        val data = messagesRV.getData() ?: return
        ids.forEach { id ->
            for ((index: Int, item: MessageListItem) in data.withIndex()) {
                if (item is MessageItem) {
                    val oldMessage = item.message.clone()
                    if (item.message.id == id) {
                        if (item.message.deliveryStatus < status) {
                            item.message.deliveryStatus = status
                            updateItem(index, item, oldMessage.diff(item.message))
                        }
                        break
                    }
                }
            }
        }
    }

    internal fun updateProgress(data: TransferData, updateRecycler: Boolean) {
        val messages = messagesRV.getData() ?: return
        ArrayList(messages).findIndexed { item -> item is MessageItem && item.message.tid == data.messageTid }?.let { (index, it) ->
            val predicate: (SceytAttachment) -> Boolean = when (data.state) {
                Uploading, PendingUpload, PauseUpload, Uploaded, Preparing, WaitingToUpload -> { attachment ->
                    attachment.messageTid == data.messageTid
                }

                else -> { attachment ->
                    attachment.url == data.url
                }
            }
            val foundAttachmentFile = (it as MessageItem).message.files?.find { listItem -> predicate(listItem.file) }

            if (data.state == ThumbLoaded) {
                if (data.thumbData?.key == ThumbFor.MessagesLisView.value)
                    foundAttachmentFile?.let { listItem ->
                        listItem.thumbPath = data.filePath
                    }
                return
            }

            val foundAttachment = it.message.attachments?.find(predicate)

            foundAttachment?.let { attachment ->
                attachment.updateWithTransferData(data)
                foundAttachmentFile?.file?.updateWithTransferData(data)
            }

            if (updateRecycler)
                updateItem(index, it, MessageDiff.DEFAULT_FALSE.copy(filesChanged = true))
        }
    }

    internal fun messageSendFailed(tid: Long) {
        messagesRV.getData()?.findIndexed { it is MessageItem && it.message.tid == tid }?.let {
            val message = (it.second as MessageItem).message
            val oldMessage = message.clone()
            message.deliveryStatus = DeliveryStatus.Pending
            updateItem(it.first, it.second, oldMessage.diff(message))
        }
    }

    internal fun updateReplyCount(replyMessage: SceytMessage?) {
        messagesRV.getData()?.findIndexed {
            it is MessageItem && it.message.id == replyMessage?.parentMessage?.id
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

    internal fun setScrollStateChangeListener(listener: (Int) -> Unit) {
        messagesRV.setScrollStateChangeListener(listener)
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

    internal fun deleteAllMessagesBefore(predicate: Predicate<MessageListItem>) {
        messagesRV.deleteAllMessagesBefore(predicate)
    }

    internal fun setUnreadCount(unreadCount: Int) {
        scrollDownIcon.setUnreadCount(unreadCount)
    }

    internal fun setOnWindowFocusChangeListener(listener: (Boolean) -> Unit) {
        onWindowFocusChangeListener = listener
    }

    internal fun getMessageCommandEventListener() = messageCommandEventListener

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

    fun setNeedDownloadListener(callBack: (NeedMediaInfoData) -> Unit) {
        messagesRV.getViewHolderFactory().setNeedMediaDataCallback(callBack)
    }

    fun scrollToMessage(msgId: Long, highlight: Boolean) {
        MessagesAdapter.awaitUpdating {
            messagesRV.awaitAnimationEnd {
                messagesRV.getData()?.findIndexed { it is MessageItem && it.message.id == msgId }?.let {
                    if (highlight)
                        it.second.highlighted = true
                    (messagesRV.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(it.first, 200)
                }
            }
        }
    }

    fun scrollToPositionAndHighlight(position: Int, highlight: Boolean) {
        MessagesAdapter.awaitUpdating {
            messagesRV.awaitAnimationEnd {
                (messagesRV.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 200)
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
                    (messagesRV.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(it.first, 0)
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

    fun cancelMultiSelectMode() {
        (messagesRV.getMessagesAdapter())?.setMultiSelectableMode(false)
        messagesRV.enableDisableSwipeToReply(enabledActions)
        for (i in 0 until messagesRV.childCount) {
            messagesRV.getChildAt(i)?.let {
                val holder = messagesRV.getChildViewHolder(it)
                (holder as? BaseMsgViewHolder)?.cancelSelectableState()
            }
        }
        ArrayList(messagesRV.getData() ?: return).forEach { item ->
            if (item is MessageItem)
                item.message.isSelected = false
        }
    }

    fun setMultiselectDestination(map: Map<Long, SceytMessage>) {
        multiselectDestination = map
    }

    fun removeUnreadMessagesSeparator() {
        messagesRV.removeUnreadMessagesSeparator()
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

    fun setMessagePopupClickListener(listener: MessageActionsViewClickListeners) {
        messageActionsViewClickListeners.setListener(listener)
    }

    fun setCustomMessageClickListener(listener: MessageClickListenersImpl) {
        clickListeners = listener
    }

    fun setCustomMessageActionsViewClickListener(listener: MessageActionsViewClickListenersImpl) {
        messageActionsViewClickListeners = listener
    }

    fun enableDisableActions(enabled: Boolean, force: Boolean) {
        if (force) {
            forceDisabledActions = !enabled
            enabledActions = enabled
        } else if (!forceDisabledActions)
            enabledActions = enabled

        messagesRV.enableDisableSwipeToReply(enabledActions)
    }

    fun startSearchMessages() {
        messageCommandEventListener?.invoke(MessageCommandEvent.SearchMessages(true))
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        onWindowFocusChangeListener?.invoke(hasWindowFocus)
        if (!hasWindowFocus && context.asActivity().isFinishing)
            AudioPlayerHelper.stopAll()
    }

    // Click events
    override fun onMessageClick(view: View, item: MessageItem) {
        if (enabledActions) {
            if (reactionsPopupWindow == null)
                showModifyReactionsPopup(view, item.message)
        }
    }

    override fun onMessageLongClick(view: View, item: MessageItem) {
        if (enabledActions)
            messageCommandEventListener?.invoke(MessageCommandEvent.OnMultiselectEvent(item.message))
    }

    override fun onAvatarClick(view: View, item: MessageItem) {
        messageCommandEventListener?.invoke(MessageCommandEvent.UserClick(view, item.message.user?.id
                ?: return))
    }

    override fun onReplyMessageContainerClick(view: View, item: MessageItem) {
        onReplyMessageContainerClick(item)
    }

    override fun onReplyCountClick(view: View, item: MessageItem) {
        onReplyMessageInThreadClick(item.message)
    }

    override fun onAddReactionClick(view: View, message: SceytMessage) {
        if (enabledActions)
            showAddEmojiDialog(message)
    }

    override fun onReactionClick(view: View, item: ReactionItem.Reaction) {
        context.getFragmentManager()?.let {
            BottomSheetReactionsInfoFragment.newInstance(item.message).also { fragment ->
                fragment.setClickListener { reaction ->
                    if (reaction.user?.id == SceytKitClient.myId)
                        reactionClickListeners.onRemoveReaction(ReactionItem.Reaction(SceytReactionTotal(reaction.key, containsSelf = true), item.message, reaction.pending))
                }
            }.show(it, null)
        }
    }

    override fun onReactionLongClick(view: View, item: ReactionItem.Reaction) {
        if (enabledActions)
            showReactionActionsPopup(view, item)
    }

    override fun onAttachmentClick(view: View, item: FileListItem) {
        when (item) {
            is FileListItem.Image -> {
                SceytMediaActivity.openMediaView(context, item.file, item.sceytMessage.user, item.message.channelId)
            }

            is FileListItem.Video -> {
                SceytMediaActivity.openMediaView(context, item.file, item.sceytMessage.user, item.message.channelId)
            }

            else -> item.file.openFile(context)
        }
    }

    override fun onAttachmentLongClick(view: View, item: FileListItem) {
        clickListeners.onMessageLongClick(view, MessageItem(item.sceytMessage))
    }

    override fun onMentionClick(view: View, userId: String) {
        messageCommandEventListener?.invoke(MessageCommandEvent.UserClick(view, userId))
    }

    override fun onAttachmentLoaderClick(view: View, item: FileListItem) {
        onAttachmentLoaderClick(item)
    }

    override fun onLinkClick(view: View, item: MessageItem) {
        item.message.attachments?.firstOrNull()?.let {
            context.openLink(it.url)
        }
    }

    override fun onLinkDetailsClick(view: View, item: MessageItem) {
        item.message.attachments?.firstOrNull()?.let {
            context.openLink(it.url)
        }
    }

    override fun onMultiSelectClick(view: View, message: SceytMessage) {
        messageCommandEventListener?.invoke(MessageCommandEvent.OnMultiselectEvent(message))
    }

    override fun onScrollToDownClick(view: ScrollToDownView) {
        messageCommandEventListener?.invoke(MessageCommandEvent.ScrollToDown(view))
    }


    // Message popup events
    override fun onCopyMessagesClick(vararg messages: SceytMessage) {
        val text = messages.joinToString("\n\n") { it.body.trim() }
        context.setClipboard(text.trim())
        val toastMessage = if (messages.size == 1) context.getString(R.string.sceyt_message_copied)
        else context.getString(R.string.sceyt_messages_copied)
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteMessageClick(vararg messages: SceytMessage, requireForMe: Boolean, actionFinish: () -> Unit) {
        DeleteMessageDialog(context)
            .setDeleteMessagesCount(messages.size)
            .setRequireForMe(requireForMe)
            .setAcceptCallback { forMe ->
                actionFinish()
                messageCommandEventListener?.invoke(MessageCommandEvent.DeleteMessage(messages.toList(),
                    onlyForMe = requireForMe || forMe))
            }.show()
    }

    override fun onEditMessageClick(message: SceytMessage) {
        messageCommandEventListener?.invoke(MessageCommandEvent.EditMessage(message))
    }

    override fun onForwardMessageClick(vararg messages: SceytMessage) {
        SceytForwardActivity.launch(context, *messages)
    }

    override fun onReactMessageClick(message: SceytMessage) {
        onAddReactionClick(this, message)
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
        reactionEventListener?.invoke(ReactionEvent.RemoveReaction(reactionItem.message, reactionItem.reaction.key, reactionItem.isPending))
    }
}