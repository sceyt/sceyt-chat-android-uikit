package com.sceyt.chatuikit.presentation.uicomponents.sharebaleactivity.adapter.viewholders

import android.widget.TextView
import androidx.core.view.isVisible
import com.sceyt.chat.models.user.User
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Broadcast
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Direct
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Group
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Private
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum.Public
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemShareChannelBinding
import com.sceyt.chatuikit.extensions.getPresentableNameCheckDeleted
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.extensions.getChannelType
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isPeerDeleted
import com.sceyt.chatuikit.presentation.customviews.SceytAvatarView
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.viewholders.BaseChannelViewHolder
import com.sceyt.chatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chatuikit.sceytstyles.UserStyle

open class ShareableChannelViewHolder(private val binding: SceytItemShareChannelBinding,
                                      private val clickListener: ChannelClickListeners.ChannelClickListener,
                                      private val userNameBuilder: ((User) -> String)?) : BaseChannelViewHolder(binding.root) {

    override fun bind(item: ChannelListItem, diff: ChannelDiff) {
        super.bind(item, diff)

        val channel = (item as? ChannelListItem.ChannelItem)?.channel ?: return
        setAvatar(channel, channel.channelSubject, channel.iconUrl, binding.avatar)
        setSubject(channel, binding.userName)

        binding.checkbox.isChecked = item.selected

        binding.tvMemberCount.apply {
            val membersText = when (channel.getChannelType()) {
                Private, Group -> {
                    if (channel.memberCount > 0)
                        getString(R.string.sceyt_members_count, channel.memberCount)
                    else getString(R.string.sceyt_member_count, channel.memberCount)
                }

                Public, Broadcast -> {
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

    open fun setAvatar(channel: SceytChannel, name: String, url: String?, avatar: SceytAvatarView) {
        if (channel.isPeerDeleted()) {
            binding.avatar.setImageUrl(null, UserStyle.deletedUserAvatar)
        } else
            binding.avatar.setNameAndImageUrl(name, url, if (channel.isGroup) 0 else UserStyle.userDefaultAvatar)
    }

    open fun setSubject(channel: SceytChannel, textView: TextView) {
        textView.text = if (channel.isGroup) channel.channelSubject
        else {
            channel.getPeer()?.user?.let { from ->
                userNameBuilder?.invoke(from) ?: from.getPresentableNameCheckDeleted(context)
            }
        }
    }
}