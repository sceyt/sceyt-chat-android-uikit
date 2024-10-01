package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders

import android.annotation.SuppressLint
import android.text.SpannableStringBuilder
import android.text.util.Linkify
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.extensions.extractLinksWithPositions
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.extensions.setOnClickListenerAvailable
import com.sceyt.chatuikit.extensions.setOnLongClickListenerAvailable
import com.sceyt.chatuikit.extensions.toSpannableString
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.extensions.isSelf
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChatReactionMessagesCache
import com.sceyt.chatuikit.persistence.mappers.toSceytReaction
import com.sceyt.chatuikit.presentation.components.channel.input.mention.MessageBodyStyleHelper
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.ChannelsAdapter
import com.sceyt.chatuikit.presentation.components.channel_list.channels.listeners.click.ChannelClickListeners
import com.sceyt.chatuikit.presentation.custom_views.AvatarView
import com.sceyt.chatuikit.presentation.custom_views.DecoratedTextView
import com.sceyt.chatuikit.presentation.custom_views.PresenceStateIndicatorView
import com.sceyt.chatuikit.presentation.extensions.getFormattedBody
import com.sceyt.chatuikit.presentation.extensions.setChannelAvatar
import com.sceyt.chatuikit.presentation.extensions.setChannelMessageDateAndStatusIcon
import com.sceyt.chatuikit.styles.ChannelItemStyle
import java.util.Date

open class ChannelViewHolder(
        private val binding: SceytItemChannelBinding,
        private val itemStyle: ChannelItemStyle,
        private var listeners: ChannelClickListeners.ClickListeners,
        private val attachDetachListener: ((ChannelListItem?, attached: Boolean) -> Unit)? = null,
) : BaseChannelViewHolder(binding.root) {

    protected var isSelf = false
    private val myId: String? get() = SceytChatUIKit.chatUIFacade.myId

    init {
        with(binding) {
            setChannelItemStyle()

            root.setOnClickListenerAvailable(ChannelsAdapter.clickAvailableData) {
                listeners.onChannelClick(channelItem as ChannelListItem.ChannelItem)
            }

            parentLayout.setOnLongClickListenerAvailable(ChannelsAdapter.longClickAvailableData) {
                listeners.onChannelLongClick(it, channelItem as ChannelListItem.ChannelItem)
            }

            avatar.setOnClickListenerAvailable(ChannelsAdapter.clickAvailableData) {
                listeners.onAvatarClick(channelItem as ChannelListItem.ChannelItem)
            }
        }
    }

    @CallSuper
    override fun bind(item: ChannelListItem, diff: ChannelDiff) {
        super.bind(item, diff)
        when (item) {
            is ChannelListItem.ChannelItem -> {
                val channel = item.channel
                val name: String = channel.channelSubject
                val url = channel.iconUrl
                isSelf = channel.isSelf()

                // this ui states is changed more often, and to avoid wrong ui states we need to set them every time
                setUnreadCount(channel, binding.unreadMessagesCount)
                setMentionUserSymbol(channel, binding.icMention)
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
                        setAvatar(channel, name, url, binding.avatar)

                    if (markedUsUnreadChanged)
                        setChannelMarkedUsUnread(channel, binding.unreadMessagesCount)

                    if (typingStateChanged)
                        setTypingState(channel, binding.lastMessage)

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

    open fun setLastMessagedText(channel: SceytChannel, textView: TextView) {
        if (checkHasLastReaction(channel, textView))
            return

        if (checkHasDraftMessage(channel, textView))
            return

        val message = channel.lastMessage
        if (message == null) {
            textView.text = null
            return
        }
        if (message.state == MessageState.Deleted) {
            val text = SpannableStringBuilder(context.getString(R.string.sceyt_message_was_deleted))
            itemStyle.deletedTextStyle.apply(context, text)
            textView.text = text
            binding.dateStatus.setIcons(null)
            return
        }
        val body = message.getFormattedBody(
            context = context,
            mentionTextStyle = itemStyle.mentionTextStyle,
            attachmentNameFormatter = itemStyle.attachmentNameFormatter,
            mentionUserNameFormatter = itemStyle.mentionUserNameFormatter
        )
        val senderName = itemStyle.lastMessageSenderNameFormatter.format(context, channel)
        val attachmentIcon = message.attachments?.getOrNull(0)?.let {
            itemStyle.attachmentIconProvider.provide(context, it)
        }

        setTextAutoLinkMasks(textView, message.body)

        textView.setText(buildSpannedString {
            if (senderName.isNotEmpty()) {
                append(senderName)
                itemStyle.lastMessageSenderNameTextStyle.apply(context, this, 0, senderName.length)
            }
            append(attachmentIcon.toSpannableString())
            append(body)
        }, TextView.BufferType.SPANNABLE)
    }

    protected open fun setTextAutoLinkMasks(messageText: TextView, body: String) {
        val hasLinks = body.extractLinksWithPositions().isNotEmpty()
        messageText.autoLinkMask = if (hasLinks)
            Linkify.WEB_URLS else 0
    }

    open fun checkHasLastReaction(channel: SceytChannel, textView: TextView): Boolean {
        if (channel.lastMessage?.deliveryStatus == DeliveryStatus.Pending) return false
        val pendingAddOrRemoveReaction = channel.pendingReactions?.filter { !it.incomingMsg }?.groupBy { it.isAdd }
        val addReactions = pendingAddOrRemoveReaction?.get(true)
        val removeReactions = pendingAddOrRemoveReaction?.get(false) ?: emptyList()
        val lastReaction = addReactions?.maxByOrNull { it.createdAt }?.toSceytReaction()
                ?: channel.newReactions?.filter {
                    it.user?.id != myId && removeReactions.none { rm ->
                        rm.key == it.key && rm.messageId == it.messageId && it.user?.id == myId
                    }
                }?.maxByOrNull { it.id } ?: return false

        val message = ChatReactionMessagesCache.getMessageById(lastReaction.messageId)
                ?: return false

        if (lastReaction.id > (channel.lastMessage?.id ?: 0) || lastReaction.pending) {
            val toMessage = SpannableStringBuilder(message.getFormattedBody(
                context = context,
                mentionTextStyle = itemStyle.mentionTextStyle,
                attachmentNameFormatter = itemStyle.attachmentNameFormatter,
                mentionUserNameFormatter = itemStyle.mentionUserNameFormatter
            ))
            val reactedWord = itemView.getString(R.string.sceyt_reacted)

            val reactUserName = when {
                channel.isGroup -> {
                    val name = lastReaction.user?.let { itemStyle.reactedUserNameFormatter.format(context, it) }
                            ?: ""
                    "$name ${reactedWord.lowercase()}"
                }

                lastReaction.user?.id == myId -> "${itemView.getString(R.string.sceyt_you)} ${itemView.getString(R.string.sceyt_reacted).lowercase()}"
                else -> itemView.getString(R.string.sceyt_reacted)
            }

            val text = "$reactUserName ${lastReaction.key} ${itemView.getString(R.string.sceyt_to)}"
            val title = SpannableStringBuilder("$text ")
            title.append("\"")
            title.append(toMessage)
            title.append("\"")

            textView.setText(title, TextView.BufferType.SPANNABLE)
            return true
        }
        return false
    }

    open fun checkHasDraftMessage(channel: SceytChannel, textView: TextView): Boolean {
        val draftMessage = channel.draftMessage
        return if (draftMessage != null) {
            val draft = context.getString(R.string.sceyt_draft)
            val text = SpannableStringBuilder("$draft: ").apply {
                append(MessageBodyStyleHelper.buildWithAttributes(
                    context = context,
                    body = draftMessage.message.toString(),
                    mentionUsers = draftMessage.mentionUsers,
                    bodyAttributes = draftMessage.bodyAttributes,
                    mentionTextStyle = itemStyle.mentionTextStyle,
                    mentionUserNameFormatter = itemStyle.mentionUserNameFormatter)
                )
                itemStyle.draftPrefixTextStyle.apply(context, this, 0, draft.length + 1)
            }
            textView.text = text
            true
        } else false
    }

    open fun setSubject(channel: SceytChannel, textView: TextView) {
        textView.text = itemStyle.channelNameFormatter.format(context, channel)
    }

    open fun setMuteState(channel: SceytChannel, textView: TextView) {
        if (channel.muted) {
            textView.setCompoundDrawablesWithIntrinsicBounds(null, null, itemStyle.mutedIcon, null)
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    open fun setAutoDeleteState(channel: SceytChannel, imageView: ImageView) {
        imageView.isVisible = channel.autoDeleteEnabled
    }

    open fun setPinState(channel: SceytChannel, pinImage: ImageView) {
        val isPinned = channel.pinned
        pinImage.setImageDrawable(itemStyle.pinIcon)
        pinImage.isVisible = isPinned
        binding.viewPinned.isVisible = isPinned
    }

    open fun setAvatar(channel: SceytChannel, name: String, url: String?, avatarView: AvatarView) {
        avatarView.setChannelAvatar(channel, itemStyle.channelDefaultAvatarProvider, isSelf)
    }

    open fun setLastMessageStatusAndDate(channel: SceytChannel, decoratedTextView: DecoratedTextView) {
        val data = getDateData(channel)
        val shouldShowStatus = data.second
        channel.lastMessage.setChannelMessageDateAndStatusIcon(
            decoratedTextView = decoratedTextView,
            itemStyle = itemStyle,
            dateText = data.first,
            edited = false,
            shouldShowStatus = shouldShowStatus)
    }

    open fun setPresenceState(channel: SceytChannel?, indicatorView: PresenceStateIndicatorView) {
        val state = channel?.getPeer()?.user?.presence?.state ?: PresenceState.Offline
        val showState = !isSelf && channel?.isDirect() == true && state == PresenceState.Online
        indicatorView.setIndicatorColor(itemStyle.presenceStateColorProvider.provide(context, state))
        indicatorView.isVisible = showState
    }

    open fun setUnreadCount(channel: SceytChannel, textView: TextView) {
        val unreadCount = channel.newMessageCount
        if (unreadCount == 0L) {
            textView.isVisible = channel.unread
            return
        }

        textView.apply {
            text = itemStyle.unreadCountFormatter.format(context, channel.newMessageCount)
            isVisible = true
            if (channel.muted)
                itemStyle.unreadCountMutedStateTextStyle.apply(this)
            else itemStyle.unreadCountTextStyle.apply(this)
        }
    }

    @SuppressLint("SetTextI18n")
    open fun setChannelMarkedUsUnread(channel: SceytChannel, textView: TextView) {
        if (channel.newMessageCount > 0) return
        if (channel.unread)
            textView.text = "  "

        textView.isVisible = channel.unread
    }

    open fun setMentionUserSymbol(channel: SceytChannel, icMention: TextView) {
        val showMention = channel.newMentionCount > 0 && channel.newMessageCount > 0
        if (showMention) {
            icMention.isVisible = true
            if (channel.muted)
                itemStyle.unreadMentionMutedStateTextStyle.apply(icMention)
            else itemStyle.unreadMentionTextStyle.apply(icMention)
        } else icMention.isVisible = false
    }

    @SuppressLint("SetTextI18n")
    open fun setTypingState(channel: SceytChannel, textView: TextView) {
        val data = channel.typingData ?: return
        if (data.typing) {
            val title: SpannableStringBuilder = if (channel.isGroup) {
                val name = itemStyle.typingUserNameFormatter.format(context, data.member.user)
                SpannableStringBuilder("$name ${context.getString(R.string.sceyt_typing_)}")
            } else
                SpannableStringBuilder(context.getString(R.string.sceyt_typing_))
            itemStyle.typingTextStyle.apply(context, title)
            textView.setText(title, TextView.BufferType.SPANNABLE)
        } else setLastMessagedText(channel, textView)
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

    private fun SceytItemChannelBinding.setChannelItemStyle() {
        viewPinned.setBackgroundColor(itemStyle.pinnedChannelBackgroundColor)
        divider.setBackgroundColor(itemStyle.dividerColor)
        icAutoDeleted.setImageDrawable(itemStyle.autoDeletedChannelIcon)
        dateStatus.styleBuilder()
            .setLeadingIconSize(itemStyle.deliveryStatusIndicatorSize)
            .setTextStyle(itemStyle.dateTextStyle)
            .setLeadingText(context.getString(R.string.sceyt_edited))
            .build()
        lastMessage.setLinkTextColor(itemStyle.linkTextColor)
        itemStyle.subjectTextStyle.apply(channelTitle)
        itemStyle.lastMessageTextStyle.apply(lastMessage)
    }
}