package com.sceyt.chatuikit.persistence.interactor

import com.sceyt.chatuikit.data.models.channels.SceytChannel
import com.sceyt.chatuikit.data.models.messages.SceytUser
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentKind
import com.sceyt.chatuikit.data.models.search.GlobalSearchAttachmentResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchMessageResult
import com.sceyt.chatuikit.data.models.search.GlobalSearchPage

interface GlobalSearchDataSource {
    suspend fun getRecentChats(offset: Int, limit: Int): GlobalSearchPage<SceytChannel>
    suspend fun searchChats(query: String, offset: Int, limit: Int): GlobalSearchPage<SceytChannel>
    suspend fun getRecentChannels(offset: Int, limit: Int): GlobalSearchPage<SceytChannel>
    suspend fun searchChannels(
        query: String,
        offset: Int,
        limit: Int
    ): GlobalSearchPage<SceytChannel>

    suspend fun searchMessages(
        query: String,
        senderId: String?,
        channelTypes: List<String>,
        onlyJoined: Boolean,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchMessageResult>

    suspend fun searchAttachments(
        kind: GlobalSearchAttachmentKind,
        query: String,
        senderId: String?,
        offset: Int,
        limit: Int,
    ): GlobalSearchPage<GlobalSearchAttachmentResult>

    suspend fun searchUsersLinkedToJoinedChannelsByDisplayName(
        query: String,
        limit: Int,
    ): List<SceytUser>
}
