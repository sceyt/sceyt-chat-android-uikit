package com.sceyt.chatuikit.benchmarks

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import com.sceyt.chatuikit.persistence.database.SceytDatabase
import com.sceyt.chatuikit.persistence.database.dao.GlobalSearchDao
import kotlinx.coroutines.runBlocking
import java.io.File

class SearchMessagesBenchmarkBridge private constructor(
    private val context: Context,
    private val databaseName: String,
    private val database: SceytDatabase,
    private val globalSearchDao: GlobalSearchDao,
    private val writableDatabase: SupportSQLiteDatabase,
) : AutoCloseable {

    fun seedAndVerify() = runBlocking {
        seedDatabase(SearchMessagesBenchmarkFixture.DEFAULT_TOTAL_MESSAGES)
        assertFixture()
    }

    fun seed(
        totalMessages: Long,
    ) {
        seedDatabase(totalMessages)
    }

    fun search(
        query: String,
        senderId: String?,
    ): SearchMessagesBenchmarkSummary = runBlocking {
        val results = globalSearchDao.searchMessages(
            query = query,
            senderId = senderId,
            channelTypes = SearchMessagesBenchmarkFixture.privateChannelTypes,
            onlyJoined = true,
            limit = SearchMessagesBenchmarkFixture.QUERY_LIMIT,
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

    fun exportDatabase(
        targetDatabaseName: String,
    ) {
        checkpointDatabase()
        database.close()
        context.deleteDatabase(targetDatabaseName)
        copyDatabaseArtifact(targetDatabaseName, suffix = "")
        copyDatabaseArtifact(targetDatabaseName, suffix = "-wal")
        copyDatabaseArtifact(targetDatabaseName, suffix = "-shm")
    }

    private fun seedDatabase(
        totalMessages: Long,
    ) {
        SearchMessagesBenchmarkFixture.insertChannels(writableDatabase)
        SearchMessagesBenchmarkFixture.insertMessages(
            database = writableDatabase,
            totalMessages = totalMessages,
            chunkSize = SearchMessagesBenchmarkFixture.DEFAULT_CHUNK_SIZE,
            logTag = TAG,
        )

        val messageCount = SearchMessagesBenchmarkFixture.countMessages(writableDatabase)
        check(messageCount == totalMessages) {
            "Expected $totalMessages seeded messages, found $messageCount."
        }
    }

    private suspend fun assertFixture() {
        assertSearch(
            query = SearchMessagesBenchmarkFixture.SINGLE_WORD_QUERY,
            senderId = null,
        )
        assertSearch(
            query = SearchMessagesBenchmarkFixture.MULTI_WORD_QUERY,
            senderId = null,
        )
        assertSearch(
            query = SearchMessagesBenchmarkFixture.SINGLE_WORD_QUERY,
            senderId = SearchMessagesBenchmarkFixture.ALICE_ID,
        )
        assertSearch(
            query = "",
            senderId = SearchMessagesBenchmarkFixture.SELECTED_MEMBER_ID,
        )
    }

    private suspend fun assertSearch(
        query: String,
        senderId: String?,
    ) {
        val results = globalSearchDao.searchMessages(
            query = query,
            senderId = senderId,
            channelTypes = SearchMessagesBenchmarkFixture.privateChannelTypes,
            onlyJoined = true,
            limit = SearchMessagesBenchmarkFixture.QUERY_LIMIT,
            offset = 0,
        )

        check(results.size == SearchMessagesBenchmarkFixture.QUERY_LIMIT) {
            "Expected ${SearchMessagesBenchmarkFixture.QUERY_LIMIT} results for query='$query' sender='$senderId', got ${results.size}."
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
        check(results.all { it.messageEntity.channelId in SearchMessagesBenchmarkFixture.includedChannelIds }) {
            "Search fixture returned excluded channel types for query='$query' sender='$senderId'."
        }
    }

    private fun checkpointDatabase() {
        writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
            while (cursor.moveToNext()) {
                // Consume the checkpoint result rows so SQLite finalizes the operation.
            }
        }
    }

    private fun copyDatabaseArtifact(
        targetDatabaseName: String,
        suffix: String,
    ) {
        val source = databaseArtifact(databaseName, suffix)
        if (!source.exists()) return

        val target = databaseArtifact(targetDatabaseName, suffix)
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
    }

    private fun databaseArtifact(
        name: String,
        suffix: String,
    ): File {
        val databaseFile = context.getDatabasePath(name)
        return if (suffix.isEmpty()) {
            databaseFile
        } else {
            File(databaseFile.parentFile, "${databaseFile.name}$suffix")
        }
    }

    companion object {
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
