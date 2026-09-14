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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.sceyt.chatuikit.navigation.Destination
import com.sceyt.chatuikit.navigation.MediaPreviewParams
import com.sceyt.chatuikit.navigation.navigate
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
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                PinnedMessagesViewModel(channel) as T
        }
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
            viewHolderFactory = viewHolderFactory,
            onNavigateClick = ::finishWithJump,
        )
    }

    private var selectedMessage: SceytMessage? = null
    private var reactionsPopup: ReactionsPopup? = null

    protected open val messageClickListeners = object : MessageClickListeners.ClickListeners {

        override fun onMessageClick(view: View, item: MessageItem) {
            if (reactionsPopup == null) showReactionsPopup(view, item.message)
        }

        override fun onMessageLongClick(view: View, item: MessageItem) {
            showMessageActions(view, item.message)
        }

        override fun onAttachmentLongClick(view: View, item: FileListItem, message: SceytMessage) {
            showMessageActions(view, message)
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
        override fun onMultiSelectClick(view: View, message: SceytMessage) = Unit
        override fun onReadMoreClick(view: View, item: MessageItem) {
            viewModel.expandBody(item.message.tid)
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
                if (binding.toolbarMessageActions.isVisible) hideMessageActions() else finish()
            }
        })
    }

    protected open fun getDataFromIntent() {
        channel = requireNotNull(intent?.parcelable(CHANNEL))
    }

    protected open fun initViews() = with(binding) {
        toolbar.setNavigationClickListener {
            if (toolbarMessageActions.isVisible) hideMessageActions() else finish()
        }
        rvPinnedMessages.layoutManager = LinearLayoutManager(this@PinnedMessagesActivity).apply {
            stackFromEnd = true
        }
        rvPinnedMessages.adapter = adapter
    }

    /** Override for custom message types; use [applyFactoryDefaults] to retain listeners and style. */
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
                    adapter.submitList(items)
                    // The screen stays up when the last pin goes away rather than popping.
                    binding.emptyStateView.isVisible = items.isEmpty()
                    // Unpinning or deleting the selected message takes the row away with it.
                    val selectedTid = selectedMessage?.tid
                    if (items.none { it is MessageItem && it.message.tid == selectedTid })
                        hideMessageActions()
                }
            }
        }
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
                // Ignore the tap that dismissed the popup.
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

    /** Override to present a custom menu and dispatch selections through [onMessageAction]. */
    protected open fun showMessageActions(view: View, message: SceytMessage) {
        selectedMessage = message
        with(binding) {
            toolbarMessageActions.setToolbarIconsVisibilityInitializer { messages, menu ->
                MessageActionsMenuInitializer.init(this@PinnedMessagesActivity, menu, *messages)
                // Message info belongs to the conversation, where the message sits among the
                // others it was delivered with.
                menu.findItem(R.id.sceyt_message_info)?.isVisible = false
            }
            toolbarMessageActions.setupMenuWithMessages(style.messageActionsMenuStyle, message)
            toolbarMessageActions.setMenuItemClickListener { item ->
                onMessageActionClick(item, message)
            }
            toolbarMessageActions.isVisible = true
            // The toolbar stays for its back arrow; only its title gives way.
            toolbar.setTitle("")
        }
    }

    protected open fun hideMessageActions() = with(binding) {
        if (!toolbarMessageActions.isVisible) return@with
        selectedMessage = null
        toolbarMessageActions.isVisible = false
        toolbar.setTitle(style.toolbarTitle)
    }

    protected open fun onMessageActionClick(item: MenuItem, message: SceytMessage) {
        onMessageAction(item.itemId, message)
    }

    /** Handles actions from the default menu or a subclass's custom menu. */
    protected open fun onMessageAction(actionId: Int, message: SceytMessage) {
        when (actionId) {
            R.id.sceyt_reply -> finishWithAction(Action.Reply, message)
            R.id.sceyt_edit_message -> finishWithAction(Action.Edit, message)

            R.id.sceyt_forward -> {
                hideMessageActions()
                SceytChatUIKit.navigator.navigate(this, Destination.Forward(message))
            }

            R.id.sceyt_copy_message -> {
                hideMessageActions()
                setClipboard(MessageCopyHelper.buildCopyableText(this, message))
                Toast.makeText(this, R.string.sceyt_message_copied, Toast.LENGTH_SHORT).show()
            }

            R.id.sceyt_delete_message -> DeleteMessageDialog(this)
                .setDeleteMessagesCount(1)
                .setRequireForMe(message.incoming)
                .setAcceptCallback { forMe ->
                    hideMessageActions()
                    viewModel.delete(message, deleteType(forMe))
                }
                .show()

            R.id.sceyt_pin_message -> {
                // Everything on this screen is pinned, so this is always the unpin side of
                // the shared menu item.
                hideMessageActions()
                viewModel.unpin(message.tid)
            }

            R.id.sceyt_retract_vote -> {
                hideMessageActions()
                viewModel.retractVote(message)
            }

            R.id.sceyt_end_vote -> SceytDialog.showDialog(
                context = this,
                titleId = R.string.sceyt_end_poll_dialog_title,
                descId = R.string.sceyt_end_poll_dialog_desc,
                positiveBtnTitleId = R.string.sceyt_end,
                negativeBtnTitleId = R.string.sceyt_not_now,
                positiveCb = {
                    hideMessageActions()
                    viewModel.endVote(message)
                }
            )
        }
    }

    private fun deleteType(forMe: Boolean) = when {
        forMe -> DeleteMessageType.DeleteForMe
        SceytChatUIKit.config.hardDeleteMessageForAll -> DeleteMessageType.DeleteHard
        else -> DeleteMessageType.DeleteForEveryone
    }

    protected open fun finishWithJump(message: SceytMessage) {
        // A pin whose message has no server id yet cannot be jumped to.
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
