package com.sceyt.chatuikit.presentation.components.create_poll.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sceyt.chatuikit.databinding.SceytItemPollOptionBinding
import com.sceyt.chatuikit.presentation.components.create_poll.PollOption
import com.sceyt.chatuikit.styles.create_poll.CreatePollStyle
import java.util.Collections

class PollOptionsAdapter(
        private val style: CreatePollStyle,
        private val onTextChanged: (PollOption, String) -> Unit,
        private val onRemoveClick: (PollOption) -> Unit,
        private val onOptionMoved: (Int, Int) -> Unit,
) : ListAdapter<PollOption, PollOptionViewHolder>(PollOptionDiffCallback()) {

    private val minOptionsCount = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollOptionViewHolder {
        val binding = SceytItemPollOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PollOptionViewHolder(binding, style, onTextChanged, onRemoveClick)
    }

    override fun onBindViewHolder(holder: PollOptionViewHolder, position: Int) {
        val option = getItem(position)
        val canRemove = currentList.size > minOptionsCount
        holder.bind(option, canRemove)
    }

    override fun onViewRecycled(holder: PollOptionViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val list = currentList.toMutableList()
        if (fromPosition in list.indices && toPosition in list.indices) {
            Collections.swap(list, fromPosition, toPosition)
            submitList(list)
            onOptionMoved(fromPosition, toPosition)
        }
    }

    private class PollOptionDiffCallback : DiffUtil.ItemCallback<PollOption>() {
        override fun areItemsTheSame(oldItem: PollOption, newItem: PollOption): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PollOption, newItem: PollOption): Boolean {
            return oldItem == newItem
        }
    }
}

