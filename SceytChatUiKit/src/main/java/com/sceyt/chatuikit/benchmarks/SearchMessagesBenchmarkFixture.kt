package com.sceyt.chatuikit.benchmarks

import android.util.Log
import androidx.annotation.RestrictTo
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object SearchMessagesBenchmarkFixture {
    const val QUERY_LIMIT = 15
    const val DEFAULT_TOTAL_MESSAGES = 1_000_000L
    const val DEFAULT_CHUNK_SIZE = 10_000

    const val ALICE_ID = "alice"
    const val SELECTED_MEMBER_ID = "selected-member"
    const val SINGLE_WORD_QUERY = "release"
    const val MULTI_WORD_QUERY = "release notes"

    val privateChannelTypes: List<String> = listOf(
        ChannelTypeEnum.Direct.value,
        ChannelTypeEnum.Group.value,
    )

    val includedChannelIds: Set<Long> = setOf(
        DIRECT_JOINED_CHANNEL_ID,
        GROUP_JOINED_CHANNEL_ID,
    )

    fun insertChannels(
        database: SupportSQLiteDatabase,
    ) {
        val statement = database.compileStatement(
            """
            INSERT OR REPLACE INTO sceyt_channel_table (
                chat_id,
                parentChannelId,
                uri,
                type,
                subject,
                avatarUrl,
                metadata,
                createdAt,
                updatedAt,
                messagesClearedAt,
                memberCount,
                createdById,
                userRole,
                unread,
                newMessageCount,
                newMentionCount,
                newReactedMessageCount,
                hidden,
                archived,
                muted,
                mutedTill,
                pinnedAt,
                lastReceivedMessageId,
                lastDisplayedMessageId,
                messageRetentionPeriod,
                lastMessageTid,
                lastMessageAt,
                pending,
                isSelf
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        )

        channelSpecs().forEach { spec ->
            statement.bindLong(1, spec.id)
            statement.bindNull(2)
            statement.bindString(3, "benchmark-channel-${spec.id}")
            statement.bindString(4, spec.type)
            statement.bindString(5, "Benchmark Channel ${spec.id}")
            statement.bindNull(6)
            statement.bindNull(7)
            statement.bindLong(8, spec.id)
            statement.bindLong(9, spec.id)
            statement.bindLong(10, 0L)
            statement.bindLong(11, 2L)
            statement.bindString(12, "system")
            statement.bindNullableString(13, spec.userRole)
            statement.bindLong(14, 0L)
            statement.bindLong(15, 0L)
            statement.bindLong(16, 0L)
            statement.bindLong(17, 0L)
            statement.bindLong(18, 0L)
            statement.bindLong(19, 0L)
            statement.bindLong(20, 0L)
            statement.bindNull(21)
            statement.bindNull(22)
            statement.bindLong(23, 0L)
            statement.bindLong(24, 0L)
            statement.bindLong(25, 0L)
            statement.bindNull(26)
            statement.bindNull(27)
            statement.bindLong(28, 0L)
            statement.bindLong(29, 0L)
            statement.executeInsert()
            statement.clearBindings()
        }
    }

    fun insertMessages(
        database: SupportSQLiteDatabase,
        totalMessages: Long = DEFAULT_TOTAL_MESSAGES,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        logTag: String? = null,
    ) {
        insertMessageRange(
            database = database,
            startTid = 1L,
            count = totalMessages,
            totalMessageWindow = totalMessages,
            chunkSize = chunkSize,
            logTag = logTag,
        )
    }

    fun insertMessageRange(
        database: SupportSQLiteDatabase,
        startTid: Long,
        count: Long,
        totalMessageWindow: Long,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        logTag: String? = null,
    ) {
        require(startTid > 0L) { "startTid must be positive." }
        require(count >= 0L) { "count must not be negative." }
        require(totalMessageWindow >= startTid + count - 1L) {
            "totalMessageWindow must cover the inserted range."
        }
        if (count == 0L) return

        val statement = database.compileStatement(
            """
            INSERT INTO sceyt_message_table (
                tid,
                message_id,
                channelId,
                body,
                type,
                metadata,
                createdAt,
                updatedAt,
                incoming,
                isTransient,
                silent,
                viewOnce,
                deliveryStatus,
                state,
                fromId,
                markerCount,
                mentionedUsersIds,
                parentId,
                replyCount,
                displayCount,
                autoDeleteAt,
                bodyAttribute,
                disableMentionsCount,
                unList,
                messageId,
                userId,
                hops
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        )

        var inserted = 0L
        while (inserted < count) {
            database.beginTransaction()
            try {
                val chunkCount = minOf(inserted + chunkSize, count)
                val chunkEndTid = startTid + chunkCount - 1L

                for (tid in startTid + inserted..chunkEndTid) {
                    bindMessage(statement, tid, messageSpecFor(tid, totalMessageWindow))
                    statement.executeInsert()
                    statement.clearBindings()
                }

                database.setTransactionSuccessful()
                inserted = chunkCount
                if (logTag != null && (inserted % 100_000L == 0L || inserted == count)) {
                    val currentTid = startTid + inserted - 1L
                    Log.i(logTag, "Seeded $inserted / $count benchmark messages up to tid=$currentTid")
                }
            } finally {
                database.endTransaction()
            }
        }
    }

    fun countMessages(
        database: SupportSQLiteDatabase,
    ): Long = database.countRows(MESSAGE_TABLE)

    private fun bindMessage(
        statement: SupportSQLiteStatement,
        tid: Long,
        spec: BenchmarkMessageSpec,
    ) {
        statement.bindLong(1, tid)
        statement.bindNullableLong(2, spec.messageId)
        statement.bindLong(3, spec.channelId)
        statement.bindString(4, spec.body)
        statement.bindString(5, "text")
        statement.bindNull(6)
        statement.bindLong(7, tid)
        statement.bindLong(8, tid)
        statement.bindLong(9, 0L)
        statement.bindLong(10, 0L)
        statement.bindLong(11, 0L)
        statement.bindLong(12, 0L)
        statement.bindLong(13, spec.deliveryStatus.ordinal.toLong())
        statement.bindLong(14, MessageState.Unmodified.ordinal.toLong())
        statement.bindNullableString(15, spec.fromId)
        statement.bindNull(16)
        statement.bindNull(17)
        statement.bindNull(18)
        statement.bindLong(19, 0L)
        statement.bindLong(20, 0L)
        statement.bindNull(21)
        statement.bindNull(22)
        statement.bindLong(23, 0L)
        statement.bindLong(24, if (spec.unList) 1L else 0L)
        statement.bindNull(25)
        statement.bindNull(26)
        statement.bindNull(27)
    }

    private fun channelSpecs(): List<ChannelSpec> = listOf(
        ChannelSpec(
            id = DIRECT_JOINED_CHANNEL_ID,
            type = ChannelTypeEnum.Direct.value,
            userRole = "member",
        ),
        ChannelSpec(
            id = GROUP_JOINED_CHANNEL_ID,
            type = ChannelTypeEnum.Group.value,
            userRole = "owner",
        ),
        ChannelSpec(
            id = DIRECT_UNJOINED_CHANNEL_ID,
            type = ChannelTypeEnum.Direct.value,
            userRole = "",
        ),
        ChannelSpec(
            id = PUBLIC_JOINED_CHANNEL_ID,
            type = ChannelTypeEnum.Public.value,
            userRole = "member",
        ),
    )

    private fun messageSpecFor(
        tid: Long,
        totalMessages: Long,
    ): BenchmarkMessageSpec {
        if (tid > totalMessages - RECENT_WINDOW_SIZE) {
            return recentMessageSpecFor(tid, totalMessages)
        }

        return BenchmarkMessageSpec(
            messageId = if (tid % 16_667L == 0L) null else tid,
            channelId = when {
                tid % 15L == 0L -> PUBLIC_JOINED_CHANNEL_ID
                tid % 10L == 0L -> DIRECT_UNJOINED_CHANNEL_ID
                tid % 2L == 0L -> DIRECT_JOINED_CHANNEL_ID
                else -> GROUP_JOINED_CHANNEL_ID
            },
            body = "bulk filler message $tid",
            fromId = when {
                tid % 7L == 0L -> SELECTED_MEMBER_ID
                tid % 5L == 0L -> ALICE_ID
                else -> BOB_ID
            },
            deliveryStatus = if (tid % 12_500L == 0L) {
                MessageDeliveryStatus.Pending
            } else {
                MessageDeliveryStatus.Displayed
            },
            unList = tid % 10_001L == 0L,
        )
    }

    private fun recentMessageSpecFor(
        tid: Long,
        totalMessages: Long,
    ): BenchmarkMessageSpec {
        return when (((totalMessages - tid) % 6).toInt()) {
            0 -> BenchmarkMessageSpec(
                messageId = tid,
                channelId = DIRECT_JOINED_CHANNEL_ID,
                body = "release benchmark window $tid",
                fromId = ALICE_ID,
            )

            1 -> BenchmarkMessageSpec(
                messageId = tid,
                channelId = GROUP_JOINED_CHANNEL_ID,
                body = "release notes benchmark window $tid",
                fromId = BOB_ID,
            )

            2 -> BenchmarkMessageSpec(
                messageId = tid,
                channelId = DIRECT_JOINED_CHANNEL_ID,
                body = "member timeline benchmark $tid",
                fromId = SELECTED_MEMBER_ID,
            )

            3 -> BenchmarkMessageSpec(
                messageId = tid,
                channelId = GROUP_JOINED_CHANNEL_ID,
                body = "alpha beta benchmark window $tid",
                fromId = ALICE_ID,
            )

            4 -> when ((tid % 3).toInt()) {
                0 -> BenchmarkMessageSpec(
                    messageId = tid,
                    channelId = DIRECT_JOINED_CHANNEL_ID,
                    body = "release notes hidden excluded $tid",
                    fromId = ALICE_ID,
                    unList = true,
                )

                1 -> BenchmarkMessageSpec(
                    messageId = tid,
                    channelId = GROUP_JOINED_CHANNEL_ID,
                    body = "release notes pending excluded $tid",
                    fromId = ALICE_ID,
                    deliveryStatus = MessageDeliveryStatus.Pending,
                )

                else -> BenchmarkMessageSpec(
                    messageId = null,
                    channelId = DIRECT_JOINED_CHANNEL_ID,
                    body = "release notes local excluded $tid",
                    fromId = ALICE_ID,
                )
            }

            else -> BenchmarkMessageSpec(
                messageId = tid,
                channelId = if (tid % 2L == 0L) {
                    DIRECT_UNJOINED_CHANNEL_ID
                } else {
                    PUBLIC_JOINED_CHANNEL_ID
                },
                body = "release notes channel excluded $tid",
                fromId = ALICE_ID,
            )
        }
    }
}

private const val MESSAGE_TABLE = "sceyt_message_table"
private const val RECENT_WINDOW_SIZE = 240L

private const val DIRECT_JOINED_CHANNEL_ID = 1L
private const val GROUP_JOINED_CHANNEL_ID = 2L
private const val DIRECT_UNJOINED_CHANNEL_ID = 3L
private const val PUBLIC_JOINED_CHANNEL_ID = 4L

private const val BOB_ID = "bob"

private data class ChannelSpec(
    val id: Long,
    val type: String,
    val userRole: String?,
)

private data class BenchmarkMessageSpec(
    val messageId: Long?,
    val channelId: Long,
    val body: String,
    val fromId: String?,
    val deliveryStatus: MessageDeliveryStatus = MessageDeliveryStatus.Displayed,
    val unList: Boolean = false,
)

private fun SupportSQLiteStatement.bindNullableLong(
    index: Int,
    value: Long?,
) {
    if (value == null) bindNull(index) else bindLong(index, value)
}

private fun SupportSQLiteStatement.bindNullableString(
    index: Int,
    value: String?,
) {
    if (value == null) bindNull(index) else bindString(index, value)
}

private fun SupportSQLiteDatabase.countRows(tableName: String): Long {
    query("SELECT COUNT(*) FROM $tableName").use { cursor ->
        check(cursor.moveToFirst()) { "Failed to read seeded row count from $tableName." }
        return cursor.getLong(0)
    }
}
