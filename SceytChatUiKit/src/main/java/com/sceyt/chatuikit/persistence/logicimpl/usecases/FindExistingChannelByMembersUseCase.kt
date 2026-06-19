package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.mappers.toChannel

internal class FindExistingChannelByMembersUseCase(
    private val channelDao: ChannelDao
) {

    suspend operator fun invoke(
        data: CreateChannelData,
        currentUserId: String?
    ): SceytChannel? {
        val members = data.members.distinctBy { it.id }
        val membersCount = members.size
        val isSelf = membersCount == 1
                && members[0].id == currentUserId
                && data.type == ChannelTypeEnum.Direct.value

        val channelDb = if (isSelf) {
            channelDao.getSelfChannel()
        } else {
            val directMemberIds = if (data.type == ChannelTypeEnum.Direct.value && currentUserId != null)
                (members.map { it.id } + currentUserId).distinct()
            else emptyList()
            val directPeerId = directMemberIds
                .takeIf { it.size == 2 }
                ?.firstOrNull { it != currentUserId }

            if (directPeerId != null) {
                channelDao.getChannelByUserAndType(directPeerId, data.type)
            } else if (membersCount == 1) {
                channelDao.getChannelByUserAndType(members[0].id, data.type)
            } else {
                val ids = members.map { it.id }.distinct()
                channelDao.getChannelByUsersAndType(ids, data.type)
            }
        }

        return channelDb?.toChannel()
    }
}
