package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.widget.ImageView
import android.widget.TextView
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
import com.sceyt.sceytchatuikit.presentation.customviews.SceytColorSpannableTextView
import com.sceyt.sceytchatuikit.presentation.customviews.SceytDateStatusView
import com.sceyt.sceytchatuikit.presentation.customviews.SceytOnlineView
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.sceytchatuikit.sceytconfigs.AvatarStyle
import com.sceyt.sceytchatuikit.sceytconfigs.ChannelStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil

open class ChannelViewHolder(private val binding: SceytItemChannelBinding,
                             private var listeners: ChannelClickListeners.ClickListeners,
                             private val attachDetachListener: ((ChannelListItem?, attached: Boolean) -> Unit)?) : BaseChannelViewHolder(binding.root) {

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

                diff.run {
                    if (!hasDifference()) return@run

                    if (muteStateChanged)
                        setMuteState(channel, binding.channelTitle)

                    if (subjectChanged)
                        setSubject(channel, binding.channelTitle)

                    if (subjectChanged || avatarViewChanged)
                        setAvatar(channel, name, url, binding.avatar)

                    if (lastMessageStatusChanged)
                        setLastMessageStatus(channel, binding.dateStatus)

                    if (lastMessageChanged)
                        setLastMessagedText(channel, binding.lastMessage)

                    if (unreadCountChanged)
                        setUnreadCount(channel.unreadMessageCount, binding.unreadMessagesCount)

                    if (onlineStateChanged)
                        setOnlineStatus(channel, binding.onlineStatus)

                    if (markedUsUnreadChanged)
                        setChannelMarkedUsUnread(channel, binding.unreadMessagesCount)
                }
            }
            ChannelListItem.LoadingMoreItem -> Unit
        }
    }

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        attachDetachListener?.invoke(getChannelItem(), true)
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        attachDetachListener?.invoke(getChannelItem(), false)
    }

    private fun getChannelItem() = if (::channelItem.isInitialized) channelItem else null

    open fun setLastMessagedText(channel: SceytChannel, textView: TextView) {
        val message = channel.lastMessage
        if (message == null) {
            textView.text = ""
            return
        }
        if (message.state == MessageState.Deleted) {
            textView.text = itemView.context.getString(R.string.sceyt_message_was_deleted)
            textView.setTypeface(null, Typeface.ITALIC)
        } else {
            val body = if (message.body.isBlank() && !message.attachments.isNullOrEmpty())
                textView.context.getString(R.string.sceyt_attachment) else message.body

            val fromText = if (message.incoming) {
                val userFirstName = channel.lastMessage?.from?.getPresentableFirstName()?.trim()
                if (channel.isGroup && !userFirstName.isNullOrBlank()) {
                    "${userFirstName}: "
                } else ""
            } else
                "${textView.getString(R.string.sceyt_your_last_message)}: "

            (textView as SceytColorSpannableTextView).buildSpannable()
                .setString("$fromText${body.trim()}")
                .setForegroundColorId(R.color.sceyt_color_last_message_from)
                .setIndexSpan(0, fromText.length)
                .build()

            textView.setTypeface(null, Typeface.NORMAL)
        }
    }

    open fun setSubject(channel: SceytChannel, textView: TextView) {
        textView.text = channel.channelSubject
    }

    open fun setMuteState(channel: SceytChannel, textView: TextView) {
        if (channel.muted) {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, ChannelStyle.mutedIcon, 0)
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    open fun setAvatar(channel: SceytChannel, name: String, url: String?, avatar: ImageView) {
        binding.avatar.setNameAndImageUrl(name, url, if (channel.isGroup) 0 else AvatarStyle.userDefaultAvatar)
    }

    open fun setLastMessageStatus(channel: SceytChannel, dateStatusView: SceytDateStatusView) {
        channel.lastMessage.setMessageDateAndStatusIcon(dateStatusView, getDateTxt(channel), false)
    }

    open fun setOnlineStatus(channel: SceytChannel?, onlineStatus: SceytOnlineView) {
        val isOnline = (channel?.channelType == ChannelTypeEnum.Direct)
                && (channel as? SceytDirectChannel)?.peer?.user?.presence?.state == PresenceState.Online
        onlineStatus.isVisible = isOnline
    }

    open fun setUnreadCount(unreadCount: Long?, textView: TextView) {
        if (unreadCount == null || unreadCount == 0L) {
            textView.isVisible = false
            return
        }
        val title = if (unreadCount > 99L) {
            "99+"
        } else unreadCount.toString()

        textView.apply {
            text = title
            isVisible = true
        }
    }

    open fun setChannelMarkedUsUnread(channel: SceytChannel, unreadMessagesCount: TextView) {
        if (channel.unreadMessageCount == 0L) {
            if (channel.markedUsUnread)
                unreadMessagesCount.apply {
                    text = "  "
                    isVisible = true
                }
            else unreadMessagesCount.isVisible = false
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