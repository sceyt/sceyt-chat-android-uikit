package com.sceyt.sceytchatuikit.persistence.logics

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import kotlinx.coroutines.flow.Flow

internal interface PersistenceMembersLogic {
    fun onChannelMemberEvent(data: com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData)
    fun onChannelOwnerChangedEvent(data: com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelOwnerChangedEventData)
    suspend fun changeChannelOwner(channel: SceytChannel, newOwnerId: String): SceytResponse<SceytChannel>
    suspend fun changeChannelMemberRole(channel: SceytChannel, member: SceytMember): SceytResponse<SceytChannel>
    suspend fun addMembersToChannel(channel: SceytChannel, members: List<Member>): SceytResponse<SceytChannel>
    suspend fun blockAndDeleteMember(channel: SceytChannel, memberId: String): SceytResponse<SceytChannel>
    suspend fun deleteMember(channel: SceytChannel, memberId: String): SceytResponse<SceytChannel>
    suspend fun blockUnBlockUser(userId: String, block: Boolean): SceytResponse<List<User>>
    suspend fun loadChannelMembers(channelId: Long, offset: Int): Flow<PaginationResponse<SceytMember>>
}