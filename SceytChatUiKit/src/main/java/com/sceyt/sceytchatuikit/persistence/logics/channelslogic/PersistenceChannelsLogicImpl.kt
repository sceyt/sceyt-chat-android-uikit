package com.sceyt.sceytchatuikit.persistence.logics.channelslogic

import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.Message
import com.sceyt.chat.models.user.User
import com.sceyt.sceytchatuikit.data.SceytSharedPreference
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelUnreadCountUpdatedEventData
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver.awaitToConnectSceyt
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.ChannelsRepository
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.data.toSceytUiChannel
import com.sceyt.sceytchatuikit.persistence.dao.ChannelDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.UserChatLink
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.mappers.*
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.CHANNELS_LOAD_SIZE
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onCompletion

internal class PersistenceChannelsLogicImpl(
        private val channelsRepository: ChannelsRepository,
        private val channelDao: ChannelDao,
        private val usersDao: UserDao,
        private val messageDao: MessageDao,
        private val preference: SceytSharedPreference,
        private val channelsCash: ChannelsCash) : PersistenceChannelsLogic {

    override suspend fun onChannelEvent(data: ChannelEventData) {
        when (data.eventType) {
            Created, Joined -> {
                onChanelCreatedOrJoined(data.channel)
            }
            Deleted -> {
                data.channelId?.let { channelId -> deleteChannelDb(channelId) }
            }
            Left -> {
                val leftUser = (data.channel as? GroupChannel)?.members?.getOrNull(0)?.id
                if (leftUser == preference.getUserId()) {
                    data.channelId?.let { channelId -> deleteChannelDb(channelId) }
                }
            }
            ClearedHistory -> {
                data.channelId?.let { channelId ->
                    channelDao.updateLastMessage(channelId, null, null)
                    channelsCash.clearedHistory(channelId)
                }
            }
            Updated -> {
                data.channel?.let { channel ->
                    channelDao.insertChannel(channel.toChannelEntity())
                    channelsCash.upsertChannel(channel.toSceytUiChannel())
                }
            }
            Muted -> {
                data.channelId?.let { channelId ->
                    val time = data.channel?.muteExpireDate()?.time ?: 0
                    channelDao.updateMuteState(channelId, true, time)
                    channelsCash.updateMuteState(channelId, true, time)
                }
            }
            UnMuted -> {
                data.channelId?.let { channelId ->
                    channelDao.updateMuteState(channelId, false)
                    channelsCash.updateMuteState(channelId, false)
                }
            }
            MarkedUsUnread -> updateChannelDbAndCash(data.channel?.toSceytUiChannel())
            Blocked, UnBlocked -> deleteChannelDb(data.channelId ?: return)
            Invited -> TODO()
            Hidden -> TODO()
            UnHidden -> TODO()
        }
    }

    private suspend fun onChanelCreatedOrJoined(channel: Channel?) {
        channel?.let {
            val members = if (it is GroupChannel) it.members else arrayListOf((it as DirectChannel).peer)
            val sceytChannel = channel.toSceytUiChannel()
            insertChannel(sceytChannel, *members.map { member -> member.toSceytMember() }.toTypedArray())
            channelsCash.add(sceytChannel)
        }
    }

    override suspend fun onChannelUnreadCountUpdatedEvent(data: ChannelUnreadCountUpdatedEventData) {
        updateChannelDbAndCash((data.channel ?: return).toSceytUiChannel())
    }

    override suspend fun onChannelMarkersUpdated(data: MessageStatusChangeData) {
        updateChannelDbAndCash(data.channel)
    }

    override suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>) {
        updateChannelDbAndCash(data.first)
    }

    override suspend fun onMessageEditedOrDeleted(data: Message) {
        channelsCash.get(data.channelId)?.let { channel ->
            channel.lastMessage?.let { lastMessage ->
                if (lastMessage.id == data.id) {
                    channelsCash.updateLastMessage(data.channelId, data.toSceytUiMessage(channel.isGroup))
                }
            } ?: run {
                channel.lastMessage = data.toSceytUiMessage(channel.isGroup)
                channelsCash.upsertChannel(channel)
            }
        }
    }

    private suspend fun updateChannelDbAndCash(channel: SceytChannel?) {
        channel ?: return
        channelDao.updateChannel(channel.toChannelEntity(preference.getUserId()))
        channelsCash.upsertChannel(channel)
    }

    private suspend fun insertChannel(channel: SceytChannel, vararg members: SceytMember) {
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

    override suspend fun loadChannels(offset: Int, searchQuery: String, loadKey: Long,
                                      ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>> {
        return callbackFlow {
            if (offset == 0) channelsCash.clear()

            val dbChannels = getChannelsDb(offset, searchQuery)
            var hasNext = dbChannels.size == CHANNELS_LOAD_SIZE

            channelsCash.addAll(dbChannels.map { it.clone() }, false)
            trySend(PaginationResponse.DBResponse(data = dbChannels, loadKey = loadKey, offset = offset,
                hasNext = hasNext, hasPrev = false))

            awaitToConnectSceyt()

            val response = if (offset == 0) channelsRepository.getChannels(searchQuery)
            else channelsRepository.loadMoreChannels()

            if (response is SceytResponse.Success) {
                val channels = response.data ?: arrayListOf()

                saveChannelsToDb(channels)
                val hasDiff = channelsCash.addAll(channels.map { it.clone() }, true)
                hasNext = response.data?.size == CHANNELS_LOAD_SIZE

                trySend(PaginationResponse.ServerResponse(data = response, cashData = channelsCash.getSorted(),
                    loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext, hasPrev = false,
                    loadType = LoadNext, ignoredDb = ignoreDb))
            }

            channel.close()
            awaitClose()
        }
    }

    override suspend fun syncChannels(limit: Int): Flow<SceytResponse<List<SceytChannel>>> {
        return callbackFlow {
            val oldChannelsIds = channelDao.getAllChannelsIds().toSet()
            awaitToConnectSceyt()
            val syncedChannels = arrayListOf<SceytChannel>()
            channelsRepository.getAllChannels(limit)
                .onCompletion {
                    if (syncedChannels.isNotEmpty()) {
                        val syncedIds = syncedChannels.map { it.id }
                        val deletedChannels = channelDao.getNotExistingChannelIdsByIds(syncedIds)
                        val addedChannelsIds = syncedIds.minus(oldChannelsIds)

                        deletedChannels.forEach { deleteChannelDb(channelId = it) }
                        upsertChannelsToCash(syncedChannels.filter { addedChannelsIds.contains(it.id) })
                    }
                    channel.close()
                }
                .collect { response ->
                    if (response is SceytResponse.Success) {
                        response.data?.let {
                            saveChannelsToDb(it)
                            channelsCash.upsertChannel(*it.toTypedArray())
                            syncedChannels.addAll(it)
                        }
                    }
                    trySend(response)
                }

            awaitClose()
        }
    }

    private suspend fun getChannelsDb(offset: Int, searchQuery: String): List<SceytChannel> {
        return if (searchQuery.isBlank()) {
            channelDao.getChannels(limit = CHANNELS_LOAD_SIZE, offset = offset).map { channel -> channel.toChannel() }
        } else {
            channelDao.getChannelsByQuery(
                limit = CHANNELS_LOAD_SIZE,
                offset = offset,
                query = searchQuery
            ).map { channel -> channel.toChannel() }
        }
    }

    private suspend fun saveChannelsToDb(list: List<SceytChannel>) {
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
                lastMessage.parent?.let { parent ->
                    lastMessages.add(parent.toMessageDb())
                    if (lastMessage.incoming)
                        parent.from?.let { user -> users.add(user.toUserEntity()) }
                }

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
                insertChannel(channel, SceytMember(user))
                channelsCash.add(channel)
            }
        }

        return response
    }

    override suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel> {
        val response = channelsRepository.createChannel(createChannelData)

        if (response is SceytResponse.Success) {
            response.data?.let { channel ->
                insertChannel(channel, *(channel as SceytGroupChannel).members.toTypedArray())
                channelsCash.add(channel)
            }
        }

        return response
    }

    override suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.markChannelAsRead(channelId)

        if (response is SceytResponse.Success) {
            response.data?.let {
                messageDao.updateAllMessagesStatusAsRead(channelId)
                updateChannelDbAndCash(it)
            }
        }

        return response
    }

    override suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.markChannelAsUnRead(channelId)

        if (response is SceytResponse.Success)
            response.data?.let {
                updateChannelDbAndCash(it)
            }

        return response
    }

    override suspend fun clearHistory(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.clearHistory(channelId)

        if (response is SceytResponse.Success) {
            channelDao.updateLastMessage(channelId, null, null)
            messageDao.deleteAllMessages(channelId)
            channelsCash.clearedHistory(channelId)
        }

        return response
    }

    override suspend fun blockAndLeaveChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.blockChannel(channelId)

        if (response is SceytResponse.Success)
            deleteChannelDb(channelId)

        return response
    }

    override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.leaveChannel(channelId)

        if (response is SceytResponse.Success)
            deleteChannelDb(channelId)

        return response
    }

    override suspend fun deleteChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.deleteChannel(channelId)

        if (response is SceytResponse.Success)
            deleteChannelDb(channelId)

        return response
    }

    private suspend fun deleteChannelDb(channelId: Long) {
        channelDao.deleteChannelAndLinks(channelId)
        messageDao.deleteAllMessages(channelId)
        channelsCash.deleteChannel(channelId)
    }

    private fun upsertChannelsToCash(channels: List<SceytChannel>) {
        if (channels.isEmpty()) return
        channelsCash.upsertChannel(*channels.toTypedArray())
    }

    override suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.muteChannel(channelId, muteUntil)

        if (response is SceytResponse.Success) {
            channelDao.updateMuteState(channelId = channelId, muted = true, muteUntil = muteUntil)
            channelsCash.updateMuteState(channelId, true, muteUntil)
        }

        return response
    }

    override suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.unMuteChannel(channelId)

        if (response is SceytResponse.Success) {
            channelDao.updateMuteState(channelId = channelId, muted = false)
            channelsCash.updateMuteState(channelId, false)
        }

        return response
    }

    override suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.getChannel(channelId)

        if (response is SceytResponse.Success)
            response.data?.toChannelEntity(preference.getUserId())?.let {
                channelDao.insertChannel(it)
                channelsCash.upsertChannel(response.data)
            }

        return response
    }

    override suspend fun getChannelFromServerByUrl(url: String): SceytResponse<List<SceytChannel>> {
        //Not use yet
        return channelsRepository.getChannelFromServerByUrl(url)
    }

    override suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel> {
        var newUrl = data.avatarUrl
        if (data.avatarEdited && data.avatarUrl != null) {
            when (val uploadResult = channelsRepository.uploadAvatar(data.avatarUrl)) {
                is SceytResponse.Success -> {
                    newUrl = uploadResult.data
                }
                is SceytResponse.Error -> return SceytResponse.Error(uploadResult.exception)

            }
        }
        val response = channelsRepository.editChannel(channelId, data)
        if (response is SceytResponse.Success) {
            channelDao.updateChannelSubjectAndAvatarUrl(channelId, data.newSubject, newUrl)
            channelsCash.updateChannelSubjectAndAvatarUrl(channelId, data.newSubject, newUrl)
        }

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

                    channelsCash.addedMembers(channelId, sceytMember)
                }
            }

        return response
    }

    override suspend fun updateLastMessageWithLastRead(channelId: Long, message: SceytMessage) {
        if (message.deliveryStatus == DeliveryStatus.Pending) {
            channelDao.updateLastMessage(channelId, message.tid, message.createdAt)
            channelsCash.updateLastMessage(channelId, message)
        } else {
            channelDao.updateLastMessageWithLastRead(channelId, message.tid, message.id, message.createdAt)
            channelsCash.updateLastMessageWithLastRead(channelId, message)
        }
    }

    override suspend fun setUnreadCount(channelId: Long, count: Int) {
        channelDao.updateUnreadCount(channelId, count)
        channelsCash.updateUnreadCount(channelId, count)
    }
}