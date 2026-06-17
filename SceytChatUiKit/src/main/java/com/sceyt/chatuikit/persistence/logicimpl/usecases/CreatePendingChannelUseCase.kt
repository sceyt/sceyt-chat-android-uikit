package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.google.gson.Gson
import com.sceyt.chat.models.role.Role
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.channels.SelfChannelMetadata
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.extensions.toSha256
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.mappers.createPendingChannel
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.persistence.mappers.toUserDb

internal class CreatePendingChannelUseCase(
    private val channelDao: ChannelDao,
    private val usersDao: UserDao,
    private val channelsCache: ChannelsCache
) {

    suspend operator fun invoke(
        data: CreateChannelData,
        currentUserId: String?
    ): SceytResponse<SceytChannel> {
        val myId = currentUserId
            ?: return createErrorResponse("Failed to create direct channel myId is null")
        val currentUser = SceytChatUIKit.currentUser
            ?: usersDao.getUserById(myId)?.toSceytUser()
            ?: SceytUser(myId)

        var members = data.members.distinctBy { it.id }
        if (members.none { it.id == myId }) {
            members = members.plus(
                SceytMember(
                    role = Role(RoleTypeEnum.Owner.value),
                    user = currentUser
                )
            )
        }

        val isSelfChannel =
            members.size == 1 && members[0].id == myId && data.type == ChannelTypeEnum.Direct.value
        val metadata = if (isSelfChannel)
            Gson().toJson(SelfChannelMetadata(1)) else data.metadata

        val channelId = if (data.uri.isNotBlank()) {
            data.uri.toSha256()
        } else {
            members.map { it.id }.distinct().sorted().joinToString(separator = "$").toSha256()
        }

        val channel = createPendingChannel(
            channelId = channelId,
            createdBy = currentUser,
            data = data.copy(
                metadata = metadata,
                members = members
            )
        )

        usersDao.insertUsersWithMetadata(members.map { it.toUserDb() })
        channelDao.insertChannelAndLinks(channel.toChannelEntity(), members.map {
            UserChatLinkEntity(userId = it.id, chatId = channel.id, role = it.role.name)
        })
        channelsCache.upsertPendingChannel(channel)
        return SceytResponse.Success(channel)
    }
}
