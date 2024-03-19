package com.sceyt.sceytchatuikit.data.channeleventobserver

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.ChannelEvent
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_listeners.ChannelListener
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytUiChannel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ChannelEventsObserver : ChannelEventManager.AllEventManagers {
    private var eventManager = ChannelEventManagerImpl(this)

    private val onTotalUnreadChangedFlow_ = MutableSharedFlow<ChannelUnreadCountUpdatedEventData>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onTotalUnreadChangedFlow: SharedFlow<ChannelUnreadCountUpdatedEventData> = onTotalUnreadChangedFlow_.asSharedFlow()


    private val onChannelEventFlow_ = MutableSharedFlow<ChannelEventData>(
        extraBufferCapacity = 5,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onChannelEventFlow: SharedFlow<ChannelEventData> = onChannelEventFlow_.asSharedFlow()


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


    private val onChannelTypingEventFlow_ = MutableSharedFlow<ChannelTypingEventData>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onChannelTypingEventFlow: SharedFlow<ChannelTypingEventData> = onChannelTypingEventFlow_.asSharedFlow()


    private val onMessageStatusFlow_: MutableSharedFlow<MessageStatusChangeData> = MutableSharedFlow(
        extraBufferCapacity = 30,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageStatusFlow = onMessageStatusFlow_.asSharedFlow()


    init {
        ChatClient.getClient().addChannelListener(TAG, object : ChannelListener {
            override fun onTotalUnreadCountUpdated(channel: Channel?, totalUnreadChannelCount: Long, totalUnreadMessageCount: Long) {
                val data = ChannelUnreadCountUpdatedEventData(channel, totalUnreadChannelCount, totalUnreadMessageCount)
                eventManager.onTotalUnreadChanged(data)
            }

            override fun onDeleteAllMessagesForMe(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.ClearedHistory))
            }

            override fun onDeleteAllMessagesForEveryone(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.ClearedHistory))
            }

            override fun onChannelUpdated(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.Updated))
            }

            override fun onChannelCreated(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.Created))
            }

            override fun onChannelDeleted(channelId: Long) {
                val data = ChannelEventData(null, ChannelEventEnum.Deleted, channelId)
                eventManager.onChannelEvent(data)
            }

            override fun onChannelMuted(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.Mute(true)))
            }

            override fun onChannelUnMuted(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.Mute(false)))
            }

            override fun onChannelPinned(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.Pin(true)))
            }

            override fun onChannelUnPinned(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.Pin(false)))
            }

            override fun onChannelLeft(channel: Channel?, leftMembers: MutableList<Member>?) {
                val members = leftMembers?.map { it.toSceytMember() } ?: emptyList()
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.Left(members)))
            }

            override fun onChannelJoined(channel: Channel?, joinedMembers: MutableList<Member>?) {
                val members = joinedMembers?.map { it.toSceytMember() } ?: emptyList()
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.Joined(members)))
            }

            override fun onChannelHidden(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.Hide(true)))
            }

            override fun onChannelUnHidden(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.Hide(false)))
            }

            override fun onMarkedUsUnread(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.MarkedUs(false)))
            }

            override fun onMarkedUsRead(channel: Channel?) {
                eventManager.onChannelEvent(ChannelEventData(channel?.toSceytUiChannel(), ChannelEventEnum.MarkedUs(true)))
            }

            override fun onChannelInvited(channelId: Long?) {
                val data = ChannelEventData(null, ChannelEventEnum.Invited, channelId)
                eventManager.onChannelEvent(data)
            }

            override fun onChannelBlocked(channelId: Long) {
                val data = ChannelEventData(null, ChannelEventEnum.Block(true), channelId)
                eventManager.onChannelEvent(data)
            }

            override fun onChannelUnBlocked(channelId: Long) {
                val data = ChannelEventData(null, ChannelEventEnum.Block(false), channelId)
                eventManager.onChannelEvent(data)
            }

            override fun onOwnerChanged(channel: Channel, newOwner: Member, oldOwner: Member) {
                eventManager.onOwnerChanged(channel.toSceytUiChannel(), newOwner, oldOwner)
            }

            override fun onMemberStartedTyping(channel: Channel, member: Member) {
                eventManager.onChannelTypingEvent(ChannelTypingEventData(channel.toSceytUiChannel(),
                    member.toSceytMember(), true))
            }

            override fun onMemberStoppedTyping(channel: Channel, member: Member) {
                eventManager.onChannelTypingEvent(ChannelTypingEventData(channel.toSceytUiChannel(),
                    member.toSceytMember(), false))
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

            override fun onDeliveryReceiptReceived(channel: Channel, from: User, messageIds: MutableList<Long>) {
                eventManager.onMessageStatusEvent(MessageStatusChangeData(channel.toSceytUiChannel(), from, DeliveryStatus.Received, messageIds))
            }

            override fun onReadReceiptReceived(channel: Channel, from: User, messageIds: MutableList<Long>) {
                eventManager.onMessageStatusEvent(MessageStatusChangeData(channel.toSceytUiChannel(), from, DeliveryStatus.Displayed, messageIds))
            }

            override fun onChannelEvent(channel: Channel?, event: ChannelEvent?) {
            }
        })
    }

    override fun onTotalUnreadChanged(data: ChannelUnreadCountUpdatedEventData) {
        onTotalUnreadChangedFlow_.tryEmit(data)
    }

    override fun onChannelEvent(data: ChannelEventData) {
        onChannelEventFlow_.tryEmit(data)
    }

    override fun onOwnerChanged(channel: SceytChannel, newOwner: Member, oldOwner: Member) {
        onChannelOwnerChangedEventFlow_.tryEmit(ChannelOwnerChangedEventData(channel, newOwner, oldOwner))
    }

    override fun onChannelTypingEvent(data: ChannelTypingEventData) {
        onChannelTypingEventFlow_.tryEmit(data)
    }

    override fun onChangedMembersEvent(data: ChannelMembersEventData) {
        onChannelMembersEventFlow_.tryEmit(data)
    }

    override fun onMessageStatusEvent(data: MessageStatusChangeData) {
        onMessageStatusFlow_.tryEmit(data)
    }


    fun setCustomListener(listener: ChannelEventManagerImpl) {
        eventManager = listener
        eventManager.setDefaultListeners(this)
    }
}