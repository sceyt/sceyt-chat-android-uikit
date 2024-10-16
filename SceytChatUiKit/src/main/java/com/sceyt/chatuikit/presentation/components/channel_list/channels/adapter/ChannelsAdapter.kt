package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.extensions.asComponentActivity
import com.sceyt.chatuikit.extensions.awaitAnimationEnd
import com.sceyt.chatuikit.extensions.dispatchUpdatesToSafety
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.presentation.common.ClickAvailableData
import com.sceyt.chatuikit.presentation.common.SyncArrayList
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.BaseChannelViewHolder
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.ChannelViewHolderFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class ChannelsAdapter(private var channels: SyncArrayList<ChannelListItem>,
                      private var viewHolderFactory: ChannelViewHolderFactory) :
        RecyclerView.Adapter<BaseChannelViewHolder>() {

    companion object {
        val clickAvailableData by lazy { ClickAvailableData(true) }
        val longClickAvailableData by lazy { ClickAvailableData(true) }
    }

    private var updateJob: Job? = null
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

    fun removeLoading() {
        if (channels.remove(mLoadingItem))
            notifyItemRemoved(channels.lastIndex + 1)
    }

    fun notifyUpdate(channels: List<ChannelListItem>, recyclerView: RecyclerView) {
        updateJob?.cancel()
        updateJob = recyclerView.context.asComponentActivity().lifecycleScope.launch {
            recyclerView.awaitAnimationEnd {
                val myDiffUtil = ChannelsDiffUtil(this@ChannelsAdapter.channels, channels)
                val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
                productDiffResult.dispatchUpdatesToSafety(recyclerView)
                this@ChannelsAdapter.channels.clear()
                this@ChannelsAdapter.channels.addAll(channels)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addList(items: List<ChannelListItem>) {
        removeLoading()

        val filteredItems = items.minus(channels.toSet())
        channels.addAll(filteredItems)

        if (channels.size == filteredItems.size)
            notifyDataSetChanged()
        else
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
}