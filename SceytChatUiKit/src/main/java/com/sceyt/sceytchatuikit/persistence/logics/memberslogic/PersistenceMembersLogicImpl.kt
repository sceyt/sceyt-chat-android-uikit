package com.sceyt.sceytchatuikit.persistence.logics.memberslogic

import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.SceytKitClient
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelOwnerChangedEventData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.repositories.ChannelsRepository
import com.sceyt.sceytchatuikit.data.toMember
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.dao.ChannelDao
import com.sceyt.sceytchatuikit.persistence.dao.MembersDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.UserChatLink
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.ChannelsCache
import com.sceyt.sceytchatuikit.persistence.logics.channelslogic.PersistenceChannelsLogic
import com.sceyt.sceytchatuikit.persistence.mappers.toChannel
import com.sceyt.sceytchatuikit.persistence.mappers.toChannelEntity
import com.sceyt.sceytchatuikit.persistence.mappers.toMessageDb
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytMember
import com.sceyt.sceytchatuikit.persistence.mappers.toUserEntity
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.CHANNELS_MEMBERS_LOAD_SIZE
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.koin.core.component.inject

internal class PersistenceMembersLogicImpl(
        private val channelsRepository: ChannelsRepository,
        private val channelDao: ChannelDao,
        private val messageDao: MessageDao,
        private val membersDao: MembersDao,
        private val usersDao: UserDao,
        private val channelsCache: ChannelsCache) : PersistenceMembersLogic, SceytKoinComponent {

    private val persistenceChannelsLogic: PersistenceChannelsLogic by inject()

    override suspend fun onChannelMemberEvent(data: ChannelMembersEventData) {
        val chatId = data.channel.id
        when (data.eventType) {
            ChannelMembersEventEnum.Role, ChannelMembersEventEnum.Added -> {
                usersDao.insertUsers(data.members.map { it.toUserEntity() })
                channelDao.insertUserChatLinks(data.members.map {
                    UserChatLink(userId = it.id, chatId = chatId, role = it.role.name)
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
                    channelsCache.add(data.channel)
                }
            }

            ChannelMembersEventEnum.Kicked, ChannelMembersEventEnum.Blocked -> {
                if (data.members.any { it.id == SceytKitClient.myId }) {
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
        membersDao.updateOwner(data.channel.id, data.oldOwner.id, data.newOwner.id)
    }

    override fun loadChannelMembers(channelId: Long, offset: Int, role: String?): Flow<PaginationResponse<SceytMember>> {
        val normalizedOffset = offset / CHANNELS_MEMBERS_LOAD_SIZE * CHANNELS_MEMBERS_LOAD_SIZE
        return callbackFlow {
            val dbMembers = getMembersDb(channelId, normalizedOffset, role, CHANNELS_MEMBERS_LOAD_SIZE)
            val hasNextDb = dbMembers.size == CHANNELS_MEMBERS_LOAD_SIZE
            if (dbMembers.isNotEmpty())
                trySend(PaginationResponse.DBResponse(dbMembers, null, normalizedOffset, hasNextDb))

            val response = channelsRepository.loadChannelMembers(channelId, normalizedOffset, role)

            if (response is SceytResponse.Success) {
                saveMembersToDb(channelId, response.data)
                // Check has removed items, and if exist delete from DB
                getRemovedItemsAndDeleteFromDb(channelId, dbMembers, response.data)

                // Get new updated items from DB
                val updatedMembers = getMembersDb(channelId, 0, role, normalizedOffset + CHANNELS_MEMBERS_LOAD_SIZE)
                val hasNextServer = response.data?.size == CHANNELS_MEMBERS_LOAD_SIZE
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
            membersDao.getChannelMembersWithRole(channelId, limit = limit, offset = offset, role)
        else membersDao.getChannelMembers(channelId, limit, offset)
        return data.map { memberEntity -> memberEntity.toSceytMember() }
    }

    override suspend fun loadChannelMembersByDisplayName(channelId: Long, name: String): List<SceytMember> {
        return membersDao.getChannelMembersByDisplayName(channelId, name).map { it.toSceytMember() }
    }

    override suspend fun filterOnlyMembersByIds(channelId: Long, ids: List<String>): List<String> {
        return membersDao.filterOnlyMembersByIds(channelId, ids)
    }

    override suspend fun loadChannelMembersByIds(channelId: Long, vararg ids: String): List<SceytMember> {
        return membersDao.getChannelMembersByIds(channelId, *ids).map { it.toSceytMember() }
    }

    private suspend fun saveMembersToDb(channelId: Long, list: List<SceytMember>?) {
        if (list.isNullOrEmpty()) return

        val links = arrayListOf<UserChatLink>()
        val users = arrayListOf<UserEntity>()

        list.forEach { member ->
            links.add(UserChatLink(userId = member.id, chatId = channelId, role = member.role.name))
            users.add(member.toUserEntity())
        }

        usersDao.insertUsers(users)
        channelDao.insertUserChatLinks(links)
    }

    private suspend fun getRemovedItemsAndDeleteFromDb(channelId: Long, dbMembers: List<SceytMember>,
                                                       serverResponse: List<SceytMember>?): List<SceytMember> {
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
        messageDao.deleteAllMessages(channelId)
        channelsCache.deleteChannel(channelId)
    }

    override suspend fun changeChannelOwner(channelId: Long, newOwnerId: String): SceytResponse<SceytChannel> {
        val response = channelsRepository.changeChannelOwner(channelId, newOwnerId)

        if (response is SceytResponse.Success) {
            response.data?.members?.getOrNull(0)?.let { member ->
                membersDao.updateOwner(channelId = channelId, newOwnerId = member.id)
                channelDao.getChannelById(channelId)?.let {
                    channelsCache.upsertChannel(it.toChannel())
                }
            }
        }
        return response
    }

    override suspend fun changeChannelMemberRole(channelId: Long, vararg member: SceytMember): SceytResponse<SceytChannel> {
        val response = channelsRepository.changeChannelMemberRole(channelId, *member.map { it.toMember() }.toTypedArray())

        if (response is SceytResponse.Success) {
            response.data?.members?.let { members ->
                channelDao.insertUserChatLinks(members.map { sceytMember ->
                    UserChatLink(
                        userId = sceytMember.id,
                        chatId = channelId,
                        role = sceytMember.role.name
                    )
                })
            }
        }
        return response
    }

    override suspend fun addMembersToChannel(channelId: Long, members: List<Member>): SceytResponse<SceytChannel> {
        val response = channelsRepository.addMembersToChannel(channelId, members)

        if (response is SceytResponse.Success) {
            usersDao.insertUsers(members.map { it.toUserEntity() })
            channelDao.insertUserChatLinks(members.map {
                UserChatLink(userId = it.id, chatId = channelId, role = it.role.name)
            })
            response.data?.let { channelsCache.updateMembersCount(it) }
        }
        return response
    }

    override suspend fun blockAndDeleteMember(channelId: Long, memberId: String): SceytResponse<SceytChannel> {
        val response = channelsRepository.blockAndDeleteMember(channelId, memberId)

        if (response is SceytResponse.Success) {
            channelDao.deleteUserChatLinks(channelId, memberId)
            response.data?.let { channelsCache.updateMembersCount(it) }
        }

        return response
    }

    override suspend fun deleteMember(channelId: Long, memberId: String): SceytResponse<SceytChannel> {
        val response = channelsRepository.deleteMember(channelId, memberId)

        if (response is SceytResponse.Success) {
            channelDao.deleteUserChatLinks(channelId, memberId)
            response.data?.let { channelsCache.updateMembersCount(it) }
        }

        return response
    }

    override suspend fun blockUnBlockUser(userId: String, block: Boolean): SceytResponse<List<User>> {
        val response = if (block) {
            channelsRepository.blockUser(userId)
        } else
            channelsRepository.unblockUser(userId)

        if (response is SceytResponse.Success) {
            usersDao.blockUnBlockUser(userId, block)
            persistenceChannelsLogic.blockUnBlockUser(userId, block)
        }

        return response
    }

    override suspend fun getMembersCountDb(channelId: Long): Int {
        return membersDao.getMembersCount(channelId)
    }
}