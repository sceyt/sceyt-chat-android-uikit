package com.sceyt.chatuikit.presentation.components.create_poll.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sceyt.chatuikit.databinding.SceytItemCreatePollOptionBinding
import com.sceyt.chatuikit.presentation.components.create_poll.PollOptionItem
import com.sceyt.chatuikit.styles.create_poll.CreatePollStyle
import java.util.Collections

class CreatePollOptionsAdapter(
        private val style: CreatePollStyle,
        private val onTextChanged: (PollOptionItem, String) -> Unit,
        private val onRemoveClick: (PollOptionItem) -> Unit,
        private val onOptionMoved: (Int, Int) -> Unit,
        private val onNextClick: (PollOptionItem) -> Unit,
        private val onOptionClick: (EditText, PollOptionItem) -> Unit,
) : ListAdapter<PollOptionItem, PollOptionViewHolder>(PollOptionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollOptionViewHolder {
        val binding = SceytItemCreatePollOptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PollOptionViewHolder(
            binding = binding,
            style = style,
            onTextChanged = onTextChanged,
            onRemoveClick = onRemoveClick,
            onNextClick = onNextClick,
            onOptionClick = onOptionClick
        )
    }

    override fun onBindViewHolder(holder: PollOptionViewHolder, position: Int) {
        val option = getItem(position)
        holder.bind(option)
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

    private class PollOptionDiffCallback : DiffUtil.ItemCallback<PollOptionItem>() {
        override fun areItemsTheSame(oldItem: PollOptionItem, newItem: PollOptionItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: PollOptionItem, newItem: PollOptionItem): Boolean {
            return oldItem.id == newItem.id && oldItem.isCurrent == newItem.isCurrent
        }

        override fun getChangePayload(oldItem: PollOptionItem, newItem: PollOptionItem): Any {
            return Any()
        }
    }
}

