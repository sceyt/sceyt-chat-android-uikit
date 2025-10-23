package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.PollOptionUiModel
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PollOptionAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PollOptionViewHolderFactory
import com.sceyt.chatuikit.presentation.components.channel.messages.helpers.PollVoteHelper
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

abstract class BasePollMessageViewHolder(
        view: View,
        protected val style: MessageItemStyle,
        protected val messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)? = null,
) : BaseMessageViewHolder(view, style, messageListeners, displayedListener) {

    protected var pollOptionAdapter: PollOptionAdapter? = null
    protected var currentPoll: SceytPollDetails? = null
    protected val pollStyle = style.pollStyle
    private val pollOptionViewHolderFactory by lazy {
        PollOptionViewHolderFactory(
            context = context,
            pollStyle = pollStyle,
            isClosedProvider = { currentPoll?.closed ?: false },
            isAnonymousProvider = { currentPoll?.anonymous ?: false },
            totalVotesProvider = { currentPoll?.totalVotes ?: 0 },
            onOptionClick = { option -> onPollOptionClick(option) }
        )
    }

    protected fun setupPollViews(
            poll: SceytPollDetails,
            rvPollOptions: RecyclerView,
            tvPollQuestion: TextView,
            tvPollType: TextView,
            tvViewResults: TextView,
            divider: View,
    ) {
        val isSamePoll = currentPoll?.id == poll.id
        currentPoll = poll

        tvPollQuestion.text = poll.name
        tvPollType.text = pollStyle.pollTypeFormatter.format(context, poll)

        divider.isVisible = !poll.anonymous

        val totalVotes = poll.totalVotes
        with(tvViewResults) {
            isVisible = !poll.anonymous
            isEnabled = totalVotes > 0

            if (totalVotes > 0) {
                pollStyle.viewResultsTextStyle.apply(this)
            } else {
                pollStyle.viewResultsDisabledTextStyle.apply(this)
            }
        }

        setOptions(poll = poll, isSamePoll = isSamePoll, rvPollOptions = rvPollOptions)
    }

    protected open fun setOptions(
            poll: SceytPollDetails,
            isSamePoll: Boolean,
            rvPollOptions: RecyclerView,
    ) {
        val shouldAnimate = pollOptionAdapter != null && isSamePoll

        if (pollOptionAdapter == null || !isSamePoll) {
            pollOptionAdapter = PollOptionAdapter(viewHolderFactory = pollOptionViewHolderFactory)

            with(rvPollOptions) {
                itemAnimator = null
                adapter = pollOptionAdapter
            }
        }

        pollOptionAdapter?.submitData(
            poll = poll,
            animate = shouldAnimate
        )
    }

    protected open fun onPollOptionClick(option: PollOptionUiModel) {
        val messageItem = messageListItem as? MessageListItem.MessageItem ?: return
        val message = messageItem.message

        // Get current user
        val currentUser = PollVoteHelper.getCurrentUser()

        // Toggle vote and get updated poll
        val updatedPoll = PollVoteHelper.toggleVote(message, option, currentUser) ?: return

        // Update UI with animation
        updatePollViews(updatedPoll)

        // Notify listener for backend sync
        messageListeners?.onPollOptionClick(
            view = itemView,
            item = messageItem,
            option = PollOption(id = option.id, name = option.text)
        )
    }

    protected fun onViewResultsClick() {
        messageListeners?.onPollViewResultsClick(
            view = itemView,
            item = messageListItem as MessageListItem.MessageItem
        )
    }

    protected abstract fun updatePollViews(poll: SceytPollDetails)

    protected open fun applyStyle(
            tvPollQuestion: TextView,
            tvPollType: TextView,
            tvViewResults: TextView,
            divider: View,
    ) {
        pollStyle.questionTextStyle.apply(tvPollQuestion)
        pollStyle.pollTypeTextStyle.apply(tvPollType)
        pollStyle.viewResultsTextStyle.apply(tvViewResults)
        divider.setBackgroundColor(pollStyle.dividerColor)
    }
}

