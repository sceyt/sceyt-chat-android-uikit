package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.messages.PollOption
import com.sceyt.chatuikit.data.models.messages.SceytPoll
import com.sceyt.chatuikit.databinding.SceytItemPollOptionBinding
import com.sceyt.chatuikit.persistence.differs.PollOptionDiff
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.styles.messages_list.item.PollStyle

class PollOptionAdapter(
        private var poll: SceytPoll,
        private val pollStyle: PollStyle,
        private val onOptionClick: ((PollOption) -> Unit)? = null,
) : ListAdapter<PollOption, PollOptionAdapter.PollOptionViewHolder>(PollOptionDiffCallback()) {
    private val totalVotes: Int get() = poll.totalVotes
    private val isAnonymous: Boolean get() = poll.anonymous
    private val isClosed: Boolean get() = poll.closed
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

    override fun getItemId(position: Int): Long {
        return getItem(position).id.hashCode().toLong()
    }

    override fun onViewDetachedFromWindow(holder: PollOptionViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    fun updatePoll(
            poll: SceytPoll,
            animate: Boolean,
    ) {
        this.poll = poll
        shouldAnimate = animate
        submitList(poll.options)
    }

    inner class PollOptionViewHolder(
            private val binding: SceytItemPollOptionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var currentProgress = 0
        private var progressAnimator: ObjectAnimator? = null
        private var votersAdapter: VoterAvatarAdapter? = null

        init {
            applyStyle()
            binding.root.setOnClickListener {
                if (!isClosed) {
                    val position = bindingAdapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        onOptionClick?.invoke(getItem(position))
                    }
                }
            }
        }

        fun bind(
                option: PollOption,
                diff: PollOptionDiff,
                animate: Boolean = false,
        ) = with(binding) {
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

                if (animate && percentage != currentProgress && diff != PollOptionDiff.DEFAULT) {
                    animateProgress(currentProgress, percentage)
                } else {
                    // Set progress immediately without animation
                    progressAnimator?.cancel()
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
                    votersAdapter?.submitList(emptyList())
                }
            }
        }

        fun onViewDetachedFromWindow() {
            progressAnimator?.cancel()
        }

        private fun animateProgress(from: Int, to: Int) {
            progressAnimator?.cancel()
            progressAnimator = ObjectAnimator.ofInt(
                binding.progressBar,
                "progress",
                from, to
            ).apply {
                duration = 300
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    currentProgress = animator.animatedValue as Int
                }
                start()
            }
        }

        private fun applyStyle() = with(binding) {
            pollStyle.optionTextStyle.apply(tvOptionText)
            pollStyle.voteCountTextStyle.apply(tvVoteCount)
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

