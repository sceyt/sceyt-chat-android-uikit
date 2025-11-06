package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.PollOptionUiModel
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.data.models.messages.getOptionsUiModels
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PollOptionAdapter
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.PollOptionViewHolderFactory
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
            isAnonymousProvider = { currentPoll?.anonymous ?: false },
            bubbleBackgroundStyleProvider = {
                if (incoming) style.incomingBubbleBackgroundStyle
                else style.outgoingBubbleBackgroundStyle
            },
            onOptionClick = { option -> onPollOptionClick(option) }
        )
    }

    protected fun setupPollViews(
        poll: SceytPollDetails?,
        rvPollOptions: RecyclerView,
        tvPollQuestion: TextView,
        tvPollType: TextView,
        tvViewResults: TextView,
        divider: View,
    ) {
        if (poll == null) {
            setOptions(options = emptyList(), isSamePoll = false, rvPollOptions = rvPollOptions)
            return
        }
        val isSamePoll = currentPoll?.id == poll.id
        currentPoll = poll

        tvPollQuestion.text = poll.name
        tvPollType.text = pollStyle.pollTypeFormatter.format(context, poll)

        divider.isVisible = !poll.anonymous

        val totalVotes = poll.maxVotedCountWithPendingVotes
        with(tvViewResults) {
            isVisible = !poll.anonymous
            isEnabled = totalVotes > 0

            if (totalVotes > 0) {
                pollStyle.viewResultsTextStyle.apply(this)
            } else {
                pollStyle.viewResultsDisabledTextStyle.apply(this)
            }
        }

        setOptions(
            options = poll.getOptionsUiModels(),
            isSamePoll = isSamePoll,
            rvPollOptions = rvPollOptions
        )
    }

    protected open fun setOptions(
        options: List<PollOptionUiModel>,
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
            options = options,
            animate = shouldAnimate
        )
    }

    protected open fun onPollOptionClick(option: PollOptionUiModel) {
        messageListeners?.onPollOptionClick(
            view = itemView,
            item = requireMessageItem,
            option = PollOption(id = option.id, name = option.text, order = option.order)
        )
    }

    protected fun onViewResultsClick() {
        messageListeners?.onPollViewResultsClick(
            view = itemView,
            item = requireMessageItem
        )
    }

    protected abstract fun updatePollViews(poll: SceytPollDetails?)

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

