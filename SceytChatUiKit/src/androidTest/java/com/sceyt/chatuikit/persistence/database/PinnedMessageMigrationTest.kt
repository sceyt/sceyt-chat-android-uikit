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

/**
 * Guards the v31 -> v32 hop, which adds the pinned-messages table, and the v32 -> v33 hop,
 * which splits unconfirmed pins into their own table.
 *
 * The DAO tests all build a fresh database, so only this exercises the migration against an
 * existing one — and the cascade assertion is what the "delete a message, the pin goes with
 * it" requirement actually rests on.
 */
@RunWith(AndroidJUnit4::class)
class PinnedMessageMigrationTest {

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
    fun migrate31To32_addsPinnedMessageTableAndKeepsExistingMessages() {
        helper.createDatabase(TEST_DB_NAME, 31).apply {
            insertMessageRow(tid = 1L, messageId = 101L, body = "pin me")
            insertMessageRow(tid = 2L, messageId = 102L, body = "leave me")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_NAME, 32, true).use { migrated ->
            assertThat(tableExists(migrated, DatabaseConstants.PINNED_MESSAGE_TABLE)).isTrue()

            // Purely additive: existing rows survive untouched.
            assertThat(
                queryLong(migrated, "SELECT COUNT(*) FROM ${DatabaseConstants.MESSAGE_TABLE}")
            ).isEqualTo(2L)
            assertThat(
                queryLong(migrated, "SELECT COUNT(*) FROM ${DatabaseConstants.PINNED_MESSAGE_TABLE}")
            ).isEqualTo(0L)
        }
    }

    @Test
    fun migrate31To32_deletingAMessageCascadesToItsPin() {
        helper.createDatabase(TEST_DB_NAME, 31).apply {
            insertMessageRow(tid = 1L, messageId = 101L, body = "pin me")
            insertMessageRow(tid = 2L, messageId = 102L, body = "leave me")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_NAME, 32, true).use { migrated ->
            // MigrationTestHelper does not enable foreign keys for us.
            migrated.execSQL("PRAGMA foreign_keys = ON")
            migrated.insertPinRow(messageTid = 1L, messageId = 101L, serverPinId = 10L)
            migrated.insertPinRow(messageTid = 2L, messageId = 102L, serverPinId = 11L)
            assertThat(pinCount(migrated)).isEqualTo(2L)

            migrated.execSQL("DELETE FROM ${DatabaseConstants.MESSAGE_TABLE} WHERE tid = 1")

            assertThat(pinCount(migrated)).isEqualTo(1L)
            assertThat(
                queryLong(
                    migrated,
                    "SELECT messageTid FROM ${DatabaseConstants.PINNED_MESSAGE_TABLE}"
                )
            ).isEqualTo(2L)
        }
    }

    @Test
    fun migrate31To32_pinsAreUniquePerMessageAndChannel() {
        helper.createDatabase(TEST_DB_NAME, 31).apply {
            insertMessageRow(tid = 1L, messageId = 101L, body = "pin me")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_NAME, 32, true).use { migrated ->
            migrated.insertPinRow(messageTid = 1L, messageId = 101L, serverPinId = 10L)
            // A second pin for the same message replaces rather than duplicates, which is
            // what makes the flush path and the realtime event safe to both apply.
            migrated.insertPinRow(messageTid = 1L, messageId = 101L, serverPinId = 20L)

            assertThat(pinCount(migrated)).isEqualTo(1L)
            assertThat(
                queryLong(
                    migrated,
                    "SELECT serverPinId FROM ${DatabaseConstants.PINNED_MESSAGE_TABLE}"
                )
            ).isEqualTo(20L)
        }
    }
    @Test
    fun migrate32To33_keepsMessagesAndConfirmedPins() {
        helper.createDatabase(TEST_DB_NAME, 32).apply {
            insertMessageRow(tid = 1L, messageId = 101L, body = "pin me")
            insertMessageRow(tid = 2L, messageId = 102L, body = "leave me")
            insertPinRow(messageTid = 1L, messageId = 101L, serverPinId = 10L)
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_NAME, 33, true).use { migrated ->
            assertThat(
                queryLong(migrated, "SELECT COUNT(*) FROM ${DatabaseConstants.MESSAGE_TABLE}")
            ).isEqualTo(2L)
            assertThat(
                queryLong(migrated, "SELECT serverPinId FROM ${DatabaseConstants.PINNED_MESSAGE_TABLE}")
            ).isEqualTo(10L)
            assertThat(pendingPinCount(migrated)).isEqualTo(0L)
        }
    }

    @Test
    fun migrate32To33_movesUnconfirmedPinsOfSentMessagesToThePendingTable() {
        helper.createDatabase(TEST_DB_NAME, 32).apply {
            insertMessageRow(tid = 1L, messageId = 101L, body = "pin me")
            insertMessageRow(tid = 2L, messageId = 0L, body = "not sent yet")
            insertPinRow(messageTid = 1L, messageId = 101L, serverPinId = Long.MAX_VALUE, syncState = 2)
            insertPinRow(messageTid = 2L, messageId = 0L, serverPinId = Long.MAX_VALUE, syncState = 2)
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_NAME, 33, true).use { migrated ->
            assertThat(pinCount(migrated)).isEqualTo(0L)
            // The pin of the unsent message is dropped: pending messages can no longer be pinned.
            assertThat(pendingPinCount(migrated)).isEqualTo(1L)
            assertThat(
                queryLong(
                    migrated,
                    "SELECT COUNT(*) FROM ${DatabaseConstants.PENDING_PIN_TABLE} " +
                            "WHERE messageTid = 1 AND messageId = 101 AND isPin = 1 AND pinScope = 2 AND createdAt = 500"
                )
            ).isEqualTo(1L)
        }
    }

    @Test
    fun migrate31To33_keepsExistingMessages() {
        helper.createDatabase(TEST_DB_NAME, 31).apply {
            insertMessageRow(tid = 1L, messageId = 101L, body = "keep me")
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_NAME, 33, true).use { migrated ->
            assertThat(
                queryLong(migrated, "SELECT COUNT(*) FROM ${DatabaseConstants.MESSAGE_TABLE}")
            ).isEqualTo(1L)
            assertThat(tableExists(migrated, DatabaseConstants.PENDING_PIN_TABLE)).isTrue()
        }
    }
}

private const val TEST_DB_NAME = "pinned-message-migration-test.db"

private fun pinCount(database: SupportSQLiteDatabase): Long =
    queryLong(database, "SELECT COUNT(*) FROM ${DatabaseConstants.PINNED_MESSAGE_TABLE}")

private fun pendingPinCount(database: SupportSQLiteDatabase): Long =
    queryLong(database, "SELECT COUNT(*) FROM ${DatabaseConstants.PENDING_PIN_TABLE}")

private fun SupportSQLiteDatabase.insertPinRow(
    messageTid: Long,
    messageId: Long,
    serverPinId: Long,
    channelId: Long = 1L,
    syncState: Int = 1, // v32: 1 synced, 2 pending pin, 3 pending unpin
) {
    execSQL(
        """
        INSERT OR REPLACE INTO ${DatabaseConstants.PINNED_MESSAGE_TABLE} (
            messageTid, channelId, messageId, pinScope, pinnedAt, pinnedUntil,
            pinnedByUserId, messageCreatedAt, serverPinId, syncState, retryCount, lastAttemptAt
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            messageTid, channelId, messageId, 2, 500L, null,
            null, messageTid * 100L, serverPinId, syncState, 0, 0L,
        )
    )
}

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
            tid, message_id, channelId, body, type, createdAt, updatedAt, incoming,
            isTransient, silent, deliveryStatus, state, replyCount, displayCount, unList
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            tid, messageId, channelId, body, "text", createdAt, createdAt, 0,
            0, 0, MessageDeliveryStatus.Displayed.ordinal, MessageState.Unmodified.ordinal, 0, 0, 0,
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

private fun queryLong(
    database: SupportSQLiteDatabase,
    sql: String,
): Long = database.query(sql).use { cursor ->
    check(cursor.moveToFirst()) { "Expected a row for query: $sql" }
    cursor.getLong(0)
}
