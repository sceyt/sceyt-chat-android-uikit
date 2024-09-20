package com.sceyt.chatuikit.presentation.components.channel_list.channels.adapter.holders

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.PresenceState
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getPresentableFirstName
import com.sceyt.chatuikit.extensions.getPresentableNameCheckDeleted
import com.sceyt.chatuikit.extensions.getPresentableNameWithYou
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.extensions.setOnClickListenerAvailable
import com.sceyt.chatuikit.extensions.setOnLongClickListenerAvailable
import com.sceyt.chatuikit.formatters.UserNameFormatter
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
import com.sceyt.chatuikit.presentation.customviews.AvatarView
import com.sceyt.chatuikit.presentation.customviews.ColorSpannableTextView
import com.sceyt.chatuikit.presentation.customviews.DecoratedTextView
import com.sceyt.chatuikit.presentation.customviews.PresenceStateIndicatorView
import com.sceyt.chatuikit.presentation.extensions.getAttachmentIconAsString
import com.sceyt.chatuikit.presentation.extensions.getFormattedBody
import com.sceyt.chatuikit.presentation.extensions.getFormattedLastMessageBody
import com.sceyt.chatuikit.presentation.extensions.setChannelAvatar
import com.sceyt.chatuikit.presentation.extensions.setChannelMessageDateAndStatusIcon
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import com.sceyt.chatuikit.styles.ChannelListViewStyle
import java.text.NumberFormat
import java.util.Locale

open class ChannelViewHolder(
        private val binding: SceytItemChannelBinding,
        private val channelStyle: ChannelListViewStyle,
        private var listeners: ChannelClickListeners.ClickListeners,
        private val attachDetachListener: ((ChannelListItem?, attached: Boolean) -> Unit)? = null,
        private val userNameFormatter: UserNameFormatter? = SceytChatUIKit.formatters.userNameFormatter
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
                setOnlineStatus(channel, binding.onlineStatus)

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
                        setAutoDeleteState(channel, binding.ivAutoDeleted)
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
            textView.text = context.getString(R.string.sceyt_message_was_deleted)
            textView.setTypeface(null, Typeface.ITALIC)
            binding.dateStatus.setIcons(null)
        } else {
            val body = message.getFormattedLastMessageBody(context)

            val fromText = when {
                message.incoming -> {
                    val from = channel.lastMessage.user
                    val userFirstName = from?.let {
                        userNameFormatter?.format(from)
                                ?: from.getPresentableNameCheckDeleted(context)
                    }
                    if (channel.isGroup && !userFirstName.isNullOrBlank()) {
                        "${userFirstName}: "
                    } else ""
                }

                isSelf -> ""
                else -> "${context.getString(R.string.sceyt_your_last_message)}: "
            }

            (textView as ColorSpannableTextView).buildSpannable()
                .append(fromText)
                .append(message.getAttachmentIconAsString(channelStyle))
                .append(body)
                .setForegroundColorId(SceytChatUIKit.theme.textPrimaryColor)
                .setIndexSpan(0, fromText.length)
                .build()

            textView.setTypeface(null, Typeface.NORMAL)
        }
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
            val toMessage = SpannableStringBuilder(message.getFormattedBody(context))
            val reactedWord = itemView.getString(R.string.sceyt_reacted)

            val reactUserName = when {
                channel.isGroup -> {
                    val name = lastReaction.user?.let {
                        SceytChatUIKit.formatters.userNameFormatter?.format(it)
                    } ?: lastReaction.user?.getPresentableNameWithYou(context)
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
            textView.setTypeface(null, Typeface.NORMAL)
            return true
        }
        return false
    }

    open fun checkHasDraftMessage(channel: SceytChannel, textView: TextView): Boolean {
        val draftMessage = channel.draftMessage
        return if (draftMessage != null) {
            val draft = context.getString(R.string.sceyt_draft)
            val text = SpannableStringBuilder("$draft: ").apply {
                append(MessageBodyStyleHelper.buildOnlyBoldMentionsAndStylesWithAttributes(draftMessage.message.toString(),
                    draftMessage.mentionUsers, draftMessage.bodyAttributes))
                setSpan(ForegroundColorSpan(context.getCompatColor(SceytChatUIKit.theme.errorColor)), 0, draft.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            textView.text = text
            textView.setTypeface(null, Typeface.NORMAL)
            true
        } else false
    }

    open fun setSubject(channel: SceytChannel, textView: TextView) {
        textView.text = if (channel.isGroup) channel.channelSubject
        else {
            if (channel.isSelf()) {
                context.getString(R.string.sceyt_self_notes)
            } else {
                channel.getPeer()?.user?.let { from ->
                    userNameFormatter?.format(from) ?: from.getPresentableNameCheckDeleted(context)
                }
            }
        }
    }

    open fun setMuteState(channel: SceytChannel, textView: TextView) {
        if (channel.muted) {
            textView.setCompoundDrawablesWithIntrinsicBounds(null, null, channelStyle.mutedIcon, null)
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    open fun setAutoDeleteState(channel: SceytChannel, imageView: ImageView) {
        imageView.isVisible = channel.autoDeleteEnabled
    }

    open fun setPinState(channel: SceytChannel, pinImage: ImageView) {
        val isPinned = channel.pinned
        pinImage.setImageDrawable(channelStyle.pinIcon)
        pinImage.isVisible = isPinned
        binding.viewPinned.isVisible = isPinned
    }

    open fun setAvatar(channel: SceytChannel, name: String, url: String?, avatarView: AvatarView) {
        avatarView.setChannelAvatar(channel, isSelf)
    }

    open fun setLastMessageStatusAndDate(channel: SceytChannel, decoratedTextView: DecoratedTextView) {
        val data = getDateData(channel)
        val shouldShowStatus = data.second
        channel.lastMessage.setChannelMessageDateAndStatusIcon(decoratedTextView, channelStyle, data.first, false, shouldShowStatus)
    }

    open fun setOnlineStatus(channel: SceytChannel?, onlineStatus: PresenceStateIndicatorView) {
        val isOnline = !isSelf && channel?.isDirect() == true &&
                channel.getPeer()?.user?.presence?.state == PresenceState.Online
        onlineStatus.isVisible = isOnline
    }

    open fun setUnreadCount(channel: SceytChannel, textView: TextView) {
        val unreadCount = channel.newMessageCount
        if (unreadCount == 0L) {
            textView.isVisible = channel.unread
            return
        }

        // User NumberFormat for arabic language
        val title = if (unreadCount > 99L) {
            "${NumberFormat.getInstance(Locale.getDefault()).format(99)}+"
        } else NumberFormat.getInstance(Locale.getDefault()).format(unreadCount)

        textView.apply {
            text = title
            isVisible = true
        }
    }

    @SuppressLint("SetTextI18n")
    open fun setChannelMarkedUsUnread(channel: SceytChannel, textView: TextView) {
        if (channel.newMessageCount > 0) return
        if (channel.unread)
            textView.text = "  "

        textView.isVisible = channel.unread
    }

    open fun setMentionUserSymbol(channel: SceytChannel, icMention: ImageView) {
        icMention.isVisible = channel.newMentionCount > 0 && channel.newMessageCount > 0
    }

    @SuppressLint("SetTextI18n")
    open fun setTypingState(channel: SceytChannel, textView: TextView) {
        val data = channel.typingData ?: return
        if (data.typing) {
            textView.setTypeface(null, Typeface.ITALIC)
            if (channel.isGroup) {
                val name = userNameFormatter?.format(data.member.user)
                        ?: data.member.getPresentableFirstName()
                textView.text = "$name ${context.getString(R.string.sceyt_typing_)}"
            } else
                textView.text = context.getString(R.string.sceyt_typing_)

        } else setLastMessagedText(channel, textView)
    }

    protected open fun getDateData(channel: SceytChannel?): Pair<String, Boolean> {
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
        return Pair(DateTimeUtil.getDateTimeStringWithDateFormatter(context, lastMsgCreatedAt, channelStyle.channelDateFormat), shouldShowStatus)
    }

    private fun SceytItemChannelBinding.setChannelItemStyle() {
        channelTitle.setTextColor(channelStyle.titleColor)
        lastMessage.setTextColor(channelStyle.lastMessageTextColor)
        unreadMessagesCount.backgroundTintList = ColorStateList.valueOf(channelStyle.unreadCountColor)
        icMention.backgroundTintList = ColorStateList.valueOf(channelStyle.unreadCountColor)
        onlineStatus.setIndicatorColor(channelStyle.onlineStatusColor)
        viewPinned.setBackgroundColor(channelStyle.pinnedChannelBackgroundColor)
        ivAutoDeleted.setImageDrawable(channelStyle.autoDeletedChannelIcon)
        dateStatus.styleBuilder()
            .setLeadingIconSize(channelStyle.statusIconSize)
            .setTextColor(channelStyle.dateTextColor)
            .setLeadingText(context.getString(R.string.sceyt_edited))
            .build()

        divider.isVisible = if (channelStyle.enableDivider) {
            divider.setBackgroundColor(channelStyle.dividerColor)
            true
        } else false
    }
}