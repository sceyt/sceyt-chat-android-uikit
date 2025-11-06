package com.sceyt.chatuikit.presentation.components.poll_results.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.diff.VoterItemPayloadDiff
import com.sceyt.chatuikit.presentation.components.poll_results.adapter.holders.VotersViewHolderFactory
import com.sceyt.chatuikit.presentation.root.BaseViewHolder

class VotersAdapter(
        private val viewHolderFactory: VotersViewHolderFactory
) : RecyclerView.Adapter<BaseViewHolder<VoterItem>>() {

    private val differ = AsyncListDiffer(this, VoterItemDiffCallback())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<VoterItem> {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VoterItem>, position: Int) {
        holder.bind(item = differ.currentList[position])
    }

    override fun onBindViewHolder(holder: BaseViewHolder<VoterItem>, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }

        val payload = payloads.firstOrNull() as? VoterItemPayloadDiff
        if (payload != null && !payload.hasDifference()) {
            return
        }

        holder.bind(differ.currentList[position])
    }

    override fun getItemCount(): Int = differ.currentList.size

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(differ.currentList[position])
    }

    fun getData(): MutableList<VoterItem> = differ.currentList

    fun submitList(data: List<VoterItem>) {
        differ.submitList(data)
    }

    fun getSkip(): Int {
        return differ.currentList.filterIsInstance<VoterItem.Voter>().size
    }

    private class VoterItemDiffCallback : DiffUtil.ItemCallback<VoterItem>() {
        override fun areItemsTheSame(oldItem: VoterItem, newItem: VoterItem): Boolean {
            return when {
                oldItem is VoterItem.HeaderItem && newItem is VoterItem.HeaderItem ->
                    true

                oldItem is VoterItem.Voter && newItem is VoterItem.Voter ->
                    oldItem.vote.user?.id == newItem.vote.user?.id

                else -> oldItem == newItem
            }
        }

        override fun areContentsTheSame(oldItem: VoterItem, newItem: VoterItem): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: VoterItem, newItem: VoterItem): Any? {
            return when {
                oldItem is VoterItem.Voter && newItem is VoterItem.Voter -> {
                    VoterItemPayloadDiff(
                        userChanged = oldItem.vote.user != newItem.vote.user,
                        createdAtChanged = oldItem.vote.createdAt != newItem.vote.createdAt
                    )
                }

                else -> Any()
            }
        }
    }
}