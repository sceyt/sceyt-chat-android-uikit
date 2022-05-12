package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.annotation.SuppressLint
import android.text.format.DateUtils
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.messages.SceytUiMessage
import com.sceyt.chat.ui.databinding.SceytUiRecyclerReplayContainerBinding
import com.sceyt.chat.ui.extensions.getAttachmentUrl
import com.sceyt.chat.ui.extensions.getShowBody
import com.sceyt.chat.ui.presentation.customviews.DateStatusView
import com.sceyt.chat.ui.presentation.customviews.ToReplayLineView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.MessageListItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionItem
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.ReactionsAdapter
import com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.reactions.viewholders.ReactionViewHolderFactory
import com.sceyt.chat.ui.utils.DateTimeUtil.getDateTimeString
import com.sceyt.chat.ui.utils.RecyclerItemOffsetDecoration
import kotlin.math.min

abstract class BaseMsgViewHolder(view: View) : BaseViewHolder<MessageListItem>(view) {
    private var reactionsAdapter: ReactionsAdapter? = null
    private val editedString = view.context.getString(R.string.edited)

    @SuppressLint("SetTextI18n")
    protected fun setReplayCount(tvReplayCount: TextView, toReplayLine: ToReplayLineView, replayCount: Long) {
        if (replayCount > 0) {
            tvReplayCount.text = "$replayCount ${itemView.context.getString(R.string.replays)}"
            tvReplayCount.isVisible = true
            toReplayLine.isVisible = true
        } else {
            tvReplayCount.isVisible = false
            toReplayLine.isVisible = false
        }
    }

    protected fun setOrUpdateReactions(reactionScores: Array<ReactionScore>?, rvReactions: RecyclerView,
                                       viewPool: RecyclerView.RecycledViewPool) {
        if (reactionScores.isNullOrEmpty()) {
            rvReactions.isVisible = false
            return
        }

        val reactions = ArrayList<ReactionItem>(reactionScores.sortedByDescending { it.score }.map {
            ReactionItem.Reaction(it)
        }).also {
            it.add(ReactionItem.AddItem)
        }

        val spanCount = min(4, reactions.size)

        if (reactionsAdapter == null) {
            reactionsAdapter = ReactionsAdapter(reactions, ReactionViewHolderFactory(itemView.context))

            with(rvReactions) {
                setRecycledViewPool(viewPool)
                if (itemDecorationCount == 0)
                    addItemDecoration(RecyclerItemOffsetDecoration(0, 4, 8, 4))

                layoutManager = GridLayoutManager(itemView.context, spanCount)
                adapter = reactionsAdapter
            }
        } else {
            rvReactions.layoutManager = GridLayoutManager(itemView.context, spanCount)
            reactionsAdapter?.submitData(reactions)
        }
        rvReactions.isVisible = true
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
        val text = if (isEdited) "$editedString ${getDateTimeString(createdAt)}"
        else getDateTimeString(createdAt)
        messageDate.setDateText(text)
    }
}