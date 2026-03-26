package com.sceyt.chatuikit.presentation.components.shareable.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.presentation.common.recyclerview.AsyncListDiffer
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItemDiffCallback
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.BaseChannelViewHolder
import com.sceyt.chatuikit.presentation.components.shareable.adapter.holders.ShareableChannelViewHolderFactory
import kotlinx.coroutines.CoroutineScope

class ShareableChannelsAdapter(
    scope: CoroutineScope,
    private var viewHolderFactory: ShareableChannelViewHolderFactory,
) : RecyclerView.Adapter<BaseChannelViewHolder>() {

    private val differ = AsyncListDiffer(this, ChannelListItemDiffCallback, scope)

    val currentList get() = differ.currentList

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseChannelViewHolder {
        return viewHolderFactory.createViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: BaseChannelViewHolder, position: Int) {
        holder.bind(item = currentList[position], diff = ChannelDiff.DEFAULT)
    }

    override fun onBindViewHolder(
        holder: BaseChannelViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        val diff = payloads.find { it is ChannelDiff } as? ChannelDiff
        holder.bind(item = currentList[position], diff ?: ChannelDiff.DEFAULT)
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

    fun notifyUpdate(channels: List<ChannelListItem>) {
        differ.submitList(channels)
    }

    fun getSkip() = currentList.filterIsInstance<ChannelListItem.ChannelItem>().size

    fun getData() = currentList

    fun getChannels() = currentList.filterIsInstance<ChannelListItem.ChannelItem>()

    fun updateChannelSelectedState(selected: Boolean, channelItem: ChannelListItem.ChannelItem) {
        differ.updateItem(
            predicate = { it is ChannelListItem.ChannelItem && it.channel.id == channelItem.channel.id },
            newItem = ChannelListItem.ChannelItem(channelItem.channel).apply { this.selected = selected },
            payloads = Unit,
        )
    }

    fun setViewHolderFactory(viewHolderFactory: ShareableChannelViewHolderFactory) {
        this.viewHolderFactory = viewHolderFactory
    }
}