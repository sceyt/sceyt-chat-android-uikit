package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytPoll
import com.sceyt.chatuikit.databinding.SceytItemIncPollMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PollOptionAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BaseMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.helpers.PollVoteHelper
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class IncPollMessageViewHolder(
        private val binding: SceytItemIncPollMessageBinding,
        private val viewPool: RecyclerView.RecycledViewPool,
        style: MessageItemStyle,
        private val messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)?,
) : BaseMessageViewHolder(binding.root, style, messageListeners, displayedListener) {

    private val gson = Gson()
    private var pollOptionAdapter: PollOptionAdapter? = null
    private var currentPoll: SceytPoll? = null

    init {
        with(binding) {
            setMessageItemStyle()

            root.setOnClickListener {
                messageListeners?.onMessageClick(it, messageListItem as MessageListItem.MessageItem)
            }

            root.setOnLongClickListener {
                messageListeners?.onMessageLongClick(it, messageListItem as MessageListItem.MessageItem)
                return@setOnLongClickListener true
            }

            tvViewResults.setOnClickListener {
                onViewResultsClick()
            }
        }
    }

    override fun bind(item: MessageListItem, diff: MessageDiff) {
        super.bind(item, diff)
        if (!diff.hasDifference()) return

        if (item is MessageListItem.MessageItem) {
            with(binding) {
                val message = item.message
                tvForwarded.isVisible = message.isForwarded

                if (diff.edited || diff.statusChanged)
                    setMessageStatusAndDateText(message, messageDate)

                if (diff.pollChanged) {
                    val poll = parsePoll(message.metadata)
                    if (poll != null) {
                        setupPollViews(poll)
                    }
                }

                if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, viewReply)

                if (diff.reactionsChanged || diff.edited)
                    setOrUpdateReactions(item, rvReactions, viewPool)

                if (item.message.shouldShowAvatarAndName)
                    avatar.setOnClickListener {
                        messageListeners?.onAvatarClick(it, item)
                    }
            }
        }
    }

    private fun parsePoll(metadata: String?): SceytPoll? {
        return try {
            metadata?.let { gson.fromJson(it, SceytPoll::class.java) }
        } catch (e: Exception) {
            null
        }
    }

    private fun setupPollViews(poll: SceytPoll) {
        val shouldAnimate = pollOptionAdapter != null && currentPoll?.id == poll.id
        currentPoll = poll
        with(binding) {
            tvPollQuestion.text = poll.question
            tvPollType.text = if (poll.anonymous) {
                root.context.getString(R.string.sceyt_anonymous_poll)
            } else {
                root.context.getString(R.string.sceyt_public_poll)
            }

            if (pollOptionAdapter == null) {
                pollOptionAdapter = PollOptionAdapter(poll = poll) { option ->
                    onPollOptionClick(option)
                }
                rvPollOptions.adapter = pollOptionAdapter
            }
            pollOptionAdapter?.submitListWithAnimation(poll.options, poll.totalVotes, animate = shouldAnimate)
        }
    }

    private fun onPollOptionClick(option: PollOption) {
        val messageItem = messageListItem as? MessageListItem.MessageItem ?: return
        val message = messageItem.message as? SceytMessage ?: return
        val poll = currentPoll ?: return

        // Get current user
        val currentUser = PollVoteHelper.getCurrentUser()

        // Toggle vote and get updated poll
        val updatedPoll = PollVoteHelper.toggleVote(message, option, currentUser) ?: return

        // Update UI with animation
        setupPollViews(updatedPoll)

        // Notify listener for backend sync
        messageListeners?.onPollOptionClick(
            binding.root,
            messageItem,
            option
        )
    }

    private fun onViewResultsClick() {
        messageListeners?.onPollViewResultsClick(
            binding.root,
            messageListItem as MessageListItem.MessageItem
        )
    }

    override val selectMessageView get() = binding.selectView

    override val layoutBubbleConfig get() = Pair(binding.layoutDetails, true)

    override val incoming: Boolean
        get() = true

    private fun SceytItemIncPollMessageBinding.setMessageItemStyle() {
        applyCommonStyle(
            layoutDetails = layoutDetails,
            tvForwarded = tvForwarded,
            messageBody = null,
            tvThreadReplyCount = tvReplyCount,
            toReplyLine = toReplyLine,
            tvSenderName = tvUserName,
            avatarView = avatar
        )
    }
}

