package com.sceyt.sceytchatuikit.persistence.logics

import android.util.Log
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelOwnerChangedEventData
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig.CHANNELS_MEMBERS_LOAD_SIZE
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.SceytChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytGroupChannel
import com.sceyt.sceytchatuikit.data.models.channels.SceytMember
import com.sceyt.sceytchatuikit.data.repositories.ChannelsRepository
import com.sceyt.sceytchatuikit.data.toMember
import com.sceyt.sceytchatuikit.extensions.TAG
import com.sceyt.sceytchatuikit.persistence.dao.ChannelDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.UserChatLink
import com.sceyt.sceytchatuikit.persistence.mappers.toSceytMember
import com.sceyt.sceytchatuikit.persistence.mappers.toUserEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class PersistenceMembersLogicImpl(
        private val channelsRepository: ChannelsRepository,
        private val channelDao: ChannelDao,
        private val usersDao: UserDao) : PersistenceMembersLogic {

    override fun onChannelMemberEvent(data: ChannelMembersEventData) {
        if (data.channel == null || data.members == null) return
        val chatId = data.channel.id
        when (data.eventType) {
            com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Role, com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Added -> {
                usersDao.insertUsers(data.members.map { it.toUserEntity() })
                channelDao.insertUserChatLinks(data.members.map {
                    UserChatLink(userId = it.id, chatId = chatId, role = it.role.name)
                })
            }
            com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Kicked, com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.Blocked -> {
                channelDao.deleteUserChatLinks(chatId, *data.members.map { it.id }.toTypedArray())
            }
            com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelMembersEventEnum.UnBlocked -> {}
        }
    }

    override fun onChannelOwnerChangedEvent(data: ChannelOwnerChangedEventData) {
        channelDao.updateOwner(data.channel.id, data.oldOwner.id, data.newOwner.id)
    }

    override suspend fun loadChannelMembers(channelId: Long, offset: Int): Flow<PaginationResponse<SceytMember>> {
        val normalizedOffset = offset / CHANNELS_MEMBERS_LOAD_SIZE * CHANNELS_MEMBERS_LOAD_SIZE
        Log.i(TAG, "normalizedOffset  old->$offset normalized-> $normalizedOffset")
        return callbackFlow {
            val dbMembers = getMembersDb(channelId, normalizedOffset, CHANNELS_MEMBERS_LOAD_SIZE)
            val hasNextDb = dbMembers.size == CHANNELS_MEMBERS_LOAD_SIZE
            if (dbMembers.isNotEmpty())
                trySend(PaginationResponse.DBResponse(dbMembers, normalizedOffset, hasNextDb))

            val response = channelsRepository.loadChannelMembers(channelId, normalizedOffset)

            if (response is SceytResponse.Success) {
                saveMembersToDb(channelId, response.data)
                // Check has removed items, and if exist delete from DB
                getRemovedItemsAndDeleteFromDb(channelId, dbMembers, response.data)

                // Get new updated items from DB
                val updatedMembers = getMembersDb(channelId, 0, normalizedOffset + CHANNELS_MEMBERS_LOAD_SIZE)
                val hasNextServer = response.data?.size == CHANNELS_MEMBERS_LOAD_SIZE
                trySend(PaginationResponse.ServerResponse(response, updatedMembers, normalizedOffset, hasNextServer))
            } else
                trySend(PaginationResponse.ServerResponse(response, arrayListOf(), 0))

            awaitClose()
        }
    }

    private fun getMembersDb(channelId: Long, offset: Int, limit: Int): List<SceytMember> {
        return channelDao.getChannelMembers(channelId, limit = limit, offset = offset)
            .map { memberEntity -> memberEntity.toSceytMember() }
    }

    private fun saveMembersToDb(channelId: Long, list: List<SceytMember>?) {
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

    private fun getRemovedItemsAndDeleteFromDb(channelId: Long, dbMembers: List<SceytMember>, serverResponse: List<SceytMember>?): List<SceytMember> {
        serverResponse ?: return emptyList()
        val removedItems: List<SceytMember> = dbMembers.minus(serverResponse.toSet())
        if (removedItems.isNotEmpty()) {
            Log.i(TAG, "removed items  ${removedItems.map { it.fullName }}")
            (dbMembers as ArrayList).removeAll(removedItems.toSet())
            channelDao.deleteUserChatLinks(channelId, *removedItems.map { it.user.id }.toTypedArray())
        }
        return removedItems
    }

    override suspend fun changeChannelOwner(channel: SceytChannel, newOwnerId: String): SceytResponse<SceytChannel> {
        require(channel is SceytGroupChannel) { "Channel must be group" }
        val response = channelsRepository.changeChannelOwner(channel, newOwnerId)

        if (response is SceytResponse.Success) {
            (response.data as? SceytGroupChannel)?.members?.getOrNull(0)?.let { member ->
                channelDao.updateOwner(channelId = channel.id, newOwnerId = member.id)
            }
        }
        return response
    }

    override suspend fun changeChannelMemberRole(channel: SceytChannel, member: SceytMember): SceytResponse<SceytChannel> {
        require(channel is SceytGroupChannel) { "Channel must be group" }
        val response = channelsRepository.changeChannelMemberRole(channel, member.toMember())

        if (response is SceytResponse.Success) {
            (response.data as? SceytGroupChannel)?.members?.let { members ->
                channelDao.insertUserChatLinks(members.map { sceytMember ->
                    UserChatLink(
                        userId = sceytMember.id,
                        chatId = channel.id,
                        role = sceytMember.role.name
                    )
                })
            }
        }
        return response
    }

    override suspend fun addMembersToChannel(channel: SceytChannel, members: List<Member>): SceytResponse<SceytChannel> {
        require(channel is SceytGroupChannel) { "Channel must be group" }
        val response = channelsRepository.addMembersToChannel(channel, members)

        if (response is SceytResponse.Success) {
            usersDao.insertUsers(members.map { it.toUserEntity() })
            channelDao.insertUserChatLinks(members.map {
                UserChatLink(userId = it.id, chatId = channel.id, role = it.role.name)
            })
        }
        return response
    }

    override suspend fun blockAndDeleteMember(channel: SceytChannel, memberId: String): SceytResponse<SceytChannel> {
        require(channel is SceytGroupChannel) { "Channel must be group" }
        val response = channelsRepository.blockAndDeleteMember(channel, memberId)

        if (response is SceytResponse.Success)
            channelDao.deleteUserChatLinks(channel.id, memberId)

        return response
    }

    override suspend fun deleteMember(channel: SceytChannel, memberId: String): SceytResponse<SceytChannel> {
        require(channel is SceytGroupChannel) { "Channel must be group" }
        val response = channelsRepository.deleteMember(channel, memberId)

        if (response is SceytResponse.Success)
            channelDao.deleteUserChatLinks(channel.id, memberId)

        return response
    }

    override suspend fun blockUnBlockUser(userId: String, block: Boolean): SceytResponse<List<User>> {
        val response = if (block) {
            channelsRepository.blockUser(userId)
        } else
            channelsRepository.unblockUser(userId)

        if (response is SceytResponse.Success)
            usersDao.blockUnBlockUser(userId, block)

        return response
    }
}