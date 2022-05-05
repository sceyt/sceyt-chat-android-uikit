package com.sceyt.chat.ui.presentation.channels.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chat.ui.presentation.channels.adapter.viewholders.BaseChannelViewHolder
import com.sceyt.chat.ui.utils.MyDiffUtil

class ChannelsAdapter(private var channels: ArrayList<ChannelListItem>,
                      private var viewHolderFactory: ChannelViewHolderFactory) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mLoadingItem by lazy { ChannelListItem.LoadingMoreItem }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as BaseChannelViewHolder).bindViews(item = channels[position])
    }

    override fun getItemCount(): Int = channels.size

    override fun getItemViewType(position: Int): Int {
        return viewHolderFactory.getItemViewType(channels[position])
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        (holder as BaseChannelViewHolder).onViewDetachedFromWindow()
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder as BaseChannelViewHolder).onViewAttachedFromWindow()
    }

    private fun removeLoading() {
        if (channels.remove(mLoadingItem))
            notifyItemRemoved(channels.lastIndex + 1)
    }

    fun addLoadingItem() {
        if (channels.contains(mLoadingItem)) return
        channels.add(mLoadingItem)
        notifyItemInserted(channels.size - 1)
    }

    fun notifyUpdate(channels: List<ChannelListItem>) {
        val myDiffUtil = MyDiffUtil(this.channels, channels)
        val productDiffResult = DiffUtil.calculateDiff(myDiffUtil, true)
        this.channels.clear()
        this.channels.addAll(channels)
        productDiffResult.dispatchUpdatesTo(this)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addList(items: MutableList<ChannelListItem>) {
        removeLoading()
        channels.addAll(items)
        if (channels.size == items.size)
            notifyDataSetChanged()
        else
            notifyItemRangeInserted(channels.size - items.size, items.size)
    }

    fun getSkip() = channels.filter { it !is ChannelListItem.LoadingMoreItem }.size
}