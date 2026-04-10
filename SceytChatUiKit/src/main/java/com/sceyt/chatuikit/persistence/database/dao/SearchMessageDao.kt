package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHANNEL_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb

@Dao
internal abstract class SearchMessageDao {

    /**
     * Searches messages globally with automatic query normalization:
     * - Leading/trailing whitespace is trimmed
     * - Multi-word queries (words separated by any whitespace) use AND logic:
     *   every word must appear somewhere in the body (order-independent)
     */
    @Transaction
    open suspend fun searchMessagesGlobally(
        query: String,
        senderId: String?,
        limit: Int,
        offset: Int,
        pendingStatus: MessageDeliveryStatus = MessageDeliveryStatus.Pending,
    ): List<MessageDb> {
        val words = query.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val senderIgnored = senderId.isNullOrBlank()

        if (words.size <= 1) {
            return searchMessagesGloballyQuery(
                query = words.firstOrNull() ?: "",
                senderId = senderId,
                limit = limit,
                offset = offset,
                queryEmpty = words.isEmpty(),
                senderIgnored = senderIgnored,
                pendingStatus = pendingStatus,
            )
        }

        // Multi-word: every word must appear somewhere in the body (AND, any order)
        val wordConditions = words.joinToString(" AND ") {
            "message.body LIKE '%' || ? || '%'"
        }
        val senderFilter = if (!senderIgnored) "AND message.fromId = ?" else ""
        val sql = """
            SELECT message.*
            FROM $MESSAGE_TABLE AS message
            JOIN $CHANNEL_TABLE AS channel ON channel.chat_id = message.channelId
            WHERE message.message_id IS NOT NULL
              AND message.deliveryStatus != $PENDING_STATUS
              AND message.unList = 0
              $senderFilter
              AND ($wordConditions)
            ORDER BY message.createdAt DESC, message.message_id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val args = buildList<Any> {
            if (!senderIgnored) add(senderId.orEmpty())
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
          AND (:queryEmpty OR message.body LIKE '%' || :query || '%')
        ORDER BY message.createdAt DESC, message.message_id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    protected abstract suspend fun searchMessagesGloballyQuery(
        query: String,
        senderId: String?,
        limit: Int,
        offset: Int,
        queryEmpty: Boolean,
        senderIgnored: Boolean,
        pendingStatus: MessageDeliveryStatus = MessageDeliveryStatus.Pending,
    ): List<MessageDb>

    @Transaction
    @RawQuery
    protected abstract suspend fun searchMessagesRaw(query: SupportSQLiteQuery): List<MessageDb>

    private companion object {
        private const val PENDING_STATUS = 0
    }
}
