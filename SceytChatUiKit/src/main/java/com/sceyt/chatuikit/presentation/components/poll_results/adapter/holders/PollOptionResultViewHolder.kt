package com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders

import androidx.core.view.isVisible
import com.sceyt.chatuikit.databinding.SceytItemPollResultOptionBinding
import com.sceyt.chatuikit.presentation.components.poll_results.PollResultsStyle
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VotersAdapter
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.PollResultClickListeners
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.VoterClickListeners.VoterClickListener
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class PollOptionResultViewHolder(
        private val binding: SceytItemPollResultOptionBinding,
        private val style: PollResultsStyle,
        private val clickListeners: PollResultClickListeners.ClickListeners
) : BaseViewHolder<PollResultItem>(binding.root) {

    private lateinit var pollOptionItem: PollResultItem.PollOptionItem
    private var votersAdapter: VotersAdapter? = null

    init {
        binding.applyStyle()
        binding.btnShowAll.setOnClickListener {
            clickListeners.onShowAllClick(it, pollOptionItem)
        }
        setupVotersAdapter()
    }

    override fun bind(item: PollResultItem) {
        pollOptionItem = (item as? PollResultItem.PollOptionItem) ?: return

        with(binding) {
            tvOptionName.text = pollOptionItem.pollOption.name
            tvVoteCount.text = style.pollResultItemStyle.voteCountFormatter.format(context, pollOptionItem.voteCount)

            val votersToShow = pollOptionItem.voters.take(5)
            val voterItems = votersToShow.map { VoterItem.Voter(it) }
            votersAdapter?.submitList(voterItems)

            btnShowAll.isVisible = pollOptionItem.hasMore
            rvVoters.isVisible = pollOptionItem.voters.isNotEmpty()
        }
    }

    fun setIsLastItem(isLast: Boolean) {
        binding.divider.isVisible = !isLast
    }

    private fun setupVotersAdapter() {
        votersAdapter = VotersAdapter(
            viewHolderFactory = VotersViewHolderFactory(
                context = context,
                style = style.pollResultItemStyle.voterItemStyle)
                .also { factory ->
                    factory.setOnClickListener(VoterClickListener(clickListeners::onVoterClick))
                }
        )
        binding.rvVoters.adapter = votersAdapter
    }

    private fun SceytItemPollResultOptionBinding.applyStyle() {
        with(style.pollResultItemStyle) {
            optionNameTextStyle.apply(tvOptionName)
            voteCountTextStyle.apply(tvVoteCount)
            showAllButtonTextStyle.apply(btnShowAll)
            itemDividerStyle.apply(divider)
        }
    }
}