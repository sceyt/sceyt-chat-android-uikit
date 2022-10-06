package com.sceyt.sceytchatuikit.persistence.logics.channelslogic

import com.sceyt.chat.Types
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.member.Member
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionObserver
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
import com.sceyt.sceytchatuikit.sceytconfigs.SceytUIKitConfig.CHANNELS_LOAD_SIZE
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume

internal class PersistenceChannelsLogicImpl(
        private val channelsRepository: ChannelsRepository,
        private val channelDao: ChannelDao,
        private val usersDao: UserDao,
        private val messageDao: MessageDao,
        private val preference: SceytSharedPreference) : PersistenceChannelsLogic {

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
                if (leftUser == preference.getUserId()) {
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
        //Message tid is message id
        val tid = lastMsg.tid
        channelDao.updateLastMessage(data.first.id, tid, lastMsg.createdAt)
    }

    private fun insertChannel(channel: SceytChannel, vararg members: Member) {
        val users = members.map { it.toUserEntity() }
        channel.lastMessage?.let {
            it.lastReactions?.map { reaction -> reaction.user }?.let { it1 ->
                (users as ArrayList).addAll(it1.map { user -> user.toUserEntity() })
            }
        }
        usersDao.insertUsers(members.map { it.toUserEntity() })
        channelDao.insertChannelAndLinks(channel.toChannelEntity(preference.getUserId()), members.map {
            UserChatLink(userId = it.id, chatId = channel.id, role = it.role.name)
        })
    }

    override suspend fun loadChannels(offset: Int, searchQuery: String): Flow<PaginationResponse<SceytChannel>> {
        return callbackFlow {
            val dbChannels = getChannelsDb(offset, searchQuery)
            trySend(PaginationResponse.DBResponse(dbChannels, offset, hasNext = dbChannels.size == CHANNELS_LOAD_SIZE))

            awaitToConnectSceyt()

            val response = if (offset == 0) channelsRepository.getChannels(searchQuery)
            else channelsRepository.loadMoreChannels()

            val hasNext = response is SceytResponse.Success && response.data?.size == CHANNELS_LOAD_SIZE
            trySend(PaginationResponse.ServerResponse(data = response, offset = offset, dbData = arrayListOf(), hasNext = hasNext))

            if (response is SceytResponse.Success)
                saveChannelsToDb(response.data ?: return@callbackFlow)

            awaitClose()
        }
    }

    private suspend fun awaitToConnectSceyt(): Boolean {
        if (ConnectionObserver.connectionState == Types.ConnectState.StateConnected)
            return true

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return suspendCancellableCoroutine { continuation ->
            scope.launch {
                ConnectionObserver.onChangedConnectStatusFlow.collect {
                    if (it.first == Types.ConnectState.StateConnected) {
                        continuation.resume(true)
                        scope.cancel()
                    }
                }
            }
        }
    }

    private fun getChannelsDb(offset: Int, searchQuery: String): List<SceytChannel> {
        return if (searchQuery.isBlank())
            channelDao.getChannels(limit = CHANNELS_LOAD_SIZE, offset = offset)
                .map { channel -> channel.toChannel() }
        else channelDao.getChannelsByQuery(
            limit = CHANNELS_LOAD_SIZE,
            offset = offset,
            query = searchQuery
        ).map { channel -> channel.toChannel() }
    }

    private fun saveChannelsToDb(list: List<SceytChannel>) {
        if (list.isEmpty()) return

        val links = arrayListOf<UserChatLink>()
        val users = arrayListOf<UserEntity>()
        val lastMessages = arrayListOf<MessageDb>()

        fun addEntitiesToLists(channelId: Long, members: List<SceytMember>, lastMessage: SceytMessage?) {
            members.forEach { member ->
                links.add(UserChatLink(userId = member.id, chatId = channelId, role = member.role.name))
                users.add(member.toUserEntity())
            }

            lastMessage?.let {
                lastMessages.add(it.toMessageDb())

                //Add user from last message
                it.from?.let { user ->
                    // Add if not exist
                    users.find { entity -> entity.id == user.id } ?: run {
                        users.add(user.toUserEntity())
                    }
                }

                //Add users from reactions
                it.lastReactions?.let { lastReactions ->
                    users.addAll(lastReactions.map { reaction -> reaction.user.toUserEntity() })
                }
            }
        }

        list.forEach { channel ->
            if (channel.isGroup) {
                addEntitiesToLists(channel.id, (channel as SceytGroupChannel).members, channel.lastMessage)
            } else {
                val members = arrayListOf<SceytMember>()
                (channel as SceytDirectChannel).peer?.let {
                    members.add(it)
                }
                addEntitiesToLists(channel.id, members, channel.lastMessage)
            }
        }
        usersDao.insertUsers(users)
        messageDao.insertMessages(lastMessages)
        channelDao.insertChannelsAndLinks(list.map { it.toChannelEntity(preference.getUserId()) }, links)
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

    override suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.markAsRead(channelId)

        if (response is SceytResponse.Success) {
            response.data?.let {
                messageDao.updateAllMessagesStatusAsRead(channelId)
                channelDao.updateChannel(it.toChannelEntity(preference.getUserId()))
            }
        }

        return response
    }

    override suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.markAsUnRead(channelId)

        if (response is SceytResponse.Success)
            response.data?.let {
                channelDao.updateChannel(it.toChannelEntity(preference.getUserId()))
            }

        return response
    }

    override suspend fun clearHistory(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.clearHistory(channelId)

        if (response is SceytResponse.Success) {
            channelDao.updateLastMessage(channelId, null, null)
            messageDao.deleteAllMessages(channelId)
        }

        return response
    }

    override suspend fun blockAndLeaveChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.blockChannel(channelId)

        if (response is SceytResponse.Success) {
            channelDao.deleteChannelAndLinks(channelId)
            messageDao.deleteAllMessages(channelId)
        }

        return response
    }

    override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.leaveChannel(channelId)

        if (response is SceytResponse.Success) {
            channelDao.deleteChannelAndLinks(channelId)
            messageDao.deleteAllMessages(channelId)
        }

        return response
    }

    override suspend fun deleteChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.deleteChannel(channelId)

        if (response is SceytResponse.Success) {
            channelDao.deleteChannelAndLinks(channelId)
            messageDao.deleteAllMessages(channelId)
        }

        return response
    }

    override suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.muteChannel(channelId, muteUntil)

        if (response is SceytResponse.Success)
            channelDao.updateMuteState(channelId = channelId, muted = true, muteUntil = muteUntil)

        return response
    }

    override suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.unMuteChannel(channelId)

        if (response is SceytResponse.Success)
            channelDao.updateMuteState(channelId = channelId, muted = false)

        return response
    }

    override suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.getChannel(channelId)

        if (response is SceytResponse.Success)
            response.data?.toChannelEntity(preference.getUserId())?.let {
                channelDao.insertChannel(it)
            }

        return response
    }

    override suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel> {
        var newUrl = data.avatarUrl
        if (data.avatarEdited && data.avatarUrl != null) {
            val uploadResult = channelsRepository.uploadAvatar(data.avatarUrl)
            if (uploadResult is SceytResponse.Success) {
                newUrl = uploadResult.data
            } else
                return SceytResponse.Error(uploadResult.message)
        }
        val response = channelsRepository.editChannel(channelId, data)
        if (response is SceytResponse.Success)
            channelDao.updateChannelSubjectAndAvatarUrl(channelId, data.newSubject, newUrl)

        return response
    }

    override suspend fun join(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.join(channelId)

        if (response is SceytResponse.Success)
            response.data?.let {
                (it as SceytGroupChannel).members.getOrNull(0)?.let { sceytMember ->
                    channelDao.insertUserChatLink(UserChatLink(
                        userId = sceytMember.id,
                        chatId = it.id,
                        role = sceytMember.role.name))
                }
            }

        return response
    }
}