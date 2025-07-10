package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.differs.diff
import com.sceyt.chatuikit.presentation.common.AsyncListDiffer
import com.sceyt.chatuikit.presentation.common.ClickAvailableData
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.BaseChannelViewHolder
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.ChannelViewHolderFactory
import kotlinx.coroutines.CoroutineScope


class ChannelsAdapter(
        scope: CoroutineScope,
        private val viewHolderFactory: ChannelViewHolderFactory,
) : RecyclerView.Adapter<BaseChannelViewHolder>() {

    companion object {
        val clickAvailableData by lazy { ClickAvailableData(true) }
        val longClickAvailableData by lazy { ClickAvailableData(true) }

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChannelListItem>() {
            override fun areItemsTheSame(oldItem: ChannelListItem, newItem: ChannelListItem): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: ChannelListItem, newItem: ChannelListItem): Boolean {
                return when {
                    oldItem is ChannelListItem.ChannelItem && newItem is ChannelListItem.ChannelItem
                            && oldItem.channel.id == newItem.channel.id -> {
                        !oldItem.channel.diff(newItem.channel).hasDifference()
                    }

                    oldItem is ChannelListItem.LoadingMoreItem && newItem is ChannelListItem.LoadingMoreItem -> true
                    else -> false
                }
            }
        }
    }

    private val differ = AsyncListDiffer(adapter = this, diffCallback = DIFF_CALLBACK, scope = scope)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseChannelViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseChannelViewHolder, position: Int) {
        holder.bind(item = currentList[position], diff = ChannelDiff.DEFAULT)
    }

    override fun onBindViewHolder(holder: BaseChannelViewHolder, position: Int, payloads: MutableList<Any>) {
        val diff = payloads.find { it is ChannelDiff } as? ChannelDiff ?: ChannelDiff.DEFAULT
        holder.bind(item = currentList[position], diff)
    }

    override fun getItemCount(): Int = currentList.size

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(currentList[position], position)
    }

    override fun onViewAttachedToWindow(holder: BaseChannelViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseChannelViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    fun removeLoading(commitCallback: (() -> Unit)? = null) {
        differ.removeItem(commitCallback) { it is ChannelListItem.LoadingMoreItem }
    }

    fun notifyUpdate(
            channels: List<ChannelListItem>,
            commitCallback: (() -> Unit)? = null,
    ) {
        differ.submitList(channels, commitCallback)
    }

    fun addList(items: List<ChannelListItem>) {
        removeLoading {
            val filteredItems = items.minus(currentList.toSet())
            differ.addItems(filteredItems)
        }
    }

    val currentList get() = differ.currentList

    fun getSkip() = getChannels().size

    fun getChannels() = currentList.mapNotNull { it as? ChannelListItem.ChannelItem }

    fun deleteChannel(id: Long, commitCallback: (() -> Unit)? = null) {
        differ.removeItem(
            predicate = { it is ChannelListItem.ChannelItem && it.channel.id == id },
            commitCallback = commitCallback)
    }

    fun updateChannel(
            predicate: (ChannelListItem) -> Boolean,
            newItem: ChannelListItem,
            payloads: Any? = null,
            commitCallback: (() -> Unit)? = null,
    ) {
        differ.updateItem(predicate, newItem, payloads, commitCallback)
    }
}