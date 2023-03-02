package com.sceyt.sceytchatuikit.persistence.logics.channelslogic

import android.app.Application
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.WorkManager
import com.sceyt.chat.models.channel.Channel
import com.sceyt.chat.models.channel.DirectChannel
import com.sceyt.chat.models.channel.GroupChannel
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserActivityStatus
import com.sceyt.sceytchatuikit.SceytKitClient.myId
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelEventEnum.*
import com.sceyt.sceytchatuikit.data.channeleventobserver.ChannelUnreadCountUpdatedEventData
import com.sceyt.sceytchatuikit.data.connectionobserver.ConnectionEventsObserver.awaitToConnectSceyt
import com.sceyt.sceytchatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.sceytchatuikit.data.models.LoadKeyData
import com.sceyt.sceytchatuikit.data.models.PaginationResponse
import com.sceyt.sceytchatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.sceytchatuikit.data.models.SceytResponse
import com.sceyt.sceytchatuikit.data.models.channels.*
import com.sceyt.sceytchatuikit.data.models.messages.SceytMessage
import com.sceyt.sceytchatuikit.data.repositories.ChannelsRepository
import com.sceyt.sceytchatuikit.data.toSceytMember
import com.sceyt.sceytchatuikit.data.toSceytUiChannel
import com.sceyt.sceytchatuikit.di.SceytKoinComponent
import com.sceyt.sceytchatuikit.persistence.dao.ChannelDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.UserChatLink
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageDb
import com.sceyt.sceytchatuikit.persistence.logics.messageslogic.PersistenceMessagesLogic
import com.sceyt.sceytchatuikit.persistence.mappers.*
import com.sceyt.sceytchatuikit.sceytconfigs.SceytKitConfig.CHANNELS_LOAD_SIZE
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onCompletion
import org.koin.core.component.inject

internal class PersistenceChannelsLogicImpl(
        private val channelsRepository: ChannelsRepository,
        private val channelDao: ChannelDao,
        private val usersDao: UserDao,
        private val messageDao: MessageDao,
        private val application: Application,
        private val channelsCache: ChannelsCache) : PersistenceChannelsLogic, SceytKoinComponent {

    private val messageLogic: PersistenceMessagesLogic by inject()

    override suspend fun onChannelEvent(data: ChannelEventData) {
        when (data.eventType) {
            Created, Joined -> {
                onChanelCreatedOrJoinedOrUnHidden(data.channel)
            }
            Deleted -> {
                data.channelId?.let { channelId -> deleteChannelDb(channelId) }
            }
            Left -> {
                val leftUser = (data.channel as? GroupChannel)?.lastActiveMembers?.getOrNull(0)?.id
                        ?: return
                data.channelId?.let { channelId ->
                    if (leftUser == myId) {
                        deleteChannelDb(channelId)
                    } else
                        channelDao.deleteUserChatLinks(channelId, leftUser)
                }
            }
            ClearedHistory -> {
                data.channelId?.let { channelId ->
                    channelDao.updateLastMessage(channelId, null, null)
                    channelsCache.clearedHistory(channelId)
                }
            }
            Updated -> {
                data.channel?.let { channel ->
                    val sceytChannel = channel.toSceytUiChannel()
                    initPendingLastMessageBeforeInsert(sceytChannel)
                    channelDao.insertChannel(sceytChannel.toChannelEntity())
                    channelsCache.upsertChannel(sceytChannel)
                }
            }
            Muted -> {
                data.channelId?.let { channelId ->
                    val time = data.channel?.muteExpireDate()?.time ?: 0
                    channelDao.updateMuteState(channelId, true, time)
                    channelsCache.updateMuteState(channelId, true, time)
                }
            }
            UnMuted -> {
                data.channelId?.let { channelId ->
                    channelDao.updateMuteState(channelId, false)
                    channelsCache.updateMuteState(channelId, false)
                }
            }
            MarkedUsUnread -> updateChannelDbAndCache(data.channel?.toSceytUiChannel())
            Blocked -> deleteChannelDb(data.channelId ?: return)
            Hidden -> data.channelId?.let { deleteChannelDb(it) }
            UnHidden -> onChanelCreatedOrJoinedOrUnHidden(data.channel)
            UnBlocked -> TODO()
            Invited -> TODO()
        }
    }

    private suspend fun onChanelCreatedOrJoinedOrUnHidden(channel: Channel?) {
        channel?.let {
            val members = if (it is GroupChannel) it.lastActiveMembers else arrayListOf((it as DirectChannel).peer)
            val sceytChannel = channel.toSceytUiChannel()
            insertChannel(sceytChannel, *members.map { member -> member.toSceytMember() }.toTypedArray())
            channelsCache.add(sceytChannel)
        }
    }

    override suspend fun onChannelUnreadCountUpdatedEvent(data: ChannelUnreadCountUpdatedEventData) {
        updateChannelDbAndCache((data.channel ?: return).toSceytUiChannel())
    }

    override suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        channelsCache.get(data.channel.id)?.let { channel ->
            channel.lastMessage?.let { lastMessage ->
                if (data.messageIds.contains(lastMessage.id)) {
                    data.channel.lastMessage?.let {
                        channelsCache.updateLastMessage(channel.id, it)
                    }
                }
            }
        }
    }

    override suspend fun onMessage(data: Pair<SceytChannel, SceytMessage>) {
        updateChannelDbAndCache(data.first)
    }

    override suspend fun onFcmMessage(data: Pair<SceytChannel, SceytMessage>) {
        val dataChannel = data.first
        val dataMessage = data.second
        //Update channel last message if channel exist
        channelDao.getChannelById(dataChannel.id)?.let {
            channelDao.updateLastMessage(it.channelEntity.id, dataMessage.id, dataMessage.createdAt)
        } ?: run {
            // Insert channel from push data
            channelDao.insertChannel(dataChannel.toChannelEntity())
        }
    }

    override suspend fun onMessageEditedOrDeleted(data: SceytMessage) {
        channelsCache.get(data.channelId)?.let { channel ->
            channel.lastMessage?.let { lastMessage ->
                if (lastMessage.tid == data.tid) {
                    if (data.deliveryStatus == DeliveryStatus.Pending && data.state == MessageState.Deleted)
                        channelsCache.updateLastMessage(data.channelId, null)
                    else
                        channelsCache.updateLastMessage(data.channelId, data)
                }
            } ?: run {
                if (data.deliveryStatus == DeliveryStatus.Pending && data.state == MessageState.Deleted)
                    deleteMessage(data.channelId, data)
                else {
                    channel.lastMessage = data
                    channelsCache.upsertChannel(channel)
                }
            }
        }
    }

    private suspend fun updateChannelDbAndCache(channel: SceytChannel?) {
        channel ?: return
        channelDao.updateChannel(channel.toChannelEntity())
        channelsCache.upsertChannel(channel)
    }

    private suspend fun insertChannel(channel: SceytChannel, vararg members: SceytMember) {
        initPendingLastMessageBeforeInsert(channel)
        val users = members.map { it.toUserEntity() }
        channel.lastMessage?.let {
            it.selfReactions?.map { reaction -> reaction.user }?.let { it1 ->
                (users as ArrayList).addAll(it1.map { user -> user.toUserEntity() })
            }
        }
        usersDao.insertUsers(members.map { it.toUserEntity() })
        channelDao.insertChannelAndLinks(channel.toChannelEntity(), members.map {
            UserChatLink(userId = it.id, chatId = channel.id, role = it.role.name)
        })
    }

    override suspend fun loadChannels(offset: Int, searchQuery: String, loadKey: LoadKeyData?,
                                      ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>> {
        return callbackFlow {
            if (offset == 0) channelsCache.clear()

            val dbChannels = getChannelsDb(offset, searchQuery)
            var hasNext = dbChannels.size == CHANNELS_LOAD_SIZE

            channelsCache.addAll(dbChannels.map { it.clone() }, false)
            trySend(PaginationResponse.DBResponse(data = dbChannels, loadKey = loadKey, offset = offset,
                hasNext = hasNext, hasPrev = false))

            awaitToConnectSceyt()

            val response = if (offset == 0) channelsRepository.getChannels(searchQuery)
            else channelsRepository.loadMoreChannels()

            if (response is SceytResponse.Success) {
                val channels = response.data ?: arrayListOf()

                val savedChannels = saveChannelsToDb(channels)
                val hasDiff = channelsCache.addAll(savedChannels.map { it.clone() }, offset != 0) || offset == 0
                hasNext = response.data?.size == CHANNELS_LOAD_SIZE

                trySend(PaginationResponse.ServerResponse(data = response, cacheData = channelsCache.getSorted(),
                    loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext, hasPrev = false,
                    loadType = LoadNext, ignoredDb = ignoreDb))

                messageLogic.onSyncedChannels(channels)
            }

            channel.close()
            awaitClose()
        }
    }

    override suspend fun searchChannels(offset: Int, limit: Int, searchItems: List<String>, loadKey: LoadKeyData?,
                                        onlyMine: Boolean, ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>> {
        return callbackFlow {
            if (offset == 0) channelsCache.clear()

            val dbChannels = searchChannelsDb(offset, limit, searchItems, onlyMine)
            var hasNext = dbChannels.size == if (searchItems.isEmpty()) CHANNELS_LOAD_SIZE else limit

            channelsCache.addAll(dbChannels.map { it.clone() }, false)
            trySend(PaginationResponse.DBResponse(data = dbChannels, loadKey = loadKey, offset = offset,
                hasNext = hasNext, hasPrev = false))

            awaitToConnectSceyt()

            val response = if (offset == 0) channelsRepository.getChannels(searchItems.firstOrNull()
                    ?: "")
            else channelsRepository.loadMoreChannels()

            if (response is SceytResponse.Success) {
                val channels = response.data ?: arrayListOf()

                val savedChannels = saveChannelsToDb(channels)
                val hasDiff = channelsCache.addAll(savedChannels.map { it.clone() }, offset != 0) || offset == 0
                hasNext = response.data?.size == CHANNELS_LOAD_SIZE

                trySend(PaginationResponse.ServerResponse(data = response, cacheData = channelsCache.getSorted(),
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
                        upsertChannelsToCache(syncedChannels.filter { addedChannelsIds.contains(it.id) })
                    }
                    channel.close()
                }
                .collect { response ->
                    if (response is SceytResponse.Success) {
                        response.data?.let {
                            val savedChannels = saveChannelsToDb(it)
                            channelsCache.upsertChannel(*savedChannels.toTypedArray())
                            syncedChannels.addAll(it)
                            messageLogic.onSyncedChannels(it)
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

    private suspend fun saveChannelsToDb(list: List<SceytChannel>): List<SceytChannel> {
        if (list.isEmpty()) return emptyList()

        val links = arrayListOf<UserChatLink>()
        val users = arrayListOf<UserEntity>()
        val directChatsWithDeletedPeers = arrayListOf<Long>()
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
            }
        }

        list.forEach { channel ->
            if (channel.isGroup) {
                addEntitiesToLists(channel.id, (channel as SceytGroupChannel).members, channel.lastMessage)
            } else {
                val members = arrayListOf<SceytMember>()
                (channel as SceytDirectChannel).peer?.let {
                    if (it.user.activityState == UserActivityStatus.Deleted)
                        directChatsWithDeletedPeers.add(channel.id)
                    members.add(it)
                }
                addEntitiesToLists(channel.id, members, channel.lastMessage)
            }
        }
        usersDao.insertUsers(users)
        messageDao.insertMessages(lastMessages)

        // Delete old links where channel peer is deleted.
        directChatsWithDeletedPeers.forEach {
            myId?.let { id -> channelDao.deleteChatLinksExceptUser(it, id) }
        }

        initPendingLastMessageBeforeInsert(*list.toTypedArray())
        channelDao.insertChannelsAndLinks(list.map { it.toChannelEntity() }, links)
        return list
    }

    override suspend fun createDirectChannel(user: User): SceytResponse<SceytChannel> {
        val channelDb = channelDao.getDirectChannel(user.id)
        if (channelDb != null)
            return SceytResponse.Success(channelDb.toChannel())

        val response = channelsRepository.createDirectChannel(user)

        if (response is SceytResponse.Success) {
            response.data?.let { channel ->
                insertChannel(channel, SceytMember(user))
                channelsCache.add(channel)
            }
        }

        return response
    }

    override suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel> {
        val response = channelsRepository.createChannel(createChannelData)

        if (response is SceytResponse.Success) {
            response.data?.let { channel ->
                insertChannel(channel, *(channel as SceytGroupChannel).members.toTypedArray())
                channelsCache.add(channel)
            }
        }

        return response
    }

    override suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.markChannelAsRead(channelId)

        if (response is SceytResponse.Success) {
            response.data?.let {
                messageDao.updateAllMessagesStatusAsRead(channelId)
                updateChannelDbAndCache(it)
            }
        }

        return response
    }

    override suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.markChannelAsUnRead(channelId)

        if (response is SceytResponse.Success)
            response.data?.let {
                updateChannelDbAndCache(it)
            }

        return response
    }

    override suspend fun clearHistory(channelId: Long, forEveryone: Boolean): SceytResponse<Long> {
        val response = channelsRepository.clearHistory(channelId, forEveryone)

        if (response is SceytResponse.Success) {
            WorkManager.getInstance(application).cancelAllWorkByTag(channelId.toString())
            channelDao.updateLastMessage(channelId, null, null)
            messageDao.deleteAllMessages(channelId)
            channelsCache.clearedHistory(channelId)
        }

        return response
    }

    override suspend fun blockAndLeaveChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.blockChannel(channelId)

        if (response is SceytResponse.Success) {
            WorkManager.getInstance(application).cancelAllWorkByTag(channelId.toString())
            deleteChannelDb(channelId)
        }

        return response
    }

    override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.leaveChannel(channelId)

        if (response is SceytResponse.Success) {
            WorkManager.getInstance(application).cancelAllWorkByTag(channelId.toString())
            deleteChannelDb(channelId)
        }

        return response
    }

    override suspend fun deleteChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.deleteChannel(channelId)

        if (response is SceytResponse.Success) {
            WorkManager.getInstance(application).cancelAllWorkByTag(channelId.toString())
            deleteChannelDb(channelId)
        }

        return response
    }

    override suspend fun muteChannel(channelId: Long, muteUntil: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.muteChannel(channelId, muteUntil)

        if (response is SceytResponse.Success) {
            channelDao.updateMuteState(channelId = channelId, muted = true, muteUntil = muteUntil)
            channelsCache.updateMuteState(channelId, true, muteUntil)
        }

        return response
    }

    override suspend fun unMuteChannel(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.unMuteChannel(channelId)

        if (response is SceytResponse.Success) {
            channelDao.updateMuteState(channelId = channelId, muted = false)
            channelsCache.updateMuteState(channelId, false)
        }

        return response
    }

    override suspend fun hideChannel(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.hideChannel(channelId)

        if (response is SceytResponse.Success) {
            channelDao.deleteChannel(channelId = channelId)
            channelsCache.deleteChannel(channelId)
        }

        return response
    }

    override suspend fun getChannelFromDb(channelId: Long): SceytChannel? {
        return channelDao.getChannelById(channelId)?.toChannel()
    }

    override suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.getChannel(channelId)

        if (response is SceytResponse.Success) {
            response.data?.let { channel ->
                initPendingLastMessageBeforeInsert(channel)
                channel.toChannelEntity().let {
                    channelDao.insertChannel(it)
                    channelsCache.upsertChannel(channel)
                    messageLogic.onSyncedChannels(arrayListOf(channel))
                }
            }
        }

        return response
    }

    override suspend fun getChannelFromServerByUrl(url: String): SceytResponse<List<SceytChannel>> {
        //Not use yet
        return channelsRepository.getChannelFromServerByUrl(url)
    }

    override suspend fun editChannel(channelId: Long, data: EditChannelData): SceytResponse<SceytChannel> {
        if (data.avatarEdited && data.avatarUrl != null) {
            when (val uploadResult = channelsRepository.uploadAvatar(data.avatarUrl.toString())) {
                is SceytResponse.Success -> {
                    data.avatarUrl = uploadResult.data
                }
                is SceytResponse.Error -> return SceytResponse.Error(uploadResult.exception)
            }
        }
        val response = channelsRepository.editChannel(channelId, data)
        if (response is SceytResponse.Success) {
            response.data?.let {
                channelDao.updateChannel(it.toChannelEntity())
                channelsCache.upsertChannel(it)
            }
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

                    if (channelsCache.get(channelId) == null)
                        channelsCache.add(it)
                    else
                        channelsCache.addedMembers(channelId, sceytMember)
                }
            }

        return response
    }

    override suspend fun updateLastMessageWithLastRead(channelId: Long, message: SceytMessage) {
        if (message.deliveryStatus == DeliveryStatus.Pending) {
            channelDao.updateLastMessage(channelId, message.tid, message.createdAt)
            channelsCache.updateLastMessage(channelId, message)
        } else {
            channelDao.updateLastMessageWithLastRead(channelId, message.tid, message.id, message.createdAt)
            channelsCache.updateLastMessageWithLastRead(channelId, message)
        }
    }

    override suspend fun setUnreadCount(channelId: Long, count: Int) {
        channelDao.updateUnreadCount(channelId, count)
        channelsCache.updateUnreadCount(channelId, count)
    }

    override suspend fun blockUnBlockUser(userId: String, block: Boolean) {
        val channels = channelDao.getChannelByPeerId(userId)
        channelsCache.upsertChannel(*channels.map { it.toChannel() }.toTypedArray())
    }

    override fun getTotalUnreadCount(): Flow<Int> {
        return channelDao.getTotalUnreadCountAsFlow().filterNotNull()
    }

    private suspend fun initPendingLastMessageBeforeInsert(vararg channel: SceytChannel) {
        val pendingLastMessages = channelDao.getChannelsById(channel.map { it.id })
            .mapNotNull { it.lastMessage }
            .filter { it.messageEntity.deliveryStatus == DeliveryStatus.Pending }

        pendingLastMessages.forEach { messageDb ->
            channel.find { it.id == messageDb.messageEntity.channelId }?.let { channel ->
                channel.lastMessage = messageDb.toSceytMessage()
            }
        }
    }

    private suspend fun deleteChannelDb(channelId: Long) {
        channelDao.deleteChannelAndLinks(channelId)
        messageDao.deleteAllMessages(channelId)
        channelsCache.deleteChannel(channelId)
    }

    private fun upsertChannelsToCache(channels: List<SceytChannel>) {
        if (channels.isEmpty()) return
        channelsCache.upsertChannel(*channels.toTypedArray())
    }

    private suspend fun deleteMessage(channelId: Long, message: SceytMessage) {
        channelsCache.get(channelId)?.let {
            if (it.lastMessage?.id == message.id) {
                val lastMessage = messageDao.getLastMessage(channelId)
                with(lastMessage?.messageEntity) {
                    channelDao.updateLastMessage(channelId, this?.tid, this?.createdAt)
                }
                channelsCache.updateLastMessage(channelId, lastMessage?.toSceytMessage())
            }
        }
    }


    // TODO need further improvements

    private suspend fun searchChannelsDb(offset: Int, limit: Int, searchItems: List<String>, onlyMine: Boolean): List<SceytChannel> {
        return if (searchItems.isEmpty()) {
            channelDao.getChannels(limit = CHANNELS_LOAD_SIZE, offset = offset).map { channel -> channel.toChannel() }
        } else {

            val globOrUserId = concatWithSeparator(searchItems, "link.user_id", "LIKE", "", "%", "or")
            val globOrSubject = concatWithSeparator(searchItems, "subject", "LIKE", "", "%", "or")
//            val inSubject = concatWithPrefix(searchItems, "link.user_id", "IN", ",")

            var whereQuery = "(((${globOrSubject}) and channels.type != 0) "
            whereQuery += "or "
            whereQuery += "((${globOrUserId}) and channels.type == 0)) "
            if (onlyMine)
                whereQuery += "and channels.myRole != ${RoleTypeEnum.None.ordinal}"

            val finalQuery =
                    "select * from channels " +
                            "join UserChatLink as link on link.chat_id = channels.chat_id " +
                            "join users as usr on link.user_id = usr.user_id " +
                            "where $whereQuery " +
                            "group by channels.chat_id " +
                            "order by case when lastMessageAt is not null then lastMessageAt end desc, createdAt desc limit $limit offset $offset"

            val simpleSQLiteQuery = SimpleSQLiteQuery(finalQuery)
            channelDao.searchChannelsRaw(simpleSQLiteQuery).map { channel -> channel.toChannel() }
        }
    }

    private fun concatWithSeparator(items: List<String>, dbKey: String, dbFunction: String, dbPatternPrefix: String, dbPatternSuffix: String, dbSeparator: String): String {
        return items.asSequence().map {
            "$dbKey $dbFunction '${dbPatternPrefix}${it}${dbPatternSuffix}'"
        }.joinToString(" $dbSeparator ")
    }

    private fun concatWithPrefix(items: List<String>, dbKey: String, dbFunction: String, dbSeparator: String): String {
        return "$dbKey $dbFunction (" + items.asSequence().map {
            "'${it}'"
        }.joinToString(" $dbSeparator ") + ")"
    }
}