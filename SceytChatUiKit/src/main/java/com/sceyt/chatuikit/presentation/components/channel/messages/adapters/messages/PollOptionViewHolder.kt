package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.PollOptionUiModel
import com.sceyt.chatuikit.databinding.SceytItemPollOptionBinding
import com.sceyt.chatuikit.extensions.dpToPx
import com.sceyt.chatuikit.persistence.differs.PollOptionDiff
import com.sceyt.chatuikit.presentation.common.OverlapDecoration
import com.sceyt.chatuikit.styles.common.BackgroundStyle
import com.sceyt.chatuikit.styles.messages_list.item.PollStyle

open class PollOptionViewHolder(
    private val binding: SceytItemPollOptionBinding,
    private val pollStyle: PollStyle,
    private val isClosedProvider: () -> Boolean,
    private val isAnonymousProvider: () -> Boolean,
    private val bubbleBackgroundStyleProvider: () -> BackgroundStyle,
    private var onOptionClick: ((PollOptionUiModel) -> Unit)? = null,
) : RecyclerView.ViewHolder(binding.root) {
    private val context = binding.root.context
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

        root.isEnabled = !isClosed
        checkbox.isVisible = !isClosed

        if (diff.selectedChanged) {
            checkbox.isChecked = option.selected
        }

        if (diff.textChanged) {
            tvOptionText.text = option.text
        }

        if (diff.voteCountChanged || diff.totalVoteCountChanged) {
            tvVoteCount.text = pollStyle.voteCountFormatter.format(context, option)

            val percentage = option.getPercentage().toInt()
            val shouldAnimate =
                animate && percentage != currentProgress && diff != PollOptionDiff.DEFAULT

            progressBar.setProgress(percentage, animate = shouldAnimate)
            currentProgress = percentage
        }

        // Setup voters avatars (hide if anonymous)
        if (diff.votersChanged) {
            if (!isAnonymous && option.voters.isNotEmpty()) {
                if (votersAdapter == null) {
                    votersAdapter = VoterAvatarAdapter(
                        pollStyle = pollStyle,
                        bubbleBackgroundStyleProvider = bubbleBackgroundStyleProvider
                    )
                    rvVoters.itemAnimator = null
                    rvVoters.adapter = votersAdapter
                    rvVoters.addItemDecoration(OverlapDecoration(9.dpToPx()))
                }
                votersAdapter?.submitList(option.voters.take(2))
            } else {
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

