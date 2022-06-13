package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.SceytMessage
import com.sceyt.chat.ui.databinding.SceytRecyclerReplayContainerBinding
import com.sceyt.chat.ui.extensions.dpToPx
import com.sceyt.chat.ui.extensions.getCompatColor
import com.sceyt.chat.ui.presentation.common.setMessageDateAndStatusIcon
import com.sceyt.chat.ui.presentation.customviews.SceytAvatarView
import com.sceyt.chat.ui.presentation.customviews.SceytDateStatusView
import com.sceyt.chat.ui.presentation.customviews.SceytToReplayLineView
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.files.FileListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageItemPayloadDiff
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.getAttachmentUrl
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.getShowBody
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionsAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders.ReactionViewHolderFactory
import com.sceyt.chat.ui.presentation.uicomponents.conversation.listeners.MessageClickListenersImpl
import com.sceyt.chat.ui.utils.DateTimeUtil.getDateTimeString
import com.sceyt.chat.ui.utils.RecyclerItemOffsetDecoration
import kotlin.math.min

abstract class BaseMsgViewHolder(view: View,
                                 private val messageListeners: MessageClickListenersImpl? = null)
    : RecyclerView.ViewHolder(view) {


    abstract fun bind(item: MessageListItem, diff: MessageItemPayloadDiff)
    open fun onViewDetachedFromWindow() {}
    open fun onViewAttachedToWindow() {}

    private var reactionsAdapter: ReactionsAdapter? = null

    @SuppressLint("SetTextI18n")
    protected fun setReplayCount(tvReplayCount: TextView, toReplayLine: SceytToReplayLineView, item: MessageListItem.MessageItem) {
        val replayCount = item.message.replyCount
        if (replayCount > 0) {
            tvReplayCount.text = "$replayCount ${itemView.context.getString(R.string.replays)}"
            tvReplayCount.isVisible = true
            toReplayLine.isVisible = true

            tvReplayCount.setOnClickListener { messageListeners?.onReplayCountClick(it, item) }
        } else {
            tvReplayCount.isVisible = false
            toReplayLine.isVisible = false
        }
    }

    protected fun setMessageStatusAndDateText(message: SceytMessage, messageDate: SceytDateStatusView) {
        val isEdited = message.state == MessageState.Edited
        val dateText = getDateTimeString(message.createdAt)
        message.setMessageDateAndStatusIcon(messageDate, dateText, isEdited)
    }

    protected fun setReplayedMessageContainer(message: SceytMessage, viewBinding: SceytRecyclerReplayContainerBinding) {
        if (message.parent == null || message.replyInThread || message.parent?.id == 0L) {
            viewBinding.root.isVisible = false
            return
        }

        with(viewBinding) {
            val parent = message.parent
            tvName.text = parent?.from?.fullName?.trim()
            if (parent?.state == MessageState.Deleted) {
                tvMessageBody.setTypeface(tvMessageBody.typeface, Typeface.ITALIC)
                tvMessageBody.setTextColor(itemView.context.getCompatColor(R.color.sceyt_color_gray_400))
            } else {
                tvMessageBody.setTypeface(tvMessageBody.typeface, Typeface.NORMAL)
                tvMessageBody.setTextColor(itemView.context.getCompatColor(R.color.sceyt_color_black_themed))
            }

            tvMessageBody.text = parent?.getShowBody(itemView.context)
            imageAttachment.isVisible = if (parent?.attachments.isNullOrEmpty()) {
                false
            } else {
                val url = parent?.getAttachmentUrl(itemView.context)
                if (!url.isNullOrBlank()) {
                    Glide.with(itemView.context)
                        .load(url)
                        .override(imageAttachment.width, imageAttachment.height)
                        .into(imageAttachment)
                } else imageAttachment.setImageDrawable(ContextCompat.getDrawable(itemView.context, R.drawable.sceyt_ic_file_with_bg))
                true
            }
            root.isVisible = true
        }
    }

    protected fun setMessageUserAvatarAndName(avatarView: SceytAvatarView, tvName: TextView, message: SceytMessage) {
        if (!message.isGroup) return

        if (message.canShowAvatarAndName) {
            avatarView.setNameAndImageUrl(message.from?.fullName, message.from?.avatarURL)
            tvName.text = message.from?.fullName?.trim()
            tvName.isVisible = true
            avatarView.isVisible = true
        } else {
            avatarView.isInvisible = true
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
        val gridLayoutManager = GridLayoutManager(itemView.context, getReactionSpanCount(reactions.size, item.message.incoming))

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

    protected fun setMessageDateDependAttachments(messageDate: SceytDateStatusView, attachments: List<FileListItem>?) {
        messageDate.apply {
            val lastAttachment = attachments?.lastOrNull()
            val needHighlight = lastAttachment is FileListItem.Image || lastAttachment is FileListItem.Video
            setHighlighted(needHighlight)
            val marginEndBottom = if (needHighlight) Pair(25, 25) else {
                //Set the value which is set in xml
                Pair(dpToPx(5f), dpToPx(2f))
            }
            (layoutParams as ViewGroup.MarginLayoutParams).apply {
                setMargins(0, marginTop, 0, marginEndBottom.second)
                marginEnd = marginEndBottom.first
            }
        }
    }

    private fun initReactionsList(scores: Array<ReactionScore>, message: SceytMessage): ArrayList<ReactionItem> {
        return ArrayList<ReactionItem>(scores.sortedByDescending { it.score }.map {
            ReactionItem.Reaction(it, MessageListItem.MessageItem(message))
        }).also {
            if (it.isNotEmpty())
                it.add(ReactionItem.AddItem(MessageListItem.MessageItem(message)))
        }
    }

    private fun getReactionSpanCount(reactionsSize: Int, incoming: Boolean): Int {
        if (incoming) return 5
        return min(5, reactionsSize)
    }

    fun updateReaction(scores: Array<ReactionScore>, message: SceytMessage) {
        val reactions = initReactionsList(scores, message)
        message.reactionScores = scores
        if (reactionsAdapter != null) {
            reactionsAdapter?.recyclerView?.isVisible = scores.isNotEmpty()
            if (scores.isNotEmpty())
                (reactionsAdapter?.recyclerView?.layoutManager as? GridLayoutManager)?.spanCount = getReactionSpanCount(reactions.size, message.incoming)
            reactionsAdapter?.submitData(reactions)
        } else
            bindingAdapter?.notifyItemChanged(bindingAdapterPosition)
    }
}