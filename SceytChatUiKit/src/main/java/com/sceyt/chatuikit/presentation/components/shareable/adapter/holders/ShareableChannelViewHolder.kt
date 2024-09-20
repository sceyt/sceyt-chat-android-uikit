package com.sceyt.chatuikit.presentation.components.shareable.adapter.holders

import android.widget.TextView
import androidx.core.view.isVisible
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Direct
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Group
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Public
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemShareChannelBinding
import com.sceyt.chatuikit.extensions.getPresentableNameCheckDeleted
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.formatters.UserNameFormatter
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders.BaseChannelViewHolder
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.extensions.setChannelAvatar

open class ShareableChannelViewHolder(private val binding: SceytItemShareChannelBinding,
                                      private val clickListener: ChannelClickListeners.ChannelClickListener,
                                      private val userNameFormatter: UserNameFormatter?) : BaseChannelViewHolder(binding.root) {

    override fun bind(item: ChannelListItem, diff: ChannelDiff) {
        super.bind(item, diff)

        val channel = (item as? ChannelListItem.ChannelItem)?.channel ?: return
        binding.avatar.setChannelAvatar(channel)
        setSubject(channel, binding.userName)

        binding.checkbox.isChecked = item.selected

        binding.tvMemberCount.apply {
            val membersText = when (channel.getChannelType()) {
                Group -> {
                    if (channel.memberCount > 0)
                        getString(R.string.sceyt_members_count, channel.memberCount)
                    else getString(R.string.sceyt_member_count, channel.memberCount)
                }

                Public -> {
                    if (channel.memberCount > 0)
                        getString(R.string.sceyt_subscribers_count, channel.memberCount)
                    else getString(R.string.sceyt_subscriber_count, channel.memberCount)
                }

                Direct -> null
            }
            text = membersText
            isVisible = channel.isGroup
        }

        binding.root.setOnClickListener {
            clickListener.onChannelClick(item)
        }
    }

    open fun setSubject(channel: SceytChannel, textView: TextView) {
        textView.text = if (channel.isGroup) channel.channelSubject
        else {
            channel.getPeer()?.user?.let { from ->
                userNameFormatter?.format(from) ?: from.getPresentableNameCheckDeleted(context)
            }
        }
    }
}