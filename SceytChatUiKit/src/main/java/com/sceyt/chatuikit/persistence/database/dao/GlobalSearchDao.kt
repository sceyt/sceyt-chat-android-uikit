package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.ATTACHMENT_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHANNEL_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelDb
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb

@Dao
internal abstract class GlobalSearchDao {

    /**
     * Unified message search for both Chats and Channels tabs.
     *
     * - [channelTypes] empty  → no channel type filter (all channels)
     * - [channelTypes] non-empty → `channel.type IN (:channelTypes)` filter
     * - [onlyJoined] true → requires `channel.userRole IS NOT NULL AND channel.userRole != ''`
     *
     * Multi-word queries (words separated by whitespace) use AND logic:
     * every word must appear somewhere in the body (order-independent).
     */
    @Transaction
    open suspend fun searchMessages(
        query: String,
        senderId: String?,
        channelTypes: List<String>,
        onlyJoined: Boolean,
        limit: Int,
        offset: Int,
        pendingStatus: MessageDeliveryStatus = MessageDeliveryStatus.Pending,
    ): List<MessageDb> {
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val senderIgnored = senderId.isNullOrBlank()
        val channelTypesEmpty = channelTypes.isEmpty()

        if (words.size <= 1) {
            return searchMessagesQuery(
                query = words.firstOrNull() ?: "",
                senderId = senderId,
                channelTypes = channelTypes,
                onlyJoined = onlyJoined,
                limit = limit,
                offset = offset,
                queryEmpty = words.isEmpty(),
                senderIgnored = senderIgnored,
                channelTypesEmpty = channelTypesEmpty,
                pendingStatus = pendingStatus,
            )
        }

        // Multi-word: every word must appear somewhere in the body (AND, any order)
        val wordConditions = words.joinToString(" AND ") {
            "message.body LIKE '%' || ? || '%'"
        }
        val senderFilter = if (!senderIgnored) "AND message.fromId = ?" else ""
        val typeFilter = if (!channelTypesEmpty) {
            "AND channel.type IN (${channelTypes.joinToString(",") { "?" }})"
        } else ""
        val joinedFilter = if (onlyJoined) {
            "AND channel.userRole IS NOT NULL AND channel.userRole != ''"
        } else ""

        val sql = """
            SELECT message.*
            FROM $MESSAGE_TABLE AS message
            JOIN $CHANNEL_TABLE AS channel ON channel.chat_id = message.channelId
            WHERE message.message_id IS NOT NULL
              AND message.deliveryStatus != $PENDING_STATUS
              AND message.unList = 0
              $senderFilter
              $typeFilter
              $joinedFilter
              AND ($wordConditions)
            ORDER BY message.createdAt DESC, message.message_id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val args = buildList<Any> {
            if (!senderIgnored) add(senderId)
            addAll(channelTypes)
            addAll(words)
            add(limit)
            add(offset)
        }

        return searchMessagesRaw(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

    @Transaction
    @Query(
        """
        SELECT message.*
        FROM $MESSAGE_TABLE AS message
        JOIN $CHANNEL_TABLE AS channel ON channel.chat_id = message.channelId
        WHERE message.message_id IS NOT NULL
          AND message.deliveryStatus != :pendingStatus
          AND message.unList = 0
          AND (:senderIgnored OR message.fromId = :senderId)
          AND (:channelTypesEmpty OR channel.type IN (:channelTypes))
          AND (NOT :onlyJoined OR (channel.userRole IS NOT NULL AND channel.userRole != ''))
          AND (:queryEmpty OR message.body LIKE '%' || :query || '%')
        ORDER BY message.createdAt DESC, message.message_id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    protected abstract suspend fun searchMessagesQuery(
        query: String,
        senderId: String?,
        channelTypes: List<String>,
        onlyJoined: Boolean,
        limit: Int,
        offset: Int,
        queryEmpty: Boolean,
        senderIgnored: Boolean,
        channelTypesEmpty: Boolean,
        pendingStatus: MessageDeliveryStatus = MessageDeliveryStatus.Pending,
    ): List<MessageDb>

    @Transaction
    @RawQuery
    protected abstract suspend fun searchMessagesRaw(query: SupportSQLiteQuery): List<MessageDb>


    @Transaction
    @Query(
        """
        SELECT attachment.*
        FROM $ATTACHMENT_TABLE AS attachment
        JOIN $MESSAGE_TABLE AS message ON message.tid = attachment.messageTid
        JOIN $CHANNEL_TABLE AS channel ON channel.chat_id = message.channelId
        WHERE message.message_id IS NOT NULL
          AND message.deliveryStatus != :pendingStatus
          AND message.unList = 0
          AND attachment.viewOnce != 1
          AND attachment.type IN (:types)
          AND (:senderIgnored OR message.fromId = :senderId)
          AND (
              :queryEmpty
              OR message.body LIKE '%' || :query || '%'
              OR (:matchAttachmentName AND attachment.name LIKE '%' || :query || '%')
              OR (:matchUrl AND attachment.url LIKE '%' || :query || '%')
          )
        ORDER BY attachment.createdAt DESC, attachment.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun searchAttachments(
        query: String,
        senderId: String?,
        types: List<String>,
        limit: Int,
        offset: Int,
        queryEmpty: Boolean,
        senderIgnored: Boolean,
        matchAttachmentName: Boolean,
        matchUrl: Boolean,
        pendingStatus: MessageDeliveryStatus = MessageDeliveryStatus.Pending,
    ): List<AttachmentDb>

    // endregion

    @Transaction
    @Query(
        """
        SELECT *
        FROM $CHANNEL_TABLE
        WHERE (:typesEmpty OR type IN (:types))
          AND (NOT pending OR lastMessageTid != 0)
          AND (:queryEmpty OR subject LIKE '%' || :query || '%')
        ORDER BY
          CASE WHEN pinnedAt > 0 THEN pinnedAt END DESC,
          CASE WHEN lastMessageAt IS NOT NULL THEN lastMessageAt END DESC,
          createdAt DESC
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun searchChannelsBySubjectAndTypes(
        query: String,
        types: List<String>,
        limit: Int,
        offset: Int,
        queryEmpty: Boolean = query.isBlank(),
        typesEmpty: Boolean = types.isEmpty(),
    ): List<ChannelDb>

    private companion object {
        private const val PENDING_STATUS = 0
    }
}
