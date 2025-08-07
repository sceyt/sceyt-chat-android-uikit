package com.sceyt.chatuikit.persistence.logicimpl.channel

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.google.gson.Gson
import com.sceyt.chat.models.SceytException
import com.sceyt.chat.models.channel.ChannelListQuery.ChannelListOrder
import com.sceyt.chat.models.message.DeliveryStatus
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chat.models.role.Role
import com.sceyt.chat.models.user.UserState
import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.config.ChannelListConfig
import com.sceyt.chatuikit.config.SearchChannelParams
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.ClearedHistory
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.Created
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.Deleted
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.Joined
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.Left
import com.sceyt.chatuikit.data.managers.channel.event.ChannelActionEvent.Updated
import com.sceyt.chatuikit.data.managers.channel.event.ChannelUnreadCountUpdatedEventData
import com.sceyt.chatuikit.data.managers.connection.ConnectionEventManager
import com.sceyt.chatuikit.data.managers.message.event.MessageStatusChangeData
import com.sceyt.chatuikit.data.models.LoadKeyData
import com.sceyt.chatuikit.data.models.PaginationResponse
import com.sceyt.chatuikit.data.models.PaginationResponse.LoadType.LoadNext
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.channels.CreateChannelData
import com.sceyt.chatuikit.data.models.channels.EditChannelData
import com.sceyt.chatuikit.data.models.channels.GetAllChannelsResponse
import com.sceyt.chatuikit.data.models.channels.RoleTypeEnum
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.channels.SceytMember
import com.sceyt.chatuikit.data.models.channels.SelfChannelMetadata
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytReaction
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.onError
import com.sceyt.chatuikit.data.models.onSuccessNotNull
import com.sceyt.chatuikit.extensions.findIndexed
import com.sceyt.chatuikit.extensions.toSha256
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.ChatUserReactionDao
import com.sceyt.chatuikit.persistence.database.dao.DraftMessageDao
import com.sceyt.chatuikit.persistence.database.dao.LoadRangeDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingReactionDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChatUserReactionEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLinkEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageUserLinkEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserDb
import com.sceyt.chatuikit.persistence.extensions.getPeer
import com.sceyt.chatuikit.persistence.extensions.isDirect
import com.sceyt.chatuikit.persistence.extensions.toArrayList
import com.sceyt.chatuikit.persistence.logic.PersistenceChannelsLogic
import com.sceyt.chatuikit.persistence.logic.PersistenceMessagesLogic
import com.sceyt.chatuikit.persistence.mappers.createEmptyUser
import com.sceyt.chatuikit.persistence.mappers.createPendingChannel
import com.sceyt.chatuikit.persistence.mappers.toBodyAttribute
import com.sceyt.chatuikit.persistence.mappers.toChannel
import com.sceyt.chatuikit.persistence.mappers.toChannelEntity
import com.sceyt.chatuikit.persistence.mappers.toDraftMessage
import com.sceyt.chatuikit.persistence.mappers.toReactionData
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.mappers.toSceytReaction
import com.sceyt.chatuikit.persistence.mappers.toSceytUiChannel
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import com.sceyt.chatuikit.persistence.mappers.toUserDb
import com.sceyt.chatuikit.persistence.mappers.toUserReactionsEntity
import com.sceyt.chatuikit.persistence.repositories.ChannelsRepository
import com.sceyt.chatuikit.persistence.workers.SendForwardMessagesWorkManager
import com.sceyt.chatuikit.persistence.workers.UploadAndSendAttachmentWorkManager
import com.sceyt.chatuikit.presentation.components.channel.input.format.BodyStyleRange
import com.sceyt.chatuikit.presentation.components.channel.input.mention.Mention
import com.sceyt.chatuikit.presentation.extensions.isDeleted
import com.sceyt.chatuikit.presentation.extensions.isDeletedOrHardDeleted
import com.sceyt.chatuikit.presentation.extensions.isHardDeleted
import com.sceyt.chatuikit.push.PushData
import com.sceyt.chatuikit.services.SceytPresenceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.koin.core.component.inject

internal class PersistenceChannelsLogicImpl(
        private val context: Context,
        private val channelsRepository: ChannelsRepository,
        private val channelDao: ChannelDao,
        private val usersDao: UserDao,
        private val messageDao: MessageDao,
        private val rangeDao: LoadRangeDao,
        private val draftMessageDao: DraftMessageDao,
        private val chatUserReactionDao: ChatUserReactionDao,
        private val pendingReactionDao: PendingReactionDao,
        private val channelsCache: ChannelsCache,
) : PersistenceChannelsLogic, SceytKoinComponent {

    private val messageLogic: PersistenceMessagesLogic by inject()
    private val myId: String? get() = SceytChatUIKit.chatUIFacade.myId
    private val channelsLoadSize get() = SceytChatUIKit.config.queryLimits.channelListQueryLimit

    override suspend fun onChannelEvent(event: ChannelActionEvent) {
        when (event) {
            is Created -> onChanelAdded(event.channel)
            is Joined -> onChanelJoined(event.channel)
            is Deleted -> deleteChannelFromDbAndCache(event.channelId)
            is Left -> {
                val leftUsers = event.leftMembers
                leftUsers.forEach { leftUser ->
                    if (leftUser.id == myId) {
                        deleteChannelFromDbAndCache(event.channelId)
                        return
                    } else {
                        channelDao.deleteUserChatLinks(event.channelId, leftUser.id)
                        updateMembersCount(event.channelId, event.channel.memberCount.toInt())
                    }
                }
            }

            is ClearedHistory -> {
                clearHistory(event.channelId)
            }

            is Updated -> {
                val lastMessage = getChannelCurrentLastMessage(event.channel)
                val channel = event.channel.copy(lastMessage = lastMessage)
                channelDao.insertChannel(channel.toChannelEntity())
                getAndUpdateCashedChannel(channel.id)
            }

            is ChannelActionEvent.Mute -> {
                val channelId = event.channelId
                if (event.muted) {
                    val time = event.channel.mutedTill ?: 0
                    channelDao.updateMuteState(channelId, true, time)
                    channelsCache.updateMuteState(channelId, true, time)
                } else {
                    channelDao.updateMuteState(channelId, false)
                    channelsCache.updateMuteState(channelId, false)
                }
            }

            is ChannelActionEvent.Pin -> onChannelPinStateChange(event.channel)
            is ChannelActionEvent.MarkedUs -> onChannelMarkedAsReadOrUnread(event.channel)
            is ChannelActionEvent.Block -> {
                if (event.blocked)
                    deleteChannelFromDbAndCache(event.channelId)
            }

            is ChannelActionEvent.Hide -> {
                if (event.hidden)
                    deleteChannelFromDbAndCache(event.channelId)
                else onChanelAdded(event.channel)
            }

            is ChannelActionEvent.Event -> Unit
        }
    }

    private suspend fun onChanelJoined(channel: SceytChannel?) {
        val joinedMember = channel?.members?.firstOrNull()?.id ?: return
        if (joinedMember == myId) {
            onChanelAdded(channel)
        } else updateMembersCount(channel.id, channel.memberCount.toInt())
    }

    private suspend fun updateMembersCount(channelId: Long, count: Int) {
        channelDao.updateMemberCount(channelId, count)
        getAndUpdateCashedChannel(channelId)
    }

    private suspend fun onChanelAdded(channel: SceytChannel?) {
        channel?.let {
            val members = it.members ?: return
            insertChannelWithMembers(channel, *members.toTypedArray())
            channelsCache.upsertChannel(channel)
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
        channelsCache.getOneOf(data.channel.id)?.let { channel ->
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

    override suspend fun handlePush(data: PushData) {
        val dataChannel = data.channel
        val dataMessage = data.message

        val channel = channelDao.getChannelById(dataChannel.id)
        if (channel != null) {
            if ((channel.lastMessage?.id ?: 0) < dataMessage.id) {
                channelDao.updateLastMessage(channel.channelEntity.id, dataMessage.id, dataMessage.createdAt)
            }
        } else {
            // Insert channel from push data
            channelDao.insertChannel(dataChannel.toChannelEntity())
        }
        getAndUpdateCashedChannel(dataChannel.id)
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
        getAndUpdateCashedChannel(channel.id)
    }

    private suspend fun getAndUpdateCashedChannel(channelId: Long): SceytChannel? {
        return channelDao.getChannelById(channelId)?.toChannel()?.also {
            channelsCache.upsertChannel(it)
        }
    }

    private suspend fun onChannelMarkedAsReadOrUnread(channel: SceytChannel?) {
        channel ?: return
        channelDao.updateChannel(channel.toChannelEntity())
        channelsCache.onChannelMarkedAsReadOrUnread(channel)
    }

    private suspend fun insertChannelWithMembers(
            channel: SceytChannel,
            vararg members: SceytMember,
    ) {
        var users = members.map { it.toUserDb() }
        channel.lastMessage?.let { message ->
            message.userReactions?.mapNotNull { it.user?.toUserDb() }?.let { userList ->
                users = users.plus(userList)
            }
            messageLogic.saveChannelLastMessagesToDb(listOf(message))
        }
        usersDao.insertUsersWithMetadata(users)
        channelDao.insertChannelAndLinks(channel.toChannelEntity(), members.map {
            UserChatLinkEntity(userId = it.id, chatId = channel.id, role = it.role.name)
        })
    }

    override fun loadChannels(
            offset: Int,
            searchQuery: String,
            loadKey: LoadKeyData?,
            onlyMine: Boolean,
            ignoreDb: Boolean,
            awaitForConnection: Boolean,
            config: ChannelListConfig,
    ): Flow<PaginationResponse<SceytChannel>> {
        return callbackFlow {
            if (offset == 0) channelsCache.clear(config)

            if (!ignoreDb) {
                val dbChannels = getChannelsDb(offset, searchQuery, config, onlyMine)
                val hasNext = dbChannels.size == channelsLoadSize
                trySend(PaginationResponse.DBResponse(data = dbChannels, loadKey = loadKey, offset = offset,
                    hasNext = hasNext, hasPrev = false, query = searchQuery))

                channelsCache.addAll(config, dbChannels, false)
                ChatReactionMessagesCache.getNeededMessages(dbChannels)
            }

            if (awaitForConnection)
                ConnectionEventManager.awaitToConnectSceyt()

            val response = if (offset == 0)
                channelsRepository.getChannels(searchQuery, config, SearchChannelParams.default)
            else channelsRepository.loadMoreChannels(searchQuery, config, SearchChannelParams.default)

            if (response is SceytResponse.Success) {
                val channels = response.data ?: arrayListOf()

                val savedChannels = saveChannelsToDb(channels)
                val hasDiff = channelsCache.addAll(config, savedChannels, offset != 0) || offset == 0
                val hasNext = response.data?.size == channelsLoadSize

                trySend(PaginationResponse.ServerResponse(data = response, cacheData = channelsCache.getSorted(config),
                    loadKey = loadKey, offset = offset, hasDiff = hasDiff, hasNext = hasNext, hasPrev = false,
                    loadType = LoadNext, ignoredDb = ignoreDb, query = searchQuery))

                ChatReactionMessagesCache.getNeededMessages(response.data ?: arrayListOf())

                messageLogic.onSyncedChannels(channels)
            }

            channel.close()
            awaitClose()
        }
    }

    override suspend fun searchChannelsWithUserIds(
            offset: Int,
            searchQuery: String,
            userIds: List<String>,
            config: ChannelListConfig,
            includeSearchByUserDisplayName: Boolean,
            onlyMine: Boolean,
            ignoreDb: Boolean,
            loadKey: LoadKeyData?,
            directChatType: String,
    ): Flow<PaginationResponse<SceytChannel>> {
        return callbackFlow {
            if (offset == 0) channelsCache.clear(config)

            val searchUserIds = HashSet<String>(userIds)
            if (includeSearchByUserDisplayName) {
                val ids = usersDao.getUserIdsByDisplayName(searchQuery)
                searchUserIds.addAll(ids)
            }
            val orderByLastMessage = when (config.order) {
                ChannelListOrder.ListQueryChannelOrderLastMessage -> true
                ChannelListOrder.ListQueryChannelOrderCreatedAt -> false
            }
            val dbChannels = channelDao.searchChannelsByUserIds(
                query = searchQuery,
                userIds = searchUserIds.toList(),
                offset = offset,
                onlyMine = onlyMine,
                limit = config.queryLimit,
                types = config.types,
                orderByLastMessage = orderByLastMessage,
                directType = directChatType
            ).map { it.toChannel() }

            var hasNext = dbChannels.size == config.queryLimit
            channelsCache.addAll(config, dbChannels, false)
            trySend(PaginationResponse.DBResponse(data = dbChannels, loadKey = loadKey, offset = offset,
                hasNext = hasNext, hasPrev = false, query = searchQuery))

            val response = if (offset == 0)
                channelsRepository.getChannels(searchQuery, config, SearchChannelParams.default)
            else channelsRepository.loadMoreChannels(searchQuery, config, SearchChannelParams.default)

            if (response is SceytResponse.Success) {
                val channels = response.data ?: arrayListOf()

                val savedChannels = saveChannelsToDb(channels)
                val hasDiff = channelsCache.addAll(config, savedChannels, offset != 0) || offset == 0
                hasNext = response.data?.size == channelsLoadSize

                trySend(PaginationResponse.ServerResponse(
                    data = response,
                    cacheData = channelsCache.getSorted(config),
                    loadKey = loadKey,
                    offset = offset,
                    hasDiff = hasDiff,
                    hasNext = hasNext,
                    hasPrev = false,
                    loadType = LoadNext,
                    ignoredDb = ignoreDb,
                    query = searchQuery))
            }

            channel.close()
            awaitClose()
        }
    }

    override suspend fun getChannelsBySQLiteQuery(query: SimpleSQLiteQuery): List<SceytChannel> {
        return channelDao.getChannelsBySQLiteQuery(query).map { it.toChannel() }
    }

    override suspend fun syncChannels(config: ChannelListConfig) = callbackFlow {
        val oldChannelsIds = channelDao.getAllChannelsIds().toSet()
        val syncedChannels = arrayListOf<SceytChannel>()
        channelsRepository.getAllChannels(config.queryLimit)
            .collect { response ->
                when (response) {
                    is GetAllChannelsResponse.Proportion -> {
                        val filledChannels = saveChannelsToDb(response.channels)
                        syncedChannels.addAll(filledChannels)
                        messageLogic.onSyncedChannels(filledChannels)
                        channelsCache.updateChannel(config, *filledChannels.toTypedArray())
                        trySend(response)
                    }

                    is GetAllChannelsResponse.SuccessfullyFinished -> {
                        if (syncedChannels.isNotEmpty()) {
                            val syncedIds = syncedChannels.map { it.id }
                            val deletedChannelIds = channelDao.getNotExistingChannelIdsByIdsAndTypes(
                                ids = syncedIds,
                                types = config.types
                            )
                            deleteChannelsFromDbAndCache(channelIds = deletedChannelIds)
                            val newChannelsIds = syncedIds.minus(oldChannelsIds)
                            val newChannels = syncedChannels.filter { newChannelsIds.contains(it.id) }
                            if (newChannels.isNotEmpty()) {
                                channelsCache.newChannelsOnSync(config, newChannels)
                            }
                            SceytLog.i("syncChannelsResult",
                                "deletedChannelsIds: ${deletedChannelIds.map { it }}," +
                                        " newChannelsCount: ${newChannelsIds.size} " +
                                        " syncedChannelsCount: ${syncedChannels.size} ")
                        } else {
                            val ids = channelDao.getAllChannelIdsByTypes(config.types)
                            deleteChannelsFromDbAndCache(ids)
                            SceytLog.i("syncChannelsResult", "syncedChannels is empty, " +
                                    "clear all channels. To be deleted size: ${ids.size}")
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

    private suspend fun getChannelsDb(
            offset: Int,
            searchQuery: String,
            config: ChannelListConfig,
            onlyMine: Boolean,
    ): List<SceytChannel> {
        val orderByLastMessage = when (config.order) {
            ChannelListOrder.ListQueryChannelOrderLastMessage -> true
            ChannelListOrder.ListQueryChannelOrderCreatedAt -> false
        }
        return if (searchQuery.isBlank()) {
            channelDao.getChannels(
                limit = config.queryLimit,
                offset = offset,
                types = config.types,
                orderByLastMessage = orderByLastMessage,
                onlyMine = onlyMine
            ).map { it.toChannel() }
        } else {
            val ids = usersDao.getUserIdsByDisplayName(searchQuery)
            channelDao.searchChannelsByUserIds(
                query = searchQuery,
                userIds = ids,
                offset = offset,
                onlyMine = onlyMine,
                limit = config.queryLimit,
                types = config.types,
                orderByLastMessage = orderByLastMessage
            ).map { channel -> channel.toChannel() }
        }
    }

    private suspend fun saveChannelsToDb(list: List<SceytChannel>): List<SceytChannel> {
        if (list.isEmpty()) return emptyList()

        val links = arrayListOf<UserChatLinkEntity>()
        val users = arrayListOf<UserDb>()
        val directChatsWithDeletedPeers = arrayListOf<Long>()
        val lastMessages = arrayListOf<SceytMessage>()
        val userReactions = arrayListOf<ChatUserReactionEntity>()

        fun addEntitiesToLists(
                channelId: Long,
                members: List<SceytMember>?,
                lastMessage: SceytMessage?,
                userMessageReactions: List<SceytReaction>?,
        ) {
            members?.forEach { member ->
                links.add(UserChatLinkEntity(userId = member.id, chatId = channelId, role = member.role.name))
                users.add(member.toUserDb())
            }

            lastMessage?.let {
                lastMessages.add(it)
            }

            userMessageReactions?.forEach {
                userReactions.add(it.toUserReactionsEntity(channelId))
            }
        }

        var updatedList = list.map { channel ->
            if (channel.isGroup) {
                addEntitiesToLists(channel.id, channel.members, channel.lastMessage, channel.newReactions)
            } else {
                val members = arrayListOf<SceytMember>()
                channel.getPeer()?.let {
                    if (it.user.state == UserState.Deleted)
                        directChatsWithDeletedPeers.add(channel.id)
                    members.add(it)
                }
                addEntitiesToLists(channel.id, members, channel.lastMessage, channel.newReactions)
            }

            fillChannelsNeededInfo(channel)
        }
        usersDao.insertUsersWithMetadata(users)
        messageLogic.saveChannelLastMessagesToDb(lastMessages)
        chatUserReactionDao.replaceChannelUserReactions(userReactions)

        // Delete old links where channel peer is deleted.
        directChatsWithDeletedPeers.forEach {
            myId?.let { id -> channelDao.deleteChatLinksExceptUser(it, id) }
        }

        updatedList = updateChannelPendingLastMessages(updatedList)
        channelDao.insertChannelsAndLinks(updatedList.map { it.toChannelEntity() }, links)
        return updatedList
    }

    override suspend fun findOrCreatePendingChannelByMembers(
            data: CreateChannelData,
    ): SceytResponse<SceytChannel> {
        var metadata = data.metadata
        val members = data.members.toSet().toList()
        val membersCount = members.size
        val isSelf = membersCount == 1 && members[0].id == myId && data.type == ChannelTypeEnum.Direct.value
        val channelDb = if (isSelf) {
            metadata = Gson().toJson(SelfChannelMetadata(1))
            channelDao.getSelfChannel()
        } else {
            if (membersCount == 1) {
                channelDao.getChannelByUserAndType(members[0].id, data.type)
            } else {
                val ids = members.map { it.id }.toSet().toList()
                channelDao.getChannelByUsersAndType(ids, data.type)
            }
        }
        if (channelDb != null) {
            if (channelDb.channelEntity.pending)
                channelsCache.addPendingChannel(channelDb.toChannel())
            return SceytResponse.Success(channelDb.toChannel())
        }
        val channelId = members.map { it.id }.toSet().sorted().joinToString(separator = "$").toSha256()
        return createPendingChannelAndSave(data.copy(metadata = metadata), channelId)
    }

    override suspend fun findOrCreatePendingChannelByUri(
            data: CreateChannelData,
    ): SceytResponse<SceytChannel> {
        if (data.uri.isBlank()) return SceytResponse.Error(SceytException(0, "Uri is empty"))
        val channelDb = channelDao.getChannelByUri(data.uri)
        if (channelDb != null) {
            if (channelDb.channelEntity.pending)
                channelsCache.addPendingChannel(channelDb.toChannel())
            return SceytResponse.Success(channelDb.toChannel())
        }
        // Try to fetch channel from server
        val response = getChannelFromServerByUri(data.uri)
        if (response is SceytResponse.Success && response.data != null) {
            return SceytResponse.Success(response.data)
        }
        val channelId = data.uri.toSha256()
        return createPendingChannelAndSave(data, channelId)
    }

    private suspend fun createPendingChannelAndSave(
            data: CreateChannelData,
            channelId: Long,
    ): SceytResponse<SceytChannel> {
        val fail = SceytResponse.Error<SceytChannel>(SceytException(0, "Failed to create direct channel myId is null"))
        val myId = myId ?: return fail
        val currentUser = SceytChatUIKit.currentUser
                ?: usersDao.getUserById(myId)?.toSceytUser()
                ?: SceytUser(myId)

        var members = data.members.toSet().toList()
        if (members.none { it.id == myId }) {
            members = members.plus(SceytMember(
                role = Role(RoleTypeEnum.Owner.value),
                user = currentUser
            ))
        }
        val channel = createPendingChannel(
            channelId = channelId,
            createdBy = currentUser,
            data = data.copy(members = members)
        )

        insertChannelWithMembers(channel, *members.toTypedArray())
        channelsCache.addPendingChannel(channel)
        return SceytResponse.Success(channel)
    }

    override suspend fun createChannel(createChannelData: CreateChannelData): SceytResponse<SceytChannel> {
        return channelsRepository.createChannel(createChannelData).onSuccessNotNull { channel ->
            channel.members?.toTypedArray()?.let {
                val lastMessage = getChannelCurrentLastMessage(channel)
                val updated = channel.copy(lastMessage = lastMessage)
                insertChannelWithMembers(updated, *it)
                channelsCache.upsertChannel(updated)
            }
        }
    }

    override suspend fun createNewChannelInsteadOfPendingChannel(channel: SceytChannel): SceytResponse<SceytChannel> {
        val pendingChannelId = channel.id
        val response = channelsRepository.createChannel(CreateChannelData(
            type = channel.type,
            uri = channel.uri ?: "",
            subject = channel.subject ?: "",
            avatarUrl = channel.avatarUrl ?: "",
            metadata = channel.metadata ?: "",
            members = channel.members ?: arrayListOf()))
        if (response is SceytResponse.Success) {
            var newChannel = response.data
                    ?: return SceytResponse.Error(SceytException(0, "create channel response is success, but channel is null"))

            val newChannelId = newChannel.id
            // Set new channel last message to pending channel last message with new channel id
            newChannel = newChannel.copy(lastMessage = channel.lastMessage?.copy(channelId = newChannelId))

            channelDao.deleteChannelAndLinks(pendingChannelId)
            channelDao.insertChannelAndLinks(newChannel.toChannelEntity(), newChannel.members?.map {
                UserChatLinkEntity(userId = it.id, chatId = newChannelId, role = it.role.name)
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
        if (channelsCache.isPending(channelId)) {
            deleteChannelFromDbAndCache(channelId)
            return SceytResponse.Success(channelId)
        }

        val response = channelsRepository.clearHistory(channelId, forEveryone)

        if (response is SceytResponse.Success) {
            UploadAndSendAttachmentWorkManager.cancelWorksByTag(context, channelId.toString())
            SendForwardMessagesWorkManager.cancelWorksByTag(context, channelId.toString())
            clearHistory(channelId)
        }

        return response
    }

    override suspend fun blockAndLeaveChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.blockChannel(channelId)

        if (response is SceytResponse.Success) {
            UploadAndSendAttachmentWorkManager.cancelWorksByTag(context, channelId.toString())
            SendForwardMessagesWorkManager.cancelWorksByTag(context, channelId.toString())
            deleteChannelFromDbAndCache(channelId)
        }

        return response
    }

    override suspend fun unblockChannel(channelId: Long): SceytResponse<SceytChannel> {
        return channelsRepository.unBlockChannel(channelId)
    }

    override suspend fun leaveChannel(channelId: Long): SceytResponse<Long> {
        val response = channelsRepository.leaveChannel(channelId)

        if (response is SceytResponse.Success) {
            UploadAndSendAttachmentWorkManager.cancelWorksByTag(context, channelId.toString())
            SendForwardMessagesWorkManager.cancelWorksByTag(context, channelId.toString())
            deleteChannelFromDbAndCache(channelId)
        }

        return response
    }

    override suspend fun deleteChannel(channelId: Long): SceytResponse<Long> {
        if (channelsCache.isPending(channelId)) {
            deleteChannelFromDbAndCache(channelId)
            return SceytResponse.Success(channelId)
        }

        val response = channelsRepository.deleteChannel(channelId)

        if (response is SceytResponse.Success) {
            UploadAndSendAttachmentWorkManager.cancelWorksByTag(context, channelId.toString())
            SendForwardMessagesWorkManager.cancelWorksByTag(context, channelId.toString())
            deleteChannelFromDbAndCache(channelId)
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

    override suspend fun unHideChannel(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.unHideChannel(channelId)

        if (response is SceytResponse.Success)
            response.data?.let { channel ->
                channel.members?.toTypedArray()?.let {
                    insertChannelWithMembers(channel, *it)
                    channelsCache.upsertChannel(channel)
                }
            }

        return response
    }

    override suspend fun getChannelFromDb(channelId: Long): SceytChannel? {
        return channelDao.getChannelById(channelId)?.toChannel()
    }

    override suspend fun getRetentionPeriodByChannelId(channelId: Long): Long {
        return channelDao.getRetentionPeriodByChannelId(channelId) ?: 0
    }

    override suspend fun getDirectChannelFromDb(peerId: String): SceytChannel? {
        return channelDao.getChannelByUserAndType(peerId, ChannelTypeEnum.Direct.value)?.toChannel()
    }

    override suspend fun getChannelFromServer(
            channelId: Long,
    ): SceytResponse<SceytChannel> = withContext(Dispatchers.IO) {
        return@withContext channelsRepository.getChannel(channelId)
            .onSuccessNotNull { channel ->
                val lastMessage = getChannelCurrentLastMessage(channel)
                channel.copy(lastMessage = lastMessage).toChannelEntity().let {
                    insertChannelWithMembers(channel, *channel.members?.toTypedArray().orEmpty())
                    getAndUpdateCashedChannel(channelId)?.let { updatedChannel ->
                        messageLogic.onSyncedChannels(arrayListOf(updatedChannel))
                    }
                }
            }
            .onError {
                getChannelFromDb(channelId)?.let {
                    if (it.pending)
                        return@withContext SceytResponse.Success(it)
                }
            }
    }

    override suspend fun getChannelFromServerByUri(uri: String): SceytResponse<SceytChannel?> {
        val response = channelsRepository.getChannelByUri(uri)
        if (response is SceytResponse.Success) {
            response.data?.let { channel ->
                insertChannelWithMembers(channel, *(channel.members ?: emptyList()).toTypedArray())
                channelsCache.getCachedData().entries.forEach { (_, map) ->
                    map.entries.find { it.value.uri == uri }?.let { (id, cachedChannel) ->
                        if (cachedChannel.pending) {
                            channelsCache.pendingChannelCreated(id, channel)
                        }
                    }
                }
            }
        }
        return response
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
                getAndUpdateCashedChannel(it.id)
            }
        }

        return response
    }

    override suspend fun join(channelId: Long): SceytResponse<SceytChannel> {
        val response = channelsRepository.join(channelId).onSuccessNotNull {
            insertChannelWithMembers(
                channel = it,
                members = it.members?.toTypedArray().orEmpty()
            )
            channelsCache.upsertChannel(it)
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
            channelsCache.getOneOf(channelId)?.let {
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

    override suspend fun updateLastMessageIfNeeded(channelId: Long, message: SceytMessage?) {
        if (message?.deliveryStatus != DeliveryStatus.Pending) {
            channelDao.updateLastMessage(channelId, message?.tid, message?.createdAt)
            channelsCache.updateLastMessage(channelId, message)
        }
    }

    override suspend fun setUnreadCount(channelId: Long, count: Int) {
        channelDao.updateUnreadCount(channelId, count)
        channelsCache.updateUnreadCount(channelId, count)
    }

    override suspend fun blockUnBlockUser(userId: String, block: Boolean) {
        val channels = channelDao.getChannelByPeerId(userId)
        channelsCache.upsertChannels(channels.map { it.toChannel() })
    }

    override suspend fun sendChannelEvent(channelId: Long, event: String) {
        channelsRepository.sendChannelEvent(channelId, event)
    }

    override suspend fun updateDraftMessage(
            channelId: Long,
            message: String?,
            mentionUsers: List<Mention>,
            styling: List<BodyStyleRange>?,
            replyOrEditMessage: SceytMessage?,
            isReply: Boolean,
    ) {
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
            val links = mentionUsers.map {
                DraftMessageUserLinkEntity(chatId = channelId, userId = it.recipientId)
            }
            draftMessageDao.insertWithUserLinks(draftMessageEntity, links)
            draftMessageEntity.toDraftMessage(mentionUsers.map {
                createEmptyUser(it.recipientId, it.name)
            }, replyOrEditMessage)
        }
        channelsCache.updateChannelDraftMessage(channelId, draftMessage)
    }

    override suspend fun getChannelsCountFromDb(): Int {
        return channelDao.getAllChannelsCount()
    }

    override suspend fun onUserPresenceChanged(users: List<SceytPresenceChecker.PresenceUser>) {
        users.forEach { presenceUser ->
            channelsCache.getCachedData().values.forEach {
                it.values.toMutableList().forEach { channel ->
                    val user = presenceUser.user
                    if (channel.isDirect() && channel.getPeer()?.id == user.id)
                        channelsCache.updateChannelPeer(channel.id, user)
                }
            }
        }
    }

    override fun getChannelMessageCount(channelId: Long): Flow<Long> {
        return messageDao.getMessagesCountAsFlow(channelId).map {
            it ?: 0
        }.distinctUntilChanged()
    }

    override fun getTotalUnreadCount(channelTypes: List<String>): Flow<Long> {
        return channelDao.getTotalUnreadCountAsFlow(channelTypes).map {
            it ?: 0
        }.distinctUntilChanged()
    }

    private suspend fun updateChannelPendingLastMessages(channels: List<SceytChannel>): List<SceytChannel> {
        if (channels.isEmpty()) return channels
        val mutableList = channels.toList().toArrayList()
        val messageTIds = channelDao.getChannelsLastMessageTIds(mutableList.map { it.id })
        if (messageTIds.isEmpty()) return channels

        val pendingLastMessages = messageDao.getPendingMessagesByTIds(messageTIds)

        pendingLastMessages.forEach { messageDb ->
            mutableList.findIndexed { it.id == messageDb.messageEntity.channelId }?.let { (index, item) ->
                if (messageDb.messageEntity.createdAt > (item.lastMessage?.createdAt ?: 0))
                    mutableList[index] = item.copy(lastMessage = messageDb.toSceytMessage())
            }
        }
        return mutableList
    }

    private suspend fun getChannelCurrentLastMessage(channel: SceytChannel): SceytMessage? {
        val messageTid = channelDao.getChannelLastMessageTid(channel.id)
                ?: return channel.lastMessage

        return messageDao.getPendingMessageByTid(messageTid)?.let { message ->
            if (message.messageEntity.createdAt > (channel.lastMessage?.createdAt ?: 0)) {
                message.toSceytMessage()
            } else channel.lastMessage
        } ?: channel.lastMessage
    }

    private suspend fun deleteChannelFromDbAndCache(channelId: Long) {
        channelDao.deleteChannelAndLinks(channelId)
        messageDao.deleteAllMessagesByChannel(channelId)
        rangeDao.deleteChannelLoadRanges(channelId)
        channelsCache.deleteChannel(channelId)
    }

    private suspend fun deleteChannelsFromDbAndCache(channelIds: List<Long>) {
        channelDao.deleteAllChannelsAndLinksById(channelIds)
        messageDao.deleteAllChannelsMessages(channelIds)
        rangeDao.deleteChannelsLoadRanges(channelIds)
        channelsCache.deleteChannel(*channelIds.toLongArray())
    }

    private suspend fun clearHistory(channelId: Long) {
        channelDao.updateLastMessage(channelId, null, null)
        messageDao.deleteAllMessagesByChannel(channelId)
        rangeDao.deleteChannelLoadRanges(channelId)
        channelsCache.clearedHistory(channelId)
    }

    private suspend fun deleteMessage(channelId: Long, message: SceytMessage) {
        channelsCache.getOneOf(channelId)?.let {
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