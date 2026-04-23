package com.sceyt.chatuikit.benchmarks

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.channels.ChannelTypeEnum
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.GlobalSearchDao
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import kotlinx.coroutines.runBlocking

class SearchMessagesBenchmarkBridge private constructor(
    private val context: Context,
    private val databaseName: String,
    private val database: SceytDatabase,
    private val globalSearchDao: GlobalSearchDao,
    private val writableDatabase: SupportSQLiteDatabase,
) : AutoCloseable {

    fun seedAndVerify() = runBlocking {
        seedDatabase()
        assertFixture()
    }

    fun search(
        query: String,
        senderId: String?,
    ): SearchMessagesBenchmarkSummary = runBlocking {
        val results = globalSearchDao.searchMessages(
            query = query,
            senderId = senderId,
            channelTypes = PRIVATE_CHANNEL_TYPES,
            onlyJoined = true,
            limit = QUERY_LIMIT,
            offset = 0,
        )

        SearchMessagesBenchmarkSummary(
            resultSize = results.size,
            firstMessageId = results.firstOrNull()?.messageEntity?.id ?: -1L,
        )
    }

    override fun close() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private suspend fun seedDatabase() {
        val channels = listOf(
            channel(
                id = DIRECT_JOINED_CHANNEL_ID,
                type = ChannelTypeEnum.Direct.value,
                userRole = "member",
            ),
            channel(
                id = GROUP_JOINED_CHANNEL_ID,
                type = ChannelTypeEnum.Group.value,
                userRole = "owner",
            ),
            channel(
                id = DIRECT_UNJOINED_CHANNEL_ID,
                type = ChannelTypeEnum.Direct.value,
                userRole = "",
            ),
            channel(
                id = PUBLIC_JOINED_CHANNEL_ID,
                type = ChannelTypeEnum.Public.value,
                userRole = "member",
            ),
        )
        database.channelDao().insertChannelsAndLinks(channels, emptyList())
        insertMessages()

        val messageCount = writableDatabase.countRows("sceyt_message_table")
        check(messageCount == TOTAL_MESSAGES.toLong()) {
            "Expected $TOTAL_MESSAGES seeded messages, found $messageCount."
        }
    }

    private suspend fun assertFixture() {
        assertSearch(
            query = SINGLE_WORD_QUERY,
            senderId = null,
        )
        assertSearch(
            query = MULTI_WORD_QUERY,
            senderId = null,
        )
        assertSearch(
            query = SINGLE_WORD_QUERY,
            senderId = ALICE_ID,
        )
        assertSearch(
            query = "",
            senderId = SELECTED_MEMBER_ID,
        )
    }

    private suspend fun assertSearch(
        query: String,
        senderId: String?,
    ) {
        val results = globalSearchDao.searchMessages(
            query = query,
            senderId = senderId,
            channelTypes = PRIVATE_CHANNEL_TYPES,
            onlyJoined = true,
            limit = QUERY_LIMIT,
            offset = 0,
        )

        check(results.size == QUERY_LIMIT) {
            "Expected $QUERY_LIMIT results for query='$query' sender='$senderId', got ${results.size}."
        }
        check(results.all { it.messageEntity.id != null }) {
            "Search fixture returned local-only messages for query='$query' sender='$senderId'."
        }
        check(results.none { it.messageEntity.deliveryStatus == MessageDeliveryStatus.Pending }) {
            "Search fixture returned pending messages for query='$query' sender='$senderId'."
        }
        check(results.none { it.messageEntity.unList }) {
            "Search fixture returned unlisted messages for query='$query' sender='$senderId'."
        }
        check(results.all { it.messageEntity.channelId in INCLUDED_CHANNEL_IDS }) {
            "Search fixture returned excluded channel types for query='$query' sender='$senderId'."
        }
    }

    private fun insertMessages() {
        val statement = writableDatabase.compileStatement(
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
        while (inserted < TOTAL_MESSAGES) {
            writableDatabase.beginTransaction()
            try {
                val upperBound = minOf(
                    inserted + CHUNK_SIZE,
                    TOTAL_MESSAGES.toLong(),
                )

                for (tid in inserted + 1..upperBound) {
                    bindMessage(statement, tid, messageSpecFor(tid))
                    statement.executeInsert()
                    statement.clearBindings()
                }

                writableDatabase.setTransactionSuccessful()
                inserted = upperBound
                if (inserted % 100_000L == 0L || inserted == TOTAL_MESSAGES.toLong()) {
                    Log.i(TAG, "Seeded $inserted / $TOTAL_MESSAGES messages")
                }
            } finally {
                writableDatabase.endTransaction()
            }
        }
    }

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

    companion object {
        const val QUERY_LIMIT = 15

        fun create(context: Context): SearchMessagesBenchmarkBridge {
            val appContext = context.applicationContext
            appContext.deleteDatabase(DATABASE_NAME)

            val database = Room.databaseBuilder(
                appContext,
                SceytDatabase::class.java,
                DATABASE_NAME,
            )
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration(false)
                .build()

            return SearchMessagesBenchmarkBridge(
                context = appContext,
                databaseName = DATABASE_NAME,
                database = database,
                globalSearchDao = database.globalSearchDao(),
                writableDatabase = database.openHelper.writableDatabase,
            )
        }
    }
}

data class SearchMessagesBenchmarkSummary(
    val resultSize: Int,
    val firstMessageId: Long,
)

private const val TAG = "SearchMessagesBenchmark"
private const val DATABASE_NAME = "search_messages_benchmark.db"
private const val TOTAL_MESSAGES = 1_000_000
private const val CHUNK_SIZE = 10_000
private const val RECENT_WINDOW_SIZE = 240L

private const val DIRECT_JOINED_CHANNEL_ID = 1L
private const val GROUP_JOINED_CHANNEL_ID = 2L
private const val DIRECT_UNJOINED_CHANNEL_ID = 3L
private const val PUBLIC_JOINED_CHANNEL_ID = 4L

private const val ALICE_ID = "alice"
private const val BOB_ID = "bob"
private const val SELECTED_MEMBER_ID = "selected-member"

private const val SINGLE_WORD_QUERY = "release"
private const val MULTI_WORD_QUERY = "release notes"

private val PRIVATE_CHANNEL_TYPES = listOf(
    ChannelTypeEnum.Direct.value,
    ChannelTypeEnum.Group.value,
)

private val INCLUDED_CHANNEL_IDS = setOf(
    DIRECT_JOINED_CHANNEL_ID,
    GROUP_JOINED_CHANNEL_ID,
)

private fun channel(
    id: Long,
    type: String,
    userRole: String?,
) = ChannelEntity(
    id = id,
    parentChannelId = null,
    uri = "benchmark-channel-$id",
    type = type,
    subject = "Benchmark Channel $id",
    avatarUrl = null,
    metadata = null,
    createdAt = id,
    updatedAt = id,
    messagesClearedAt = 0L,
    memberCount = 2L,
    createdById = "system",
    userRole = userRole,
    unread = false,
    newMessageCount = 0L,
    newMentionCount = 0L,
    newReactedMessageCount = 0L,
    hidden = false,
    archived = false,
    muted = false,
    mutedTill = null,
    pinnedAt = null,
    lastReceivedMessageId = 0L,
    lastDisplayedMessageId = 0L,
    messageRetentionPeriod = 0L,
    lastMessageTid = null,
    lastMessageAt = null,
    pending = false,
    isSelf = false,
)

private fun messageSpecFor(tid: Long): BenchmarkMessageSpec {
    if (tid > TOTAL_MESSAGES - RECENT_WINDOW_SIZE) {
        return recentMessageSpecFor(tid)
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

private fun recentMessageSpecFor(tid: Long): BenchmarkMessageSpec {
    return when (((TOTAL_MESSAGES - tid) % 6).toInt()) {
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
