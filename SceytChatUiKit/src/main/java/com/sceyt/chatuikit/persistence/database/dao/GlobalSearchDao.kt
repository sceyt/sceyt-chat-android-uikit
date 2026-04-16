package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.ATTACHMENT_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHANNEL_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LINK_DETAILS_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_CHAT_LINK_TABLE
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelDb
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentDb
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageDb
import com.sceyt.chatuikit.shared.utils.GLOBAL_SEARCH_SQL_SEPARATOR_CHARS
import com.sceyt.chatuikit.shared.utils.tokenizeGlobalSearchQuery

@Dao
internal abstract class GlobalSearchDao {

    /**
     * Unified message search for both Chats and Channels tabs.
     *
     * - [channelTypes] empty → no channel type filter (all channels)
     * - [channelTypes] non-empty → `channel.type IN (:channelTypes)` filter
     * - [onlyJoined] true → requires `channel.userRole IS NOT NULL AND channel.userRole != ''`
     *
     * Word-prefix semantics: each query token must appear at the start of some token in the body.
     * Token boundaries recognised: beginning of string or any ASCII whitespace / punctuation
     * separator. Multi-token queries use AND logic and are order-independent.
     *
     * Limitations:
     * - SQLite LIKE is case-insensitive for ASCII only; Unicode queries may not fold correctly.
     * - The after-space conditions use a leading `%` wildcard and cannot be accelerated by a
     *   B-tree index. For production-scale search migrate to an FTS5 virtual table with the
     *   unicode61 tokenizer.
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
        val words = tokenizeGlobalSearchQuery(query)
        val senderIgnored = senderId.isNullOrBlank()
        val searchableBody = searchableColumnExpression("message.body")

        val senderClause = if (!senderIgnored) "AND message.fromId = ?" else ""
        val typeClause = if (channelTypes.isNotEmpty()) {
            "AND channel.type IN (${channelTypes.joinToString(",") { "?" }})"
        } else ""
        val joinedClause = if (onlyJoined) {
            "AND channel.userRole IS NOT NULL AND channel.userRole != ''"
        } else ""

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
              AND (${buildWordPrefixCondition(words, listOf(searchableBody))})
            ORDER BY message.createdAt DESC, message.message_id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val args = buildList {
            add(pendingStatus.toDbValue())
            if (!senderIgnored) add(senderId)
            addAll(channelTypes)
            addWordPrefixArgs(words, fieldCount = 1)
            add(limit)
            add(offset)
        }

        return searchMessagesRaw(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

    @Transaction
    @RawQuery
    protected abstract suspend fun searchMessagesRaw(query: SupportSQLiteQuery): List<MessageDb>

    @Transaction
    open suspend fun searchAttachments(
        query: String,
        senderId: String?,
        types: List<String>,
        limit: Int,
        offset: Int,
        queryEmpty: Boolean,
        senderIgnored: Boolean,
        matchAttachmentName: Boolean,
        pendingStatus: MessageDeliveryStatus = MessageDeliveryStatus.Pending,
    ): List<AttachmentDb> {
        if (types.isEmpty()) return emptyList()

        val words = if (queryEmpty) emptyList() else tokenizeGlobalSearchQuery(query)
        val isSenderIgnored = senderIgnored || senderId.isNullOrBlank()
        val searchableFields = buildList {
            add(searchableColumnExpression("message.body"))
            if (matchAttachmentName) add(searchableColumnExpression("attachment.name"))
        }

        val senderClause = if (!isSenderIgnored) "AND message.fromId = ?" else ""

        val sql = """
            SELECT attachment.*
            FROM $ATTACHMENT_TABLE AS attachment
            JOIN $MESSAGE_TABLE AS message ON message.tid = attachment.messageTid
            JOIN $CHANNEL_TABLE AS channel ON channel.chat_id = message.channelId
            WHERE message.message_id IS NOT NULL
              AND message.deliveryStatus != ?
              AND message.unList = 0
              AND attachment.viewOnce != 1
              AND attachment.type IN (${types.joinToString(",") { "?" }})
              AND channel.userRole IS NOT NULL AND channel.userRole != ''
              $senderClause
              AND (${buildWordPrefixCondition(words, searchableFields)})
            ORDER BY attachment.createdAt DESC, attachment.id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val args = buildList {
            add(pendingStatus.toDbValue())
            addAll(types)
            if (!isSenderIgnored) add(senderId)
            addWordPrefixArgs(words, searchableFields.size)
            add(limit)
            add(offset)
        }

        return searchAttachmentsRaw(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

    @Transaction
    @RawQuery
    protected abstract suspend fun searchAttachmentsRaw(query: SupportSQLiteQuery): List<AttachmentDb>

    @Transaction
    open suspend fun searchLinkAttachments(
        query: String,
        senderId: String?,
        types: List<String>,
        limit: Int,
        offset: Int,
        queryEmpty: Boolean,
        senderIgnored: Boolean,
        pendingStatus: MessageDeliveryStatus = MessageDeliveryStatus.Pending,
    ): List<AttachmentDb> {
        if (types.isEmpty()) return emptyList()

        val words = if (queryEmpty) emptyList() else tokenizeGlobalSearchQuery(query)
        val isSenderIgnored = senderIgnored || senderId.isNullOrBlank()
        val searchableFields = listOf(
            searchableColumnExpression("attachment.url"),
            searchableColumnExpression("link_details.title"),
            searchableColumnExpression("link_details.description"),
        )

        val senderClause = if (!isSenderIgnored) "AND message.fromId = ?" else ""

        val sql = """
            SELECT attachment.*
            FROM $ATTACHMENT_TABLE AS attachment
            JOIN $MESSAGE_TABLE AS message ON message.tid = attachment.messageTid
            JOIN $CHANNEL_TABLE AS channel ON channel.chat_id = message.channelId
            LEFT JOIN $LINK_DETAILS_TABLE AS link_details ON link_details.link = attachment.url
            WHERE message.message_id IS NOT NULL
              AND message.deliveryStatus != ?
              AND message.unList = 0
              AND attachment.viewOnce != 1
              AND attachment.type IN (${types.joinToString(",") { "?" }})
              AND channel.userRole IS NOT NULL AND channel.userRole != ''
              $senderClause
              AND (${buildWordPrefixCondition(words, searchableFields)})
            ORDER BY attachment.createdAt DESC, attachment.id DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val args = buildList {
            add(pendingStatus.toDbValue())
            addAll(types)
            if (!isSenderIgnored) add(senderId)
            addWordPrefixArgs(words, searchableFields.size)
            add(limit)
            add(offset)
        }

        return searchLinkAttachmentsRaw(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

    @Transaction
    @RawQuery
    protected abstract suspend fun searchLinkAttachmentsRaw(query: SupportSQLiteQuery): List<AttachmentDb>

    @Transaction
    open suspend fun searchChannelsByUserIds(
        query: String,
        userIds: List<String>,
        limit: Int,
        offset: Int,
        onlyMine: Boolean,
        types: List<String>,
        orderByLastMessage: Boolean,
        typesEmpty: Boolean = types.isEmpty(),
        directType: String = ChannelTypeEnum.Direct.value,
    ): List<ChannelDb> {
        val words = tokenizeGlobalSearchQuery(query)
        val ignoreTypes = typesEmpty || types.isEmpty()
        val typeClause = if (!ignoreTypes) {
            "AND channel.type IN (${types.joinToString(",") { "?" }})"
        } else ""
        val onlyMineClause = if (onlyMine) "AND channel.userRole <> ''" else ""

        val groupSubjectCondition = buildWordPrefixCondition(
            words = words,
            normalizedFields = listOf(searchableColumnExpression("channel.subject")),
        )
        val selfUserIdCondition = buildWordPrefixCondition(
            words = words,
            normalizedFields = listOf(searchableColumnExpression("link.user_id")),
        )

        val directMatchClauses = buildList {
            if (userIds.isNotEmpty()) {
                add("link.user_id IN (${userIds.joinToString(",") { "?" }})")
            }
            add("(channel.isSelf AND $selfUserIdCondition)")
        }

        val sql = """
            SELECT *
            FROM $CHANNEL_TABLE AS channel
            WHERE (
                (
                    channel.type <> ?
                    AND (NOT channel.pending OR channel.lastMessageTid != 0)
                    $onlyMineClause
                    AND ($groupSubjectCondition)
                )
                OR (
                    channel.type = ?
                    AND EXISTS (
                        SELECT 1
                        FROM $USER_CHAT_LINK_TABLE AS link
                        WHERE link.chat_id = channel.chat_id
                          AND (${directMatchClauses.joinToString(" OR ")})
                    )
                )
            )
            $typeClause
            ORDER BY
              CASE WHEN channel.pinnedAt > 0 THEN channel.pinnedAt END DESC,
              CASE WHEN $orderByLastMessage AND channel.lastMessageAt IS NOT NULL THEN channel.lastMessageAt END DESC,
              channel.createdAt DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val args = buildList {
            add(directType)
            addWordPrefixArgs(words, fieldCount = 1)
            add(directType)
            addAll(userIds)
            addWordPrefixArgs(words, fieldCount = 1)
            if (!ignoreTypes) addAll(types)
            add(limit)
            add(offset)
        }

        return searchChannelsByUserIdsRaw(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

    @Transaction
    @RawQuery
    protected abstract suspend fun searchChannelsByUserIdsRaw(
        query: SupportSQLiteQuery,
    ): List<ChannelDb>

    @Transaction
    open suspend fun searchChannelsBySubjectAndTypes(
        query: String,
        types: List<String>,
        limit: Int,
        offset: Int,
        queryEmpty: Boolean = query.isBlank(),
        typesEmpty: Boolean = types.isEmpty(),
    ): List<ChannelDb> {
        val words = if (queryEmpty) emptyList() else tokenizeGlobalSearchQuery(query)
        val ignoreTypes = typesEmpty || types.isEmpty()
        val typeClause = if (!ignoreTypes) {
            "AND type IN (${types.joinToString(",") { "?" }})"
        } else ""

        val sql = """
            SELECT *
            FROM $CHANNEL_TABLE
            WHERE (NOT pending OR lastMessageTid != 0)
              $typeClause
              AND (${buildWordPrefixCondition(words, listOf(searchableColumnExpression("subject")))})
            ORDER BY
              CASE WHEN pinnedAt > 0 THEN pinnedAt END DESC,
              CASE WHEN lastMessageAt IS NOT NULL THEN lastMessageAt END DESC,
              createdAt DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val args = buildList {
            if (!ignoreTypes) addAll(types)
            addWordPrefixArgs(words, fieldCount = 1)
            add(limit)
            add(offset)
        }

        return searchChannelsBySubjectAndTypesRaw(SimpleSQLiteQuery(sql, args.toTypedArray()))
    }

    @Transaction
    @RawQuery
    protected abstract suspend fun searchChannelsBySubjectAndTypesRaw(
        query: SupportSQLiteQuery,
    ): List<ChannelDb>

    private fun buildWordPrefixCondition(
        words: List<String>,
        normalizedFields: List<String>,
    ): String {
        if (words.isEmpty()) return "1"

        val anyFieldCondition = normalizedFields.joinToString(
            separator = " OR ",
            prefix = "(",
            postfix = ")",
        ) { fieldExpression ->
            buildSingleFieldWordPrefixCondition(fieldExpression)
        }

        return words.joinToString(" AND ") { anyFieldCondition }
    }

    private fun buildSingleFieldWordPrefixCondition(fieldExpression: String): String =
        buildList {
            add("$fieldExpression LIKE ? ESCAPE '\\'")
            GLOBAL_SEARCH_SQL_SEPARATOR_CHARS.forEach { _ ->
                add("$fieldExpression LIKE ? ESCAPE '\\'")
            }
        }.joinToString(separator = " OR ", prefix = "(", postfix = ")")

    private fun searchableColumnExpression(columnExpression: String): String =
        "coalesce($columnExpression, '')"

    private fun MutableList<Any>.addWordPrefixArgs(words: List<String>, fieldCount: Int) {
        words.forEach { word ->
            val escapedWord = escapeLikePatternLiteral(word)
            repeat(fieldCount) {
                add("$escapedWord%")
                GLOBAL_SEARCH_SQL_SEPARATOR_CHARS.forEach { separator ->
                    val escapedSeparator = escapeLikePatternLiteral(separator.toString())
                    add("%$escapedSeparator$escapedWord%")
                }
            }
        }
    }

    private fun escapeLikePatternLiteral(value: String): String =
        buildString(value.length) {
            value.forEach { char ->
                if (char == '\\' || char == '%' || char == '_') append('\\')
                append(char)
            }
        }

    private companion object {
        /**
         * Converts a [MessageDeliveryStatus] to the integer stored in the database.
         * Must stay in sync with
         * [com.sceyt.chatuikit.persistence.database.converters.MessageConverter.deliveryStatusToInt].
         */
        private fun MessageDeliveryStatus.toDbValue(): Int = ordinal
    }
}
