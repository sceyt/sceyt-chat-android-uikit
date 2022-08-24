package com.sceyt.chat.ui.data.channeleventobserver

import com.sceyt.chat.ChatClient
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.user.User
import com.sceyt.chat.sceyt_listeners.ChannelListener
import com.sceyt.chat.ui.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chat.ui.data.toSceytMember
import com.sceyt.chat.ui.data.toSceytUiChannel
import com.sceyt.chat.ui.extensions.TAG
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ChannelEventsObserver {

    private val onChannelEventFlow_ = MutableSharedFlow<ChannelEventData>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onChannelEventFlow: SharedFlow<ChannelEventData> = onChannelEventFlow_.asSharedFlow()


    private val onChannelMembersEventFlow_ = MutableSharedFlow<ChannelMembersEventData>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onChannelMembersEventFlow: SharedFlow<ChannelMembersEventData> = onChannelMembersEventFlow_.asSharedFlow()


    private val onChannelOwnerChangedEventFlow_ = MutableSharedFlow<ChannelOwnerChangedEventData>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onChannelOwnerChangedEventFlow: SharedFlow<ChannelOwnerChangedEventData> = onChannelOwnerChangedEventFlow_.asSharedFlow()


    private val onChannelTypingEventFlow_ = MutableSharedFlow<ChannelTypingEventData>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val onChannelTypingEventFlow: SharedFlow<ChannelTypingEventData> = onChannelTypingEventFlow_.asSharedFlow()


    private val onMessageStatusFlow_: MutableSharedFlow<MessageStatusChangeData> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val onMessageStatusFlow = onMessageStatusFlow_.asSharedFlow()


    init {
        ChatClient.getClient().addChannelListener(TAG, object : ChannelListener {

            override fun onClearedHistory(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.ClearedHistory))
            }

            override fun onChannelUpdated(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.Updated))
            }

            override fun onChannelCreated(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.Created))
            }

            override fun onChannelDeleted(channelId: Long) {
                val data = ChannelEventData(null, ChannelEventEnum.Deleted, channelId)
                onChannelEventFlow_.tryEmit(data)
            }

            override fun onChannelMuted(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.Muted))
            }

            override fun onChannelUnMuted(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.UnMuted))
            }

            override fun onChannelLeft(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.Left))
            }

            override fun onChannelJoined(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.Joined))
            }

            override fun onChannelHidden(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.Hidden))
            }

            override fun onChannelUnHidden(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.UnHidden))
            }

            override fun onMarkedUsUnread(channel: Channel?) {
                onChannelEventFlow_.tryEmit(ChannelEventData(channel, ChannelEventEnum.MarkedUsUnread))
            }

            override fun onChannelInvited(channelId: Long?) {
                val data = ChannelEventData(null, ChannelEventEnum.Invited, channelId)
                onChannelEventFlow_.tryEmit(data)
            }

            override fun onChannelBlocked(channelId: Long) {
                val data = ChannelEventData(null, ChannelEventEnum.Blocked, channelId)
                onChannelEventFlow_.tryEmit(data)
            }

            override fun onChannelUnBlocked(channelId: Long) {
                val data = ChannelEventData(null, ChannelEventEnum.UnBlocked, channelId)
                onChannelEventFlow_.tryEmit(data)
            }

            override fun onOwnerChanged(channel: Channel, newOwner: Member, oldOwner: Member) {
                onChannelOwnerChangedEventFlow_.tryEmit(ChannelOwnerChangedEventData(channel, newOwner, oldOwner))
            }

            override fun onMemberStartedTyping(channel: Channel, member: Member) {
                onChannelTypingEventFlow_.tryEmit(ChannelTypingEventData(channel.toSceytUiChannel(),
                    member.toSceytMember(), true))
            }

            override fun onMemberStoppedTyping(channel: Channel, member: Member) {
                onChannelTypingEventFlow_.tryEmit(ChannelTypingEventData(channel.toSceytUiChannel(),
                    member.toSceytMember(), false))
            }

            override fun onChangedMembersRole(channel: Channel?, members: MutableList<Member>?) {
                onChannelMembersEventFlow_.tryEmit(ChannelMembersEventData(channel, members, ChannelMembersEventEnum.Role))
            }

            override fun onMembersKicked(channel: Channel?, members: MutableList<Member>?) {
                onChannelMembersEventFlow_.tryEmit(ChannelMembersEventData(channel, members, ChannelMembersEventEnum.Kicked))
            }

            override fun onMembersBlocked(channel: Channel?, members: MutableList<Member>?) {
                onChannelMembersEventFlow_.tryEmit(ChannelMembersEventData(channel, members, ChannelMembersEventEnum.Blocked))
            }

            override fun onMembersUnblocked(channel: Channel?, members: MutableList<Member>?) {
                onChannelMembersEventFlow_.tryEmit(ChannelMembersEventData(channel, members, ChannelMembersEventEnum.UnBlocked))
            }

            override fun onMembersAdded(channel: Channel?, members: MutableList<Member>?) {
                onChannelMembersEventFlow_.tryEmit(ChannelMembersEventData(channel, members, ChannelMembersEventEnum.Added))
            }

            override fun onDeliveryReceiptReceived(channel: Channel?, from: User?, messageIds: MutableList<Long>) {
                onMessageStatusFlow_.tryEmit(MessageStatusChangeData(channel?.id, from, DeliveryStatus.Delivered, messageIds))
            }

            override fun onReadReceiptReceived(channel: Channel?, from: User?, messageIds: MutableList<Long>) {
                onMessageStatusFlow_.tryEmit(MessageStatusChangeData(channel?.id, from, DeliveryStatus.Read, messageIds))
            }
        })
    }
}