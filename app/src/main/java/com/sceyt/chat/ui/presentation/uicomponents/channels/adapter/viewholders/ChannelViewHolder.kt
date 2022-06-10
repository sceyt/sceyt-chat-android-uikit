package com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.viewholders

import android.content.res.ColorStateList
import android.graphics.Typeface
import androidx.core.view.isVisible
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chat.ui.R
import com.sceyt.chat.ui.data.models.channels.ChannelTypeEnum
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytDirectChannel
import com.sceyt.chat.ui.databinding.SceytItemChannelBinding
import com.sceyt.chat.ui.extensions.getCompatColorByTheme
import com.sceyt.chat.ui.extensions.getString
import com.sceyt.chat.ui.presentation.common.setMessageDateAndStatusIcon
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.chat.ui.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chat.ui.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.chat.ui.sceytconfigs.ChannelStyle
import com.sceyt.chat.ui.utils.DateTimeUtil

class ChannelViewHolder(private val binding: SceytItemChannelBinding,
                        private var listeners: ChannelClickListenersImpl) : BaseChannelViewHolder(binding.root) {

    private lateinit var channelItem: ChannelListItem.ChannelItem

    init {
        with(binding) {
            setChannelItemStyle()

            root.setOnClickListener {
                listeners.onChannelClick(channelItem)
            }

            root.setOnLongClickListener {
                listeners.onChannelLongClick(channelItem)
                return@setOnLongClickListener true
            }

            avatar.setOnClickListener {
                listeners.onAvatarClick(channelItem)
            }
        }
    }

    override fun bind(item: ChannelListItem, diff: ChannelItemPayloadDiff) {
        when (item) {
            is ChannelListItem.ChannelItem -> {
                channelItem = item

                val channel = item.channel
                val name: String = channel.channelSubject
                val url = channel.iconUrl

                with(binding) {
                    diff.run {
                        if (!hasDifference()) return@run

                        if (muteStateChanged)
                            setMuteState(channel)

                        if (subjectChanged)
                            channelTitle.text = channel.channelSubject

                        if (subjectChanged || avatarViewChanged)
                            avatar.setNameAndImageUrl(name, url)

                        if (lastMessageStatusChanged)
                            channel.lastMessage.setMessageDateAndStatusIcon(updateDate, getDateTxt(channel), false)

                        if (lastMessageChanged) {
                            setLastMessageText(channel)
                            setUnreadCount(channel.unreadCount)
                        }
                    }

                    setOnlineStatus(channel)
                }
            }
            ChannelListItem.LoadingMoreItem -> Unit
        }
    }

    private fun SceytItemChannelBinding.setMuteState(channel: SceytChannel) {
        if (channel.muted) {
            channelTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, ChannelStyle.mutedIcon, 0)
        } else {
            channelTitle.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    private fun SceytItemChannelBinding.setOnlineStatus(channel: SceytChannel?) {
        val isOnline = (channel?.channelType == ChannelTypeEnum.Direct)
                && (channel as? SceytDirectChannel)?.peer?.presence?.state == PresenceState.Online
        groupOnline.isVisible = isOnline
    }

    private fun SceytItemChannelBinding.setLastMessageText(channel: SceytChannel) {
        val message = channel.lastMessage
        if (message == null) {
            lastMessage.text = ""
            return
        }
        if (message.state == MessageState.Deleted) {
            lastMessage.text = itemView.context.getString(R.string.message_was_deleted)
            lastMessage.setTypeface(null, Typeface.ITALIC)
        } else {
            val body = if (message.body.isBlank() && !message.attachments.isNullOrEmpty())
                lastMessage.context.getString(R.string.attachment) else message.body

            val showText = if (!message.incoming) {
                lastMessage.getString(R.string.your_last_message).format(body.trim())
            } else body.trim()
            lastMessage.text = showText
            lastMessage.setTypeface(null, Typeface.NORMAL)
        }
    }

    private fun SceytItemChannelBinding.setUnreadCount(unreadCount: Long?) {
        if (unreadCount == null || unreadCount == 0L) {
            unreadMessagesCount.isVisible = false
            return
        }
        val title = if (unreadCount > 99L) {
            "99+"
        } else unreadCount.toString()

        unreadMessagesCount.apply {
            text = title
            isVisible = true
        }
    }

    private fun getDateTxt(channel: SceytChannel?): String {
        if (channel == null) return ""
        val lastMsgCreatedAt = channel.lastMessage?.createdAt
        return if (lastMsgCreatedAt != null && lastMsgCreatedAt != 0L)
            DateTimeUtil.getDateTimeString(lastMsgCreatedAt)
        else
            DateTimeUtil.getDateTimeString(channel.updatedAt)
    }

    private fun SceytItemChannelBinding.setChannelItemStyle() {
        with(root.context) {
            channelTitle.setTextColor(getCompatColorByTheme(ChannelStyle.titleColor))
            lastMessage.setTextColor(getCompatColorByTheme(ChannelStyle.lastMessageTextColor))
            unreadMessagesCount.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(ChannelStyle.unreadCountColor))
        }
    }
}