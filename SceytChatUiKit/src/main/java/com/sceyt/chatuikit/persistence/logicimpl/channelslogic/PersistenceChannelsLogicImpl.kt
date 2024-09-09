package com.sceyt.chatuikit.persistence.logicimpl.channelslogic

import android.content.Context
import com.google.gson.Gson
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.User
import com.sceyt.chat.models.user.UserState
import com.sceyt.chat.wrapper.ClientWrapper
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventData
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventEnum
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventEnum.ClearedHistory
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventEnum.Created
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventEnum.Deleted
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventEnum.Invited
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventEnum.Joined
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventEnum.Left
import com.sceyt.chatuikit.data.channeleventobserver.ChannelEventEnum.Updated
import com.sceyt.chatuikit.data.channeleventobserver.ChannelUnreadCountUpdatedEventData
import com.sceyt.chatuikit.data.connectionobserver.ConnectionEventsObserver.awaitToConnectSceyt
import com.sceyt.chatuikit.data.messageeventobserver.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.data.models.channels.GetAllChannelsResponse
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.channels.SelfChannelMetadata
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.toMember
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.extensions.toSha256
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.dao.ChannelDao
import com.sceyt.chatuikit.persistence.dao.ChatUserReactionDao
import com.sceyt.chatuikit.persistence.dao.DraftMessageDao
import com.sceyt.chatuikit.persistence.dao.LoadRangeDao
import com.sceyt.chatuikit.persistence.dao.MessageDao
import com.sceyt.chatuikit.persistence.dao.PendingReactionDao
import com.sceyt.chatuikit.persistence.dao.UserDao
import com.sceyt.chatuikit.persistence.entity.UserEntity
import com.sceyt.chatuikit.persistence.entity.channel.ChatUserReactionEntity
import com.sceyt.chatuikit.persistence.entity.channel.UserChatLink
import com.sceyt.chatuikit.persistence.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.entity.messages.DraftMessageUserLink
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.mappers.createEmptyUser
import com.sceyt.chatuikit.persistence.mappers.createPendingDirectChannelData
import com.sceyt.chatuikit.persistence.mappers.toBodyAttribute
import com.sceyt.chatuikit.persistence.mappers.toChannel
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.persistence.mappers.toDraftMessage
import com.sceyt.chatuikit.persistence.mappers.toReactionData
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.mappers.toSceytReaction
import com.sceyt.chatuikit.persistence.mappers.toSceytUiChannel
import com.sceyt.chatuikit.persistence.mappers.toUser
import com.sceyt.chatuikit.persistence.mappers.toUserEntity
import com.sceyt.chatuikit.persistence.mappers.toUserReactionsEntity
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import com.sceyt.chatuikit.persistence.workers.SendAttachmentWorkManager
import com.sceyt.chatuikit.persistence.workers.SendForwardMessagesWorkManager
import com.sceyt.chatuikit.presentation.extensions.isDeleted
import com.sceyt.chatuikit.presentation.extensions.isDeletedOrHardDeleted
import com.sceyt.chatuikit.presentation.extensions.isHardDeleted
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.mention.Mention
import com.sceyt.chatuikit.presentation.uicomponents.messageinput.style.BodyStyleRange
import com.sceyt.chatuikit.pushes.RemoteMessageData
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import org.koin.core.component.inject

internal class PersistenceChannelsLogicImpl(
        private val channelsRepository: ChannelsRepository,
        private val channelDao: ChannelDao,
        private val usersDao: UserDao,
        private val messageDao: MessageDao,
        private val rangeDao: LoadRangeDao,
        private val draftMessageDao: DraftMessageDao,
        private val chatUserReactionDao: ChatUserReactionDao,
        private val pendingReactionDao: PendingReactionDao,
        private val context: Context,
        private val channelsCache: ChannelsCache) : PersistenceChannelsLogic, SceytKoinComponent {

    private val messageLogic: PersistenceMessagesLogic by inject()
    private val myId: String? get() = SceytChatUIKit.chatUIFacade.myId
    private val channelsLoadSize get() = SceytChatUIKit.config.channelsLoadSize

    override suspend fun onChannelEvent(data: ChannelEventData) {
        when (val event = data.eventType) {
            is Created -> onChanelAdded(data.channel)
            is Joined -> onChanelJoined(data.channel)
            is Deleted -> data.channelId?.let { deleteChannelDb(it) }
            is Left -> {
                val leftUsers = event.leftMembers
                leftUsers.forEach { leftUser ->
                    data.channelId?.let { channelId ->
                        if (leftUser.id == myId) {
                            deleteChannelDb(channelId)
                            return
                        } else {
                            channelDao.deleteUserChatLinks(channelId, leftUser.id)
                            updateMembersCount(channelId, data.channel?.memberCount?.toInt() ?: 0)
                        }
                    }
                }
            }

            is ClearedHistory -> {
                data.channelId?.let { channelId ->
                    clearHistory(channelId)
                }
            }

            is Updated -> {
                data.channel?.let { sceytChannel ->
                    val withLastMessage = initPendingLastMessageBeforeInsert(sceytChannel)
                    channelDao.insertChannel(withLastMessage.toChannelEntity())
                    val updated = fillChannelsNeededInfo(withLastMessage)
                    channelsCache.upsertChannel(updated)
                }
            }

            is ChannelEventEnum.Mute -> {
                data.channelId?.let { channelId ->
                    if (event.muted) {
                        val time = data.channel?.mutedTill ?: 0
                        channelDao.updateMuteState(channelId, true, time)
                        channelsCache.updateMuteState(channelId, true, time)
                    } else {
                        channelDao.updateMuteState(channelId, false)
                        channelsCache.updateMuteState(channelId, false)
                    }
                }
            }

            is ChannelEventEnum.Pin -> onChannelPinStateChange(data.channel)
            is ChannelEventEnum.MarkedUs -> onChannelMarkedAsReadOrUnread(data.channel)
            is ChannelEventEnum.Block -> {
                if (event.blocked)
                    data.channelId?.let { deleteChannelDb(it) }
            }

            is ChannelEventEnum.Hide -> {
                if (event.hidden)
                    data.channelId?.let { deleteChannelDb(it) }
                else onChanelAdded(data.channel)
            }

            is Invited -> onChanelJoined(data.channel)
        }
    }

    private suspend fun onChanelJoined(channel: SceytChannel?) {
        val joinedMember = channel?.members?.getOrNull(0)?.id ?: return
        if (joinedMember == myId) {
            onChanelAdded(channel)
        } else updateMembersCount(channel.id, channel.memberCount.toInt())
    }

    private suspend fun updateMembersCount(channelId: Long, count: Int) {
        channelDao.updateMemberCount(channelId, count)
        channelDao.getChannelById(channelId)?.let { channel ->
            channelsCache.upsertChannel(fillChannelsNeededInfo(channel.toChannel()))
        }
    }

    private suspend fun onChanelAdded(channel: SceytChannel?) {
        channel?.let {
            val members = it.members ?: return
            insertChannel(channel, *members.toTypedArray())
            channelsCache.add(channel)
        }
    }

    override suspend fun onChannelUnreadCountUpdatedEvent(data: ChannelUnreadCountUpdatedEventData) {
        /* Todo need refactoring after sdk update. Commented code is the right way to update unread count
         data.channel ?: return
         channelDao.updateUnreadCount(data.channel.id, data.channel.unreadMessageCount.toInt())
         channelsCache.updateUnreadCount(data.channel.id, data.channel.unreadMessageCount.toInt())*/
        updateChannelDbAndCache((data.channel ?: return).toSceytUiChannel())
    }

    override suspend fun onMessageStatusChangeEvent(data: MessageStatusChangeData) {
        channelsCache.get(data.channel.id)?.let { channel ->
            channel.lastMessage?.let { lastMessage ->
                if (data.marker.messageIds.contains(lastMessage.id)) {
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

    override suspend fun onFcmMessage(data: RemoteMessageData) {
        val dataChannel = data.channel ?: return
        val dataMessage = data.message ?: return

        var channel: SceytChannel
        val channelDb = channelDao.getChannelById(dataChannel.id)
        if (channelDb != null) {
            channel = channelDb.toChannel()
            if ((channel.lastMessage?.id ?: 0) < dataMessage.id) {
                channel = channel.copy(lastMessage = dataMessage)
                channelDao.updateLastMessage(channelDb.channelEntity.id, dataMessage.id, dataMessage.createdAt)
            }
        } else {
            // Insert channel from push data
            channelDao.insertChannel(dataChannel.toChannelEntity())
            channel = dataChannel
        }
        channelsCache.upsertChannel(fillChannelsNeededInfo(channel))
    }

    override suspend fun onMessageEditedOrDeleted(message: SceytMessage) {
        val state = message.state
        if (state.isDeletedOrHardDeleted()) {
            chatUserReactionDao.deleteChannelMessageUserReaction(message.channelId, message.id)
            channelsCache.removeChannelMessageReactions(message.channelId, message.id)
        }

        channelDao.getChannelById(message.channelId)?.toChannel()?.let { channel ->
            channel.lastMessage?.let { lastMessage ->
                if (lastMessage.tid == message.tid) {
                    if (message.deliveryStatus == DeliveryStatus.Pending && message.state == MessageState.Deleted) {
                        channelsCache.updateLastMessage(message.channelId, null)
                    } else
                        channelsCache.updateLastMessage(message.channelId, message)
                }
            } ?: run {
                if (state.isHardDeleted() || message.deliveryStatus == DeliveryStatus.Pending && state.isDeleted())
                    deleteMessage(message.channelId, message)
                else
                    channelsCache.upsertChannel(channel.copy(lastMessage = message))
            }
        }
    }

    private suspend fun updateChannelDbAndCache(channel: SceytChannel?) {
        channel ?: return
        channelDao.updateChannel(channel.toChannelEntity())
        channelsCache.upsertChannel(fillChannelsNeededInfo(channel))
    }

    private suspend fun onChannelMarkedAsReadOrUnread(channel: SceytChannel?) {
        channel ?: return
        channelDao.updateChannel(channel.toChannelEntity())
        channelsCache.onChannelMarkedAsReadOrUnread(channel)
    }

    private suspend fun insertChannel(channel: SceytChannel, vararg members: SceytMember) {
        val updated = initPendingLastMessageBeforeInsert(channel)
        val users = members.map { it.toUserEntity() }
        updated.lastMessage?.let { message ->
            message.userReactions?.map { it.user }?.let { userList ->
                (users as ArrayList).addAll(userList.mapNotNull { user -> user?.toUserEntity() })
            }
        }
        usersDao.insertUsers(members.map { it.toUserEntity() })
        channelDao.insertChannelAndLinks(updated.toChannelEntity(), members.map {
            UserChatLink(userId = it.id, chatId = updated.id, role = it.role.name)
        })
    }

    override suspend fun loadChannels(offset: Int, searchQuery: String, loadKey: LoadKeyData?,
                                      ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>> {
        return callbackFlow {
            if (offset == 0) channelsCache.clear()

            val dbChannels = getChannelsDb(offset, searchQuery)
            var hasNext = dbChannels.size == channelsLoadSize

            trySend(PaginationResponse.DBResponse(data = dbChannels, loadKey = loadKey, offset = offset,
                hasNext = hasNext, hasPrev = false, query = searchQuery))

            channelsCache.addAll(dbChannels, false)
            ChatReactionMessagesCache.getNeededMessages(dbChannels)

            awaitToConnectSceyt()

            val response = if (offset == 0) channelsRepository.getChannels(searchQuery)
            else channelsRepository.loadMoreChannels()

            if (response is SceytResponse.Success) {
                val channels = response.data ?: arrayListOf()

                val savedChannels = saveChannelsToDb(channels)
                val hasDiff = channelsCache.addAll(savedChannels, offset != 0) || offset == 0
                hasNext = response.data?.size == channelsLoadSize

                trySend(PaginationResponse.ServerResponse(data = response, cacheData = channelsCache.getSorted(),
                    loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext, hasPrev = false,
                    loadType = LoadNext, ignoredDb = ignoreDb, query = searchQuery))

                ChatReactionMessagesCache.getNeededMessages(response.data ?: arrayListOf())

                messageLogic.onSyncedChannels(channels)
            }

            channel.close()
            awaitClose()
        }
    }

    override suspend fun searchChannelsWithUserIds(offset: Int, limit: Int, searchQuery: String, userIds: List<String>,
                                                   includeUserNames: Boolean, loadKey: LoadKeyData?,
                                                   onlyMine: Boolean, ignoreDb: Boolean): Flow<PaginationResponse<SceytChannel>> {
        return callbackFlow {
            if (offset == 0) channelsCache.clear()

            val searchUserIds = HashSet<String>(userIds)
            if (includeUserNames) {
                val ids = usersDao.getUserIdsByDisplayName(searchQuery)
                searchUserIds.addAll(ids)
            }
            val dbChannels = channelDao.getChannelsByQueryAndUserIds(searchQuery, searchUserIds.toList(), limit, offset, onlyMine).map {
                it.toChannel()
            }
            var hasNext = dbChannels.size == limit

            channelsCache.addAll(dbChannels, false)
            trySend(PaginationResponse.DBResponse(data = dbChannels, loadKey = loadKey, offset = offset,
                hasNext = hasNext, hasPrev = false, query = searchQuery))

            awaitToConnectSceyt()

            val response = if (offset == 0) channelsRepository.getChannels(searchQuery)
            else channelsRepository.loadMoreChannels()

            if (response is SceytResponse.Success) {
                val channels = response.data ?: arrayListOf()

                val savedChannels = saveChannelsToDb(channels)
                val hasDiff = channelsCache.addAll(savedChannels, offset != 0) || offset == 0
                hasNext = response.data?.size == channelsLoadSize

                trySend(PaginationResponse.ServerResponse(data = response, cacheData = channelsCache.getSorted(),
                    loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext, hasPrev = false,
                    loadType = LoadNext, ignoredDb = ignoreDb, query = searchQuery))
            }

            channel.close()
            awaitClose()
        }
    }

    override suspend fun syncChannels(limit: Int) = callbackFlow {
        val oldChannelsIds = channelDao.getAllChannelsIds().toSet()
        awaitToConnectSceyt()
        val syncedChannels = arrayListOf<SceytChannel>()
        channelsRepository.getAllChannels(limit)
            .collect { response ->
                when (response) {
                    is GetAllChannelsResponse.Proportion -> {
                        val channels = response.channels
                        val savedChannels = saveChannelsToDb(channels)
                        syncedChannels.addAll(channels)
                        messageLogic.onSyncedChannels(channels)
                        channelsCache.upsertChannel(*savedChannels.toTypedArray())
                        trySend(response)
                    }

                    is GetAllChannelsResponse.SuccessfullyFinished -> {
                        if (syncedChannels.isNotEmpty()) {
                            val syncedIds = syncedChannels.map { it.id }
                            val deletedChannelIds = channelDao.getNotExistingChannelIdsByIds(syncedIds)
                            val addedChannelsIds = syncedIds.minus(oldChannelsIds)

                            if (deletedChannelIds.isNotEmpty())
                                SceytLog.i("syncChannelsResult", "deletedChannelsIds: ${deletedChannelIds.map { it }}")

                            deletedChannelIds.forEach { deleteChannelDb(channelId = it) }
                            upsertChannelsToCache(syncedChannels.filter { addedChannelsIds.contains(it.id) })
                        }
                        trySend(response)
                        channel.close()
                    }

                    is GetAllChannelsResponse.Error -> {
                        trySend(response)
                        channel.close()
                        SceytLog.e("syncChannelsResult", "syncChannels error: ${response.error}")
                    }
                }
            }

        awaitClose()
    }

    private suspend fun getChannelsDb(offset: Int, searchQuery: String): List<SceytChannel> {
        return if (searchQuery.isBlank()) {
            channelDao.getChannels(limit = channelsLoadSize, offset = offset).map { channel ->
                channel.toChannel()
            }
        } else {
            val ids = usersDao.getUserIdsByDisplayName(searchQuery)
            channelDao.getChannelsByQueryAndUserIds(query = searchQuery, userIds = ids, limit = channelsLoadSize,
                offset = offset, false).map { channel ->
                channel.toChannel()
            }
        }
    }

    private suspend fun saveChannelsToDb(list: List<SceytChannel>): List<SceytChannel> {
        if (list.isEmpty()) return emptyList()

        val links = arrayListOf<UserChatLink>()
        val users = arrayListOf<UserEntity>()
        val directChatsWithDeletedPeers = arrayListOf<Long>()
        val lastMessages = arrayListOf<SceytMessage>()
        val userReactions = arrayListOf<ChatUserReactionEntity>()

        fun addEntitiesToLists(channelId: Long, members: List<SceytMember>?, lastMessage: SceytMessage?, userMessageReactions: List<SceytReaction>?) {
            members?.forEach { member ->
                links.add(UserChatLink(userId = member.id, chatId = channelId, role = member.role.name))
                users.add(member.toUserEntity())
            }

            lastMessage?.let {
                lastMessages.add(it)
            }

            userMessageReactions?.forEach {
                userReactions.add(it.toUserReactionsEntity(channelId))
            }
        }

        list.map { channel ->
            if (channel.isGroup) {
                addEntitiesToLists(channel.id, channel.members, channel.lastMessage, channel.newReactions)
            } else {
                val members = arrayListOf<SceytMember>()
                channel.getPeer()?.let {
                    if (it.user.activityState == UserState.Deleted)
                        directChatsWithDeletedPeers.add(channel.id)
                    members.add(it)
                }
                addEntitiesToLists(channel.id, members, channel.lastMessage, channel.newReactions)
            }

            fillChannelsNeededInfo(channel)
        }
        usersDao.insertUsers(users)
        messageLogic.saveChannelLastMessagesToDb(lastMessages)
        chatUserReactionDao.replaceChannelUserReactions(userReactions)

        // Delete old links where channel peer is deleted.
        directChatsWithDeletedPeers.forEach {
            myId?.let { id -> channelDao.deleteChatLinksExceptUser(it, id) }
        }

        val updatedList = initPendingLastMessageBeforeInsert(list)
        channelDao.insertChannelsAndLinks(updatedList.map { it.toChannelEntity() }, links)
        return updatedList
    }

    override suspend fun findOrCreateDirectChannel(user: User): SceytResponse<SceytChannel> {
        var metadata = ""
        val channelDb = if (user.id == myId) {
            metadata = Gson().toJson(SelfChannelMetadata(1))
            channelDao.getSelfChannel()
        } else channelDao.getDirectChannel(user.id)
        if (channelDb != null) {
            if (channelDb.channelEntity.pending)
                channelsCache.addPendingChannel(channelDb.toChannel())
            return SceytResponse.Success(channelDb.toChannel())
        }

        val fail = SceytResponse.Error<SceytChannel>(SceytException(0, "Failed to create direct channel myId is null"))
        val myId = myId ?: return fail
        val createdBy = ClientWrapper.currentUser ?: usersDao.getUserById(myId)?.toUser()
        ?: User(myId)

        val role = Role(RoleTypeEnum.Owner.toString())
        val members = setOf(SceytMember(role, user), SceytMember(role, createdBy)).toList()
        val channelId = members.map { it.id }.toSet().sorted().joinToString(separator = "$").toSha256()
        val channel = createPendingDirectChannelData(channelId = channelId,
            createdBy = createdBy, members = members, role = role.name, metadata = metadata)

        insertChannel(channel, *members.toTypedArray())
        channelsCache.addPendingChannel(channel)
        return SceytResponse.Success(channel)
    }

    override suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel> {
        val response = channelsRepository.createChannel(createChannelData)

        if (response is SceytResponse.Success) {
            response.data?.let { channel ->
                channel.members?.toTypedArray()?.let {
                    insertChannel(channel, *it)
                }
                channelsCache.add(channel)
            }
        }
        return response
    }

    override suspend fun createNewChannelInsteadOfPendingChannel(channel: SceytChannel): SceytResponse<SceytChannel> {
        val pendingChannelId = channel.id
        val response = channelsRepository.createChannel(CreateChannelData(
            channelType = channel.type,
            uri = channel.uri ?: "",
            subject = channel.subject ?: "",
            avatarUrl = channel.avatarUrl ?: "",
            metadata = channel.metadata ?: "",
            members = channel.members?.map { it.toMember() } ?: arrayListOf()))
        if (response is SceytResponse.Success) {
            var newChannel = response.data
                    ?: return SceytResponse.Error(SceytException(0, "create channel response is success, but channel is null"))

            val newChannelId = newChannel.id
            // Set new channel last message to pending channel last message with new channel id
            newChannel = newChannel.copy(lastMessage = channel.lastMessage?.copy(channelId = newChannelId))

            channelDao.deleteChannelAndLinks(pendingChannelId)
            channelDao.insertChannelAndLinks(newChannel.toChannelEntity(), newChannel.members?.map {
                UserChatLink(userId = it.id, chatId = newChannelId, role = it.role.name)
            } ?: emptyList())
            messageDao.updateMessagesChannelId(pendingChannelId, newChannelId)

            channelsCache.pendingChannelCreated(pendingChannelId, newChannel)
            SceytResponse.Success(newChannel)
        }
        return response
    }

    override suspend fun markChannelAsRead(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.markChannelAsRead(channelId)

        if (response is SceytResponse.Success) {
            response.data?.let {
                messageDao.updateAllIncomingMessagesStatusAsRead(channelId)
                onChannelMarkedAsReadOrUnread(it)
            }
        }

        return response
    }

    override suspend fun markChannelAsUnRead(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.markChannelAsUnRead(channelId)

        if (response is SceytResponse.Success)
            onChannelMarkedAsReadOrUnread(response.data)

        return response
    }

    override suspend fun clearHistory(channelId: Long, forEveryone: Boolean): SceytResponse<Long> {
        if (channelsCache.get(channelId)?.pending == true) {
            deleteChannelDb(channelId)
            return SceytResponse.Success(channelId)
        }

        val response = channelsRepository.clearHistory(channelId, forEveryone)

        if (response is SceytResponse.Success) {
            SendAttachmentWorkManager.cancelWorksByTag(context, channelId.toString())
            SendForwardMessagesWorkManager.cancelWorksByTag(context, channelId.toString())
            clearHistory(channelId)
        }

        return response
    }

    override suspend fun blockAndLeaveChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.blockChannel(channelId)

        if (response is SceytResponse.Success) {
            SendAttachmentWorkManager.cancelWorksByTag(context, channelId.toString())
            SendForwardMessagesWorkManager.cancelWorksByTag(context, channelId.toString())
            deleteChannelDb(channelId)
        }

        return response
    }

    override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.leaveChannel(channelId)

        if (response is SceytResponse.Success) {
            SendAttachmentWorkManager.cancelWorksByTag(context, channelId.toString())
            SendForwardMessagesWorkManager.cancelWorksByTag(context, channelId.toString())
            deleteChannelDb(channelId)
        }

        return response
    }

    override suspend fun deleteChannel(channelId: Long): SceytResponse<Long> {
        if (channelsCache.get(channelId)?.pending == true) {
            deleteChannelDb(channelId)
            return SceytResponse.Success(channelId)
        }

        val response = channelsRepository.deleteChannel(channelId)

        if (response is SceytResponse.Success) {
            SendAttachmentWorkManager.cancelWorksByTag(context, channelId.toString())
            SendForwardMessagesWorkManager.cancelWorksByTag(context, channelId.toString())
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

    override suspend fun enableAutoDelete(channelId: Long, period: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.enableAutoDelete(channelId, period)

        if (response is SceytResponse.Success) {
            channelDao.updateAutoDeleteState(channelId, period)
            channelsCache.updateAutoDeleteState(channelId, period)
        }

        return response
    }

    override suspend fun disableAutoDelete(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.disableAutoDelete(channelId)

        if (response is SceytResponse.Success) {
            channelDao.updateAutoDeleteState(channelId, 0L)
            channelsCache.updateAutoDeleteState(channelId, 0L)
        }

        return response
    }

    override suspend fun pinChannel(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.pinChannel(channelId)

        if (response is SceytResponse.Success)
            onChannelPinStateChange(response.data)

        return response
    }

    override suspend fun unpinChannel(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.unpinChannel(channelId)

        if (response is SceytResponse.Success)
            onChannelPinStateChange(response.data)

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

    override suspend fun getDirectChannelFromDb(peerId: String): SceytChannel? {
        return channelDao.getDirectChannel(peerId)?.toChannel()
    }

    override suspend fun getChannelFromServer(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.getChannel(channelId)

        if (response is SceytResponse.Success) {
            response.data?.let { channel ->
                val withLastMessage = initPendingLastMessageBeforeInsert(channel)
                withLastMessage.toChannelEntity().let {
                    channelDao.insertChannel(it)
                    val updated = fillChannelsNeededInfo(withLastMessage)
                    channelsCache.upsertChannel(updated)
                    messageLogic.onSyncedChannels(arrayListOf(updated))
                }
            }
        } else {
            getChannelFromDb(channelId)?.let {
                if (it.pending)
                    return SceytResponse.Success(it)
            }
        }

        return response
    }

    override suspend fun getChannelFromServerByUrl(url: String): SceytResponse<List<SceytChannel>> {
        //Don't use yet
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
                channelsCache.upsertChannel(fillChannelsNeededInfo(it))
            }
        }

        return response
    }

    override suspend fun join(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.join(channelId)

        if (response is SceytResponse.Success)
            response.data?.let {
                it.members?.getOrNull(0)?.let { sceytMember ->
                    channelDao.insertUserChatLink(UserChatLink(
                        userId = sceytMember.id,
                        chatId = it.id,
                        role = sceytMember.role.name))

                    channelsCache.upsertChannel(it)
                }
            }

        return response
    }

    override suspend fun updateLastMessageWithLastRead(channelId: Long, message: SceytMessage) {
        // Check if message delivery status is pending, that means message is started to send
        if (message.deliveryStatus == DeliveryStatus.Pending) {
            channelDao.updateLastMessage(channelId, message.tid, message.createdAt)
            channelsCache.updateLastMessage(channelId, message)
        } else {
            // Check if sent message is last message of channel
            channelsCache.get(channelId)?.let {
                if (it.lastMessage?.tid != message.tid) return
            } ?: run {
                channelDao.getChannelById(channelId)?.let {
                    if (it.channelEntity.lastMessageTid != message.tid) return
                }
            }
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

    override suspend fun updateDraftMessage(channelId: Long, message: String?, mentionUsers: List<Mention>,
                                            styling: List<BodyStyleRange>?, replyOrEditMessage: SceytMessage?, isReply: Boolean) {
        val draftMessage = if (message.isNullOrBlank()) {
            draftMessageDao.deleteDraftByChannelId(channelId)
            null
        } else {
            val attributes = mentionUsers.map { it.toBodyAttribute() }.toMutableList()
            styling?.let {
                attributes.addAll(it.map { styleRange -> styleRange.toBodyAttribute() })
            }
            val draftMessageEntity = DraftMessageEntity(channelId, message, System.currentTimeMillis(),
                replyOrEditMessage?.id, isReply, attributes)
            val links = mentionUsers.map { DraftMessageUserLink(chatId = channelId, userId = it.recipientId) }
            draftMessageDao.insertWithUserLinks(draftMessageEntity, links)
            draftMessageEntity.toDraftMessage(mentionUsers.map { createEmptyUser(it.recipientId, it.name) }, replyOrEditMessage)
        }
        channelsCache.updateChannelDraftMessage(channelId, draftMessage)
    }

    override suspend fun getChannelsCountFromDb(): Int {
        return channelDao.getAllChannelsCount()
    }

    override fun getTotalUnreadCount(): Flow<Int> {
        return channelDao.getTotalUnreadCountAsFlow().filterNotNull()
    }

    override suspend fun onUserPresenceChanged(users: List<SceytPresenceChecker.PresenceUser>) {
        users.forEach { presenceUser ->
            ArrayList(channelsCache.getData()).forEach { channel ->
                val user = presenceUser.user
                if (channel.isDirect() && channel.getPeer()?.id == user.id)
                    channelsCache.updateChannelPeer(channel.id, user)
            }
        }
    }

    private suspend fun initPendingLastMessageBeforeInsert(channels: List<SceytChannel>): List<SceytChannel> {
        if (channels.isEmpty()) return channels
        val mutableList = channels.toList().toArrayList()
        val messageTIds = channelDao.getChannelsLastMessageTIds(mutableList.map { it.id })
        if (messageTIds.isEmpty()) return channels

        val pendingLastMessages = messageDao.getPendingMessagesByTIds(messageTIds)

        pendingLastMessages.forEach { messageDb ->
            mutableList.findIndexed { it.id == messageDb.messageEntity.channelId }?.let { (index, item) ->
                mutableList[index] = item.copy(lastMessage = messageDb.toSceytMessage())
            }
        }
        return mutableList
    }

    private suspend fun initPendingLastMessageBeforeInsert(channel: SceytChannel): SceytChannel {
        val messageTIds = channelDao.getChannelLastMessageTid(channel.id) ?: return channel
        messageDao.getPendingMessageByTid(messageTIds)?.let { pendingLastMessage ->
            return channel.copy(lastMessage = pendingLastMessage.toSceytMessage())
        }
        return channel
    }

    private suspend fun deleteChannelDb(channelId: Long) {
        channelDao.deleteChannelAndLinks(channelId)
        messageDao.deleteAllMessages(channelId)
        rangeDao.deleteChannelLoadRanges(channelId)
        channelsCache.deleteChannel(channelId)
    }

    private suspend fun clearHistory(channelId: Long) {
        channelDao.updateLastMessage(channelId, null, null)
        messageDao.deleteAllMessages(channelId)
        rangeDao.deleteChannelLoadRanges(channelId)
        channelsCache.clearedHistory(channelId)
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
        chatUserReactionDao.deleteChannelMessageUserReaction(channelId, message.id)
    }

    private suspend fun fillChannelsNeededInfo(channel: SceytChannel): SceytChannel {
        val reactions = chatUserReactionDao.getChannelUserReactions(channel.id)
        val pendingReactions = pendingReactionDao.getAllByChannelId(channel.id)
        val draftMessage = draftMessageDao.getDraftByChannelId(channel.id)?.toDraftMessage()

        return channel.copy(
            newReactions = reactions.map { reactionDb -> reactionDb.toSceytReaction() },
            pendingReactions = pendingReactions.map { pendingReaction -> pendingReaction.toReactionData() },
            draftMessage = draftMessage
        )
    }

    private suspend fun onChannelPinStateChange(channel: SceytChannel?) {
        channel ?: return
        channelDao.updatePinState(channel.id, channel.pinnedAt)
        channelsCache.updatePinState(channel.id, channel.pinnedAt)
    }
}