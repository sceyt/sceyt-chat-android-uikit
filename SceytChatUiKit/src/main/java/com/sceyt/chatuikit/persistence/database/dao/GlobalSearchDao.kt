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
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LINK_DETAILS_TABLE
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
     * Word-prefix semantics: each query word must appear at the start of some word in the body.
     * Word boundaries recognised: start of string, space (U+0020), and newline (U+000A / LF).
     * Other delimiters (punctuation, tab, etc.) are not treated as word boundaries.
     * Multi-word queries use AND logic — all words must be present (order-independent).
     *
     * Limitations:
     * - SQLite LIKE is case-insensitive for ASCII only; Unicode queries may not fold correctly.
     * - The after-space / after-newline conditions use a leading `%` wildcard and cannot be
     *   accelerated by a B-tree index. For production-scale search migrate to an FTS5 virtual
     *   table with the unicode61 tokenizer.
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

        // Empty query → no body filter (match all); otherwise every word must word-prefix-match.
        val bodyCondition = if (words.isEmpty()) "1"
        else words.joinToString(" AND ") { WORD_PREFIX_CONDITION }

        val senderClause = if (!senderIgnored) "AND message.fromId = ?" else ""
        val typeClause = if (channelTypes.isNotEmpty())
            "AND channel.type IN (${channelTypes.joinToString(",") { "?" }})" else ""
        val joinedClause = if (onlyJoined)
            "AND channel.userRole IS NOT NULL AND channel.userRole != ''" else ""

        val sql = """
            SELECT message.*
            FROM $MESSAGE_TABLE AS message
            JOIN $CHANNEL_TABLE AS channel ON channel.chat_id = message.channelId
            WHERE message.message_id IS NOT NULL
              AND message.deliveryStatus != ?
              AND message.unList = 0
              $senderClause
              $typeClause
              $joinedClause
              AND ($bodyCondition)
            ORDER BY message.createdAt DESC, message.message_id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val args = buildList<Any> {
            add(pendingStatus.toDbValue())
            if (!senderIgnored) add(senderId)
            addAll(channelTypes)
            words.forEach { add(it); add(it); add(it) } // 3× per word: start-of-body, after-space, after-newline
            add(limit)
            add(offset)
        }

        return searchMessagesRaw(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

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
          AND channel.userRole IS NOT NULL AND channel.userRole != ''
          AND (:senderIgnored OR message.fromId = :senderId)
          AND (
              :queryEmpty
              OR message.body LIKE '%' || :query || '%'
              OR (:matchAttachmentName AND attachment.name LIKE '%' || :query || '%')
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
        pendingStatus: MessageDeliveryStatus = MessageDeliveryStatus.Pending,
    ): List<AttachmentDb>

    @Transaction
    @Query(
        """
        SELECT attachment.*
        FROM $ATTACHMENT_TABLE AS attachment
        JOIN $MESSAGE_TABLE AS message ON message.tid = attachment.messageTid
        JOIN $CHANNEL_TABLE AS channel ON channel.chat_id = message.channelId
        LEFT JOIN $LINK_DETAILS_TABLE AS link_details ON link_details.link = attachment.url
        WHERE message.message_id IS NOT NULL
          AND message.deliveryStatus != :pendingStatus
          AND message.unList = 0
          AND attachment.viewOnce != 1
          AND attachment.type IN (:types)
          AND channel.userRole IS NOT NULL AND channel.userRole != ''
          AND (:senderIgnored OR message.fromId = :senderId)
          AND (
              :queryEmpty
              OR attachment.url LIKE '%' || :query || '%'
              OR link_details.title LIKE '%' || :query || '%'
              OR link_details.description LIKE '%' || :query || '%'
          )
        ORDER BY attachment.createdAt DESC, attachment.id DESC
        LIMIT :limit OFFSET :offset
        """
    )
    abstract suspend fun searchLinkAttachments(
        query: String,
        senderId: String?,
        types: List<String>,
        limit: Int,
        offset: Int,
        queryEmpty: Boolean,
        senderIgnored: Boolean,
        pendingStatus: MessageDeliveryStatus = MessageDeliveryStatus.Pending,
    ): List<AttachmentDb>

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
        /**
         * Word-prefix LIKE condition for a single query word.
         * Matches when the word starts the body, follows a space, or follows a newline (LF).
         * Binds the same word **three times** in order: start-of-body, after-space, after-newline.
         */
        private const val WORD_PREFIX_CONDITION =
            "(message.body LIKE ? || '%' OR message.body LIKE '% ' || ? || '%' OR message.body LIKE '%' || char(10) || ? || '%')"

        /**
         * Converts a [MessageDeliveryStatus] to the integer stored in the database.
         * Must stay in sync with
         * [com.sceyt.chatuikit.persistence.database.converters.MessageConverter.deliveryStatusToInt].
         */
        private fun MessageDeliveryStatus.toDbValue(): Int = ordinal
    }
}
