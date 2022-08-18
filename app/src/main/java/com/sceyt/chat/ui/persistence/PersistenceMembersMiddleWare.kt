package com.sceyt.chat.ui.persistence

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.chat.ui.data.models.PaginationResponse
import com.sceyt.chat.ui.data.models.SceytResponse
import com.sceyt.chat.ui.data.models.channels.SceytChannel
import com.sceyt.chat.ui.data.models.channels.SceytMember
import kotlinx.coroutines.flow.Flow

interface PersistenceMembersMiddleWare {
    suspend fun changeChannelOwner(channel: SceytChannel, newOwnerId: String): SceytResponse<SceytChannel>
    suspend fun changeChannelMemberRole(channel: SceytChannel, member: SceytMember): SceytResponse<SceytChannel>
    suspend fun blockUnBlockUser(userId: String, block: Boolean): SceytResponse<List<User>>
    suspend fun addMembersToChannel(channel: SceytChannel, members: List<Member>): SceytResponse<SceytChannel>
    suspend fun blockAndDeleteMember(channel: SceytChannel, memberId: String): SceytResponse<SceytChannel>
    suspend fun deleteMember(channel: SceytChannel, memberId: String): SceytResponse<SceytChannel>
    suspend fun loadChannelMembers(channelId: Long, offset: Int): Flow<PaginationResponse<SceytMember>>
}