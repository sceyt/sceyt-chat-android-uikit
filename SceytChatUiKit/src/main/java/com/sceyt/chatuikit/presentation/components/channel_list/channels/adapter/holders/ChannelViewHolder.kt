package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders

import android.annotation.SuppressLint
import android.text.SpannableStringBuilder
import android.text.util.Linkify
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.extensions.extractLinksWithPositions
import com.sceyt.chatuikit.extensions.setOnClickListenerAvailable
import com.sceyt.chatuikit.extensions.setOnLongClickListenerAvailable
import com.sceyt.chatuikit.formatters.attributes.ChannelEventTitleFormatterAttributes
import com.sceyt.chatuikit.formatters.attributes.ChannelItemSubtitleFormatterAttributes
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.presentation.components.channel.header.helpers.ChannelEventData
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsAdapter
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.presentation.custom_views.DecoratedTextView
import com.sceyt.chatuikit.presentation.custom_views.PresenceStateIndicatorView
import com.sceyt.chatuikit.presentation.extensions.setChannelMessageDateAndStatusIcon
import com.sceyt.chatuikit.styles.ChannelItemStyle
import java.util.Date

open class ChannelViewHolder(
        private val binding: SceytItemChannelBinding,
        private val itemStyle: ChannelItemStyle,
        private var listeners: ChannelClickListeners.ClickListeners,
        private val attachDetachListener: ((ChannelListItem?, attached: Boolean) -> Unit)? = null,
) : BaseChannelViewHolder(binding.root) {

    init {
        with(binding) {
            setChannelItemStyle()

            root.setOnClickListenerAvailable(ChannelsAdapter.clickAvailableData) {
                listeners.onChannelClick(item as ChannelListItem.ChannelItem)
            }

            parentLayout.setOnLongClickListenerAvailable(ChannelsAdapter.longClickAvailableData) {
                listeners.onChannelLongClick(it, item as ChannelListItem.ChannelItem)
            }

            avatar.setOnClickListenerAvailable(ChannelsAdapter.clickAvailableData) {
                listeners.onAvatarClick(item as ChannelListItem.ChannelItem)
            }
        }
    }

    @CallSuper
    override fun bind(item: ChannelListItem, diff: ChannelDiff) {
        super.bind(item, diff)
        when (item) {
            is ChannelListItem.ChannelItem -> {
                val channel = item.channel

                // this ui states is changed more often, and to avoid wrong ui states we need to set them every time
                setUnreadCount(channel, binding.unreadMessagesCount)
                setUnreadMentions(channel, binding.icMention)
                setLastMessageStatusAndDate(channel, binding.dateStatus)
                setLastMessagedText(channel, binding.lastMessage)
                setPresenceState(channel, binding.onlineState)

                diff.run {
                    if (!hasDifference()) return@run

                    if (muteStateChanged)
                        setMuteState(channel, binding.channelTitle)

                    if (pinStateChanged)
                        setPinState(channel, binding.icPinned)

                    if (subjectChanged)
                        setSubject(channel, binding.channelTitle)

                    if (subjectChanged || avatarViewChanged)
                        setAvatar(channel, binding.avatar)

                    if (markedUsUnreadChanged)
                        setChannelMarkedUsUnread(channel, binding.unreadMessagesCount)

                    if (activityStateChanged)
                        setChannelEventTitle(channel, binding.lastMessage)

                    if (autoDeleteStateChanged) {
                        setAutoDeleteState(channel, binding.icAutoDeleted)
                    }
                }
            }

            ChannelListItem.LoadingMoreItem -> Unit
        }
    }

    override fun onViewAttachedToWindow() {
        super.onViewAttachedToWindow()
        attachDetachListener?.invoke(getChannelListItem(), true)
    }

    override fun onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow()
        attachDetachListener?.invoke(getChannelListItem(), false)
    }

    protected open fun setLastMessagedText(channel: SceytChannel, textView: TextView) {
        val text = itemStyle.channelSubtitleFormatter.format(
            context = context,
            from = ChannelItemSubtitleFormatterAttributes(
                channel = channel,
                channelItemStyle = itemStyle
            )
        )
        setTextAutoLinkMasks(textView, text)
        textView.setText(text, TextView.BufferType.SPANNABLE)
    }

    protected open fun setTextAutoLinkMasks(messageText: TextView, body: CharSequence) {
        val hasLinks = body.extractLinksWithPositions().isNotEmpty()
        messageText.autoLinkMask = if (hasLinks)
            Linkify.WEB_URLS else 0
    }

    protected open fun setSubject(channel: SceytChannel, textView: TextView) {
        textView.text = itemStyle.channelTitleFormatter.format(context, channel)
    }

    protected open fun setMuteState(channel: SceytChannel, textView: TextView) {
        if (channel.muted) {
            textView.setCompoundDrawablesWithIntrinsicBounds(null, null, itemStyle.mutedIcon, null)
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    protected open fun setAutoDeleteState(channel: SceytChannel, imageView: ImageView) {
        imageView.isVisible = channel.autoDeleteEnabled
    }

    protected open fun setPinState(channel: SceytChannel, pinImage: ImageView) {
        val isPinned = channel.pinned
        pinImage.setImageDrawable(itemStyle.pinIcon)
        pinImage.isVisible = isPinned
        binding.viewPinned.isVisible = isPinned
    }

    protected open fun setAvatar(
            channel: SceytChannel,
            avatarView: AvatarView,
    ) {
        itemStyle.channelAvatarRenderer.render(
            context = context,
            from = channel,
            style = itemStyle.avatarStyle,
            avatarView = avatarView
        )
    }

    protected open fun setLastMessageStatusAndDate(
            channel: SceytChannel,
            decoratedTextView: DecoratedTextView,
    ) {
        val data = getDateData(channel)
        val shouldShowStatus = data.second
        channel.lastMessage.setChannelMessageDateAndStatusIcon(
            decoratedTextView = decoratedTextView,
            itemStyle = itemStyle,
            dateText = data.first,
            edited = false,
            shouldShowStatus = shouldShowStatus)
    }

    protected open fun setPresenceState(
            channel: SceytChannel,
            indicatorView: PresenceStateIndicatorView,
    ) {
        val state = channel.getPeer()?.user?.presence?.state ?: PresenceState.Offline
        val showState = !channel.isSelf && channel.isDirect() && state == PresenceState.Online
        indicatorView.setIndicatorColor(itemStyle.presenceStateColorProvider.provide(context, state))
        indicatorView.isVisible = showState
    }

    protected open fun setUnreadCount(channel: SceytChannel, textView: TextView) {
        val unreadCount = channel.newMessageCount
        if (unreadCount == 0L) {
            textView.isVisible = channel.unread
            return
        }

        textView.apply {
            text = itemStyle.unreadCountFormatter.format(context, channel.newMessageCount)
            isVisible = true
            applyUnreadStyle(channel, this)
        }
    }

    protected open fun applyUnreadStyle(channel: SceytChannel, textView: TextView) {
        if (channel.muted)
            itemStyle.unreadCountMutedStateTextStyle.apply(textView)
        else itemStyle.unreadCountTextStyle.apply(textView)
    }

    @SuppressLint("SetTextI18n")
    protected open fun setChannelMarkedUsUnread(channel: SceytChannel, textView: TextView) {
        if (channel.newMessageCount > 0) return
        if (channel.unread)
            textView.text = "  "

        applyUnreadStyle(channel, textView)
        textView.isVisible = channel.unread
    }

    protected open fun setUnreadMentions(channel: SceytChannel, imageView: ImageView) {
        val showMention = channel.newMentionCount > 0 && channel.newMessageCount > 0
        if (showMention) {
            imageView.isVisible = true
            if (channel.muted)
                itemStyle.unreadMentionMutedStateBackgroundStyle.apply(imageView)
            else itemStyle.unreadMentionBackgroundStyle.apply(imageView)
        } else imageView.isVisible = false
    }

    protected open fun setChannelEventTitle(channel: SceytChannel, textView: TextView) {
        val events = channel.events
        if (!events.isNullOrEmpty()) {
            val title = SpannableStringBuilder(initChannelEventTitle(channel, events))
            itemStyle.channelEventTextStyle.apply(context, title)
            textView.setText(title, TextView.BufferType.SPANNABLE)
        } else setLastMessagedText(channel, textView)
    }

    protected open fun initChannelEventTitle(
            channel: SceytChannel,
            channelEventData: List<ChannelEventData>,
    ): CharSequence {
        return itemStyle.channelEventTitleFormatter.format(
            context = context,
            from = ChannelEventTitleFormatterAttributes(
                channel = channel,
                users = channelEventData
            )
        )
    }

    protected open fun getDateData(channel: SceytChannel?): Pair<CharSequence, Boolean> {
        if (channel == null) return Pair("", false)
        var shouldShowStatus = true
        val lastMsgCreatedAt = when {
            channel.draftMessage != null -> {
                shouldShowStatus = false
                channel.draftMessage.createdAt
            }

            channel.lastMessage != null -> {
                val lastMessageCreatedAt = channel.lastMessage.createdAt
                val lastReactionCreatedAt = channel.newReactions?.maxByOrNull { it.id }?.createdAt
                        ?: 0
                if (lastReactionCreatedAt > lastMessageCreatedAt)
                    lastReactionCreatedAt
                else lastMessageCreatedAt
            }

            else -> channel.createdAt
        }

        val date = itemStyle.channelDateFormatter.format(context, Date(lastMsgCreatedAt))
        return date to shouldShowStatus
    }

    protected open fun SceytItemChannelBinding.setChannelItemStyle() {
        root.setBackgroundColor(itemStyle.backgroundColor)
        viewPinned.setBackgroundColor(itemStyle.pinnedChannelBackgroundColor)
        divider.setBackgroundColor(itemStyle.dividerColor)
        icAutoDeleted.setImageDrawable(itemStyle.autoDeletedChannelIcon)
        icMention.setImageDrawable(itemStyle.unreadMentionIcon)
        dateStatus.appearanceBuilder()
            .setLeadingIconSize(itemStyle.deliveryStatusIndicatorSize)
            .setTextStyle(itemStyle.dateTextStyle)
            .setLeadingText(context.getString(R.string.sceyt_edited))
            .build()
        itemStyle.avatarStyle.apply(avatar)
        lastMessage.setLinkTextColor(itemStyle.linkTextColor)
        itemStyle.subjectTextStyle.apply(channelTitle)
        itemStyle.lastMessageTextStyle.apply(lastMessage)
    }
}