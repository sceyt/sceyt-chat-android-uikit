package com.sceyt.chatuikit.persistence.logic

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentKind
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchMessageResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchPage
import com.sceyt.chatuikit.data.models.search.toAttachmentTypes
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.GlobalSearchDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelDb
import com.sceyt.chatuikit.persistence.interactor.GlobalSearchDataSource
import com.sceyt.chatuikit.persistence.mappers.toAttachment
import com.sceyt.chatuikit.persistence.mappers.toChannel
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.mappers.toSceytUser

internal class GlobalSearchLocalInteractor(
    private val channelDao: ChannelDao,
    private val messageDao: MessageDao,
    private val globalSearchDao: GlobalSearchDao,
    private val userDao: UserDao,
) : GlobalSearchDataSource {

    private val myUserId: String?
        get() = SceytChatUIKit.chatUIFacade.myId

    override suspend fun searchUsersLinkedToJoinedChannelsByDisplayName(
        query: String,
        limit: Int,
    ): List<SceytUser> {
        if (query.isBlank()) return emptyList()
        return userDao.searchUsersLinkedToJoinedChannelsByDisplayName(
            searchQuery = query,
            excludedUserId = myUserId,
            limit = limit
        ).map { it.toSceytUser() }
    }

    override suspend fun getRecentChats(
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<SceytChannel> {
        val data = channelDao.getChannels(
            limit = limit + 1,
            offset = offset,
            types = SceytChatUIKit.config.channelTypesConfig.getPrivateTypes(),
            orderByLastMessage = true,
            onlyMine = true
        )
        return data.toChannelPage(limit)
    }

    override suspend fun searchChats(
        query: String,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<SceytChannel> {
        if (query.isBlank()) return GlobalSearchPage(emptyList(), false)
        val userIds = userDao.getUserIdsByDisplayName(query)

        val data = globalSearchDao.searchChannelsByUserIds(
            query = query,
            userIds = userIds,
            limit = limit + 1,
            offset = offset,
            onlyMine = true,
            types = SceytChatUIKit.config.channelTypesConfig.getPrivateTypes(),
            orderByLastMessage = true
        )
        return data.toChannelPage(limit)
    }

    override suspend fun getRecentChannels(
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<SceytChannel> {
        val data = globalSearchDao.searchChannelsBySubjectAndTypes(
            query = "",
            types = SceytChatUIKit.config.channelTypesConfig.getDiscoverableTypes(),
            limit = limit + 1,
            offset = offset
        )
        return data.toChannelPage(limit)
    }

    override suspend fun searchChannels(
        query: String,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<SceytChannel> {
        if (query.isBlank()) return getRecentChannels(offset = offset, limit = limit)
        val data = globalSearchDao.searchChannelsBySubjectAndTypes(
            query = query,
            types = SceytChatUIKit.config.channelTypesConfig.getDiscoverableTypes(),
            limit = limit + 1,
            offset = offset
        )
        return data.toChannelPage(limit)
    }

    override suspend fun searchMessages(
        query: String,
        senderId: String?,
        channelTypes: List<String>,
        onlyJoined: Boolean,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchMessageResult> {
        val data = globalSearchDao.searchMessages(
            query = query,
            senderId = senderId,
            channelTypes = channelTypes,
            onlyJoined = onlyJoined,
            limit = limit + 1,
            offset = offset,
        )

        val hasMore = data.size > limit
        val limited = data.take(limit)
        if (limited.isEmpty()) return GlobalSearchPage(emptyList(), false)

        val channels =
            channelDao.getChannelsById(limited.map { it.messageEntity.channelId }.distinct())
                .associateBy { it.channelEntity.id }

        return GlobalSearchPage(
            data = limited.mapNotNull { messageDb ->
                val channel = channels[messageDb.messageEntity.channelId]?.toChannel()
                    ?: return@mapNotNull null
                GlobalSearchMessageResult(
                    message = messageDb.toSceytMessage(),
                    channel = channel
                )
            },
            hasMore = hasMore
        )
    }

    override suspend fun searchAttachments(
        kind: GlobalSearchAttachmentKind,
        query: String,
        senderId: String?,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchAttachmentResult> {
        val data = if (kind == GlobalSearchAttachmentKind.Link) {
            globalSearchDao.searchLinkAttachments(
                query = query,
                senderId = senderId,
                types = kind.toAttachmentTypes(),
                limit = limit + 1,
                offset = offset,
                queryEmpty = query.isBlank(),
                senderIgnored = senderId.isNullOrBlank(),
            )
        } else {
            globalSearchDao.searchAttachments(
                query = query,
                senderId = senderId,
                types = kind.toAttachmentTypes(),
                limit = limit + 1,
                offset = offset,
                queryEmpty = query.isBlank(),
                senderIgnored = senderId.isNullOrBlank(),
                matchAttachmentName = kind == GlobalSearchAttachmentKind.File,
            )
        }

        val hasMore = data.size > limit
        val limited = data.take(limit)
        if (limited.isEmpty()) return GlobalSearchPage(emptyList(), false)

        val messages =
            messageDao.getMessagesByTids(limited.map { it.attachmentEntity.messageTid }.distinct())
                .associateBy { it.messageEntity.tid }
        val channels = channelDao.getChannelsById(
            messages.values.map { it.messageEntity.channelId }.distinct()
        ).associateBy { it.channelEntity.id }

        val results = limited.mapNotNull { attachmentDb ->
            val message =
                messages[attachmentDb.attachmentEntity.messageTid] ?: return@mapNotNull null
            val channel =
                channels[message.messageEntity.channelId]?.toChannel() ?: return@mapNotNull null
            GlobalSearchAttachmentResult(
                attachment = attachmentDb.toAttachment(),
                message = message.toSceytMessage(),
                channel = channel,
                sender = message.from?.toSceytUser(),
                kind = kind
            )
        }

        return GlobalSearchPage(results, hasMore)
    }

    private fun List<ChannelDb>.toChannelPage(
        limit: Int,
    ): GlobalSearchPage<SceytChannel> {
        val hasMore = size > limit
        return GlobalSearchPage(
            data = take(limit).map { it.toChannel() },
            hasMore = hasMore
        )
    }
}
