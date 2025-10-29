package com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders

import android.view.View
import androidx.core.view.isVisible
import com.sceyt.chatuikit.databinding.SceytItemPollResultOptionBinding
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VotersAdapter
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.PollResultClickListeners
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.VoterClickListeners
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.poll_results.PollResultItemStyle
import com.sceyt.chatuikit.styles.poll_results.VoterItemStyle

class PollOptionResultViewHolder(
        private val binding: SceytItemPollResultOptionBinding,
        private val style: PollResultItemStyle,
        private val voterStyle: VoterItemStyle,
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

    fun setIsLastItem(isLast: Boolean) {
        binding.divider.isVisible = !isLast
    }

    private fun setupVotersAdapter() {
        votersAdapter = VotersAdapter(
            viewHolderFactory = VotersViewHolderFactory(context, voterStyle).also {
                it.setOnClickListener(object : VoterClickListeners.ClickListeners {
                    override fun onVoterClick(view: View, item: VoterItem.Voter) {
                        clickListeners.onVoterClick(view, item)
                    }
                })
            }
        )
        binding.rvVoters.adapter = votersAdapter
    }

    override fun bind(item: PollResultItem) {
        pollOptionItem = (item as? PollResultItem.PollOptionItem) ?: return

        with(binding) {
            tvOptionName.text = pollOptionItem.pollOption.name
            tvVoteCount.text = style.voteCountFormatter.format(root.context, pollOptionItem.voteCount)

            val votersToShow = pollOptionItem.voters.take(5)
            val voterItems = votersToShow.map { VoterItem.Voter(it) }
            votersAdapter?.submitList(voterItems)

            btnShowAll.isVisible = pollOptionItem.hasMore
            rvVoters.isVisible = pollOptionItem.voters.isNotEmpty()
        }
    }

    private fun SceytItemPollResultOptionBinding.applyStyle() {
        style.optionNameTextStyle.apply(tvOptionName)
        style.voteCountTextStyle.apply(tvVoteCount)
        style.showAllButtonTextStyle.apply(btnShowAll)
        style.itemDividerStyle.apply(divider)
    }
}