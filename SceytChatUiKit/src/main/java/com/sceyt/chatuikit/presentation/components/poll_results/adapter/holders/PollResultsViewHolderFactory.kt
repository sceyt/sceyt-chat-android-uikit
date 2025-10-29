package com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.sceyt.chatuikit.databinding.SceytItemPollResultHeaderBinding
import com.sceyt.chatuikit.databinding.SceytItemPollResultOptionBinding
import com.sceyt.chatuikit.presentation.components.poll_results.PollResultsStyle
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.PollResultClickListeners
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.PollResultClickListenersImpl
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

open class PollResultsViewHolderFactory(
        context: Context,
        private val style: PollResultsStyle,
) {
    private val layoutInflater = LayoutInflater.from(context)
    private val clickListeners = PollResultClickListenersImpl()

    fun createViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<PollResultItem> {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                PollResultHeaderViewHolder(
                    binding = SceytItemPollResultHeaderBinding.inflate(layoutInflater, parent, false),
                    questionTextStyle = style.questionTextStyle,
                    pollTypeTextStyle = style.pollTypeTextStyle,
                    headerDividerStyle = style.headerDividerStyle
                )
            }

            VIEW_TYPE_OPTION -> {
              PollOptionResultViewHolder(
                    binding = SceytItemPollResultOptionBinding.inflate(layoutInflater, parent, false),
                    style = style.pollResultItemStyle,
                    voterStyle = style.pollResultItemStyle.voterItemStyle,
                    clickListeners = clickListeners
                )
            }

            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    fun getItemViewType(item: PollResultItem): Int {
        return when (item) {
            is PollResultItem.HeaderItem -> VIEW_TYPE_HEADER
            is PollResultItem.PollOptionItem -> VIEW_TYPE_OPTION
        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_OPTION = 1
    }

    fun setOnClickListener(listener: PollResultClickListeners) {
        clickListeners.setListener(listener)
    }
}