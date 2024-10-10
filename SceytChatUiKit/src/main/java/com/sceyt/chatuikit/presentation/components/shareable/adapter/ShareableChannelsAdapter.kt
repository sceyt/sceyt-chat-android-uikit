package com.sceyt.chatuikit.presentation.components.shareable.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.extensions.dispatchUpdatesToSafety
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsDiffUtil
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.BaseChannelViewHolder
import com.sceyt.chatuikit.presentation.components.shareable.adapter.holders.ShareableChannelViewHolderFactory

class ShareableChannelsAdapter(
        private var channels: MutableList<ChannelListItem>,
        private var viewHolderFactory: ShareableChannelViewHolderFactory
) : RecyclerView.Adapter<BaseChannelViewHolder>() {
    private val mLoadingItem by lazy { ChannelListItem.LoadingMoreItem }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseChannelViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseChannelViewHolder, position: Int) {
        holder.bind(item = channels[position], diff = ChannelDiff.DEFAULT)
    }

    override fun onBindViewHolder(holder: BaseChannelViewHolder, position: Int, payloads: MutableList<Any>) {
        val diff = payloads.find { it is ChannelDiff } as? ChannelDiff
                ?: ChannelDiff.DEFAULT
        holder.bind(item = channels[position], diff)
    }

    override fun getItemCount(): Int = channels.size

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(channels[position], position)
    }

    override fun onViewAttachedToWindow(holder: BaseChannelViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: BaseChannelViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.onViewDetachedFromWindow()
    }

    private fun removeLoading() {
        if (channels.remove(mLoadingItem))
            notifyItemRemoved(channels.lastIndex + 1)
    }

    fun notifyUpdate(channels: List<ChannelListItem>, recyclerView: RecyclerView) {
        val myDiffUtil = ChannelsDiffUtil(this.channels, channels)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        productDiffResult.dispatchUpdatesToSafety(recyclerView)
        this.channels = channels.toMutableList()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addList(items: MutableList<ChannelListItem>) {
        removeLoading()

        val filteredItems = items.minus(channels.toSet())

        if (filteredItems.find { it is ChannelListItem.ChannelItem } == null)
            return

        channels.addAll(filteredItems)
        notifyItemRangeInserted(channels.size - filteredItems.size, filteredItems.size)
    }

    fun getSkip() = channels.filter { it !is ChannelListItem.LoadingMoreItem }.size

    fun getData() = channels

    fun getChannels() = channels
        .filter { it !is ChannelListItem.LoadingMoreItem }
        .map { it as ChannelListItem.ChannelItem }

    fun deleteChannel(id: Long) {
        getChannels().forEachIndexed { index, channelItem ->
            if (channelItem.channel.id == id) {
                channels.removeAt(index)
                notifyItemRemoved(index)
                return@forEachIndexed
            }
        }
    }

    fun updateChannelSelectedState(selected: Boolean, channelItem: ChannelListItem.ChannelItem) {
        val index = channels.indexOf(channelItem)
        if (index >= 0) {
            channelItem.selected = selected
            notifyItemChanged(index, Unit)
        }
    }

    fun setViewHolderFactory(viewHolderFactory: ShareableChannelViewHolderFactory) {
        this.viewHolderFactory = viewHolderFactory
    }
}