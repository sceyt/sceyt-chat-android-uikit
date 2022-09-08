package com.sceyt.sceytchatuikit.persistence.logics

import com.sceyt.chat.ClientWrapper
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.message.MessageListMarker
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.ChannelsRepository
import com.sceyt.sceytchatuikit.data.toSceytUiChannel
import com.sceyt.sceytchatuikit.persistence.dao.ChannelDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.UserChatLink
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.mappers.toChannel
import com.sceyt.sceytchatuikit.persistence.mappers.toChannelEntity
import com.sceyt.sceytchatuikit.persistence.mappers.toMessageDb
import com.sceyt.sceytchatuikit.persistence.mappers.toUserEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class PersistenceChannelsLogicImpl(
        private val channelsRepository: ChannelsRepository,
        private val channelDao: ChannelDao,
        private val usersDao: UserDao,
        private val messageDao: MessageDao) : PersistenceChannelsLogic {

    override fun onChannelEvent(data: ChannelEventData) {
        when (data.eventType) {
            Created -> {
                data.channel?.let { channel ->
                    val members = if (channel is GroupChannel) channel.members else arrayListOf((channel as DirectChannel).peer)
                    insertChannel(channel.toSceytUiChannel(), *members.toTypedArray())
                }
            }
            Deleted -> {
                data.channelId?.let { channelId ->
                    channelDao.deleteChannelAndLinks(channelId)
                    messageDao.deleteAllMessages(channelId)
                }
            }
            Left -> {
                val leftUser = (data.channel as? GroupChannel)?.members?.getOrNull(0)?.id
                if (leftUser == ClientWrapper.currentUser.id) {
                    data.channelId?.let { channelId ->
                        channelDao.deleteChannelAndLinks(channelId)
                        messageDao.deleteAllMessages(channelId)
                    }
                }
            }
            ClearedHistory -> {
                data.channelId?.let { channelId ->
                    channelDao.updateLastMessage(channelId, null, null)
                }
            }
            Updated -> {
                data.channel?.let { channel ->
                    channelDao.insertChannel(channel.toChannelEntity())
                }
            }
            Muted -> {
                data.channelId?.let { channelId ->
                    channelDao.updateMuteState(channelId, true, data.channel?.muteExpireDate()?.time)
                }
            }
            UnMuted -> {
                data.channelId?.let { channelId ->
                    channelDao.updateMuteState(channelId, false)
                }
            }
            else -> return
        }
    }

    override fun onMessage(data: Pair<SceytChannel, SceytMessage>) {
        val lastMsg = data.second
        channelDao.updateLastMessage(data.first.id, lastMsg.id, lastMsg.createdAt)
    }

    private fun insertChannel(channel: SceytChannel, vararg members: Member) {
        usersDao.insertUsers(members.map { it.toUserEntity() })
        channelDao.insertChannelAndLinks(channel.toChannelEntity(), members.map {
            UserChatLink(userId = it.id, chatId = channel.id, role = it.role.name)
        })
    }

    override suspend fun loadChannels(offset: Int, searchQuery: String): Flow<PaginationResponse<SceytChannel>> {
        return callbackFlow {
            val dbChannels = getChannelsDb(offset, searchQuery)
            trySend(PaginationResponse.DBResponse(dbChannels, offset))

            val response = if (offset == 0) channelsRepository.getChannels(searchQuery)
            else channelsRepository.loadMoreChannels()

            trySend(PaginationResponse.ServerResponse(data = response, offset = offset, dbData = arrayListOf()))

            if (response is SceytResponse.Success) {
                saveChannelsToDb(response.data ?: return@callbackFlow)
            }
            awaitClose()
        }
    }

    private fun getChannelsDb(offset: Int, searchQuery: String): List<SceytChannel> {
        return if (searchQuery.isBlank())
            channelDao.getChannels(limit = SceytUIKitConfig.CHANNELS_LOAD_SIZE, offset = offset)
                .map { channel -> channel.toChannel() }
        else channelDao.getChannelsByQuery(
            limit = SceytUIKitConfig.CHANNELS_LOAD_SIZE,
            offset = offset,
            query = searchQuery
        ).map { channel -> channel.toChannel() }
    }

    private fun saveChannelsToDb(list: List<SceytChannel>) {
        if (list.isEmpty()) return

        val links = arrayListOf<UserChatLink>()
        val users = arrayListOf<UserEntity>()
        val lastMessages = arrayListOf<MessageDb>()

        list.forEach { channel ->
            if (channel.isGroup) {
                (channel as SceytGroupChannel).members.forEach { member ->
                    links.add(UserChatLink(userId = member.id, chatId = channel.id, role = member.role.name))
                    users.add(member.toUserEntity())
                    channel.lastMessage?.let {
                        lastMessages.add(it.toMessageDb())
                    }
                }
            } else {
                val peer = (channel as SceytDirectChannel).peer ?: return
                links.add(UserChatLink(userId = peer.id, chatId = channel.id, role = peer.role.name))
                users.add(peer.toUserEntity())
                channel.lastMessage?.let {
                    lastMessages.add(it.toMessageDb())
                }
            }
        }
        usersDao.insertUsers(users)
        messageDao.insertMessages(lastMessages)
        channelDao.insertChannelsAndLinks(list.map { it.toChannelEntity() }, links)
    }

    override suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel> {
        val response = channelsRepository.createDirectChannel(user)

        if (response is SceytResponse.Success) {
            response.data?.let { channel ->
                insertChannel(channel, Member(Role(RoleTypeEnum.Member.toString()), user))
            }
        }

        return response
    }

    override suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel> {
        val response = channelsRepository.createChannel(createChannelData)

        if (response is SceytResponse.Success) {
            response.data?.let { channel ->
                insertChannel(channel, *createChannelData.members.toTypedArray())
            }
        }

        return response
    }

    override suspend fun markChannelAsRead(channel: SceytChannel): SceytResponse<MessageListMarker> {
        val response = channelsRepository.markAsRead(channel)

        if (response is SceytResponse.Success)
            messageDao.updateAllMessagesStatusAsRead(channel.id)

        return response
    }

    override suspend fun clearHistory(channel: SceytChannel): SceytResponse<Long> {
        val response = channelsRepository.clearHistory(channel)

        if (response is SceytResponse.Success) {
            channelDao.updateLastMessage(channel.id, null, null)
            messageDao.deleteAllMessages(channel.id)
        }
        return response
    }

    override suspend fun blockAndLeaveChannel(channel: SceytChannel): SceytResponse<Long> {
        require(channel is SceytGroupChannel) { "Channel must be group" }
        val response = channelsRepository.blockChannel(channel)

        if (response is SceytResponse.Success) {
            channelDao.deleteChannelAndLinks(channel.id)
            messageDao.deleteAllMessages(channel.id)
        }

        return response
    }

    override suspend fun leaveChannel(channel: SceytChannel): SceytResponse<Long> {
        require(channel is SceytGroupChannel) { "Channel must be group" }
        val response = channelsRepository.leaveChannel(channel)

        if (response is SceytResponse.Success) {
            channelDao.deleteChannelAndLinks(channel.id)
            messageDao.deleteAllMessages(channel.id)
        }

        return response
    }

    override suspend fun deleteChannel(channel: SceytChannel): SceytResponse<Long> {
        val response = channelsRepository.deleteChannel(channel)

        if (response is SceytResponse.Success) {
            channelDao.deleteChannelAndLinks(channel.id)
            messageDao.deleteAllMessages(channel.id)
        }

        return response
    }

    override suspend fun muteChannel(channel: SceytChannel, muteUntil: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.muteChannel(channel, muteUntil)

        if (response is SceytResponse.Success)
            channelDao.updateMuteState(channelId = channel.id, muted = true, muteUntil = muteUntil)

        return response
    }

    override suspend fun unMuteChannel(channel: SceytChannel): SceytResponse<SceytChannel> {
        val response = channelsRepository.unMuteChannel(channel)

        if (response is SceytResponse.Success)
            channelDao.updateMuteState(channelId = channel.id, muted = false)

        return response
    }

    override suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.getChannel(channelId)

        if (response is SceytResponse.Success)
            response.data?.toChannelEntity()?.let { channelDao.insertChannel(it) }

        return response
    }

    override suspend fun editChannel(channel: SceytGroupChannel, newSubject: String, avatarUrl: String?): SceytResponse<SceytChannel> {
        var newUrl = avatarUrl
        val editedAvatar = channel.getChannelAvatarUrl() != avatarUrl
        if (editedAvatar && avatarUrl != null) {
            val uploadResult = channelsRepository.uploadAvatar(avatarUrl)
            if (uploadResult is SceytResponse.Success) {
                newUrl = uploadResult.data
            } else
                return SceytResponse.Error(uploadResult.message)
        }
        val response = channelsRepository.editChannel(channel, newSubject, newUrl)
        if (response is SceytResponse.Success)
            channelDao.updateChannelSubjectAndAvatarUrl(channel.id, newSubject, newUrl)

        return response
    }
}