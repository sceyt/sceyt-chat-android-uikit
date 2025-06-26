package com.sceyt.chatuikit.data.managers.channel

import android.util.Log
import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.ChannelEvent
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_listeners.ChannelListener
import com.sceyt.chatuikit.data.constants.SceytConstants
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMemberActivityEvent
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventEnum
import com.sceyt.chatuikit.data.managers.channel.event.ChannelOwnerChangedEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelUnreadCountUpdatedEventData
import com.sceyt.chatuikit.data.managers.channel.event.MessageMarkerEventData
import com.sceyt.chatuikit.data.managers.channel.handler.ChannelEventHandler
import com.sceyt.chatuikit.data.managers.channel.handler.ChannelEventHandlerImpl
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.toSceytMember
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.persistence.mappers.toSceytUiChannel
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ChannelEventManager : ChannelEventHandler.AllEvents {
    private var eventManager: ChannelEventHandler.AllEvents = ChannelEventHandlerImpl(this)

    private val onTotalUnreadChangedFlow_ = MutableSharedFlow<ChannelUnreadCountUpdatedEventData>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onTotalUnreadChangedFlow: SharedFlow<ChannelUnreadCountUpdatedEventData> = onTotalUnreadChangedFlow_.asSharedFlow()


    private val onChannelEventFlow_ = MutableSharedFlow<ChannelActionEvent>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onChannelEventFlow = onChannelEventFlow_.asSharedFlow()


    private val onChannelMembersEventFlow_ = MutableSharedFlow<ChannelMembersEventData>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onChannelMembersEventFlow: SharedFlow<ChannelMembersEventData> = onChannelMembersEventFlow_.asSharedFlow()


    private val onChannelOwnerChangedEventFlow_ = MutableSharedFlow<ChannelOwnerChangedEventData>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onChannelOwnerChangedEventFlow: SharedFlow<ChannelOwnerChangedEventData> = onChannelOwnerChangedEventFlow_.asSharedFlow()


    private val onChannelMemberActivityEventFlow_ = MutableSharedFlow<ChannelMemberActivityEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onChannelMemberActivityEventFlow = onChannelMemberActivityEventFlow_.asSharedFlow()


    private val onMessageStatusFlow_: MutableSharedFlow<MessageStatusChangeData> = MutableSharedFlow(
        extraBufferCapacity = 30,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageStatusFlow = onMessageStatusFlow_.asSharedFlow()


    private val onMarkerReceivedFlow_: MutableSharedFlow<MessageMarkerEventData> = MutableSharedFlow(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMarkerReceivedFlow = onMarkerReceivedFlow_.asSharedFlow()


    init {
        ChatClient.getClient().addChannelListener(TAG, object : ChannelListener {
            override fun onTotalUnreadCountUpdated(channel: Channel?, totalUnreadChannelCount: Long, totalUnreadMessageCount: Long) {
                val data = ChannelUnreadCountUpdatedEventData(channel, totalUnreadChannelCount, totalUnreadMessageCount)
                eventManager.onTotalUnreadChanged(data)
            }

            override fun onDeleteAllMessagesForMe(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.ClearedHistory(channel.toSceytUiChannel()))
            }

            override fun onDeleteAllMessagesForEveryone(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.ClearedHistory(channel.toSceytUiChannel()))
            }

            override fun onChannelUpdated(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.Updated(channel.toSceytUiChannel()))
            }

            override fun onChannelCreated(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.Created(channel.toSceytUiChannel()))
            }

            override fun onThreadCreated(channel: Channel?) {

            }

            override fun onChannelDeleted(channelId: Long) {
                eventManager.onChannelEvent(ChannelActionEvent.Deleted(channelId))
            }

            override fun onChannelMuted(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.Mute(channel.toSceytUiChannel(), true))
            }

            override fun onChannelUnMuted(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.Mute(channel.toSceytUiChannel(), false))
            }

            override fun onChannelPinned(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.Pin(channel.toSceytUiChannel(), true))
            }

            override fun onChannelUnPinned(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.Pin(channel.toSceytUiChannel(), false))
            }

            override fun onChannelLeft(channel: Channel?, leftMembers: List<Member>?) {
                channel ?: return
                leftMembers ?: return
                val members = leftMembers.map { it.toSceytMember() }
                eventManager.onChannelEvent(ChannelActionEvent.Left(channel.toSceytUiChannel(), members))
            }

            override fun onChannelJoined(channel: Channel?, joinedMembers: MutableList<Member>?) {
                channel ?: return
                val members = joinedMembers?.map { it.toSceytMember() } ?: return
                eventManager.onChannelEvent(ChannelActionEvent.Joined(channel.toSceytUiChannel(), members))
            }

            override fun onChannelHidden(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.Hide(channel.toSceytUiChannel(), true))
            }

            override fun onChannelUnHidden(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.Hide(channel.toSceytUiChannel(), false))
            }

            override fun onMarkedUsUnread(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.MarkedUs(channel.toSceytUiChannel(), false))
            }

            override fun onMarkedUsRead(channel: Channel?) {
                channel ?: return
                eventManager.onChannelEvent(ChannelActionEvent.MarkedUs(channel.toSceytUiChannel(), true))
            }

            override fun onChannelBlocked(channelId: Long) {
                eventManager.onChannelEvent(ChannelActionEvent.Block(channelId, true))
            }

            override fun onChannelUnBlocked(channelId: Long) {
                eventManager.onChannelEvent(ChannelActionEvent.Block(channelId, false))
            }

            override fun onOwnerChanged(channel: Channel, newOwner: Member, oldOwner: Member) {
                eventManager.onOwnerChanged(channel.toSceytUiChannel(), newOwner, oldOwner)
            }

            override fun onMemberStartedTyping(channel: Channel, member: Member) {
                eventManager.onActivityEvent(ChannelMemberActivityEvent.Typing(
                    channel = channel.toSceytUiChannel(),
                    user = member.toSceytUser(),
                    typing = true
                ))
            }

            override fun onMemberStoppedTyping(channel: Channel, member: Member) {
                eventManager.onActivityEvent(ChannelMemberActivityEvent.Typing(
                    channel = channel.toSceytUiChannel(),
                    user = member.toSceytUser(),
                    typing = false
                ))
            }

            override fun onChangedMembersRole(channel: Channel?, members: MutableList<Member>?) {
                if (channel == null || members == null) return
                eventManager.onChangedMembersEvent(ChannelMembersEventData(channel.toSceytUiChannel(),
                    members.map { it.toSceytMember() }, ChannelMembersEventEnum.Role))
            }

            override fun onMembersKicked(channel: Channel?, members: MutableList<Member>?) {
                if (channel == null || members == null) return
                eventManager.onChangedMembersEvent(ChannelMembersEventData(channel.toSceytUiChannel(),
                    members.map { it.toSceytMember() }, ChannelMembersEventEnum.Kicked))
            }

            override fun onMembersBlocked(channel: Channel?, members: MutableList<Member>?) {
                if (channel == null || members == null) return
                eventManager.onChangedMembersEvent(ChannelMembersEventData(channel.toSceytUiChannel(),
                    members.map { it.toSceytMember() }, ChannelMembersEventEnum.Blocked))
            }

            override fun onMembersUnblocked(channel: Channel?, members: MutableList<Member>?) {
                if (channel == null || members == null) return
                eventManager.onChangedMembersEvent(ChannelMembersEventData(channel.toSceytUiChannel(),
                    members.map { it.toSceytMember() }, ChannelMembersEventEnum.UnBlocked))
            }

            override fun onMembersAdded(channel: Channel?, members: MutableList<Member>?) {
                if (channel == null || members == null) return
                eventManager.onChangedMembersEvent(ChannelMembersEventData(channel.toSceytUiChannel(),
                    members.map { it.toSceytMember() }, ChannelMembersEventEnum.Added))
            }

            override fun onDeliveryReceiptReceived(channel: Channel, from: User, marker: MessageListMarker) {
                eventManager.onMessageStatusEvent(MessageStatusChangeData(channel.toSceytUiChannel(),
                    from.toSceytUser(), DeliveryStatus.Received, marker))
            }

            override fun onMarkerReceived(channel: Channel, user: User, marker: MessageListMarker) {
                eventManager.onMarkerReceived(MessageMarkerEventData(channel.toSceytUiChannel(),
                    user.toSceytUser(), marker))
            }

            override fun onReadReceiptReceived(channel: Channel, from: User, marker: MessageListMarker) {
                eventManager.onMessageStatusEvent(MessageStatusChangeData(channel.toSceytUiChannel(),
                    from.toSceytUser(), DeliveryStatus.Displayed, marker))
            }

            override fun onChannelEvent(channel: Channel?, event: ChannelEvent?) {
                channel ?: return
                event ?: return
                when (event.name) {
                    SceytConstants.startTypingEvent -> {
                        eventManager.onActivityEvent(ChannelMemberActivityEvent.Typing(
                            channel = channel.toSceytUiChannel(),
                            user = event.user.toSceytUser(),
                            typing = true)
                        )
                    }

                    SceytConstants.stopTypingEvent -> {
                        eventManager.onActivityEvent(ChannelMemberActivityEvent.Typing(
                            channel = channel.toSceytUiChannel(),
                            user = event.user.toSceytUser(),
                            typing = false)
                        )
                    }

                    SceytConstants.startRecordingEvent -> {
                        eventManager.onActivityEvent(ChannelMemberActivityEvent.Recording(
                            channel = channel.toSceytUiChannel(),
                            user = event.user.toSceytUser(),
                            recording = true)
                        )
                    }

                    else -> {
                        eventManager.onChannelEvent(ChannelActionEvent.Event(channel.toSceytUiChannel(), event))
                        return
                    }
                }
            }
        })
    }

    override fun onTotalUnreadChanged(data: ChannelUnreadCountUpdatedEventData) {
        onTotalUnreadChangedFlow_.tryEmit(data)
    }

    override fun onChannelEvent(event: ChannelActionEvent) {
        onChannelEventFlow_.tryEmit(event)
    }

    override fun onOwnerChanged(channel: SceytChannel, newOwner: Member, oldOwner: Member) {
        onChannelOwnerChangedEventFlow_.tryEmit(ChannelOwnerChangedEventData(
            channel, newOwner.toSceytMember(), oldOwner.toSceytMember()))
    }

    override fun onActivityEvent(event: ChannelMemberActivityEvent) {
        onChannelMemberActivityEventFlow_.tryEmit(event)
    }

    override fun onChangedMembersEvent(data: ChannelMembersEventData) {
        onChannelMembersEventFlow_.tryEmit(data)
    }

    override fun onMessageStatusEvent(data: MessageStatusChangeData) {
        onMessageStatusFlow_.tryEmit(data)
    }

    override fun onMarkerReceived(data: MessageMarkerEventData) {
        onMarkerReceivedFlow_.tryEmit(data)
    }

    @Suppress("unused")
    fun setCustomListener(listener: ChannelEventHandler.AllEvents) {
        eventManager = listener
        (eventManager as? ChannelEventHandlerImpl)?.setDefaultListeners(this)
    }
}