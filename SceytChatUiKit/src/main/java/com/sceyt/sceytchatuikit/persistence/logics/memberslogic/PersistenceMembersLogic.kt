package com.sceyt.sceytchatuikit.persistence.logics.memberslogic

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelOwnerChangedEventData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import kotlinx.coroutines.flow.Flow

internal interface PersistenceMembersLogic {
    suspend fun onChannelMemberEvent(data: ChannelMembersEventData)
    suspend fun onChannelOwnerChangedEvent(data: ChannelOwnerChangedEventData)
    suspend fun changeChannelOwner(channelId: Long, newOwnerId: String): SceytResponse<SceytChannel>
    suspend fun changeChannelMemberRole(channelId: Long, member: SceytMember): SceytResponse<SceytChannel>
    suspend fun addMembersToChannel(channelId: Long, members: List<Member>): SceytResponse<SceytChannel>
    suspend fun blockAndDeleteMember(channelId: Long, memberId: String): SceytResponse<SceytChannel>
    suspend fun deleteMember(channelId: Long, memberId: String): SceytResponse<SceytChannel>
    suspend fun blockUnBlockUser(userId: String, block: Boolean): SceytResponse<List<User>>
    suspend fun loadChannelMembers(channelId: Long, offset: Int, role: String?): Flow<PaginationResponse<SceytMember>>
}