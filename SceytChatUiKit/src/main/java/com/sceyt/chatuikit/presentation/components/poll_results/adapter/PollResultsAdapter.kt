package com.sceyt.chatuikit.presentation.components.poll_results.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.diff.PollResultItemPayloadDiff
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders.PollOptionResultViewHolder
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders.PollResultsViewHolderFactory
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class PollResultsAdapter(
        private val viewHolderFactory: PollResultsViewHolderFactory
) : RecyclerView.Adapter<BaseViewHolder<PollResultItem>>() {

    private val differ = AsyncListDiffer(this, PollResultItemDiffCallback())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<PollResultItem> {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<PollResultItem>, position: Int) {
        holder.bind(item = differ.currentList[position])

        if (holder is PollOptionResultViewHolder) {
            val isLastPollOption = position == differ.currentList.size - 1
            holder.setIsLastItem(isLastPollOption)
        }
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(differ.currentList[position])
    }

    fun getData(): MutableList<PollResultItem> = differ.currentList

    fun submitList(data: List<PollResultItem>) {
        differ.submitList(data)
    }

    private class PollResultItemDiffCallback : DiffUtil.ItemCallback<PollResultItem>() {
        override fun areItemsTheSame(oldItem: PollResultItem, newItem: PollResultItem): Boolean {
            return when {
                oldItem is PollResultItem.HeaderItem && newItem is PollResultItem.HeaderItem ->
                    oldItem.poll.id == newItem.poll.id

                oldItem is PollResultItem.PollOptionItem && newItem is PollResultItem.PollOptionItem ->
                    oldItem.pollOption.id == newItem.pollOption.id

                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: PollResultItem, newItem: PollResultItem): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: PollResultItem, newItem: PollResultItem): Any? {
            return when {
                oldItem is PollResultItem.PollOptionItem && newItem is PollResultItem.PollOptionItem -> {
                    PollResultItemPayloadDiff(
                        optionChanged = oldItem.pollOption != newItem.pollOption,
                        voteCountChanged = oldItem.voteCount != newItem.voteCount,
                        votersChanged = oldItem.voters != newItem.voters
                    )
                }

                else -> null
            }
        }
    }
}