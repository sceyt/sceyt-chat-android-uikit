package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chatuikit.data.models.channels.SceytMember

interface SystemMessageSender {
    suspend fun sendGroupCreated(channelId: Long)
    suspend fun sendChannelCreated(channelId: Long)
    suspend fun sendMembersAdded(channelId: Long, members: List<SceytMember>)
    suspend fun sendMembersRemoved(channelId: Long, members: List<SceytMember>)
    suspend fun sendMemberLeft(channelId: Long)
    suspend fun sendJoinedByInviteLink(channelId: Long)
    suspend fun sendDisappearingMessageChanged(channelId: Long, duration: Long)
}
