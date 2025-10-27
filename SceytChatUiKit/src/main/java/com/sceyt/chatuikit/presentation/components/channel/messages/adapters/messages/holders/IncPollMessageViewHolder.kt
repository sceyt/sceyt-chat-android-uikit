package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.holders

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.SceytPollDetails
import com.sceyt.chatuikit.databinding.SceytItemIncPollMessageBinding
import com.sceyt.chatuikit.persistence.differs.MessageDiff
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.MessageListItem
import com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages.root.BasePollMessageViewHolder
import com.sceyt.chatuikit.presentation.components.channel.messages.listeners.click.MessageClickListeners
import com.sceyt.chatuikit.styles.messages_list.item.MessageItemStyle

class IncPollMessageViewHolder(
        private val binding: SceytItemIncPollMessageBinding,
        private val viewPoolReactions: RecyclerView.RecycledViewPool,
        style: MessageItemStyle,
        messageListeners: MessageClickListeners.ClickListeners?,
        displayedListener: ((MessageListItem) -> Unit)?,
) : BasePollMessageViewHolder(
    view = binding.root,
    style = style,
    messageListeners = messageListeners,
    displayedListener = displayedListener
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

                if (diff.avatarChanged || diff.showAvatarAndNameChanged)
                    setMessageUserAvatarAndName(avatar, tvUserName, message)

                if (diff.replyCountChanged)
                    setReplyCount(tvReplyCount, toReplyLine, item)

                if (diff.replyContainerChanged)
                    setReplyMessageContainer(message, viewReply)

                if (diff.reactionsChanged || diff.edited)
                    setOrUpdateReactions(item = item, rvReactionsViewStub = rvReactions, viewPool = viewPoolReactions)

                if (diff.pollChanged) {
                    updatePollViews(message.poll)
                }

                if (item.message.shouldShowAvatarAndName)
                    avatar.setOnClickListener {
                        messageListeners?.onAvatarClick(it, item)
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

        applyStyle(
            tvPollQuestion = tvPollQuestion,
            tvPollType = tvPollType,
            tvViewResults = tvViewResults,
            divider = divider
        )
    }
}

