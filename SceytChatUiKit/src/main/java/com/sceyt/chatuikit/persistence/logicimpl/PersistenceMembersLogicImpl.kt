package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventData
import com.sceyt.chatuikit.data.managers.channel.event.ChannelMembersEventEnum
import com.sceyt.chatuikit.data.managers.channel.event.ChannelOwnerChangedEventData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.onSuccess
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.data.toMember
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.LoadRangeDao
import com.sceyt.chatuikit.persistence.database.dao.MemberDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.logic.PersistenceMembersLogic
import com.sceyt.chatuikit.persistence.logicimpl.channel.ChannelsCache
import com.sceyt.chatuikit.persistence.mappers.toChannel
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.mappers.toSceytMember
import com.sceyt.chatuikit.persistence.mappers.toUserDb
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class PersistenceMembersLogicImpl(
        private val channelsRepository: ChannelsRepository,
        private val channelDao: ChannelDao,
        private val rangeDao: LoadRangeDao,
        private val messageDao: MessageDao,
        private val memberDao: MemberDao,
        private val usersDao: UserDao,
        private val channelsCache: ChannelsCache,
) : PersistenceMembersLogic, SceytKoinComponent {

    private val channelMembersLoadSize get() = SceytChatUIKit.config.queryLimits.channelMemberListQueryLimit

    override suspend fun onChannelMemberEvent(data: ChannelMembersEventData) {
        val chatId = data.channel.id
        when (data.eventType) {
            ChannelMembersEventEnum.Role, ChannelMembersEventEnum.Added -> {
                usersDao.insertUsersWithMetadata(data.members.map { it.toUserDb() })
                channelDao.insertUserChatLinks(data.members.map {
                    UserChatLinkEntity(userId = it.id, chatId = chatId, role = it.role.name)
                })

                if (data.eventType == ChannelMembersEventEnum.Added)
                    channelDao.updateMemberCount(chatId, data.channel.memberCount.toInt())

                channelDao.getChannelById(chatId)?.let {
                    channelsCache.upsertChannel(it.toChannel())
                } ?: run {
                    channelDao.insertChannel(data.channel.toChannelEntity())
                    data.channel.lastMessage?.toMessageDb(false)?.let {
                        messageDao.upsertMessage(it)
                    }
                    channelsCache.upsertChannel(data.channel)
                }
            }

            ChannelMembersEventEnum.Kicked, ChannelMembersEventEnum.Blocked -> {
                if (data.members.any { it.id == SceytChatUIKit.chatUIFacade.myId }) {
                    deleteChannelDb(chatId)
                } else {
                    channelDao.deleteUserChatLinks(chatId, *data.members.map { it.id }.toTypedArray())
                    channelDao.updateMemberCount(chatId, data.channel.memberCount.toInt())
                    channelDao.getChannelById(chatId)?.let {
                        channelsCache.upsertChannel(it.toChannel())
                    }
                }
            }

            ChannelMembersEventEnum.UnBlocked -> {
                channelDao.updateChannel(data.channel.toChannelEntity())
                channelDao.getChannelById(chatId)?.let {
                    channelsCache.upsertChannel(it.toChannel())
                }
            }
        }
    }

    override suspend fun onChannelOwnerChangedEvent(data: ChannelOwnerChangedEventData) {
        memberDao.updateOwner(data.channel.id, data.oldOwner.id, data.newOwner.id)
    }

    override fun loadChannelMembers(channelId: Long, offset: Int, role: String?): Flow<PaginationResponse<SceytMember>> {
        val normalizedOffset = offset / channelMembersLoadSize * channelMembersLoadSize
        return callbackFlow {
            val dbMembers = getMembersDb(channelId, normalizedOffset, role, channelMembersLoadSize)
            val hasNextDb = dbMembers.size == channelMembersLoadSize
            if (dbMembers.isNotEmpty())
                trySend(PaginationResponse.DBResponse(dbMembers, null, normalizedOffset, hasNextDb))

            val response = channelsRepository.loadChannelMembers(channelId, normalizedOffset, role)

            if (response is SceytResponse.Success) {
                saveMembersToDb(channelId, response.data)
                // Check has removed items, and if exist delete from DB
                getRemovedItemsAndDeleteFromDb(channelId, dbMembers, response.data)

                // Get new updated items from DB
                val updatedMembers = getMembersDb(channelId, 0, role, normalizedOffset + channelMembersLoadSize)
                val hasNextServer = response.data?.size == channelMembersLoadSize
                trySend(PaginationResponse.ServerResponse(data = response, cacheData = updatedMembers, loadKey = null,
                    normalizedOffset, hasDiff = true, hasNext = hasNextServer, hasPrev = false,
                    LoadNext, false))
            } else
                trySend(PaginationResponse.ServerResponse(response, arrayListOf(), null, 0,
                    hasDiff = false, hasNext = false, hasPrev = false, loadType = LoadNext, ignoredDb = false))

            channel.close()
            awaitClose()
        }
    }

    private suspend fun getMembersDb(channelId: Long, offset: Int, role: String?, limit: Int): List<SceytMember> {
        val data = if (!role.isNullOrBlank())
            memberDao.getChannelMembersWithRole(channelId, limit = limit, offset = offset, role)
        else memberDao.getChannelMembers(channelId, limit, offset)
        return data.map { memberEntity -> memberEntity.toSceytMember() }
    }

    override suspend fun loadChannelMembersByDisplayName(channelId: Long, name: String): List<SceytMember> {
        return memberDao.getChannelMembersByDisplayName(channelId, name).map { it.toSceytMember() }
    }

    override suspend fun filterOnlyMembersByIds(channelId: Long, ids: List<String>): List<String> {
        return memberDao.filterOnlyMembersByIds(channelId, ids)
    }

    override suspend fun loadChannelMembersByIds(channelId: Long, vararg ids: String): List<SceytMember> {
        return memberDao.getChannelMembersByIds(channelId, *ids).map { it.toSceytMember() }
    }

    private suspend fun saveMembersToDb(channelId: Long, list: List<SceytMember>?) {
        if (list.isNullOrEmpty()) return

        val links = arrayListOf<UserChatLinkEntity>()
        val users = arrayListOf<UserDb>()

        list.forEach { member ->
            links.add(UserChatLinkEntity(userId = member.id, chatId = channelId, role = member.role.name))
            users.add(member.toUserDb())
        }

        usersDao.insertUsersWithMetadata(users)
        channelDao.insertUserChatLinks(links)
    }

    private suspend fun getRemovedItemsAndDeleteFromDb(
            channelId: Long,
            dbMembers: List<SceytMember>,
            serverResponse: List<SceytMember>?,
    ): List<SceytMember> {
        serverResponse ?: return emptyList()
        val removedItems: List<SceytMember> = dbMembers.minus(serverResponse.toSet())
        if (removedItems.isNotEmpty()) {
            (dbMembers as ArrayList).removeAll(removedItems.toSet())
            channelDao.deleteUserChatLinks(channelId, *removedItems.map { it.user.id }.toTypedArray())
        }
        return removedItems
    }

    private suspend fun deleteChannelDb(channelId: Long) {
        channelDao.deleteChannelAndLinks(channelId)
        messageDao.deleteAllMessagesByChannel(channelId)
        rangeDao.deleteChannelLoadRanges(channelId)
        channelsCache.deleteChannel(channelId)
    }

    override suspend fun changeChannelOwner(channelId: Long, newOwnerId: String): SceytResponse<SceytChannel> {
        return channelsRepository.changeChannelOwner(channelId, newOwnerId).onSuccess { channel ->
            channel?.members?.firstOrNull()?.let { member ->
                memberDao.updateOwner(channelId = channelId, newOwnerId = member.id)
                channelDao.getChannelById(channelId)?.let {
                    channelsCache.upsertChannel(it.toChannel())
                }
            }
        }
    }

    override suspend fun changeChannelMemberRole(channelId: Long, vararg member: SceytMember): SceytResponse<SceytChannel> {
        val response = channelsRepository.changeChannelMemberRole(
            channelId = channelId,
            member = member.map { it.toMember() }.toTypedArray()
        )
        response.onSuccessNotNull {
            onChannelMemberEvent(
                ChannelMembersEventData(
                    channel = it,
                    members = member.toList(),
                    eventType = ChannelMembersEventEnum.Role
                )
            )
        }

        return response
    }

    override suspend fun addMembersToChannel(channelId: Long, members: List<SceytMember>): SceytResponse<SceytChannel> {
        val response = channelsRepository.addMembersToChannel(channelId, members.map { it.toMember() })
        response.onSuccessNotNull { channel ->
            channel.members?.let { addedMembers ->
                onChannelMemberEvent(ChannelMembersEventData(
                    channel = channel,
                    members = addedMembers,
                    eventType = ChannelMembersEventEnum.Added
                ))
            }
        }
        return response
    }

    override suspend fun blockAndDeleteMember(channelId: Long, memberId: String): SceytResponse<SceytChannel> {
        val response = channelsRepository.blockAndDeleteMember(channelId, memberId)
        response.onSuccessNotNull {channel->
            channel.members?.let { addedMembers ->
                onChannelMemberEvent(ChannelMembersEventData(
                    channel = channel,
                    members = addedMembers,
                    eventType = ChannelMembersEventEnum.Kicked
                ))
            }
        }

        return response
    }

    override suspend fun deleteMember(channelId: Long, memberId: String): SceytResponse<SceytChannel> {
        val response = channelsRepository.deleteMember(channelId, memberId)

        response.onSuccessNotNull {channel->
            channel.members?.let { addedMembers ->
                onChannelMemberEvent(ChannelMembersEventData(
                    channel = channel,
                    members = addedMembers,
                    eventType = ChannelMembersEventEnum.Kicked
                ))
            }
        }

        return response
    }

    override suspend fun getMembersCountFromDb(channelId: Long): Int {
        return memberDao.getMembersCount(channelId)
    }
}