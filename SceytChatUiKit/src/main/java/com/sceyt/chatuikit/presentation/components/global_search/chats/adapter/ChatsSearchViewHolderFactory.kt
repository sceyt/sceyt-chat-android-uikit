package com.sceyt.chatuikit.presentation.components.global_search.chats.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchSectionBinding
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListenersImpl
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.holders.SearchChannelItemViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.holders.SearchMessageItemViewHolder
import com.sceyt.chatuikit.presentation.components.global_search.chats.adapter.holders.SearchSectionViewHolder
import com.sceyt.chatuikit.styles.search.GlobalSearchStyle

open class ChatsSearchViewHolderFactory(
    context: Context,
    protected val style: GlobalSearchStyle,
    onChannelClick: (SceytChannel) -> Unit,
    onMessageClick: (messageId: Long, channel: SceytChannel) -> Unit,
) {
    protected val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    protected open val channelClickListeners = ChannelClickListenersImpl().apply {
        setListener(ChannelClickListeners.ChannelClickListener { _, item ->
            onChannelClick(item.channel)
        })
    }
    protected open val onMessageClickListener: ((Long, SceytChannel) -> Unit)? = onMessageClick

    open fun createViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemType.Section.ordinal -> createSectionViewHolder(parent)
            ItemType.Channel.ordinal -> createChannelViewHolder(parent)
            ItemType.Message.ordinal -> createMessageViewHolder(parent)
            else -> throw RuntimeException("Not supported view type: $viewType")
        }
    }

    open fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: GlobalSearchListItem,
        query: String,
        showMessageChannel: Boolean,
    ) {
        when (holder) {
            is SearchSectionViewHolder -> holder.bind(
                item = item as GlobalSearchListItem.SectionHeader
            )

            is SearchChannelItemViewHolder -> holder.bind(
                item = item as GlobalSearchListItem.ChannelItem,
                diff = ChannelDiff.DEFAULT
            )

            is SearchMessageItemViewHolder -> holder.bind(
                item = item as GlobalSearchListItem.MessageItem,
                query = query,
                showMessageChannel = showMessageChannel
            )
        }
    }

    open fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: GlobalSearchListItem,
        query: String,
        showMessageChannel: Boolean,
        diff: ChannelDiff,
    ) {
        when (holder) {
            is SearchChannelItemViewHolder -> holder.bind(
                item = item as GlobalSearchListItem.ChannelItem,
                diff = diff
            )

            else -> onBindViewHolder(
                holder = holder,
                item = item,
                query = query,
                showMessageChannel = showMessageChannel
            )
        }
    }

    open fun createSectionViewHolder(
        parent: ViewGroup
    ): RecyclerView.ViewHolder = SearchSectionViewHolder(
        style = style,
        binding = SceytItemGlobalSearchSectionBinding.inflate(layoutInflater, parent, false)
    )

    open fun createChannelViewHolder(
        parent: ViewGroup
    ): RecyclerView.ViewHolder = SearchChannelItemViewHolder(
        binding = SceytItemChannelBinding.inflate(layoutInflater, parent, false),
        itemStyle = style.chatsPageStyle.channelItemStyle,
        listeners = channelClickListeners
    )


    open fun createMessageViewHolder(
        parent: ViewGroup
    ): RecyclerView.ViewHolder = SearchMessageItemViewHolder(
        style = style,
        binding = SceytItemGlobalSearchMessageBinding.inflate(layoutInflater, parent, false),
        onMessageClickListener = onMessageClickListener,
    )

    open fun getItemViewType(item: GlobalSearchListItem, position: Int): Int {
        return when (item) {
            is GlobalSearchListItem.SectionHeader -> ItemType.Section.ordinal
            is GlobalSearchListItem.ChannelItem -> ItemType.Channel.ordinal
            is GlobalSearchListItem.MessageItem -> ItemType.Message.ordinal
            else -> throw RuntimeException("Not supported item type: $item")
        }
    }

    enum class ItemType {
        Section, Channel, Message
    }
}
