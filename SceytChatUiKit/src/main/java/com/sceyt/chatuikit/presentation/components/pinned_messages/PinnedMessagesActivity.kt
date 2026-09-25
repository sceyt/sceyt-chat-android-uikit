package com.sceyt.chatuikit.presentation.components.pinned_messages

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.DeleteMessageType
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReactionTotal
import com.sceyt.chatuikit.databinding.SceytActivityPinnedMessagesBinding
import com.sceyt.chatuikit.extensions.applyInsetsAndWindowColor
import com.sceyt.chatuikit.extensions.createIntent
import com.sceyt.chatuikit.extensions.openLink
import com.sceyt.chatuikit.extensions.parcelable
import com.sceyt.chatuikit.extensions.setClipboard
import com.sceyt.chatuikit.extensions.updateWithScrollCompensation
import com.sceyt.chatuikit.navigation.Destination
import com.sceyt.chatuikit.navigation.MediaPreviewParams
import com.sceyt.chatuikit.navigation.navigate
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.common.dialogs.SceytDialog
import com.sceyt.chatuikit.presentation.components.channel.header.MessageActionsMenuInitializer
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.FileListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.files.openFile
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem.MessageItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.reactions.ReactionItem
import com.sceyt.chatuikit.presentation.components.channel.messages.components.EmojiPickerBottomSheetFragment
import com.sceyt.chatuikit.presentation.components.channel.messages.dialogs.DeleteMessageDialog
import com.sceyt.chatuikit.presentation.components.channel.messages.fragments.ReactionsInfoBottomSheetFragment
import com.sceyt.chatuikit.presentation.components.channel.messages.helpers.MessageCopyHelper
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.presentation.components.channel.messages.popups.PopupReactionsAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.popups.ReactionsPopup
import com.sceyt.chatuikit.presentation.components.pinned_messages.adapter.PinnedMessagesAdapter
import com.sceyt.chatuikit.presentation.extensions.isPending
import com.sceyt.chatuikit.presentation.root.PageState
import com.sceyt.chatuikit.styles.StyleRegistry
import com.sceyt.chatuikit.styles.messages_list.MessagesListViewStyle
import com.sceyt.chatuikit.styles.pinned_messages.PinnedMessagesStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

open class PinnedMessagesActivity : AppCompatActivity() {

    protected open lateinit var binding: SceytActivityPinnedMessagesBinding
    protected open lateinit var style: PinnedMessagesStyle
    private lateinit var channel: SceytChannel

    private val viewModel: PinnedMessagesViewModel by viewModels {
        PinnedMessagesViewModelFactory(channel)
    }

    private val messagesListStyle: MessagesListViewStyle by lazy {
        StyleRegistry.getOrDefault(intent?.getStringExtra(LIST_STYLE_ID)) {
            MessagesListViewStyle.Builder(this, null).build()
        }
    }

    private val viewHolderFactory by lazy { createViewHolderFactory() }

    private val adapter by lazy {
        PinnedMessagesAdapter(
            style = style,
            messagesListStyle = messagesListStyle,
            viewHolderFactory = viewHolderFactory,
            onNavigateClick = ::finishWithJump,
            onSelectClick = ::toggleSelection,
        )
    }

    private var reactionsPopup: ReactionsPopup? = null

    protected open val messageClickListeners = object : MessageClickListeners.ClickListeners {

        override fun onMessageClick(view: View, item: MessageItem) {
            if (viewModel.selectedMessages.isNotEmpty()) {
                toggleSelection(item.message)
                return
            }
            if (reactionsPopup == null) showReactionsPopup(view, item.message)
        }

        override fun onMessageLongClick(view: View, item: MessageItem) {
            toggleSelection(item.message)
        }

        override fun onMultiSelectClick(view: View, message: SceytMessage) {
            toggleSelection(message)
        }

        override fun onAttachmentLongClick(view: View, item: FileListItem, message: SceytMessage) {
            toggleSelection(message)
        }

        override fun onAddReactionClick(view: View, message: SceytMessage) {
            showAddEmojiDialog(message)
        }

        override fun onReactionClick(
            view: View,
            item: ReactionItem.Reaction,
            message: SceytMessage,
        ) {
            ReactionsInfoBottomSheetFragment.newInstance(message).also { fragment ->
                fragment.setClickListener { reaction ->
                    if (reaction.user?.id != SceytChatUIKit.currentUserId) return@setClickListener
                    viewModel.addOrRemoveReaction(
                        message = message,
                        reaction = ReactionItem.Reaction(
                            reaction = SceytReactionTotal(
                                key = reaction.key, containsSelf = true
                            ),
                            messageTid = message.tid,
                            isPending = reaction.pending
                        )
                    )
                }
            }.show(supportFragmentManager, null)
        }

        override fun onAttachmentClick(view: View, item: FileListItem, message: SceytMessage) {
            when (item.type) {
                AttachmentTypeEnum.Image, AttachmentTypeEnum.Video -> {
                    SceytChatUIKit.navigator.navigate(
                        context = this@PinnedMessagesActivity,
                        destination = Destination.MediaPreview(
                            MediaPreviewParams.SingleAttachment(
                                attachment = item.attachment,
                                from = message.user,
                                channelId = message.channelId,
                            )
                        )
                    )
                }

                else -> item.attachment.openFile(this@PinnedMessagesActivity)
            }
        }

        override fun onAttachmentLoaderClick(
            view: View,
            item: FileListItem,
            message: SceytMessage,
        ) {
            viewModel.pauseOrResumeUpload(item)
        }

        override fun onLinkClick(view: View, item: MessageItem) {
            item.message.attachments?.firstOrNull()?.let { openLink(it.url) }
        }

        override fun onLinkDetailsClick(view: View, item: MessageItem) {
            item.message.attachments?.firstOrNull()?.let { openLink(it.url) }
        }

        override fun onReplyMessageContainerClick(view: View, item: MessageItem) {
            item.message.parentMessage?.let(::finishWithJump)
        }

        override fun onPinnedSystemMessageClick(view: View, item: MessageItem) = Unit

        override fun onPollOptionClick(view: View, item: MessageItem, option: PollOption) {
            viewModel.toggleVote(item.message, option)
        }

        override fun onPollViewResultsClick(view: View, item: MessageItem) {
            openPollResults(item.message)
        }

        override fun onPollVotersClick(view: View, item: MessageItem, option: PollOption) {
            openPollResults(item.message)
        }

        override fun onAvatarClick(view: View, item: MessageItem) {
            openUser(item.message.user?.id)
        }

        override fun onMentionClick(view: View, userId: String) {
            openUser(userId)
        }
        override fun onReplyCountClick(view: View, item: MessageItem) = Unit
        override fun onReadMoreClick(view: View, item: MessageItem) {
            val rv = binding.rvPinnedMessages
            val holder = rv.findViewHolderForItemId(item.getItemId())
                    as? PinnedMessagesAdapter.ViewHolder ?: return
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return

            viewModel.expandBody(item.message.tid)

            val expandedItem = item.copy(message = item.message.copy(isBodyExpanded = true))
            val oldTop = holder.itemView.top

            rv.updateWithScrollCompensation(
                oldTop = oldTop,
                getNewTop = { holder.itemView.top },
                onUpdate = {
                    adapter.replaceMessageItem(expandedItem, position)
                    holder.bind(expandedItem, MessageDiff.DEFAULT_FALSE.copy(bodyChanged = true))
                }
            )
        }

        override fun onScrollToDownClick(view: View) = Unit
        override fun onScrollToUnreadMentionClick(view: View) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getDataFromIntent()
        style = PinnedMessagesStyle.Builder(this, null).build()

        enableEdgeToEdge()
        binding = SceytActivityPinnedMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsAndWindowColor(binding.root)

        initViews()
        applyStyle()
        observeState()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.selectedMessages.isNotEmpty()) clearSelection() else finish()
            }
        })
    }

    protected open fun getDataFromIntent() {
        channel = requireNotNull(intent?.parcelable(CHANNEL))
    }

    protected open fun initViews() = with(binding) {
        toolbar.setNavigationClickListener {
            if (viewModel.selectedMessages.isNotEmpty()) clearSelection() else finish()
        }
        rvPinnedMessages.layoutManager = LinearLayoutManager(this@PinnedMessagesActivity).apply {
            stackFromEnd = true
        }
        rvPinnedMessages.adapter = adapter
    }

    protected open fun createViewHolderFactory(): MessageViewHolderFactory {
        return MessageViewHolderFactory(this).also { applyFactoryDefaults(it) }
    }

    protected fun applyFactoryDefaults(factory: MessageViewHolderFactory) = factory.apply {
        setStyle(messagesListStyle)
        setMessageListener(messageClickListeners)
        setNeedMediaDataCallback(viewModel::needMediaInfo)
    }

    protected open fun observeState() {
        viewModel.pageStateLiveData.observe(this) { state ->
            if (state is PageState.StateError && state.showMessage)
                Toast.makeText(this, state.errorMessage, Toast.LENGTH_SHORT).show()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pinnedMessages.collect { items ->
                    adapter.submit(items)
                    binding.emptyStateView.isVisible = items.isEmpty()
                    val tids = items.filterIsInstance<MessageItem>().map { it.message.tid }
                    if (viewModel.selectedMessages.keys.any { it !in tids }) clearSelection()
                }
            }
        }
    }

    protected open fun toggleSelection(message: SceytMessage) {
        val updated = viewModel.toggleSelection(message)
        if (updated == null) {
            val limit = SceytChatUIKit.config.messageMultiselectLimit
            Toast.makeText(
                this,
                getString(R.string.sceyt_reach_max_message_select_count, limit.toString()),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        adapter.updateItemSelection(updated)

        val selected = viewModel.selectedMessages.values
        if (selected.isEmpty()) {
            clearSelection()
        } else {
            adapter.setSelectableMode(true)
            showMessageActions(*selected.toTypedArray())
        }
    }

    protected open fun clearSelection() {
        viewModel.selectedMessages.clear()
        adapter.setSelectableMode(false)
        hideMessageActions()
    }


    protected open fun openUser(userId: String?) {
        viewModel.openUser(userId ?: return) { channel ->
            SceytChatUIKit.navigator.navigate(this, Destination.ChannelInfo(channel))
        }
    }

    private fun openPollResults(message: SceytMessage) {
        val poll = message.poll ?: return
        if (poll.anonymous || poll.maxVotedCountWithPendingVotes == 0) return
        SceytChatUIKit.navigator.navigate(this, Destination.PollResults(message))
    }

    protected open fun showReactionsPopup(view: View, message: SceytMessage) {
        if (message.isPending()) return
        reactionsPopup = ReactionsPopup.showPopup(
            anchorView = view,
            message = message,
            style = messagesListStyle.reactionPickerStyle,
            clickListener = object : PopupReactionsAdapter.OnItemClickListener {
                override fun onReactionClick(reaction: ReactionItem.Reaction) {
                    viewModel.addOrRemoveReaction(message, reaction)
                }

                override fun onAddClick() {
                    showAddEmojiDialog(message)
                }
            }
        ).also { popup ->
            popup.setOnDismissListener {
                lifecycleScope.launch {
                    delay(100.milliseconds)
                    reactionsPopup = null
                }
            }
        }
    }

    protected open fun showAddEmojiDialog(message: SceytMessage) {
        EmojiPickerBottomSheetFragment().also { fragment ->
            fragment.setEmojiListener { emoji ->
                val containsSelf = message.userReactions?.any { it.key == emoji } == true
                viewModel.addOrRemoveReaction(
                    message,
                    ReactionItem.Reaction(
                        SceytReactionTotal(key = emoji, containsSelf = containsSelf),
                        message.tid,
                        true
                    )
                )
            }
        }.show(supportFragmentManager, null)
    }

    protected open fun showMessageActions(vararg messages: SceytMessage) {
        if (messages.isEmpty()) return
        with(binding) {
            toolbarMessageActions.setToolbarIconsVisibilityInitializer { selected, menu ->
                MessageActionsMenuInitializer.init(this@PinnedMessagesActivity, menu, *selected)
                menu.findItem(R.id.sceyt_message_info)?.isVisible = false
            }
            toolbarMessageActions.setupMenuWithMessages(style.messageActionsMenuStyle, *messages)
            toolbarMessageActions.setMenuItemClickListener { item ->
                onMessageActionClick(item, *messages)
            }
            toolbarMessageActions.isVisible = true
            toolbar.setTitle("")
        }
    }

    protected open fun hideMessageActions() = with(binding) {
        if (!toolbarMessageActions.isVisible) return@with
        toolbarMessageActions.isVisible = false
        toolbar.setTitle(style.toolbarTitle)
    }

    protected open fun onMessageActionClick(item: MenuItem, vararg messages: SceytMessage) {
        onMessageAction(item.itemId, *messages)
    }

    protected open fun onMessageAction(actionId: Int, vararg messages: SceytMessage) {
        val message = messages.firstOrNull() ?: return
        val selected = messages.toList()
        clearSelection()

        when (actionId) {
            R.id.sceyt_reply -> finishWithAction(Action.Reply, message)
            R.id.sceyt_edit_message -> finishWithAction(Action.Edit, message)

            R.id.sceyt_forward -> SceytChatUIKit.navigator.navigate(
                this, Destination.Forward(selected)
            )

            R.id.sceyt_copy_message -> {
                setClipboard(MessageCopyHelper.buildCopyableText(this, *messages))
                Toast.makeText(this, R.string.sceyt_message_copied, Toast.LENGTH_SHORT).show()
            }

            R.id.sceyt_delete_message -> DeleteMessageDialog(this)
                .setDeleteMessagesCount(selected.size)
                .setRequireForMe(selected.any { it.incoming })
                .setAcceptCallback { forMe -> viewModel.delete(selected, deleteType(forMe)) }
                .show()

            R.id.sceyt_pin_message -> viewModel.unpin(message.tid)

            R.id.sceyt_retract_vote -> viewModel.retractVote(message)

            R.id.sceyt_end_vote -> SceytDialog.showDialog(
                context = this,
                titleId = R.string.sceyt_end_poll_dialog_title,
                descId = R.string.sceyt_end_poll_dialog_desc,
                positiveBtnTitleId = R.string.sceyt_end,
                negativeBtnTitleId = R.string.sceyt_not_now,
                positiveCb = { viewModel.endVote(message) }
            )
        }
    }

    private fun deleteType(forMe: Boolean) = when {
        forMe -> DeleteMessageType.DeleteForMe
        SceytChatUIKit.config.hardDeleteMessageForAll -> DeleteMessageType.DeleteHard
        else -> DeleteMessageType.DeleteForEveryone
    }

    protected open fun finishWithJump(message: SceytMessage) {
        if (message.id == 0L) return
        finishWithAction(Action.Jump, message)
    }

    protected open fun finishWithAction(action: Action, message: SceytMessage) {
        setResult(
            RESULT_OK,
            Intent()
                .putExtra(RESULT_ACTION, action.name)
                .putExtra(RESULT_MESSAGE, message)
                .putExtra(RESULT_MESSAGE_ID, message.id)
        )
        finish()
    }

    protected open fun applyStyle() = with(binding) {
        root.setBackgroundColor(style.backgroundColor)
        toolbar.setTitle(style.toolbarTitle)
        style.toolbarStyle.apply(toolbar)
        emptyStateView.text = style.emptyStateText
        style.emptyStateTextStyle.apply(emptyStateView)
    }

    override fun onDestroy() {
        reactionsPopup?.dismiss()
        StyleRegistry.unregister(intent?.getStringExtra(LIST_STYLE_ID))
        super.onDestroy()
    }

    enum class Action {
        Jump, Reply, Edit
    }

    companion object {
        const val RESULT_MESSAGE_ID = "resultMessageId"
        const val RESULT_MESSAGE = "resultMessage"
        const val RESULT_ACTION = "resultAction"
        private const val CHANNEL = "channel"
        private const val LIST_STYLE_ID = "listStyleId"

        fun createIntent(
            context: Context,
            channel: SceytChannel,
            listStyle: MessagesListViewStyle? = null,
        ) = context.createIntent<PinnedMessagesActivity>().apply {
            putExtra(CHANNEL, channel)
            listStyle?.let {
                StyleRegistry.register(it)
                putExtra(LIST_STYLE_ID, it.styleId)
            }
        }
    }
}
