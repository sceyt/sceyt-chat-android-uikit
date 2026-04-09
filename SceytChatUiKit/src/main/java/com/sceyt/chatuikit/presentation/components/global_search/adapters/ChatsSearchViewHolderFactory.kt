package com.sceyt.chatuikit.presentation.components.global_search.adapters

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchMessageBinding
import com.sceyt.chatuikit.databinding.SceytItemGlobalSearchSectionBinding
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.mappers.createEmptyUser
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.ChannelViewHolder
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListenersImpl
import com.sceyt.chatuikit.presentation.components.global_search.GlobalSearchListItem
import com.sceyt.chatuikit.presentation.components.global_search.displayName
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
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

    open fun createSectionViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return SectionViewHolder(SceytItemGlobalSearchSectionBinding.inflate(layoutInflater, parent, false))
    }

    open fun createChannelViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return ChannelItemViewHolder(
            ChannelViewHolder(
                binding = SceytItemChannelBinding.inflate(layoutInflater, parent, false),
                itemStyle = style.channelItemStyle,
                listeners = channelClickListeners
            )
        )
    }

    open fun createMessageViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        return MessageItemViewHolder(SceytItemGlobalSearchMessageBinding.inflate(layoutInflater, parent, false))
    }

    open fun getItemViewType(item: GlobalSearchListItem, position: Int): Int {
        return when (item) {
            is GlobalSearchListItem.SectionHeader -> ItemType.Section.ordinal
            is GlobalSearchListItem.ChannelItem -> ItemType.Channel.ordinal
            is GlobalSearchListItem.MessageItem -> ItemType.Message.ordinal
            else -> throw RuntimeException("Not supported item type: $item")
        }
    }

    open fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        item: GlobalSearchListItem,
        query: String,
        showMessageChannel: Boolean,
    ) {
        when (holder) {
            is SectionViewHolder -> holder.bind(item as GlobalSearchListItem.SectionHeader)
            is ChannelItemViewHolder -> holder.bind(item as GlobalSearchListItem.ChannelItem)
            is MessageItemViewHolder -> holder.bind(item as GlobalSearchListItem.MessageItem, query, showMessageChannel)
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
            is ChannelItemViewHolder -> holder.bind(item as GlobalSearchListItem.ChannelItem, diff)
            else -> onBindViewHolder(holder, item, query, showMessageChannel)
        }
    }

    enum class ItemType {
        Section, Channel, Message
    }

    protected fun highlight(text: String, query: String): CharSequence {
        if (query.isBlank() || text.isBlank()) return text
        val spannable = SpannableStringBuilder(text)
        val queryLower = query.lowercase()
        val textLower = text.lowercase()
        var start = textLower.indexOf(queryLower)
        while (start >= 0) {
            val end = start + queryLower.length
            spannable.setSpan(ForegroundColorSpan(style.highlightTextColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            start = textLower.indexOf(queryLower, end)
        }
        return spannable
    }

    inner class SectionViewHolder(
        private val binding: SceytItemGlobalSearchSectionBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GlobalSearchListItem.SectionHeader) {
            style.sectionTextStyle.apply(binding.root)
            binding.root.setText(item.titleRes)
        }
    }

    inner class ChannelItemViewHolder(
        private val holder: ChannelViewHolder,
    ) : RecyclerView.ViewHolder(holder.itemView) {
        fun bind(item: GlobalSearchListItem.ChannelItem, diff: ChannelDiff = ChannelDiff.DEFAULT) {
            holder.bind(ChannelListItem.ChannelItem(item.channel), diff)
        }
    }

    inner class MessageItemViewHolder(
        private val binding: SceytItemGlobalSearchMessageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GlobalSearchListItem.MessageItem, query: String, showMessageChannel: Boolean) {
            val result = item.result
            val user = result.message.user
                ?: createEmptyUser(result.channel.id.toString(), channelTitle(result.channel))

            SceytChatUIKit.renderers.userAvatarRenderer.render(
                binding.root.context,
                user,
                style.avatarStyle,
                binding.avatarView
            )

            style.titleTextStyle.apply(binding.tvTitle)
            style.subtitleTextStyle.apply(binding.tvBody)
            style.metaTextStyle.apply(binding.tvDate)
            style.metaTextStyle.apply(binding.tvChannel)

            binding.tvTitle.text = user.displayName()
            binding.tvDate.text = DateTimeUtil.getDateTimeString(result.message.createdAt)
            binding.tvBody.text = highlight(result.message.body, query)
            binding.tvBody.maxLines = if (showMessageChannel) 2 else 1
            binding.tvChannel.isVisible = showMessageChannel
            binding.tvChannel.text = channelTitle(result.channel)

            itemView.setOnClickListener {
                onMessageClickListener?.invoke(result.message.id, result.channel)
            }
        }

        private fun channelTitle(channel: SceytChannel): String {
            return SceytChatUIKit.formatters.channelNameFormatter
                .format(itemView.context, channel).toString()
        }
    }
}
