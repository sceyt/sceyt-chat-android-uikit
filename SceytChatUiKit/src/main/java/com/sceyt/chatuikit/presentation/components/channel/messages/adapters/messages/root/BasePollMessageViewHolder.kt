package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytPoll
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PollOptionAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.helpers.PollVoteHelper
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

abstract class BasePollMessageViewHolder(
        view: View,
        protected val style: MessageItemStyle,
        protected val messageListeners: MessageClickListeners.ClickListeners?,
        private val viewPoolPollOptions: RecyclerView.RecycledViewPool,
        displayedListener: ((MessageListItem) -> Unit)? = null,
) : BaseMessageViewHolder(view, style, messageListeners, displayedListener) {

    protected val gson = Gson()
    protected var pollOptionAdapter: PollOptionAdapter? = null
    protected var currentPoll: SceytPoll? = null
    protected val pollStyle = style.pollStyle

    protected fun parsePoll(metadata: String?): SceytPoll? {
        return try {
            metadata?.let { gson.fromJson(it, SceytPoll::class.java) }
        } catch (_: Exception) {
            null
        }
    }

    protected fun setupPollViews(
            poll: SceytPoll,
            rvPollOptions: RecyclerView,
            tvPollQuestion: TextView,
            tvPollType: TextView,
            tvViewResults: TextView,
            divider: View,
    ) {
        val isSamePoll = currentPoll?.id == poll.id
        currentPoll = poll

        tvPollQuestion.text = poll.question
        tvPollType.text = pollStyle.pollTypeFormatter.format(context, poll)

        // Apply divider color
        divider.isVisible = !poll.anonymous

        with(tvViewResults) {
            isVisible = !poll.anonymous
            isEnabled = poll.totalVotes > 0

            if (poll.totalVotes > 0) {
                pollStyle.viewResultsTextStyle.apply(this)
            } else {
                pollStyle.viewResultsDisabledTextStyle.apply(this)
            }
        }

        setOptions(poll = poll, isSamePoll = isSamePoll, rvPollOptions = rvPollOptions)
    }

    protected open fun setOptions(
            poll: SceytPoll,
            isSamePoll: Boolean,
            rvPollOptions: RecyclerView,
    ) {
        val shouldAnimate = pollOptionAdapter != null && isSamePoll

        if (pollOptionAdapter == null || !isSamePoll) {
            pollOptionAdapter = PollOptionAdapter(
                poll = poll,
                pollStyle = pollStyle
            ) { option ->
                onPollOptionClick(option)
            }

            with(rvPollOptions) {
                setRecycledViewPool(viewPoolPollOptions)
                itemAnimator = null
                adapter = pollOptionAdapter
            }
        }

        pollOptionAdapter?.updatePoll(
            poll = poll,
            animate = shouldAnimate
        )
    }

    protected open fun onPollOptionClick(option: PollOption) {
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
            option = option
        )
    }

    protected fun onViewResultsClick() {
        messageListeners?.onPollViewResultsClick(
            view = itemView,
            item = messageListItem as MessageListItem.MessageItem
        )
    }

    protected abstract fun updatePollViews(poll: SceytPoll)

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

