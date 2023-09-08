package com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.viewholders

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
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.R
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.databinding.SceytItemChannelBinding
import com.sceyt.sceytchatuikit.extensions.*
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChatReactionMessagesCache
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytReaction
import com.sceyt.sceytchatuikit.presentation.common.getFirstMember
import com.sceyt.sceytchatuikit.presentation.common.getShowBody
import com.sceyt.sceytchatuikit.presentation.common.isDirect
import com.sceyt.sceytchatuikit.presentation.common.isPeerDeleted
import com.sceyt.sceytchatuikit.presentation.common.setChannelMessageDateAndStatusIcon
import com.sceyt.sceytchatuikit.presentation.customviews.SceytColorSpannableTextView
import com.sceyt.sceytchatuikit.presentation.customviews.SceytDateStatusView
import com.sceyt.sceytchatuikit.presentation.customviews.SceytOnlineView
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelItemPayloadDiff
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.adapter.ChannelsAdapter
import com.sceyt.sceytchatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.sceytchatuikit.presentation.uicomponents.messageinput.mention.MentionUserHelper
import com.sceyt.sceytchatuikit.sceytconfigs.ChannelStyle
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.sceytchatuikit.sceytconfigs.UserStyle
import com.sceyt.sceytchatuikit.shared.utils.DateTimeUtil
import okhttp3.internal.lowercase
import java.text.NumberFormat
import java.util.*

open class ChannelViewHolder(private val binding: SceytItemChannelBinding,
                             private var listeners: ChannelClickListeners.ClickListeners,
                             private val attachDetachListener: ((ChannelListItem?, attached: Boolean) -> Unit)? = null,
                             private val userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder) : BaseChannelViewHolder(binding.root) {


    init {
        with(binding) {
            setChannelItemStyle()

            root.setOnClickListenerAvailable(ChannelsAdapter.clickAvailableData) {
                listeners.onChannelClick(channelItem as ChannelListItem.ChannelItem)
            }

            parentLayout.setOnLongClickListenerAvailable(ChannelsAdapter.longClickAvailableData) {
                listeners.onChannelLongClick(it, (channelItem as ChannelListItem.ChannelItem))
            }

            avatar.setOnClickListenerAvailable(ChannelsAdapter.clickAvailableData) {
                listeners.onAvatarClick((channelItem as ChannelListItem.ChannelItem))
            }
        }
    }

    @CallSuper
    override fun bind(item: ChannelListItem, diff: ChannelItemPayloadDiff) {
        super.bind(item, diff)
        when (item) {
            is ChannelListItem.ChannelItem -> {
                val channel = item.channel
                val name: String = channel.channelSubject
                val url = channel.iconUrl

                setUnreadCount(channel.newMessageCount, binding.unreadMessagesCount)
                setMentionUserSymbol(channel.newMentionCount, channel.newMessageCount, binding.icMention)

                diff.run {
                    if (!hasDifference()) return@run

                    if (muteStateChanged)
                        setMuteState(channel, binding.channelTitle)

                    if (subjectChanged)
                        setSubject(channel, binding.channelTitle)

                    if (subjectChanged || avatarViewChanged)
                        setAvatar(channel, name, url, binding.avatar)

                    if (lastMessageStatusChanged || lastMessageChanged)
                        setLastMessageStatusAndDate(channel, binding.dateStatus)

                    if (lastMessageChanged)
                        setLastMessagedText(channel, binding.lastMessage)

                    if (onlineStateChanged)
                        setOnlineStatus(channel, binding.onlineStatus)

                    if (markedUsUnreadChanged)
                        setChannelMarkedUsUnread(channel, binding.unreadMessagesCount)

                    if (typingStateChanged)
                        setTypingState(channel, binding.lastMessage)
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
            textView.text = ""
            return
        }
        if (message.state == MessageState.Deleted) {
            textView.text = context.getString(R.string.sceyt_message_was_deleted)
            textView.setTypeface(null, Typeface.ITALIC)
            binding.dateStatus.setStatusIcon(null)
        } else {
            val body = if (message.body.isBlank() && !message.attachments.isNullOrEmpty())
                context.getString(R.string.sceyt_attachment) else message.body

            val fromText = if (message.incoming) {
                val from = channel.lastMessage?.user
                val userFirstName = from?.let {
                    userNameBuilder?.invoke(from) ?: from.getPresentableNameCheckDeleted(context)
                }
                if (channel.isGroup && !userFirstName.isNullOrBlank()) {
                    "${userFirstName}: "
                } else ""
            } else
                "${context.getString(R.string.sceyt_your_last_message)}: "

            (textView as SceytColorSpannableTextView).buildSpannable()
                .setSpannableString(MentionUserHelper.buildOnlyNamesWithMentionedUsers(body, message.metadata, message.mentionedUsers))
                .append(fromText)
                .setForegroundColorId(R.color.sceyt_color_last_message_from)
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
                    it.user?.id != SceytKitClient.myId &&
                            removeReactions.none { rm -> rm.key == it.key && rm.messageId == it.messageId && it.user?.id == SceytKitClient.myId }
                }?.maxByOrNull { it.id } ?: return false

        val message = ChatReactionMessagesCache.getMessageById(lastReaction.messageId)
                ?: return false

        if (lastReaction.id > (channel.lastMessage?.id ?: 0) || lastReaction.pending) {
            val toMessage = "\"${message.getShowBody(context)}\""
            val reactedWord = itemView.getString(R.string.sceyt_reacted)

            val reactUserName = when {
                channel.isGroup -> {
                    val name = lastReaction.user?.let { SceytKitConfig.userNameBuilder?.invoke(it) }
                            ?: lastReaction.user?.getPresentableNameWithYou(context)
                    "$name ${reactedWord.lowercase()}"
                }

                lastReaction.user?.id == SceytKitClient.myId -> "${itemView.getString(R.string.sceyt_you)} ${reactedWord.lowercase()}"
                else -> reactedWord
            }

            val text = "$reactUserName ${lastReaction.key} ${itemView.getString(R.string.sceyt_to)} $toMessage"
            textView.text = text
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
                append(MentionUserHelper.buildOnlyNamesWithMentionedUsers(
                    draftMessage.message.toString(), draftMessage.metadata, draftMessage.mentionUsers?.toTypedArray()))
                setSpan(ForegroundColorSpan(context.getCompatColor(R.color.sceyt_color_red)), 0, draft.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            textView.text = text
            textView.setTypeface(null, Typeface.NORMAL)
            true
        } else false
    }

    open fun setSubject(channel: SceytChannel, textView: TextView) {
        textView.text = if (channel.isGroup) channel.channelSubject
        else {
            channel.getFirstMember()?.user?.let { from ->
                userNameBuilder?.invoke(from) ?: from.getPresentableNameCheckDeleted(context)
            }
        }
    }

    open fun setMuteState(channel: SceytChannel, textView: TextView) {
        if (channel.muted) {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, ChannelStyle.mutedIcon, 0)
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }
    }

    open fun setAvatar(channel: SceytChannel, name: String, url: String?, avatar: ImageView) {
        if (channel.isDirect() && channel.isPeerDeleted()) {
            binding.avatar.setImageUrl(null, UserStyle.deletedUserAvatar)
        } else
            binding.avatar.setNameAndImageUrl(name, url, if (channel.isGroup) 0 else UserStyle.userDefaultAvatar)
    }

    open fun setLastMessageStatusAndDate(channel: SceytChannel, dateStatusView: SceytDateStatusView) {
        val data = getDateData(channel)
        val shouldShowStatus = data.second
        channel.lastMessage.setChannelMessageDateAndStatusIcon(dateStatusView, data.first, false, shouldShowStatus)
    }

    open fun setOnlineStatus(channel: SceytChannel?, onlineStatus: SceytOnlineView) {
        val isOnline = channel?.isDirect() == true && channel.getFirstMember()?.user?.presence?.state == PresenceState.Online
        onlineStatus.isVisible = isOnline
    }

    open fun setUnreadCount(unreadCount: Long?, textView: TextView) {
        if (unreadCount == null || unreadCount == 0L) {
            textView.isVisible = false
            return
        }
        val title = if (unreadCount > 99L) {
            "${NumberFormat.getInstance(Locale.getDefault()).format(99)}+"
        } else NumberFormat.getInstance(Locale.getDefault()).format(unreadCount)

        textView.apply {
            text = title
            isVisible = true
        }
    }

    open fun setMentionUserSymbol(unreadMentionCount: Long, unreadCount: Long, icMention: ImageView) {
        icMention.isVisible = unreadMentionCount > 0 && unreadCount > 0
    }

    open fun setChannelMarkedUsUnread(channel: SceytChannel, unreadMessagesCount: TextView) {
        if (channel.newMessageCount == 0L) {
            if (channel.unread)
                unreadMessagesCount.apply {
                    text = "  "
                    isVisible = true
                }
            else unreadMessagesCount.isVisible = false
        }
    }

    @SuppressLint("SetTextI18n")
    open fun setTypingState(channel: SceytChannel, textView: TextView) {
        val data = channel.typingData ?: return
        if (data.typing) {
            textView.setTypeface(null, Typeface.ITALIC)
            if (channel.isGroup) {
                val name = userNameBuilder?.invoke(data.member.user)
                        ?: data.member.getPresentableFirstName()
                textView.text = "$name ${context.getString(R.string.sceyt_typing_)}"
            } else
                textView.text = context.getString(R.string.sceyt_typing_)

        } else setLastMessagedText(channel, textView)
    }

    protected fun getDateData(channel: SceytChannel?): Pair<String, Boolean> {
        if (channel == null) return Pair("", false)
        var shouldShowStatus = true
        val lastMsgCreatedAt = when {
            channel.draftMessage != null -> {
                shouldShowStatus = false
                channel.draftMessage?.createdAt
            }

            channel.lastMessage != null -> {
                val lastMessageCreatedAt = channel.lastMessage?.createdAt ?: 0L
                val lastReactionCreatedAt = channel.newReactions?.maxByOrNull { it.id }?.createdAt
                        ?: 0
                if (lastReactionCreatedAt > lastMessageCreatedAt)
                    lastReactionCreatedAt
                else lastMessageCreatedAt
            }

            else -> channel.createdAt
        }
        return Pair(DateTimeUtil.getDateTimeStringCheckToday(context, lastMsgCreatedAt), shouldShowStatus)
    }

    private fun SceytItemChannelBinding.setChannelItemStyle() {
        with(root.context) {
            channelTitle.setTextColor(getCompatColorByTheme(ChannelStyle.titleColor))
            lastMessage.setTextColor(getCompatColorByTheme(ChannelStyle.lastMessageTextColor))
            unreadMessagesCount.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(ChannelStyle.unreadCountColor))
            icMention.backgroundTintList = ColorStateList.valueOf(getCompatColorByTheme(ChannelStyle.unreadCountColor))
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