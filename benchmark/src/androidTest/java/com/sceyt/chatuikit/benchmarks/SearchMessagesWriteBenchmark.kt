package com.sceyt.chatuikit.benchmarks

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.io.File
import kotlin.system.measureNanoTime

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SearchMessagesWriteBenchmark {

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        WRITE_DATABASE_NAMES.forEach(context::deleteDatabase)
    }

    @Test
    fun measureFtsWriteOverhead_01_for100Messages() {
        measureAndAssert(batchSize = 100L)
    }

    @Test
    fun measureFtsWriteOverhead_02_for1KMessages() {
        measureAndAssert(batchSize = 1_000L)
    }

    @Test
    fun measureFtsWriteOverhead_03_for10KMessages() {
        measureAndAssert(batchSize = 10_000L)
    }

    @Test
    fun measureFtsWriteOverhead_04_for100KMessages() {
        measureAndAssert(batchSize = 100_000L)
    }

    private fun measureAndAssert(
        batchSize: Long,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val report = SearchMessagesWriteBenchmarkHarness(
            context = context,
            batchSize = batchSize,
        ).measure()

        android.util.Log.i(WRITE_TAG, report.render())

        assertTrue(report.insert.current.rowsAffected == batchSize)
        assertTrue(report.insert.afterFts.rowsAffected == batchSize)
        assertTrue(report.update.current.rowsAffected == batchSize)
        assertTrue(report.update.afterFts.rowsAffected == batchSize)
        assertTrue(report.delete.current.rowsAffected == batchSize)
        assertTrue(report.delete.afterFts.rowsAffected == batchSize)
    }
}

private class SearchMessagesWriteBenchmarkHarness(
    private val context: Context,
    private val batchSize: Long,
) {
    fun measure(): WriteBenchmarkReport {
        prepareMasterDatabase()

        return WriteBenchmarkReport(
            existingMessages = batchSize,
            batchSize = batchSize,
            insert = measureInsertOperation(),
            update = measureUpdateOperation(),
            delete = measureDeleteOperation(),
        )
    }

    private fun prepareMasterDatabase() {
        context.deleteDatabase(WRITE_MASTER_DB_NAME)
        val bridge = SearchMessagesBenchmarkBridge.create(context)
        bridge.use { bridge ->
            bridge.seed(batchSize)
            bridge.exportDatabase(WRITE_MASTER_DB_NAME)
        }
    }

    private fun measureInsertOperation(): OperationBenchmarkReport {
        val current = measureVariant(
            operation = WriteOperation.Insert,
            variant = WriteVariant.Current,
        ) { database ->
            val startingTid = queryLong(
                database,
                "SELECT COALESCE(MAX(tid), 0) FROM $WRITE_MESSAGE_TABLE",
            ) + 1L
            val startingCount = queryLong(
                database,
                "SELECT COUNT(*) FROM $WRITE_MESSAGE_TABLE",
            )
            insertWriteMessages(
                database = database,
                startTid = startingTid,
                count = batchSize,
            )
            val insertedRows = queryLong(database, "SELECT COUNT(*) FROM $WRITE_MESSAGE_TABLE") - startingCount
            check(insertedRows == batchSize) {
                "Expected to insert $batchSize messages, inserted $insertedRows."
            }
        }

        val afterFts = measureVariant(
            operation = WriteOperation.Insert,
            variant = WriteVariant.AfterFts,
        ) { database ->
            val startingTid = queryLong(
                database,
                "SELECT COALESCE(MAX(tid), 0) FROM $WRITE_MESSAGE_TABLE",
            ) + 1L
            val startingCount = queryLong(
                database,
                "SELECT COUNT(*) FROM $WRITE_MESSAGE_TABLE",
            )
            insertWriteMessages(
                database = database,
                startTid = startingTid,
                count = batchSize,
            )
            val insertedRows = queryLong(database, "SELECT COUNT(*) FROM $WRITE_MESSAGE_TABLE") - startingCount
            check(insertedRows == batchSize) {
                "Expected to insert $batchSize messages, inserted $insertedRows."
            }
        }

        return OperationBenchmarkReport(
            operation = "Insert",
            current = current,
            afterFts = afterFts,
        )
    }

    private fun measureUpdateOperation(): OperationBenchmarkReport {
        val current = measureVariant(
            operation = WriteOperation.Update,
            variant = WriteVariant.Current,
        ) { database ->
            val statement = database.compileStatement(
                """
                UPDATE $WRITE_MESSAGE_TABLE
                SET body = ?, updatedAt = ?
                WHERE tid = ?
                """.trimIndent()
            )

            database.beginTransaction()
            try {
                var updatedRows = 0L
                for (tid in 1L..batchSize) {
                    statement.bindString(1, "updated write benchmark body $tid")
                    statement.bindLong(2, batchSize + tid)
                    statement.bindLong(3, tid)
                    updatedRows += statement.executeUpdateDelete().toLong()
                    statement.clearBindings()
                }
                database.setTransactionSuccessful()
                check(updatedRows == batchSize) {
                    "Expected to update $batchSize messages, updated $updatedRows."
                }
            } finally {
                database.endTransaction()
            }
        }

        val afterFts = measureVariant(
            operation = WriteOperation.Update,
            variant = WriteVariant.AfterFts,
        ) { database ->
            val statement = database.compileStatement(
                """
                UPDATE $WRITE_MESSAGE_TABLE
                SET body = ?, updatedAt = ?
                WHERE tid = ?
                """.trimIndent()
            )

            database.beginTransaction()
            try {
                var updatedRows = 0L
                for (tid in 1L..batchSize) {
                    statement.bindString(1, "updated write benchmark body $tid")
                    statement.bindLong(2, batchSize + tid)
                    statement.bindLong(3, tid)
                    updatedRows += statement.executeUpdateDelete().toLong()
                    statement.clearBindings()
                }
                database.setTransactionSuccessful()
                check(updatedRows == batchSize) {
                    "Expected to update $batchSize messages, updated $updatedRows."
                }
            } finally {
                database.endTransaction()
            }
        }

        return OperationBenchmarkReport(
            operation = "Update",
            current = current,
            afterFts = afterFts,
        )
    }

    private fun measureDeleteOperation(): OperationBenchmarkReport {
        val current = measureVariant(
            operation = WriteOperation.Delete,
            variant = WriteVariant.Current,
        ) { database ->
            val statement = database.compileStatement(
                "DELETE FROM $WRITE_MESSAGE_TABLE WHERE tid = ?"
            )

            database.beginTransaction()
            try {
                var deletedRows = 0L
                for (tid in 1L..batchSize) {
                    statement.bindLong(1, tid)
                    deletedRows += statement.executeUpdateDelete().toLong()
                    statement.clearBindings()
                }
                database.setTransactionSuccessful()
                check(deletedRows == batchSize) {
                    "Expected to delete $batchSize messages, deleted $deletedRows."
                }
            } finally {
                database.endTransaction()
            }
        }

        val afterFts = measureVariant(
            operation = WriteOperation.Delete,
            variant = WriteVariant.AfterFts,
        ) { database ->
            val statement = database.compileStatement(
                "DELETE FROM $WRITE_MESSAGE_TABLE WHERE tid = ?"
            )

            database.beginTransaction()
            try {
                var deletedRows = 0L
                for (tid in 1L..batchSize) {
                    statement.bindLong(1, tid)
                    deletedRows += statement.executeUpdateDelete().toLong()
                    statement.clearBindings()
                }
                database.setTransactionSuccessful()
                check(deletedRows == batchSize) {
                    "Expected to delete $batchSize messages, deleted $deletedRows."
                }
            } finally {
                database.endTransaction()
            }
        }

        return OperationBenchmarkReport(
            operation = "Delete",
            current = current,
            afterFts = afterFts,
        )
    }

    private fun measureVariant(
        operation: WriteOperation,
        variant: WriteVariant,
        action: (SQLiteDatabase) -> Unit,
    ): VariantWriteMetrics {
        val databaseName = writeDatabaseName(operation, variant)
        copyDatabaseArtifacts(WRITE_MASTER_DB_NAME, databaseName)

        val databaseFile = context.getDatabasePath(databaseName)
        SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READWRITE,
        ).use { database ->
            if (variant == WriteVariant.Current) {
                dropFtsArtifacts(database)
                database.execSQL("DROP INDEX IF EXISTS $WRITE_MESSAGE_SCOPE_INDEX")
            }
            checkpointAndVacuum(database)

            val elapsedNanos = measureNanoTime {
                action(database)
            }

            return VariantWriteMetrics(
                variant = variant.displayName,
                rowsAffected = batchSize,
                elapsedNanos = elapsedNanos,
            )
        }
    }

    @Suppress("SameParameterValue")
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
        WRITE_FTS_TRIGGER_NAMES.forEach { triggerName ->
            database.execSQL("DROP TRIGGER IF EXISTS $triggerName")
        }
        database.execSQL("DROP TABLE IF EXISTS $WRITE_MESSAGE_FTS_TABLE")
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
                // Consume PRAGMA result rows so SQLite finalizes the operation.
            }
        }
    }
}

private data class WriteBenchmarkReport(
    val existingMessages: Long,
    val batchSize: Long,
    val insert: OperationBenchmarkReport,
    val update: OperationBenchmarkReport,
    val delete: OperationBenchmarkReport,
) {
    fun render(): String = buildString {
        appendLine("Search write benchmark")
        appendLine("Existing seeded dataset: $existingMessages synthetic messages")
        appendLine("Each insert/update/delete batch touches $batchSize messages")
        appendLine("current database = old schema without the new composite index or message_fts")
        appendLine("after fts = current database plus message_fts, its triggers, and the new composite index")
        appendLine()
        appendLine(insert.render())
        appendLine(update.render())
        appendLine(delete.render())
    }
}

private data class OperationBenchmarkReport(
    val operation: String,
    val current: VariantWriteMetrics,
    val afterFts: VariantWriteMetrics,
) {
    fun render(): String = buildString {
        appendLine("$operation:")
        appendLine("  current_database: ${current.render()}")
        appendLine("  after_fts: ${afterFts.render()}")
        appendLine(
            "  delta_vs_current=${(afterFts.elapsedNanos - current.elapsedNanos).formatDuration()} (${afterFts.elapsedNanos.writeGrowthPercentFrom(current.elapsedNanos)}%)"
        )
    }
}

private data class VariantWriteMetrics(
    val variant: String,
    val rowsAffected: Long,
    val elapsedNanos: Long,
) {
    fun render(): String {
        val perMessageMicros = if (rowsAffected == 0L) 0.0 else elapsedNanos / rowsAffected.toDouble() / 1_000.0
        return buildString {
            append("total=${elapsedNanos.formatDuration()}")
            append(", per_message=")
            append("%.2f us".format(perMessageMicros))
        }
    }
}

private enum class WriteOperation(
    val fileToken: String,
) {
    Insert("insert"),
    Update("update"),
    Delete("delete"),
}

private enum class WriteVariant(
    val fileToken: String,
    val displayName: String,
) {
    Current(
        fileToken = "current",
        displayName = "current_database",
    ),
    AfterFts(
        fileToken = "after_fts",
        displayName = "after_fts",
    ),
}

private fun writeDatabaseName(
    operation: WriteOperation,
    variant: WriteVariant,
): String = "search_write_${operation.fileToken}_${variant.fileToken}.db"

private fun Long.formatDuration(): String = "%.2f ms".format(this / 1_000_000.0)

private fun Long.writeGrowthPercentFrom(
    baseline: Long,
): String {
    if (baseline == 0L) return "n/a"
    val growth = ((this - baseline).toDouble() / baseline.toDouble()) * 100.0
    return "%.2f".format(growth)
}

private fun insertWriteMessages(
    database: SQLiteDatabase,
    startTid: Long,
    count: Long,
) {
    if (count == 0L) return

    val statement = database.compileStatement(
        """
        INSERT INTO $WRITE_MESSAGE_TABLE (
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
        val chunkSize = minOf(SearchMessagesBenchmarkFixture.DEFAULT_CHUNK_SIZE.toLong(), count - inserted)
        val chunkEndTid = startTid + inserted + chunkSize - 1L

        database.beginTransaction()
        try {
            for (tid in startTid + inserted..chunkEndTid) {
                bindInsertedWriteMessage(statement, tid)
                statement.executeInsert()
                statement.clearBindings()
            }
            database.setTransactionSuccessful()
            inserted += chunkSize
        } finally {
            database.endTransaction()
        }
    }
}

private fun bindInsertedWriteMessage(
    statement: android.database.sqlite.SQLiteStatement,
    tid: Long,
) {
    statement.bindLong(1, tid)
    statement.bindLong(2, tid)
    statement.bindLong(3, if (tid % 2L == 0L) 1L else 2L)
    statement.bindString(4, "write benchmark body $tid")
    statement.bindString(5, "text")
    statement.bindNull(6)
    statement.bindLong(7, tid)
    statement.bindLong(8, tid)
    statement.bindLong(9, 0L)
    statement.bindLong(10, 0L)
    statement.bindLong(11, 0L)
    statement.bindLong(12, 0L)
    statement.bindLong(13, MessageDeliveryStatus.Displayed.ordinal.toLong())
    statement.bindLong(14, MessageState.Unmodified.ordinal.toLong())
    statement.bindString(15, if (tid % 2L == 0L) "alice" else "bob")
    statement.bindNull(16)
    statement.bindNull(17)
    statement.bindNull(18)
    statement.bindLong(19, 0L)
    statement.bindLong(20, 0L)
    statement.bindNull(21)
    statement.bindNull(22)
    statement.bindLong(23, 0L)
    statement.bindLong(24, 0L)
    statement.bindNull(25)
    statement.bindNull(26)
    statement.bindNull(27)
}

private const val WRITE_TAG = "SearchWriteBenchmark"
private const val WRITE_MASTER_DB_NAME = "search_write_master.db"
private const val WRITE_MESSAGE_TABLE = "sceyt_message_table"
private const val WRITE_MESSAGE_FTS_TABLE = "message_fts"
private const val WRITE_MESSAGE_SCOPE_INDEX = "index_sceyt_message_table_channelId_createdAt_message_id"

private val WRITE_DATABASE_NAMES = buildList {
    add(WRITE_MASTER_DB_NAME)
    WriteOperation.entries.forEach { operation ->
        WriteVariant.entries.forEach { variant ->
            add(writeDatabaseName(operation, variant))
        }
    }
}

private val WRITE_FTS_TRIGGER_NAMES = listOf(
    "room_fts_content_sync_message_fts_BEFORE_UPDATE",
    "room_fts_content_sync_message_fts_BEFORE_DELETE",
    "room_fts_content_sync_message_fts_AFTER_UPDATE",
    "room_fts_content_sync_message_fts_AFTER_INSERT",
)
