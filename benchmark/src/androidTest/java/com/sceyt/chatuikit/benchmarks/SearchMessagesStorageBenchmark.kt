package com.sceyt.chatuikit.benchmarks

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class SearchMessagesStorageBenchmark {

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        DATABASE_NAMES.forEach(context::deleteDatabase)
    }

    @Test
    fun measureDatabaseFootprint_for100kMessages() {
        measureAndAssert(sampleMessages = 100_000L)
    }

    @Test
    fun measureDatabaseFootprint_for500kMessages() {
        measureAndAssert(sampleMessages = 500_000L)
    }

    @Test
    fun measureDatabaseFootprint_for1MMessages() {
        measureAndAssert(sampleMessages = 1_000_000L)
    }

    private fun measureAndAssert(
        sampleMessages: Long,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val report = SearchMessagesStorageBenchmarkHarness(
            context = context,
            sampleMessages = sampleMessages,
        ).measure()

        android.util.Log.i(TAG, report.render())

        assertTrue(
            "The after-FTS database should not be smaller than the current database.",
            report.afterFts.usedBytes >= report.current.usedBytes,
        )
    }
}

private class SearchMessagesStorageBenchmarkHarness(
    private val context: Context,
    private val sampleMessages: Long,
) {
    fun measure(): StorageBenchmarkReport {
        prepareMasterDatabase()

        copyDatabaseArtifacts(MASTER_DB_NAME, AFTER_FTS_DB_NAME)
        copyDatabaseArtifacts(MASTER_DB_NAME, CURRENT_DB_NAME)

        val afterFts = measureVariant(AFTER_FTS_DB_NAME)
        val current = measureVariant(CURRENT_DB_NAME) { database ->
            dropFtsArtifacts(database)
            database.execSQL("DROP INDEX IF EXISTS $MESSAGE_SCOPE_INDEX")
        }

        return StorageBenchmarkReport(
            sampleMessages = sampleMessages,
            current = current,
            afterFts = afterFts,
        )
    }

    private fun prepareMasterDatabase() {
        context.deleteDatabase(MASTER_DB_NAME)
        val bridge = SearchMessagesBenchmarkBridge.create(context)
        try {
            bridge.seed(sampleMessages)
            bridge.exportDatabase(MASTER_DB_NAME)
        } finally {
            bridge.close()
        }
    }

    private fun measureVariant(
        databaseName: String,
        transform: ((SQLiteDatabase) -> Unit)? = null,
    ): StorageVariantMetrics {
        val databaseFile = context.getDatabasePath(databaseName)
        SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        ).use { database ->
            transform?.invoke(database)
            checkpointAndVacuum(database)

            val pageSize = queryLong(database, "PRAGMA page_size")
            val pageCount = queryLong(database, "PRAGMA page_count")
            val freelistCount = queryLong(database, "PRAGMA freelist_count")

            return StorageVariantMetrics(
                usedBytes = (pageCount - freelistCount) * pageSize,
            )
        }
    }

    private fun copyDatabaseArtifacts(
        sourceDatabaseName: String,
        targetDatabaseName: String,
    ) {
        context.deleteDatabase(targetDatabaseName)
        copyArtifact(sourceDatabaseName, targetDatabaseName, "")
        copyArtifact(sourceDatabaseName, targetDatabaseName, "-wal")
        copyArtifact(sourceDatabaseName, targetDatabaseName, "-shm")
    }

    private fun copyArtifact(
        sourceDatabaseName: String,
        targetDatabaseName: String,
        suffix: String,
    ) {
        val source = siblingFile(sourceDatabaseName, suffix)
        val target = siblingFile(targetDatabaseName, suffix)
        if (!source.exists()) return

        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
    }

    private fun siblingFile(
        databaseName: String,
        suffix: String,
    ): File {
        val databaseFile = context.getDatabasePath(databaseName)
        return if (suffix.isEmpty()) {
            databaseFile
        } else {
            File(databaseFile.parentFile, "${databaseFile.name}$suffix")
        }
    }

    private fun checkpointAndVacuum(
        database: SQLiteDatabase,
    ) {
        consume(database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null))
        database.execSQL("VACUUM")
        consume(database.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null))
    }

    private fun dropFtsArtifacts(
        database: SQLiteDatabase,
    ) {
        FTS_TRIGGER_NAMES.forEach { triggerName ->
            database.execSQL("DROP TRIGGER IF EXISTS $triggerName")
        }
        database.execSQL("DROP TABLE IF EXISTS $MESSAGE_FTS_TABLE")
    }

    private fun queryLong(
        database: SQLiteDatabase,
        sql: String,
    ): Long = database.rawQuery(sql, null).use { cursor ->
        check(cursor.moveToFirst()) { "Expected a row for query: $sql" }
        cursor.getLong(0)
    }

    private fun consume(cursor: Cursor) {
        cursor.use {
            while (it.moveToNext()) {
                // Consume PRAGMA result rows so the command fully completes.
            }
        }
    }
}

private data class StorageBenchmarkReport(
    val sampleMessages: Long,
    val current: StorageVariantMetrics,
    val afterFts: StorageVariantMetrics,
) {
    fun render(): String = buildString {
        appendLine("Search storage benchmark")
        appendLine("Sample size: $sampleMessages synthetic messages")
        appendLine("current database = old schema without the new composite index or message_fts")
        appendLine("after fts = current database plus message_fts, its triggers, and the new composite index")
        appendLine()
        appendLine(renderCurrent(current))
        appendLine(renderAfterFts(afterFts, current))
    }

    private fun renderCurrent(
        metrics: StorageVariantMetrics,
    ): String = buildString {
        appendLine("current_database:")
        appendLine("  size=${metrics.usedBytes.formatBytes()}")
    }

    private fun renderAfterFts(
        metrics: StorageVariantMetrics,
        current: StorageVariantMetrics,
    ): String = buildString {
        appendLine("after_fts:")
        appendLine("  size=${metrics.usedBytes.formatBytes()}")
        appendLine("  delta_vs_current=${(metrics.usedBytes - current.usedBytes).formatBytes()}")
        appendLine("  growth_vs_current=${metrics.usedBytes.growthPercentFrom(current.usedBytes)}%")
    }
}

private fun Long.growthPercentFrom(
    baseline: Long,
): String {
    if (baseline == 0L) return "n/a"
    val growth = ((this - baseline).toDouble() / baseline.toDouble()) * 100.0
    return "%.2f".format(growth)
}

private fun Long.formatBytes(): String = when {
    this >= 1_048_576L -> "%.2f MiB".format(this / 1_048_576.0)
    this >= 1_024L -> "%.2f KiB".format(this / 1_024.0)
    else -> "$this B"
}

private data class StorageVariantMetrics(
    val usedBytes: Long,
)

private const val TAG = "SearchStorageBenchmark"
private const val MASTER_DB_NAME = "search_storage_master.db"
private const val AFTER_FTS_DB_NAME = "search_storage_after_fts.db"
private const val CURRENT_DB_NAME = "search_storage_current.db"

private const val MESSAGE_FTS_TABLE = "message_fts"
private const val MESSAGE_SCOPE_INDEX = "index_sceyt_message_table_channelId_createdAt_message_id"

private val DATABASE_NAMES = listOf(
    MASTER_DB_NAME,
    AFTER_FTS_DB_NAME,
    CURRENT_DB_NAME,
)

private val FTS_TRIGGER_NAMES = listOf(
    "room_fts_content_sync_message_fts_BEFORE_UPDATE",
    "room_fts_content_sync_message_fts_BEFORE_DELETE",
    "room_fts_content_sync_message_fts_AFTER_UPDATE",
    "room_fts_content_sync_message_fts_AFTER_INSERT",
)
