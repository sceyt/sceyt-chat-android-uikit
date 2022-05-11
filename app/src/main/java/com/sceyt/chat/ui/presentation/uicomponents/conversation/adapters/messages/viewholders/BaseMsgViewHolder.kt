package com.sceyt.chat.ui.presentation.uicomponents.conversation.adapters.messages.viewholders

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.models.message.ReactionScore
import com.sceyt.chat.ui.R
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


    @SuppressLint("SetTextI18n")
    protected fun setReplayCount(measuringView: View, tvReplayCount: TextView, toReplayLine: ToReplayLineView, replayCount: Long) {
        if (replayCount > 0) {
            val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            measuringView.measure(widthSpec, heightSpec)
            val messageInfoHeight: Int = measuringView.measuredHeight

            tvReplayCount.measure(widthSpec, heightSpec)
            val tvReplayCountHeight: Int = tvReplayCount.measuredHeight

            (toReplayLine.layoutParams as ViewGroup.MarginLayoutParams).setMargins(0,
                messageInfoHeight / 2, 0, tvReplayCountHeight / 2 - tvReplayCount.paddingTop)
            toReplayLine.requestLayout()
            tvReplayCount.text = "$replayCount ${itemView.context.getString(R.string.replay)}"
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

    protected fun setDate(createdAt: Long, showDate: Boolean, tvData: TextView) {
        tvData.isVisible = if (showDate) {
            tvData.text = getDateTimeString(createdAt)
            true
        } else false
    }
}