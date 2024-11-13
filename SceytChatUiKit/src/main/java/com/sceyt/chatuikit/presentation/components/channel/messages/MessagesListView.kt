package com.sceyt.chatuikit.presentation.components.channel.messages

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.Predicate
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.LinkPreviewDetails
import com.sceyt.chatuikit.data.models.messages.SceytAttachment
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.chatuikit.databinding.SceytMessagesListViewBinding
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.extensions.asActivity
import com.sceyt.chatuikit.extensions.awaitAnimationEnd
import com.sceyt.chatuikit.extensions.awaitToScrollFinish
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.extensions.getFragmentManager
import com.sceyt.chatuikit.extensions.isLastCompletelyItemDisplaying
import com.sceyt.chatuikit.extensions.maybeComponentActivity
import com.sceyt.chatuikit.extensions.openLink
import com.sceyt.chatuikit.extensions.setClipboard
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.media.audio.AudioPlayerHelper
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.persistence.file_transfer.NeedMediaInfoData
import com.sceyt.chatuikit.persistence.file_transfer.ThumbFor
import com.sceyt.chatuikit.persistence.file_transfer.TransferData
import com.sceyt.chatuikit.persistence.file_transfer.TransferState
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PauseUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.PendingUpload
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Preparing
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.ThumbLoaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploaded
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.Uploading
import com.sceyt.chatuikit.persistence.file_transfer.TransferState.WaitingToUpload
import com.sceyt.chatuikit.persistence.mappers.isLink
import com.sceyt.chatuikit.presentation.common.KeyboardEventListener
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.openFile
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessagesAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.presentation.components.channel.messages.components.EmojiPickerBottomSheetFragment
import com.sceyt.chatuikit.presentation.components.channel.messages.components.MessagesRV
import com.sceyt.chatuikit.presentation.components.channel.messages.components.ScrollToDownView
import com.sceyt.chatuikit.presentation.components.channel.messages.dialogs.DeleteMessageDialog
import com.sceyt.chatuikit.presentation.components.channel.messages.events.MessageCommandEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.events.ReactionEvent
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.ReactionsInfoBottomSheetFragment
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.action.MessageActionsViewClickListeners
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.action.MessageActionsViewClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.ReactionPopupClickListeners
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.ReactionPopupClickListenersImpl
import com.sceyt.chatuikit.presentation.components.channel.messages.popups.MessageActionsPopupMenu
import com.sceyt.chatuikit.presentation.components.channel.messages.popups.PopupReactionsAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.popups.ReactionsPopup
import com.sceyt.chatuikit.presentation.components.forward.ForwardActivity
import com.sceyt.chatuikit.presentation.components.media.MediaPreviewActivity
import com.sceyt.chatuikit.presentation.extensions.getUpdateMessage
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("Unused", "MemberVisibilityCanBePrivate")
class MessagesListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr), MessageClickListeners.ClickListeners,
        MessageActionsViewClickListeners.ActionsViewClickListeners, ReactionPopupClickListeners.PopupClickListeners {

    private val binding: SceytMessagesListViewBinding
    private var messagesRV: MessagesRV
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
    val style: MessagesListViewStyle
    var enabledActions = true
        private set

    init {
        binding = SceytMessagesListViewBinding.inflate(LayoutInflater.from(context), this)
        style = MessagesListViewStyle.Builder(context, attrs).build()

        if (background == null)
            setBackgroundColor(style.backgroundColor)

        binding.scrollDownView.setStyle(style.scrollDownButtonStyle)
        binding.pageStateView.setLoadingStateView(style.loadingState)
        binding.pageStateView.setEmptyStateView(style.emptyState)

        messagesRV = binding.rvMessages.also { it.setStyle(style) }
        messagesRV.setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)
        messagesRV.clipToPadding = clipToPadding
        super.setPadding(0, 0, 0, 0)

        messagesRV.setScrollDownControllerListener { show ->
            binding.scrollDownView.isVisible = show && style.enableScrollDownButton
        }

        messagesRV.setSwipeToReplyListener { item ->
            (item as? MessageItem)?.message?.let { message ->
                messageCommandEventListener?.invoke(MessageCommandEvent.Reply(message))
            }
        }

        initClickListeners()
        addKeyBoardListener()

        if (isInEditMode)
            binding.scrollDownView.isVisible = style.enableScrollDownButton
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

            override fun onReactionClick(view: View, item: ReactionItem.Reaction, message: SceytMessage) {
                checkMaybeInMultiSelectMode(view, message) {
                    clickListeners.onReactionClick(view, item, message)
                }
            }

            override fun onReactionLongClick(view: View, item: ReactionItem.Reaction, message: SceytMessage) {
                checkMaybeInMultiSelectMode(view, message) {
                    clickListeners.onReactionLongClick(view, item, message)
                }
            }

            override fun onAttachmentClick(view: View, item: FileListItem, message: SceytMessage) {
                checkMaybeInMultiSelectMode(view, message) {
                    clickListeners.onAttachmentClick(view, item, message)
                }
            }

            override fun onAttachmentLongClick(view: View, item: FileListItem, message: SceytMessage) {
                checkMaybeInMultiSelectMode(view, message) {
                    clickListeners.onAttachmentLongClick(view, item, message)
                }
            }

            override fun onMentionClick(view: View, userId: String) {
                clickListeners.onMentionClick(view, userId)
            }

            override fun onAttachmentLoaderClick(view: View, item: FileListItem, message: SceytMessage) {
                checkMaybeInMultiSelectMode(view, message) {
                    clickListeners.onAttachmentLoaderClick(view, item, message)
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

        binding.scrollDownView.setOnClickListener {
            clickListeners.onScrollToDownClick(it as ScrollToDownView)
        }
    }

    fun setMultiSelectableMode() {
        (messagesRV.getMessagesAdapter())?.setMultiSelectableMode(true)
        messagesRV.enableDisableSwipeToReply(false)
        for (i in 0 until messagesRV.childCount) {
            messagesRV.getChildAt(i)?.let {
                val holder = messagesRV.getChildViewHolder(it)
                (holder as? BaseMessageViewHolder)?.setSelectableState()
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

    private fun showModifyReactionsPopup(view: View, message: SceytMessage): ReactionsPopup? {
        if (message.deliveryStatus == DeliveryStatus.Pending) return null
        val maxSize = SceytChatUIKit.config.messageReactionPerUserLimit
        val reactions = message.messageReactions
            ?.sortedByDescending { it.reaction.containsSelf }
            ?.map { it.reaction.key }
            ?.toMutableList() ?: mutableListOf()

        if (reactions.size < maxSize) {
            reactions.addAll(SceytChatUIKit.config.defaultReactions
                .minus(reactions.toSet())
                .take(maxSize - reactions.size))
        }

        return ReactionsPopup.showPopup(view, message, reactions.take(maxSize), style.reactionPickerStyle,
            object : PopupReactionsAdapter.OnItemClickListener {
                override fun onReactionClick(reaction: ReactionItem.Reaction) {
                    this@MessagesListView.onAddOrRemoveReaction(reaction, message)
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

    private fun showReactionActionsPopup(view: View, reaction: ReactionItem.Reaction, message: SceytMessage) {
        val popup = PopupMenu(ContextThemeWrapper(context, R.style.SceytPopupMenuStyle), view)
        popup.inflate(R.menu.sceyt_menu_popup_reacton)
        val containsSelf = reaction.reaction.containsSelf
        popup.menu.findItem(R.id.sceyt_add).isVisible = !containsSelf
        popup.menu.findItem(R.id.sceyt_remove).isVisible = containsSelf

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sceyt_add -> reactionClickListeners.onAddReaction(message, reaction.reaction.key)
                R.id.sceyt_remove -> reactionClickListeners.onRemoveReaction(message, reaction)
            }
            false
        }
        popup.show()
    }

    private fun showMessageActionsPopup(view: View, message: SceytMessage) {
        val popup = MessageActionsPopupMenu(ContextThemeWrapper(context, R.style.SceytPopupMenuStyle), view, message)
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

    private fun onAddOrRemoveReaction(reaction: ReactionItem.Reaction, message: SceytMessage) {
        val containsSelf = reaction.reaction.containsSelf
        if (containsSelf)
            reactionClickListeners.onRemoveReaction(message, reaction)
        else
            reactionClickListeners.onAddReaction(message, reaction.reaction.key)
    }

    private fun onAttachmentLoaderClick(item: FileListItem, message: SceytMessage) {
        messageCommandEventListener?.invoke(MessageCommandEvent.AttachmentLoaderClick(message, item))
    }

    private fun showAddEmojiDialog(message: SceytMessage) {
        context.getFragmentManager()?.let {
            EmojiPickerBottomSheetFragment().also { fragment ->
                fragment.setEmojiListener { emoji ->
                    val containsSelf = message.userReactions?.find { reaction -> reaction.key == emoji } != null
                    onAddOrRemoveReaction(ReactionItem.Reaction(SceytReactionTotal(emoji,
                        containsSelf = containsSelf), message.tid, true), message)
                }
            }.show(it, null)
        }
    }

    private fun updateItem(index: Int, item: MessageListItem, diff: MessageDiff) {
        val message = (item as? MessageItem)?.message ?: return
        (messagesRV.findViewHolderForItemId(item.getItemId()) as? BaseMessageViewHolder)?.let {
            SceytLog.i("StatusIssueTag", "updateItem: found by itemId: ${item.getItemId()}, " +
                    "msgId-> ${message.id}, diff ${diff.statusChanged}, status ${message.deliveryStatus}")
            it.bind(item, diff)
        } ?: run {
            SceytLog.i("StatusIssueTag", "updateItem: notifyItemChanged by index $index, " +
                    "diff ${diff.statusChanged}, msgId-> ${message.id}, status ${message.deliveryStatus}")
            messagesRV.adapter?.notifyItemChanged(index, diff)
        }
    }

    private fun notifyItemUpdatedToVisibleItems(item: MessageListItem) {
        (messagesRV.findViewHolderForItemId(item.getItemId()) as? BaseMessageViewHolder)?.itemUpdated(item)
    }

    internal fun setMessagesList(data: List<MessageListItem>, force: Boolean = false) {
        messagesRV.setData(data, force)
        if (data.isNotEmpty())
            binding.pageStateView.updateState(PageState.Nothing)
    }

    internal fun addNextPageMessages(data: List<MessageListItem>) {
        messagesRV.addNextPageMessages(data)
    }

    internal fun addPrevPageMessages(data: List<MessageListItem>) {
        messagesRV.addPrevPageMessages(data)
    }

    internal fun addNewMessages(vararg data: MessageListItem, addedCallback: () -> Unit = {}) {
        if (data.isEmpty()) return
        messagesRV.awaitAnimationEnd {
            messagesRV.addNewMessages(*data)
            addedCallback.invoke()
        }
    }

    internal fun updateMessage(message: SceytMessage): Boolean {
        var foundToUpdate = false
        SceytLog.i(TAG, "Message updated: id ${message.id}, tid ${message.tid}," +
                " body ${message.body}, deliveryStatus ${message.deliveryStatus}")
        val data = messagesRV.getData()
        for ((index, item) in data.withIndex()) {
            if (item is MessageItem && item.message.tid == message.tid) {
                val updatedItem = item.copy(message = item.message.getUpdateMessage(message))
                val diff = item.message.diff(updatedItem.message)
                updateAdapterItem(index, updatedItem, diff)
                SceytLog.i(TAG, "Found to update: id ${item.message.id}, tid ${item.message.tid}," +
                        " diff ${diff.statusChanged}, newStatus ${message.deliveryStatus}, index $index, size ${messagesRV.getData().size}")
                foundToUpdate = true
                break
            }
        }
        return foundToUpdate
    }

    internal fun updateMessageSelection(message: SceytMessage) {
        val data = messagesRV.getData()
        for ((index, item) in data.withIndex()) {
            if (item is MessageItem && item.message.tid == message.tid) {
                val updatedItem = item.copy(message = item.message.copy(isSelected = message.isSelected))
                updateAdapterItem(index, updatedItem, MessageDiff.DEFAULT_FALSE.copy(selectionChanged = true))
                break
            }
        }
    }

    internal fun getMessageById(messageId: Long): MessageItem? {
        return messagesRV.getData().find { it is MessageItem && it.message.id == messageId }?.let {
            (it as MessageItem)
        }
    }

    internal fun getMessageIndexedById(messageId: Long): Pair<Int, MessageListItem>? {
        return messagesRV.getData().findIndexed { it is MessageItem && it.message.id == messageId }
    }

    internal fun sortMessages() {
        messagesRV.sortMessages()
    }

    internal fun messageEditedOrDeleted(updateMessage: SceytMessage) {
        if (updateMessage.deliveryStatus == DeliveryStatus.Pending && updateMessage.state == MessageState.Deleted) {
            messagesRV.deleteMessageByTid(updateMessage.tid)
            if (messagesRV.isEmpty())
                binding.pageStateView.updateState(PageState.StateEmpty())
            return
        }
        val data = messagesRV.getData()
        messagesRV.getData().findIndexed { it is MessageItem && it.message.id == updateMessage.id }?.let { (index, item) ->
            val message = (item as MessageItem).message
            val updatedItem = item.copy(message = message.getUpdateMessage(updateMessage))
            messagesRV.updateItemAt(index, updatedItem)

            if (updateMessage.state == MessageState.Deleted && message.state != MessageState.Deleted)
                messagesRV.adapter?.notifyItemChanged(index)
            else
                updateItem(index, updatedItem, message.diff(updatedItem.message))
        }

        // Check reply message to update
        data.filter { it is MessageItem && it.message.parentMessage?.id == updateMessage.id }.forEach { item ->
            data.findIndexed { it is MessageItem && it.message.id == (item as MessageItem).message.id }?.let { (index, msg) ->
                val oldMessage = (msg as MessageItem).message
                val updatedItem = msg.copy(message = oldMessage.copy(parentMessage = updateMessage))
                updateAdapterItem(index, updatedItem, oldMessage.diff(updatedItem.message))
            }
        }
    }

    internal fun forceDeleteMessageByTid(tid: Long) {
        messagesRV.deleteMessageByTid(tid)
        if (messagesRV.isEmpty())
            binding.pageStateView.updateState(PageState.StateEmpty())
    }

    internal fun updateReaction(sceytMessage: SceytMessage) {
        messagesRV.getData().findIndexed { it is MessageItem && it.message.id == sceytMessage.id }?.let { (index, item) ->
            val message = (item as MessageItem).message
            val updatedItem = item.copy(message = message.copy(
                reactionTotals = sceytMessage.reactionTotals,
                userReactions = sceytMessage.userReactions,
                messageReactions = sceytMessage.messageReactions
            ))
            updateAdapterItem(index, updatedItem, message.diff(updatedItem.message))
        }
    }

    internal fun updateViewState(state: PageState, enableErrorSnackBar: Boolean = true) {
        binding.pageStateView.updateState(state, messagesRV.isEmpty(), enableErrorSnackBar = enableErrorSnackBar)
    }

    internal suspend fun updateProgress(data: TransferData, updateRecycler: Boolean) {
        val messages = ArrayList(messagesRV.getData())
        messages.findIndexed { item -> item is MessageItem && item.message.tid == data.messageTid }?.let { (index, item) ->
            val message = (item as? MessageItem)?.message ?: return
            val attachments = message.attachments?.toMutableList() ?: return

            val predicate: (SceytAttachment) -> Boolean = when (data.state) {
                Uploading, PendingUpload, PauseUpload, Uploaded, Preparing, WaitingToUpload -> { attachment ->
                    attachment.messageTid == data.messageTid
                }

                else -> { attachment ->
                    attachment.url == data.url
                }
            }
            val foundAttachmentFile = item.message.files?.find { listItem ->
                predicate(listItem.attachment)
            }

            if (data.state == ThumbLoaded) {
                if (data.thumbData?.key == ThumbFor.MessagesLisView.value) {
                    foundAttachmentFile?.updateThumbPath(data.filePath)
                }
                return
            } else {
                for ((attachmentIndex, sceytAttachment) in attachments.withIndex()) {
                    if (predicate(sceytAttachment)) {
                        val attachmentWithTransfer = sceytAttachment.getUpdatedWithTransferData(data)
                        val updatedAttachment = foundAttachmentFile?.updateAttachment(attachmentWithTransfer)
                        attachments[attachmentIndex] = updatedAttachment ?: attachmentWithTransfer
                        val updatedItem = item.copy(message = message.copy(attachments = attachments))
                        updateAdapterItemNotifyVisible(index, updatedItem)
                        break
                    }
                }
            }

            if (updateRecycler)
                withContext(Dispatchers.Main) {
                    updateItem(index, item, MessageDiff.DEFAULT_FALSE.copy(filesChanged = true))
                }
        }

        // Update reply message
        if (data.state == TransferState.Downloaded) {
            messages.forEachIndexed { index, item ->
                if (item is MessageItem && item.message.parentMessage?.tid == data.messageTid) {
                    val message = item.message
                    val updatedItem = item.copy(message = message.copy(parentMessage = message.parentMessage?.copy(
                        attachments = item.message.parentMessage.attachments?.map { attachment ->
                            if (attachment.url == data.url) {
                                attachment.copy(filePath = data.filePath)
                            } else attachment
                        }
                    )))

                    withContext(Dispatchers.Main) {
                        updateAdapterItem(index, updatedItem, MessageDiff.DEFAULT_FALSE.copy(replyContainerChanged = true))
                    }
                }
            }
        }
    }

    internal suspend fun updateLinkPreview(linkPreviewDetails: LinkPreviewDetails) {
        val messages = ArrayList(messagesRV.getData())
        messages.forEachIndexed { itemIndex, item ->
            val message = (item as? MessageItem)?.message ?: return@forEachIndexed
            val attachments = message.attachments?.toMutableList() ?: return@forEachIndexed
            val predicate: (SceytAttachment) -> Boolean = { attachment ->
                attachment.isLink() && attachment.url == linkPreviewDetails.link
            }

            attachments.findIndexed(predicate)?.let { (index, foundAttachmentFile) ->
                val updatedAttachment = foundAttachmentFile.copy(linkPreviewDetails = linkPreviewDetails)
                attachments[index] = updatedAttachment
                val updatedItem = item.copy(message = message.copy(attachments = attachments))
                withContext(Dispatchers.Main) {
                    updateAdapterItem(itemIndex, updatedItem, MessageDiff.DEFAULT_FALSE.copy(filesChanged = true))
                }
            }
        }
    }

    internal fun messageSendFailed(tid: Long) {
        messagesRV.getData().findIndexed { it is MessageItem && it.message.tid == tid }?.let { (index, item) ->
            val message = (item as MessageItem).message
            val updatedItem = item.copy(message = item.message.copy(deliveryStatus = DeliveryStatus.Pending))
            updateAdapterItem(index, updatedItem, message.diff(updatedItem.message))
        }
    }

    internal fun updateReplyCount(replyMessage: SceytMessage?) {
        messagesRV.getData().findIndexed {
            it is MessageItem && it.message.id == replyMessage?.parentMessage?.id
        }?.let { (index, item) ->
            val message = (item as MessageItem).message
            val updatedItem = item.copy(message = item.message.copy(replyCount = message.replyCount + 1))
            updateAdapterItem(index, updatedItem, message.diff(updatedItem.message))
        }
    }

    internal fun setMessageDisplayedListener(listener: (MessageListItem) -> Unit) {
        messagesRV.setMessageDisplayedListener(listener)
    }

    internal fun setVoicePlayPauseListener(listener: (FileListItem, SceytMessage, playing: Boolean) -> Unit) {
        messagesRV.setVoicePlayPauseListener(listener)
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
        messagesRV.setreachToStartListener(listener)
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

    internal fun setUnreadCount(unreadCount: Long) {
        binding.scrollDownView.setUnreadCount(unreadCount)
    }

    internal fun setOnWindowFocusChangeListener(listener: (Boolean) -> Unit) {
        onWindowFocusChangeListener = listener
    }

    internal fun getMessageCommandEventListener() = messageCommandEventListener

    internal fun updateItemAt(index: Int, item: MessageItem) {
        updateAdapterItemNotifyVisible(index, item)
    }

    private fun updateAdapterItem(index: Int, item: MessageItem, diff: MessageDiff, notify: Boolean = true) {
        messagesRV.updateItemAt(index, item)
        if (notify)
            updateItem(index, item, diff)
    }

    private fun updateAdapterItemNotifyVisible(index: Int, item: MessageItem) {
        messagesRV.updateItemAt(index, item)
        notifyItemUpdatedToVisibleItems(item)
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
            it.setStyle(style)
        })
    }

    fun setNeedDownloadListener(callBack: (NeedMediaInfoData) -> Unit) {
        messagesRV.getViewHolderFactory().setNeedMediaDataCallback(callBack)
    }

    fun scrollToMessage(msgId: Long, highlight: Boolean, offset: Int = 0, awaitToScroll: ((Boolean) -> Unit)? = null) {
        safeScrollTo {
            messagesRV.getData().findIndexed { it is MessageItem && it.message.id == msgId }?.let {
                val (position, item) = it
                item.highligh = highlight
                (messagesRV.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, offset)

                if (highlight || awaitToScroll != null) {
                    messagesRV.awaitToScrollFinish(position, callback = {
                        if (highlight)
                            (messagesRV.findViewHolderForAdapterPosition(position) as? BaseMessageViewHolder)?.highlight()
                        awaitToScroll?.invoke(true)
                    })
                }
            } ?: run { awaitToScroll?.invoke(false) }
        }
    }

    fun scrollToPosition(position: Int, highlight: Boolean, offset: Int = 0, awaitToScroll: ((Boolean) -> Unit)? = null) {
        safeScrollTo {
            (messagesRV.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, offset)

            if (highlight || awaitToScroll != null) {
                messagesRV.awaitToScrollFinish(position, callback = {
                    if (highlight)
                        (messagesRV.findViewHolderForAdapterPosition(position) as? BaseMessageViewHolder)?.highlight()
                    awaitToScroll?.invoke(true)
                })
            }
        }
    }

    fun scrollToUnReadMessage() {
        safeScrollTo {
            messagesRV.getData().findIndexed { it is MessageListItem.UnreadMessagesSeparatorItem }?.let {
                (messagesRV.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(it.first, 0)
            }
        }
    }

    fun scrollToLastMessage() {
        safeScrollTo {
            messagesRV.scrollToPosition(messagesRV.getData().size - 1)
        }
    }

    private fun safeScrollTo(scrollTo: () -> Unit) {
        MessagesAdapter.awaitUpdating {
            try {
                scrollTo()
            } catch (e: Exception) {
                messagesRV.awaitAnimationEnd { scrollTo() }
            }
        }
    }

    fun cancelMultiSelectMode() {
        (messagesRV.getMessagesAdapter())?.setMultiSelectableMode(false)
        messagesRV.enableDisableSwipeToReply(enabledActions)
        for (i in 0 until messagesRV.childCount) {
            messagesRV.getChildAt(i)?.let {
                val holder = messagesRV.getChildViewHolder(it)
                (holder as? BaseMessageViewHolder)?.cancelSelectableState()
            }
        }
        messagesRV.getData().forEachIndexed { index, item ->
            if (item is MessageItem) {
                if (item.message.isSelected) {
                    val updated = item.copy(message = item.message.copy(isSelected = false))
                    updateAdapterItemNotifyVisible(index, updated)
                }
            }
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

    fun getPageStateView() = binding.pageStateView

    fun isLastCompletelyItemDisplaying() = messagesRV.isLastCompletelyItemDisplaying()

    // Click listeners
    fun setMessageClickListener(listener: MessageClickListeners) {
        clickListeners.setListener(listener)
    }

    fun setMessageActionsClickListener(listener: MessageActionsViewClickListeners) {
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

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        messagesRV.setPadding(left, top, right, bottom)
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

    override fun onReactionClick(view: View, item: ReactionItem.Reaction, message: SceytMessage) {
        context.getFragmentManager()?.let {
            ReactionsInfoBottomSheetFragment.newInstance(message).also { fragment ->
                fragment.setClickListener { reaction ->
                    if (reaction.user?.id == SceytChatUIKit.chatUIFacade.myId)
                        reactionClickListeners.onRemoveReaction(message,
                            ReactionItem.Reaction(SceytReactionTotal(reaction.key, containsSelf = true), message.tid, reaction.pending))
                }
            }.show(it, null)
        }
    }

    override fun onReactionLongClick(view: View, item: ReactionItem.Reaction, message: SceytMessage) {
        if (enabledActions)
            showReactionActionsPopup(view, item, message)
    }

    override fun onAttachmentClick(view: View, item: FileListItem, message: SceytMessage) {
        when (item.type) {
            AttachmentTypeEnum.Image -> {
                MediaPreviewActivity.launch(context, item.attachment, message.user, message.channelId)
            }

            AttachmentTypeEnum.Video -> {
                MediaPreviewActivity.launch(context, item.attachment, message.user, message.channelId)
            }

            else -> item.attachment.openFile(context)
        }
    }

    override fun onAttachmentLongClick(view: View, item: FileListItem, message: SceytMessage) {
        clickListeners.onMessageLongClick(view, MessageItem(message))
    }

    override fun onMentionClick(view: View, userId: String) {
        messageCommandEventListener?.invoke(MessageCommandEvent.UserClick(view, userId))
    }

    override fun onAttachmentLoaderClick(view: View, item: FileListItem, message: SceytMessage) {
        onAttachmentLoaderClick(item, message)
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

    override fun onMessageInfoClick(message: SceytMessage) {

    }

    override fun onForwardMessageClick(vararg messages: SceytMessage) {
        ForwardActivity.launch(context, *messages)
    }

    override fun onReactMessageClick(message: SceytMessage) {
        onAddReactionClick(this, message)
    }

    override fun onReplyMessageClick(message: SceytMessage) {
        messageCommandEventListener?.invoke(MessageCommandEvent.Reply(message))
    }

    override fun onReplyMessageInThreadClick(message: SceytMessage) {
        messageCommandEventListener?.invoke(MessageCommandEvent.ReplyInThread(message))
    }


    //Reaction popup events
    override fun onAddReaction(message: SceytMessage, key: String) {
        addReaction(message, key)
    }

    override fun onRemoveReaction(message: SceytMessage, reactionItem: ReactionItem.Reaction) {
        reactionEventListener?.invoke(ReactionEvent.RemoveReaction(message, reactionItem.reaction.key))
    }
}