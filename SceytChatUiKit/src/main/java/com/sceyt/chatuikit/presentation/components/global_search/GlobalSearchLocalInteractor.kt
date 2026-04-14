package com.sceyt.chatuikit.presentation.components.global_search

import com.sceyt.chatuikit.SceytChatUIKit
import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.AttachmentTypeEnum
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.koin.SceytKoinComponent
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.GlobalSearchDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelDb
import com.sceyt.chatuikit.persistence.mappers.toAttachment
import com.sceyt.chatuikit.persistence.mappers.toChannel
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.mappers.toSceytUser
import org.koin.core.component.inject

internal interface GlobalSearchDataSource {
    suspend fun searchMemberSuggestions(query: String, limit: Int): List<SceytUser>
    suspend fun getRecentChats(offset: Int, limit: Int): GlobalSearchPage<SceytChannel>
    suspend fun searchChats(query: String, limit: Int): GlobalSearchPage<SceytChannel>
    suspend fun getRecentChannels(offset: Int, limit: Int): GlobalSearchPage<SceytChannel>
    suspend fun searchChannels(query: String, limit: Int): GlobalSearchPage<SceytChannel>
    suspend fun searchMessages(
        query: String,
        senderId: String?,
        channelTypes: List<String>,
        onlyJoined: Boolean,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchMessageResult>

    suspend fun searchAttachments(
        tab: GlobalSearchTab,
        query: String,
        senderId: String?,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchAttachmentResult>
}

internal class GlobalSearchLocalInteractor :
    SceytKoinComponent,
    GlobalSearchDataSource,
    GlobalSearchMemberSuggestionsProvider {
    private val channelDao: ChannelDao by inject()
    private val messageDao: MessageDao by inject()
    private val globalSearchDao: GlobalSearchDao by inject()
    private val userDao: UserDao by inject()

    private val myUserId: String?
        get() = SceytChatUIKit.chatUIFacade.myId

    override suspend fun searchMemberSuggestions(
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

    override suspend fun provideSuggestions(query: String, limit: Int): List<SceytUser> {
        return searchMemberSuggestions(query, limit)
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
        limit: Int,
    ): GlobalSearchPage<SceytChannel> {
        if (query.isBlank()) return GlobalSearchPage(emptyList(), false)
        val userIds = userDao.getUserIdsByDisplayName(query)

        val data = channelDao.searchChannelsByUserIds(
            query = query,
            userIds = userIds,
            limit = limit + 1,
            offset = 0,
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
        limit: Int,
    ): GlobalSearchPage<SceytChannel> {
        if (query.isBlank()) return getRecentChannels(offset = 0, limit = limit)
        val data = globalSearchDao.searchChannelsBySubjectAndTypes(
            query = query,
            types = SceytChatUIKit.config.channelTypesConfig.getDiscoverableTypes(),
            limit = limit + 1,
            offset = 0
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
        tab: GlobalSearchTab,
        query: String,
        senderId: String?,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchAttachmentResult> {
        val kind = tab.toAttachmentKind() ?: return GlobalSearchPage(emptyList(), false)
        val data = if (tab == GlobalSearchTab.Links) {
            globalSearchDao.searchLinkAttachments(
                query = query,
                senderId = senderId,
                types = tab.toAttachmentTypes(),
                limit = limit + 1,
                offset = offset,
                queryEmpty = query.isBlank(),
                senderIgnored = senderId.isNullOrBlank(),
            )
        } else {
            globalSearchDao.searchAttachments(
                query = query,
                senderId = senderId,
                types = tab.toAttachmentTypes(),
                limit = limit + 1,
                offset = offset,
                queryEmpty = query.isBlank(),
                senderIgnored = senderId.isNullOrBlank(),
                matchAttachmentName = tab == GlobalSearchTab.Files,
            )
        }

        val hasMore = data.size > limit
        val limited = data.take(limit)
        if (limited.isEmpty()) return GlobalSearchPage(emptyList(), false)

        val messages =
            messageDao.getMessagesByTids(limited.map { it.attachmentEntity.messageTid }.distinct())
                .associateBy { it.messageEntity.tid }
        val channels = channelDao.getChannelsById(messages.values.map { it.messageEntity.channelId }
            .distinct())
            .associateBy { it.channelEntity.id }

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

    private fun GlobalSearchTab.toAttachmentKind(): GlobalSearchAttachmentKind? = when (this) {
        GlobalSearchTab.Media -> GlobalSearchAttachmentKind.Media
        GlobalSearchTab.Files -> GlobalSearchAttachmentKind.File
        GlobalSearchTab.Voice -> GlobalSearchAttachmentKind.Voice
        GlobalSearchTab.Links -> GlobalSearchAttachmentKind.Link
        else -> null
    }

    private fun GlobalSearchTab.toAttachmentTypes(): List<String> = when (this) {
        GlobalSearchTab.Media -> listOf(
            AttachmentTypeEnum.Image.value,
            AttachmentTypeEnum.Video.value
        )

        GlobalSearchTab.Files -> listOf(AttachmentTypeEnum.File.value)
        GlobalSearchTab.Voice -> listOf(AttachmentTypeEnum.Voice.value)
        GlobalSearchTab.Links -> listOf(AttachmentTypeEnum.Link.value)
        else -> emptyList()
    }
}
