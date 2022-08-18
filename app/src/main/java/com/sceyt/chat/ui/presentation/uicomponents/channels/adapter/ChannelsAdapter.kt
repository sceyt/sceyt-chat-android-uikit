package com.sceyt.chat.ui.presentation.uicomponents.channels.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.BaseChannelViewHolder
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders.ChannelViewHolderFactory


class ChannelsAdapter(private var channels: ArrayList<ChannelListItem>,
                      private var viewHolderFactory: ChannelViewHolderFactory) :
        RecyclerView.Adapter<BaseChannelViewHolder>() {

    private val mLoadingItem by lazy { ChannelListItem.LoadingMoreItem }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseChannelViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseChannelViewHolder, position: Int) {
        holder.bind(item = channels[position], diff = ChannelItemPayloadDiff.DEFAULT)
    }

    override fun onBindViewHolder(holder: BaseChannelViewHolder, position: Int, payloads: MutableList<Any>) {
        val diff = payloads.find { it is ChannelItemPayloadDiff } as? ChannelItemPayloadDiff
                ?: ChannelItemPayloadDiff.DEFAULT
        holder.bind(item = channels[position], diff)
    }

    override fun getItemCount(): Int = channels.size

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(channels[position], position)
    }

    private fun removeLoading() {
        if (channels.remove(mLoadingItem))
            notifyItemRemoved(channels.lastIndex + 1)
    }

    fun notifyUpdate(channels: List<ChannelListItem>) {
        val myDiffUtil = ChannelsDiffUtil(this.channels, channels)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        this.channels = channels as ArrayList
        productDiffResult.dispatchUpdatesTo(this)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addList(items: MutableList<ChannelListItem>) {
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