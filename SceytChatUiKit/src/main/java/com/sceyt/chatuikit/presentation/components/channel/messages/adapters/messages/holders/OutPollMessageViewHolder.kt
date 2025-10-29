package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.databinding.SceytItemOutPollMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BasePollMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class OutPollMessageViewHolder(
        private val binding: SceytItemOutPollMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        style: MessageItemStyle,
        messageListeners: MessageClickListeners.ClickListeners?,
) : BasePollMessageViewHolder(
    view = binding.root,
    style = style,
    messageListeners = messageListeners
) {

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

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, viewReply)

                if (diff.reactionsChanged || diff.edited)
                    setOrUpdateReactions(item, rvReactions, viewPoolReactions)

                if (diff.pollChanged) {
                    updatePollViews(message.poll)
                }
            }
        }
    }

    override fun updatePollViews(poll: SceytPollDetails?) = with(binding) {
        setupPollViews(
            poll = poll,
            rvPollOptions = rvPollOptions,
            tvPollQuestion = tvPollQuestion,
            tvPollType = tvPollType,
            tvViewResults = tvViewResults,
            divider = divider
        )
    }

    override val selectMessageView get() = binding.selectView

    override val layoutBubbleConfig: Pair<ViewGroup, Boolean>?
        get() = Pair(binding.layoutDetails, false)

    override val incoming: Boolean
        get() = false

    private fun SceytItemOutPollMessageBinding.setMessageItemStyle() {
        applyCommonStyle(
            layoutDetails = layoutDetails,
            tvForwarded = tvForwarded,
            messageBody = null,
            tvThreadReplyCount = tvReplyCount,
            toReplyLine = toReplyLine
        )

        applyStyle(
            tvPollQuestion = tvPollQuestion,
            tvPollType = tvPollType,
            tvViewResults = tvViewResults,
            divider = divider
        )
    }
}
