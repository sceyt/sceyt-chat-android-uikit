package com.sceyt.chatuikit.presentation.components.channel.messages.adapters.messages

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.sceyt.chatuikit.data.models.messages.PollOptionUiModel
import com.sceyt.chatuikit.persistence.differs.PollOptionDiff
import com.sceyt.chatuikit.persistence.differs.diff

class PollOptionAdapter(
        private val viewHolderFactory: PollOptionViewHolderFactory,
) : ListAdapter<PollOptionUiModel, PollOptionViewHolder>(DIFF_CALLBACK) {
    private var shouldAnimate = false

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PollOptionViewHolder {
        return viewHolderFactory.createViewHolder(parent)
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

    fun submitData(
            options: List<PollOptionUiModel>,
            animate: Boolean,
    ) {
        shouldAnimate = animate
        submitList(options)
    }

    companion object {

        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PollOptionUiModel>() {
            override fun areItemsTheSame(oldItem: PollOptionUiModel, newItem: PollOptionUiModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PollOptionUiModel, newItem: PollOptionUiModel): Boolean {
                return oldItem.diff(newItem).hasDifference().not()
            }

            override fun getChangePayload(oldItem: PollOptionUiModel, newItem: PollOptionUiModel): Any {
                return oldItem.diff(newItem)
            }
        }
    }
}

