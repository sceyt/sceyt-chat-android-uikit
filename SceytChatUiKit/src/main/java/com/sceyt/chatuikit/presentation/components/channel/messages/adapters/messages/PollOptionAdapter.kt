package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytPoll
import com.sceyt.chatuikit.databinding.SceytItemPollOptionBinding
import com.sceyt.chatuikit.persistence.differs.PollOptionDiff
import com.sceyt.chatuikit.persistence.differs.diff


class PollOptionAdapter(
        poll: SceytPoll,
        private val onOptionClick: ((PollOption) -> Unit)? = null,
) : ListAdapter<PollOption, PollOptionAdapter.PollOptionViewHolder>(PollOptionDiffCallback()) {
    private var totalVotes: Int = poll.totalVotes
    private val isAnonymous: Boolean = poll.anonymous
    private val isClosed: Boolean = poll.closed
    private var shouldAnimate = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollOptionViewHolder {
        val binding = SceytItemPollOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PollOptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PollOptionViewHolder, position: Int) {
        holder.bind(getItem(position), PollOptionDiff.DEFAULT, animate = shouldAnimate)
    }

    override fun onBindViewHolder(holder: PollOptionViewHolder, position: Int, payloads: MutableList<Any>) {
        val diff = payloads.find { it is PollOptionDiff } as? PollOptionDiff
                ?: PollOptionDiff.DEFAULT
        holder.bind(getItem(position), diff, animate = shouldAnimate)
    }

    fun submitListWithAnimation(newOptions: List<PollOption>, newTotalVotes: Int? = null, animate: Boolean = false) {
        newTotalVotes?.let { totalVotes = it }
        shouldAnimate = animate
        submitList(newOptions)
    }

    inner class PollOptionViewHolder(
            private val binding: SceytItemPollOptionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentProgress = 0
        private var progressAnimator: ObjectAnimator? = null
        private var votersAdapter: VoterAvatarAdapter? = null

        fun bind(option: PollOption, diff: PollOptionDiff, animate: Boolean = false) {
            with(binding) {
                if (diff.selectedChanged) {
                    checkbox.isChecked = option.selected
                }

                if (diff.textChanged) {
                    tvOptionText.text = option.text
                }

                if (diff.voteCountChanged) {
                    tvVoteCount.text = option.voteCount.toString()
                    tvVoteCount.isVisible = option.voteCount > 0

                    val percentage = option.getPercentage(totalVotes).toInt()

                    if (animate && percentage != currentProgress && currentProgress > 0) {
                        // Only animate if we have a valid previous progress
                        animateProgress(currentProgress, percentage)
                    } else {
                        // Set progress immediately without animation
                        progressBar.progress = percentage
                        currentProgress = percentage
                    }
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
                    }
                }

                // Disable clicking if poll is closed (only set once)
                if (diff == PollOptionDiff.DEFAULT) {
                    root.isEnabled = !isClosed
                    root.alpha = if (isClosed) 0.6f else 1.0f

                    root.setOnClickListener {
                        if (!isClosed) {
                            onOptionClick?.invoke(option)
                        }
                    }
                }
            }
        }

        private fun animateProgress(from: Int, to: Int) {
            progressAnimator?.cancel()
            // Use ObjectAnimator for smoother animation
            progressAnimator = ObjectAnimator.ofInt(binding.progressBar, "progress", from, to).apply {
                duration = 300 // Longer duration for smoother animation
                addUpdateListener { animator ->
                    currentProgress = animator.animatedValue as Int
                }
                start()
            }
        }
    }

    private class PollOptionDiffCallback : DiffUtil.ItemCallback<PollOption>() {
        override fun areItemsTheSame(oldItem: PollOption, newItem: PollOption): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PollOption, newItem: PollOption): Boolean {
            return oldItem.diff(newItem).hasDifference().not()
        }

        override fun getChangePayload(oldItem: PollOption, newItem: PollOption): Any {
            return oldItem.diff(newItem)
        }
    }
}

