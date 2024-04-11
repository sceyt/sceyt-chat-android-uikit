package com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.viewholders

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
import com.sceyt.chatuikit.R
import com.sceyt.chatuikit.SceytKitClient
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.databinding.SceytItemChannelBinding
import com.sceyt.chatuikit.extensions.getCompatColor
import com.sceyt.chatuikit.extensions.getPresentableFirstName
import com.sceyt.chatuikit.extensions.getPresentableNameCheckDeleted
import com.sceyt.chatuikit.extensions.getPresentableNameWithYou
import com.sceyt.chatuikit.extensions.getString
import com.sceyt.chatuikit.extensions.setOnClickListenerAvailable
import com.sceyt.chatuikit.extensions.setOnLongClickListenerAvailable
import com.sceyt.chatuikit.persistence.differs.ChannelDiff
import com.sceyt.chatuikit.persistence.logicimpl.channelslogic.ChatReactionMessagesCache
import com.sceyt.chatuikit.persistence.mappers.toSceytReaction
import com.sceyt.chatuikit.presentation.common.getAttachmentIconAsString
import com.sceyt.chatuikit.presentation.common.getFormattedBody
import com.sceyt.chatuikit.presentation.common.getFormattedLastMessageBody
import com.sceyt.chatuikit.presentation.common.getPeer
import com.sceyt.chatuikit.presentation.common.isDirect
import com.sceyt.chatuikit.presentation.common.isPeerDeleted
import com.sceyt.chatuikit.presentation.common.isSelf
import com.sceyt.chatuikit.presentation.common.setChannelMessageDateAndStatusIcon
import com.sceyt.chatuikit.presentation.customviews.SceytColorSpannableTextView
import com.sceyt.chatuikit.presentation.customviews.SceytDateStatusView
import com.sceyt.chatuikit.presentation.customviews.SceytOnlineView
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.ChannelListItem
import com.sceyt.chatuikit.presentation.uicomponents.channels.adapter.ChannelsAdapter
import com.sceyt.chatuikit.presentation.uicomponents.channels.listeners.ChannelClickListeners
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.MessageBodyStyleHelper
import com.sceyt.chatuikit.sceytconfigs.SceytKitConfig
import com.sceyt.chatuikit.sceytstyles.ChannelStyle
import com.sceyt.chatuikit.sceytstyles.UserStyle
import com.sceyt.chatuikit.shared.utils.DateTimeUtil
import java.text.NumberFormat
import java.util.Locale

open class ChannelViewHolder(private val binding: SceytItemChannelBinding,
                             private var listeners: ChannelClickListeners.ClickListeners,
                             private val attachDetachListener: ((ChannelListItem?, attached: Boolean) -> Unit)? = null,
                             private val userNameBuilder: ((User) -> String)? = SceytKitConfig.userNameBuilder) : BaseChannelViewHolder(binding.root) {

    protected var isSelf = false

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
            val body = message.getFormattedLastMessageBody(context)

            val fromText = when {
                message.incoming -> {
                    val from = channel.lastMessage?.user
                    val userFirstName = from?.let {
                        userNameBuilder?.invoke(from)
                                ?: from.getPresentableNameCheckDeleted(context)
                    }
                    if (channel.isGroup && !userFirstName.isNullOrBlank()) {
                        "${userFirstName}: "
                    } else ""
                }

                isSelf -> ""
                else -> "${context.getString(R.string.sceyt_your_last_message)}: "
            }

            (textView as SceytColorSpannableTextView).buildSpannable()
                .append(fromText)
                .append(message.getAttachmentIconAsString(context))
                .append(body)
                .setForegroundColorId(R.color.sceyt_color_text_themed)
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
            val toMessage = SpannableStringBuilder(message.getFormattedBody(context))
            val reactedWord = itemView.getString(R.string.sceyt_reacted)

            val reactUserName = when {
                channel.isGroup -> {
                    val name = lastReaction.user?.let { SceytKitConfig.userNameBuilder?.invoke(it) }
                            ?: lastReaction.user?.getPresentableNameWithYou(context)
                    "$name ${reactedWord.lowercase()}"
                }

                lastReaction.user?.id == SceytKitClient.myId -> "${itemView.getString(R.string.sceyt_you)} ${itemView.getString(R.string.sceyt_reacted).lowercase()}"
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
                    draftMessage.mentionUsers?.toTypedArray(), draftMessage.bodyAttributes))
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
            if (channel.isSelf()) {
                context.getString(R.string.self_notes)
            } else {
                channel.getPeer()?.user?.let { from ->
                    userNameBuilder?.invoke(from) ?: from.getPresentableNameCheckDeleted(context)
                }
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

    open fun setPinState(channel: SceytChannel, pinImage: ImageView) {
        val isPinned = channel.pinned
        pinImage.isVisible = isPinned
        binding.viewPinned.isVisible = isPinned
    }

    open fun setAvatar(channel: SceytChannel, name: String, url: String?, avatar: ImageView) {
        if (isSelf) {
            binding.avatar.setImageUrl(null, UserStyle.notesAvatar)
            binding.avatar.setAvatarColor(context.getCompatColor(SceytKitConfig.sceytColorAccent))
            return
        }
        binding.avatar.setAvatarColor(0)
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
        return Pair(DateTimeUtil.getDateTimeStringWithDateFormatter(context, lastMsgCreatedAt, ChannelStyle.channelDateFormat), shouldShowStatus)
    }

    private fun SceytItemChannelBinding.setChannelItemStyle() {
        with(root.context) {
            channelTitle.setTextColor(getCompatColor(ChannelStyle.titleColor))
            lastMessage.setTextColor(getCompatColor(ChannelStyle.lastMessageTextColor))
            unreadMessagesCount.backgroundTintList = ColorStateList.valueOf(getCompatColor(ChannelStyle.unreadCountColor))
            icMention.backgroundTintList = ColorStateList.valueOf(getCompatColor(ChannelStyle.unreadCountColor))
            onlineStatus.setIndicatorColor(getCompatColor(ChannelStyle.onlineStatusColor))
            viewPinned.setBackgroundColor(getCompatColor(ChannelStyle.pinnedChannelBackgroundColor))
            dateStatus.buildStyle()
                .setStatusIconSize(ChannelStyle.statusIconSize)
                .setDateColor(ChannelStyle.dateTextColor)
                .build()

            divider.isVisible = if (ChannelStyle.enableDivider) {
                divider.setBackgroundColor(getCompatColor(ChannelStyle.dividerColor))
                true
            } else false
        }
    }
}