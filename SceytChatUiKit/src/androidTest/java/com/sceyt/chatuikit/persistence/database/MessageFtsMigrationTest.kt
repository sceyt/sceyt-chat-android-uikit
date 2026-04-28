package com.sceyt.chatuikit.persistence.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageFtsMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SceytDatabase::class.java,
    )

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun migrate28To29_createsAndBackfillsMessageFts() {
        helper.createDatabase(TEST_DB_NAME, 28).apply {
            insertMessageRow(
                tid = 1L,
                messageId = 101L,
                body = "release notes",
            )
            insertMessageRow(
                tid = 2L,
                messageId = 102L,
                body = "other content",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            29,
            true,
            DatabaseMigrations.Migration_28_29,
        ).use { migrated ->
            assertThat(tableExists(migrated, DatabaseConstants.MESSAGE_FTS_TABLE)).isTrue()
            assertThat(triggerExists(migrated, "room_fts_content_sync_${DatabaseConstants.MESSAGE_FTS_TABLE}_AFTER_INSERT")).isTrue()
            assertThat(triggerExists(migrated, "room_fts_content_sync_${DatabaseConstants.MESSAGE_FTS_TABLE}_AFTER_UPDATE")).isTrue()
            assertThat(triggerExists(migrated, "room_fts_content_sync_${DatabaseConstants.MESSAGE_FTS_TABLE}_BEFORE_DELETE")).isTrue()
            assertThat(triggerExists(migrated, "room_fts_content_sync_${DatabaseConstants.MESSAGE_FTS_TABLE}_BEFORE_UPDATE")).isTrue()
            assertThat(queryLong(migrated, "SELECT COUNT(*) FROM ${DatabaseConstants.MESSAGE_FTS_TABLE}")).isEqualTo(2L)
            assertThat(queryString(migrated, "SELECT body FROM ${DatabaseConstants.MESSAGE_FTS_TABLE} WHERE docid = 1")).isEqualTo("release notes")
            assertThat(
                queryLong(
                    migrated,
                    """SELECT COUNT(*) FROM ${DatabaseConstants.MESSAGE_FTS_TABLE} WHERE ${DatabaseConstants.MESSAGE_FTS_TABLE} MATCH '"release"* "notes"*'""",
                )
            ).isEqualTo(1L)
        }
    }

    @Test
    fun migrate28To29_ftsTriggersStayInSyncOnInsertUpdateAndDelete() {
        helper.createDatabase(TEST_DB_NAME, 28).apply {
            insertMessageRow(
                tid = 1L,
                messageId = 101L,
                body = "seed body",
            )
            close()
        }

        helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            29,
            true,
            DatabaseMigrations.Migration_28_29,
        ).use { migrated ->
            migrated.insertMessageRow(
                tid = 2L,
                messageId = 102L,
                body = "migration insert body",
            )
            assertThat(queryString(migrated, "SELECT body FROM ${DatabaseConstants.MESSAGE_FTS_TABLE} WHERE docid = 2")).isEqualTo(
                "migration insert body"
            )

            migrated.execSQL(
                """
                UPDATE ${DatabaseConstants.MESSAGE_TABLE}
                SET body = 'migration updated body'
                WHERE tid = 2
                """.trimIndent()
            )
            assertThat(queryString(migrated, "SELECT body FROM ${DatabaseConstants.MESSAGE_FTS_TABLE} WHERE docid = 2")).isEqualTo(
                "migration updated body"
            )

            migrated.execSQL("DELETE FROM ${DatabaseConstants.MESSAGE_TABLE} WHERE tid = 2")
            assertThat(queryLong(migrated, "SELECT COUNT(*) FROM ${DatabaseConstants.MESSAGE_FTS_TABLE} WHERE docid = 2")).isEqualTo(0L)
        }
    }
}

private const val TEST_DB_NAME = "message-fts-migration-test.db"

private fun SupportSQLiteDatabase.insertMessageRow(
    tid: Long,
    messageId: Long,
    body: String,
    channelId: Long = 1L,
    createdAt: Long = tid * 100L,
) {
    execSQL(
        """
        INSERT INTO ${DatabaseConstants.MESSAGE_TABLE} (
            tid,
            message_id,
            channelId,
            body,
            type,
            createdAt,
            updatedAt,
            incoming,
            isTransient,
            silent,
            deliveryStatus,
            state,
            replyCount,
            displayCount,
            unList
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            tid,
            messageId,
            channelId,
            body,
            "text",
            createdAt,
            createdAt,
            0,
            0,
            0,
            MessageDeliveryStatus.Displayed.ordinal,
            MessageState.Unmodified.ordinal,
            0,
            0,
            0,
        )
    )
}

private fun tableExists(
    database: SupportSQLiteDatabase,
    tableName: String,
): Boolean = queryLong(
    database,
    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = '$tableName'",
) == 1L

private fun triggerExists(
    database: SupportSQLiteDatabase,
    triggerName: String,
): Boolean = queryLong(
    database,
    "SELECT COUNT(*) FROM sqlite_master WHERE type = 'trigger' AND name = '$triggerName'",
) == 1L

private fun queryLong(
    database: SupportSQLiteDatabase,
    sql: String,
): Long = database.query(sql).use { cursor ->
    check(cursor.moveToFirst()) { "Expected a row for query: $sql" }
    cursor.getLong(0)
}

private fun queryString(
    database: SupportSQLiteDatabase,
    sql: String,
): String = database.query(sql).use { cursor ->
    check(cursor.moveToFirst()) { "Expected a row for query: $sql" }
    cursor.getString(0)
}
