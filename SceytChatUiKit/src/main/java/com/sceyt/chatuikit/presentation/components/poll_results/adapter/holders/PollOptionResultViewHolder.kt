package com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders

import androidx.core.view.isVisible
import androidx.recyclerview.widget.SimpleItemAnimator
import com.sceyt.chatuikit.databinding.SceytItemPollResultOptionBinding
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.PollResultItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VoterItem
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.VotersAdapter
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.diff.PollResultItemPayloadDiff
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.PollResultClickListeners
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.listeners.VoterClickListeners.VoterClickListener
import com.sceyt.chatuikit.presentation.root.BaseViewHolder
import com.sceyt.chatuikit.styles.poll_results.PollResultsStyle

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

            btnShowAll.isVisible = item.hasMore
            rvVoters.isVisible = pollOptionItem.voters.isNotEmpty()
        }
    }

    fun bind(item: PollResultItem, payload: PollResultItemPayloadDiff) {
        pollOptionItem = (item as? PollResultItem.PollOptionItem) ?: return

        with(binding) {
            if (payload.optionChanged) {
                tvOptionName.text = pollOptionItem.pollOption.name
            }

            if (payload.voteCountChanged) {
                tvVoteCount.text = style.pollResultItemStyle.voteCountFormatter.format(context, pollOptionItem.voteCount)
            }

            if (payload.votersChanged) {
                val votersToShow = pollOptionItem.voters.take(5)
                val voterItems = votersToShow.map { VoterItem.Voter(it) }
                votersAdapter?.submitList(voterItems)
                rvVoters.isVisible = pollOptionItem.voters.isNotEmpty()
            }
        }
    }
    private fun setupVotersAdapter() {
        votersAdapter = VotersAdapter(
            viewHolderFactory = VotersViewHolderFactory(
                context = context,
                style = style.pollOptionVotersStyle)
                .also { factory ->
                    factory.setOnClickListener(VoterClickListener(clickListeners::onVoterClick))
                }
        )
        binding.rvVoters.adapter = votersAdapter
        (binding.rvVoters.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
    }

    private fun SceytItemPollResultOptionBinding.applyStyle() {
        with(style.pollResultItemStyle) {
            optionBackgroundStyle.apply(layoutOptionContainer)
            optionNameTextStyle.apply(tvOptionName)
            voteCountTextStyle.apply(tvVoteCount)
            showAllButtonTextStyle.apply(btnShowAll)
        }
    }
}