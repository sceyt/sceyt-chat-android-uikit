package com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders

import com.sceyt.chatuikit.databinding.SceytItemPollResultHeaderBinding
import com.sceyt.chatuikit.presentation.components.poll_results.PollResultsStyle
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultItem
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.common.DividerStyle
import com.sceyt.chatuikit.styles.common.TextStyle

class PollResultHeaderViewHolder(
        private val binding: SceytItemPollResultHeaderBinding,
        private val style: PollResultsStyle,

) : BaseViewHolder<PollResultItem>(binding.root) {

    private lateinit var headerItem: PollResultItem.HeaderItem

    init {
        binding.applyStyle()
    }

    override fun bind(item: PollResultItem) {
        headerItem = (item as? PollResultItem.HeaderItem) ?: return
        val poll = headerItem.poll

        with(binding) {
            tvPollQuestion.text = poll.name
            tvPollType.text = style.pollTypeFormatter.format(context, poll)
        }
    }

    private fun SceytItemPollResultHeaderBinding.applyStyle() {
        with(style){
            questionTextStyle.apply(tvPollQuestion)
            pollTypeTextStyle.apply(tvPollType)
            headerDividerStyle.apply(divider)
        }
    }
}