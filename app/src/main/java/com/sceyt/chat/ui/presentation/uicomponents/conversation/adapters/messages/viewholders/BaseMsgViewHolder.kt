package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.annotation.SuppressLint
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.databinding.SceytUiRecyclerReplayContainerBinding
import com.sceyt.chat.ui.extensions.getAttachmentUrl
import com.sceyt.chat.ui.extensions.getShowBody
import com.sceyt.chat.ui.presentation.customviews.Avatar
import com.sceyt.chat.ui.presentation.customviews.DateStatusView
import com.sceyt.chat.ui.presentation.customviews.ToReplayLineView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionsAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders.ReactionViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.utils.DateTimeUtil.getDateTimeString
import com.sceyt.chat.ui.utils.RecyclerItemOffsetDecoration
import kotlin.math.min

abstract class BaseMsgViewHolder(view: View,
                                 private val messageListeners: MessageClickListenersImpl) : BaseViewHolder<MessageListItem>(view) {

    private var reactionsAdapter: ReactionsAdapter? = null

    @SuppressLint("SetTextI18n")
    protected fun setReplayCount(tvReplayCount: TextView, toReplayLine: ToReplayLineView, item: MessageListItem.MessageItem) {
        val replayCount = item.message.replyCount
        if (replayCount > 0) {
            tvReplayCount.text = "$replayCount ${itemView.context.getString(R.string.replays)}"
            tvReplayCount.isVisible = true
            toReplayLine.isVisible = true

            tvReplayCount.setOnClickListener { messageListeners.onReplayCountClick(item) }
        } else {
            tvReplayCount.isVisible = false
            toReplayLine.isVisible = false
        }
    }

    protected fun setMessageDay(createdAt: Long, showDate: Boolean, tvData: TextView) {
        tvData.isVisible = if (showDate) {
            val dateText = when {
                DateUtils.isToday(createdAt) -> itemView.context.getString(R.string.today)
                else -> getDateTimeString(createdAt, "MMMM dd")
            }
            tvData.text = dateText
            true
        } else false
    }

    protected fun setMessageDateText(createdAt: Long, messageDate: DateStatusView, isEdited: Boolean) {
        messageDate.setDateText(getDateTimeString(createdAt), isEdited)
    }

    protected fun setReplayedMessageContainer(message: SceytUiMessage, viewBinding: SceytUiRecyclerReplayContainerBinding) {
        if (message.parent == null || message.replyInThread || message.parent?.id == 0L) {
            viewBinding.root.isVisible = false
            return
        }

        with(viewBinding) {
            val parent = message.parent
            tvName.text = parent?.from?.fullName?.trim()
            tvMessageBody.text = parent?.getShowBody(itemView.context)
            imageAttachment.isVisible = if (parent?.attachments.isNullOrEmpty()) {
                false
            } else {
                val url = parent?.getAttachmentUrl()
                if (!url.isNullOrBlank()) {
                    Glide.with(itemView.context)
                        .load(url)
                        .override(30)
                        .into(imageAttachment)
                } else imageAttachment.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.ic_file_with_bg))
                true
            }
            root.isVisible = true
        }
    }

    protected fun setMessageUserAvatarAndName(avatar: Avatar, tvName: TextView, message: SceytUiMessage) {
        if (!message.isGroup) return

        if (message.canShowAvatarAndName) {
            avatar.setNameAndImageUrl(message.from?.fullName, message.from?.avatarURL)
            tvName.text = message.from?.fullName?.trim()
            tvName.isVisible = true
            avatar.isVisible = true
        } else {
            avatar.isInvisible = true
            tvName.isVisible = false
        }
    }

    protected fun setOrUpdateReactions(item: MessageListItem.MessageItem, rvReactions: RecyclerView,
                                       viewPool: RecyclerView.RecycledViewPool) {
        val reactionScores = item.message.reactionScores
        if (reactionScores.isNullOrEmpty()) {
            rvReactions.isVisible = false
            return
        }

        val reactions = initReactionsList(reactionScores, item.message)
        val gridLayoutManager = GridLayoutManager(itemView.context, min(4, reactions.size))

        if (reactionsAdapter == null) {
            reactionsAdapter = ReactionsAdapter(reactions, rvReactions,
                ReactionViewHolderFactory(itemView.context, messageListeners)
            )

            with(rvReactions) {
                setRecycledViewPool(viewPool)
                itemAnimator = DefaultItemAnimator().also {
                    it.moveDuration = 100
                    it.removeDuration = 100
                }
                if (itemDecorationCount == 0)
                    addItemDecoration(RecyclerItemOffsetDecoration(0, 4, 8, 4))

                layoutManager = gridLayoutManager
                adapter = reactionsAdapter
            }
        } else {
            rvReactions.layoutManager = gridLayoutManager
            reactionsAdapter?.submitData(reactions)
        }
        rvReactions.isVisible = true
    }

    private fun initReactionsList(scores: Array<ReactionScore>, message: SceytUiMessage): ArrayList<ReactionItem> {
        return ArrayList<ReactionItem>(scores.sortedByDescending { it.score }.map {
            ReactionItem.Reaction(it, MessageListItem.MessageItem(message))
        }).also {
            if (it.isNotEmpty())
                it.add(ReactionItem.AddItem(MessageListItem.MessageItem(message)))
        }
    }

    fun updateReaction(scores: Array<ReactionScore>, message: SceytUiMessage) {
        val reactions = initReactionsList(scores, message)
        message.reactionScores = scores
        if (reactionsAdapter != null) {
            reactionsAdapter?.recyclerView?.isVisible = scores.isNotEmpty()
            if (scores.isNotEmpty())
                (reactionsAdapter?.recyclerView?.layoutManager as? GridLayoutManager)?.spanCount = min(4, reactions.size)
            reactionsAdapter?.submitData(reactions)
        } else
            bindingAdapter?.notifyItemChanged(bindingAdapterPosition)
    }
}