package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytPoll
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PollOptionAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.helpers.PollVoteHelper
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

abstract class BasePollMessageViewHolder(
        view: View,
        style: MessageItemStyle,
        protected val messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)? = null,
) : BaseMessageViewHolder(view, style, messageListeners, displayedListener) {

    protected val gson = Gson()
    protected var pollOptionAdapter: PollOptionAdapter? = null
    protected var currentPoll: SceytPoll? = null

    protected fun parsePoll(metadata: String?): SceytPoll? {
        return try {
            metadata?.let { gson.fromJson(it, SceytPoll::class.java) }?.copy(
                closed = true
            )
        } catch (_: Exception) {
            null
        }
    }

    protected fun setupPollViews(
            poll: SceytPoll,
            rvPollOptions: RecyclerView,
            tvPollType: TextView,
            tvViewResults: TextView,
            divider: View,
    ) {
        currentPoll = poll

        // Show "Poll finished" if closed, otherwise show poll type
        tvPollType.text = when {
            poll.closed -> {
                tvPollType.context.getString(R.string.sceyt_poll_finished)
            }

            poll.anonymous -> {
                tvPollType.context.getString(R.string.sceyt_anonymous_poll)
            }

            else -> {
                tvPollType.context.getString(R.string.sceyt_public_poll)
            }
        }

        divider.isVisible = !poll.anonymous


        with(tvViewResults) {
            isVisible = !poll.anonymous
            isEnabled = poll.totalVotes > 0
            setTextColor(
                if (poll.totalVotes > 0)
                    tvViewResults.context.getColor(R.color.sceyt_color_accent)
                else
                    tvViewResults.context.getColor(R.color.sceyt_color_icon_inactive)
            )
        }

        setOptions(poll, rvPollOptions)
    }

    protected open fun setOptions(
            poll: SceytPoll,
            rvPollOptions: RecyclerView,
    ) {
        val shouldAnimate = pollOptionAdapter != null && currentPoll?.id == poll.id

        if (pollOptionAdapter == null) {
            pollOptionAdapter = PollOptionAdapter(poll = poll) { option ->
                onPollOptionClick(option)
            }
            rvPollOptions.itemAnimator = null
            rvPollOptions.adapter = pollOptionAdapter
        }

        pollOptionAdapter?.setOptions(
            newOptions = poll.options,
            newTotalVotes = poll.totalVotes,
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
}

