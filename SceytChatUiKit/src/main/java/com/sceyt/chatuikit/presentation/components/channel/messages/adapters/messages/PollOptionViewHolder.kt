package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.PollOptionUiModel
import com.sceyt.chatuikit.databinding.SceytItemPollOptionBinding
import com.sceyt.chatuikit.persistence.differs.PollOptionDiff
import com.sceyt.chatuikit.styles.messages_list.item.PollStyle

open class PollOptionViewHolder(
        private val binding: SceytItemPollOptionBinding,
        private val pollStyle: PollStyle,
        private val isClosedProvider: () -> Boolean,
        private val isAnonymousProvider: () -> Boolean,
        private val totalVotesProvider: () -> Int,
        private var onOptionClick: ((PollOptionUiModel) -> Unit)? = null,
) : RecyclerView.ViewHolder(binding.root) {
    private lateinit var currentOption: PollOptionUiModel
    private var currentProgress = 0
    private var votersAdapter: VoterAvatarAdapter? = null

    init {
        applyStyle()
        binding.root.setOnClickListener {
            if (!isClosedProvider() && ::currentOption.isInitialized) {
                onOptionClick?.invoke(currentOption)
            }
        }
    }

    open fun bind(
            option: PollOptionUiModel,
            diff: PollOptionDiff,
            animate: Boolean = false,
    ) = with(binding) {
        currentOption = option

        val isClosed = isClosedProvider()
        val isAnonymous = isAnonymousProvider()
        val totalVotes = totalVotesProvider()

        root.isEnabled = !isClosed
        checkbox.isVisible = !isClosed

        if (diff.selectedChanged) {
            checkbox.isChecked = option.selected
        }

        if (diff.textChanged) {
            tvOptionText.text = option.text
        }

        if (diff.voteCountChanged) {
            tvVoteCount.text = pollStyle.voteCountFormatter.format(root.context, option)
            tvVoteCount.isVisible = option.voteCount > 0

            val percentage = option.getPercentage(totalVotes).toInt()
            val shouldAnimate = animate && percentage != currentProgress && diff != PollOptionDiff.DEFAULT

            progressBar.setProgress(percentage, animate = shouldAnimate)
            currentProgress = percentage
        }

        // Setup voters avatars (hide if anonymous)
        if (diff.votersChanged) {
            if (!isAnonymous && option.voters.isNotEmpty()) {
                if (votersAdapter == null) {
                    votersAdapter = VoterAvatarAdapter()
                    rvVoters.itemAnimator = null
                    rvVoters.adapter = votersAdapter
                }
                votersAdapter?.submitList(option.voters.take(3))
                rvVoters.isVisible = true
            } else {
                rvVoters.isVisible = false
                votersAdapter?.submitList(emptyList())
            }
        }
    }

    open fun onViewDetachedFromWindow() {
        binding.progressBar.cancelAnimation()
    }

    protected open fun applyStyle() = with(binding) {
        pollStyle.optionTextStyle.apply(tvOptionText)
        pollStyle.voteCountTextStyle.apply(tvVoteCount)
    }
}

