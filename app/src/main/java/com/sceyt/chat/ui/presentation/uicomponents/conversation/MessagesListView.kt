package com.sceyt.chat.ui.presentation.uicomponents.conversation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.PopupMenu
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.extensions.*
import com.sceyt.chat.ui.presentation.root.PageState
import com.sceyt.chat.ui.presentation.root.PageStateView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.getFileFromMetadata
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
import com.sceyt.chat.ui.utils.BindingUtil
import java.io.File

class MessagesListView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : FrameLayout(context, attrs, defStyleAttr), MessageClickListeners.ClickListeners,
        MessagePopupClickListeners.PopupClickListeners, ReactionPopupClickListeners.PopupClickListeners {

    private var messagesRV: MessagesRV
    private var pageStateView: PageStateView? = null
    private lateinit var clickListeners: MessageClickListenersImpl
    private lateinit var messagePopupClickListeners: MessagePopupClickListenersImpl
    private lateinit var reactionPopupClickListeners: ReactionPopupClickListenersImpl
    private var reactionEventListener: ((ReactionEvent) -> Unit)? = null
    private var messageEventListener: ((MessageEvent) -> Unit)? = null
    private var enabledClickActions = true

    init {
        setBackgroundColor(context.getCompatColor(R.color.sceyt_color_bg))
        if (!isInEditMode)
            BindingUtil.themedBackgroundColor(this, R.color.sceyt_color_bg)

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
        reactionPopupClickListeners = ReactionPopupClickListenersImpl(this)

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

            override fun onAddReactionClick(view: View, item: MessageListItem.MessageItem) {
                if (enabledClickActions)
                    clickListeners.onAddReactionClick(view, item)
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
        })
    }

    private fun showReactionActionsPopup(view: View, reaction: ReactionItem.Reaction) {
        val popup = PopupMenu(ContextThemeWrapper(context, R.style.SceytPopupMenuStyle), view)
        popup.inflate(R.menu.sceyt_menu_popup_reacton)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sceyt_add -> reactionPopupClickListeners.onAddReaction(reaction.messageItem.message, reaction.reactionScore.key)
                R.id.sceyt_remove -> reactionPopupClickListeners.onRemoveReaction(reaction)
                R.id.sceyt_delete -> reactionPopupClickListeners.onDeleteReaction(reaction)
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

    private fun addReaction(message: SceytMessage, score: String) {
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

    private fun showAddEmojiDialog(message: SceytMessage) {
        context.getFragmentManager()?.let {
            BottomSheetEmojisFragment(emojiListener = { emoji ->
                onAddReaction(message, emoji.unicode)
            }).show(it, null)
        }
    }

    private fun openFile(item: FileListItem) {
        val fileName = item.file?.name
        var uri: Uri? = null
        if (fileName != null) {
            val loadedFile = File(context.filesDir, fileName)
            if (loadedFile.exists()) {
                uri = context.getFileUriWithProvider(loadedFile)
            } else {
                item.getFileFromMetadata()?.let {
                    uri = context.getFileUriWithProvider(it)
                }
            }
        }

        if (uri != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                    .setData(uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.no_proper_app_to_open_file), Toast.LENGTH_SHORT).show()
            }
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
        messagesRV.updateReaction(data.id, data.reactionScores ?: arrayOf())
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

    internal fun messageSendFailed(id: Long) {
        messagesRV.getData()?.findIndexed { it is MessageListItem.MessageItem && it.message.id == id }?.let {
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

    override fun onAddReactionClick(view: View, item: MessageListItem.MessageItem) {
        showAddEmojiDialog(item.message)
    }

    override fun onReactionLongClick(view: View, item: ReactionItem.Reaction) {
        showReactionActionsPopup(view, item)
    }

    override fun onAttachmentClick(view: View, item: FileListItem) {
        openFile(item)
    }

    override fun onAttachmentLongClick(view: View, item: FileListItem) {
        showMessageActionsPopup(view, item.sceytMessage)
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
        onAddReactionClick(view, MessageListItem.MessageItem(message))
    }

    override fun onReplayMessageClick(message: SceytMessage) {
        messageEventListener?.invoke(MessageEvent.Replay(message))
    }

    override fun onReplayInThreadMessageClick(message: SceytMessage) {
        messageEventListener?.invoke(MessageEvent.ReplayInThread(message, context))
    }


    //Reaction popup events
    override fun onAddReaction(message: SceytMessage, score: String) {
        addReaction(message, score)
    }

    override fun onRemoveReaction(reactionItem: ReactionItem.Reaction) {
        onReduceReaction(reactionItem)
    }

    override fun onDeleteReaction(reactionItem: ReactionItem.Reaction) {
        reactionEventListener?.invoke(ReactionEvent.DeleteReaction(reactionItem.messageItem.message, reactionItem.reactionScore))
    }
}