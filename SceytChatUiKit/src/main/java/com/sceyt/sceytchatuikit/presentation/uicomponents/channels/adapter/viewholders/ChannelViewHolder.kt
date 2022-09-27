package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders

import android.content.res.ColorStateList
import android.graphics.Typeface
import androidx.core.view.isVisible
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytDirectChannel
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelBinding
import com.sceyt.sceytchatuikit.extensions.getCompatColorByTheme
import com.sceyt.sceytchatuikit.extensions.getPresentableFirstName
import com.sceyt.sceytchatuikit.extensions.getString
import com.sceyt.sceytchatuikit.presentation.common.setMessageDateAndStatusIcon
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListenersImpl
import com.sceyt.sceytchatuikit.sceytconfigs.AvatarStyle
import com.sceyt.sceytchatuikit.sceytconfigs.ChannelStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil

class ChannelViewHolder(private val binding: SceytItemChannelBinding,
                        private var listeners: ChannelClickListenersImpl) : BaseChannelViewHolder(binding.root) {

    private lateinit var channelItem: ChannelListItem.ChannelItem

    init {
        with(binding) {
            setChannelItemStyle()

            root.setOnClickListener {
                listeners.onChannelClick(channelItem)
            }

            parentLayout.setOnLongClickListener {
                listeners.onChannelLongClick(it, channelItem)
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
                            avatar.setNameAndImageUrl(name, url, if (channel.isGroup) 0 else AvatarStyle.userDefaultAvatar)

                        if (lastMessageStatusChanged)
                            channel.lastMessage.setMessageDateAndStatusIcon(dateStatus, getDateTxt(channel), false)

                        if (lastMessageChanged)
                            setLastMessagedText(channel)

                        if (unreadCountChanged)
                            setUnreadCount(channel.unreadMessageCount)

                        if (onlineStateChanged)
                            setOnlineStatus(channel)
                    }
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
                && (channel as? SceytDirectChannel)?.peer?.user?.presence?.state == PresenceState.Online
        onlineStatus.isVisible = isOnline
    }

    /* @SuppressLint("SetTextI18n")
     private fun SceytItemChannelBinding.setLastMessageText(channel: SceytChannel) {
         val message = channel.lastMessage
         if (message == null) {
             lastMessage.text = ""
             return
         }
         if (message.state == MessageState.Deleted) {
             lastMessage.text = itemView.context.getString(R.string.sceyt_message_was_deleted)
             lastMessage.setTypeface(null, Typeface.ITALIC)
         } else {
             val body = if (message.body.isBlank() && !message.attachments.isNullOrEmpty())
                 lastMessage.context.getString(R.string.sceyt_attachment) else message.body

             tvYou.isVisible = if (message.incoming) {
                 val userFirstName = channel.lastMessage?.from?.getPresentableFirstName()?.trim()
                 if (channel.isGroup && !userFirstName.isNullOrBlank()) {
                     tvYou.text = "$userFirstName:"
                     true
                 } else false
             } else {
                 tvYou.text = "${root.getString(R.string.sceyt_your_last_message)}:"
                 true
             }
             lastMessage.text = body.trim()
             lastMessage.setTypeface(null, Typeface.NORMAL)
         }
     }*/

    private fun SceytItemChannelBinding.setLastMessagedText(channel: SceytChannel) {
        val message = channel.lastMessage
        if (message == null) {
            lastMessage.text = ""
            return
        }
        if (message.state == MessageState.Deleted) {
            lastMessage.text = itemView.context.getString(R.string.sceyt_message_was_deleted)
            lastMessage.setTypeface(null, Typeface.ITALIC)
        } else {
            val body = if (message.body.isBlank() && !message.attachments.isNullOrEmpty())
                lastMessage.context.getString(R.string.sceyt_attachment) else message.body

            val fromText = if (message.incoming) {
                val userFirstName = channel.lastMessage?.from?.getPresentableFirstName()?.trim()
                if (channel.isGroup && !userFirstName.isNullOrBlank()) {
                    "${userFirstName}: "
                } else ""
            } else
                "${root.getString(R.string.sceyt_your_last_message)}: "

            lastMessage.buildSpannable()
                .setString("$fromText${body.trim()}")
                .setForegroundColorId(R.color.sceyt_color_last_message_from)
                .setIndexSpan(0, fromText.length)
                .build()

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
            DateTimeUtil.getDateTimeStringCheckToday(lastMsgCreatedAt)
        else
            DateTimeUtil.getDateTimeStringCheckToday(channel.createdAt / 1000)
    }

    private fun SceytItemChannelBinding.setChannelItemStyle() {
        with(root.context) {
            channelTitle.setTextColor(getCompatColorByTheme(ChannelStyle.titleColor))
            lastMessage.setTextColor(getCompatColorByTheme(ChannelStyle.lastMessageTextColor))
            unreadMessagesCount.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(ChannelStyle.unreadCountColor))
            onlineStatus.setIndicatorColor(getCompatColorByTheme(ChannelStyle.onlineStatusColor))
            dateStatus.buildStyle()
                .setStatusIconSize(ChannelStyle.statusIconSize)
                .setDateColor(ChannelStyle.dateTextColor)
                .build()

            divider.isVisible = if (ChannelStyle.enableDivider) {
                divider.setBackgroundColor(getCompatColorByTheme(ChannelStyle.dividerColor))
                true
            } else false
        }
    }
}