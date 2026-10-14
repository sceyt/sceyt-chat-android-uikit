package com.sceyt.chatuikit.persistence.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.MessageDeliveryStatus
import java.security.MessageDigest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReleasedPinnedSchemaMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SceytDatabase::class.java,
    )

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(TEST_DB_NAME)
    }

    @Test
    fun released31ToLatest_keepsMessagesAndCreatesPinTables() {
        createReleasedDatabase(31).use { database ->
            database.insertMessage(tid = 1L, messageId = 101L, body = "existing message")
        }

        withLatestDatabase { migrated ->
            assertLatestSchema(migrated)
            assertThat(queryString(migrated, "SELECT body FROM ${DatabaseConstants.MESSAGE_TABLE}"))
                .isEqualTo("existing message")
            assertThat(rowCount(migrated, DatabaseConstants.PINNED_MESSAGE_TABLE)).isEqualTo(0L)
            assertThat(rowCount(migrated, DatabaseConstants.PENDING_PIN_TABLE)).isEqualTo(0L)
        }
    }

    @Test
    fun released32ToLatest_opensWithoutAnyPinsAndKeepsMessages() {
        createReleasedDatabase(32).use { database ->
            database.insertMessage(tid = 1L, messageId = 101L, body = "never pinned")
        }

        withLatestDatabase { migrated ->
            assertLatestSchema(migrated)
            assertThat(queryString(migrated, "SELECT body FROM ${DatabaseConstants.MESSAGE_TABLE}"))
                .isEqualTo("never pinned")
            assertThat(rowCount(migrated, DatabaseConstants.PINNED_MESSAGE_TABLE)).isEqualTo(0L)
            assertThat(rowCount(migrated, DatabaseConstants.PENDING_PIN_TABLE)).isEqualTo(0L)
        }
    }

    @Test
    fun released32ToLatest_keepsConfirmedPinsAndMigratesPendingPins() {
        createReleasedDatabase(32).use { database ->
            database.insertMessage(tid = 1L, messageId = 101L, body = "confirmed pin")
            database.insertMessage(tid = 2L, messageId = 102L, body = "pending pin")
            database.insertMessage(tid = 3L, messageId = 0L, body = "unsent message")
            database.execSQL(
                """
                INSERT INTO ${DatabaseConstants.PINNED_MESSAGE_TABLE} (
                    messageTid, channelId, messageId, pinScope, pinnedAt, pinnedUntil,
                    pinnedByUserId, messageCreatedAt, serverPinId, syncState, retryCount, lastAttemptAt
                ) VALUES
                    (1, 1, 101, 2, 500, 900, 'alice', 100, 10, 1, 0, 0),
                    (2, 1, 102, 2, 600, NULL, 'alice', 200, ${Long.MAX_VALUE}, 2, 1, 700),
                    (3, 1, 0, 2, 700, NULL, 'alice', 300, ${Long.MAX_VALUE}, 2, 0, 0)
                """.trimIndent()
            )
        }

        withLatestDatabase { migrated ->
            assertLatestSchema(migrated)
            assertThat(rowCount(migrated, DatabaseConstants.MESSAGE_TABLE)).isEqualTo(3L)
            assertThat(rowCount(migrated, DatabaseConstants.PINNED_MESSAGE_TABLE)).isEqualTo(1L)
            assertThat(
                queryLong(
                    migrated,
                    "SELECT COUNT(*) FROM ${DatabaseConstants.PINNED_MESSAGE_TABLE} " +
                            "WHERE messageTid = 1 AND channelId = 1 AND messageId = 101 " +
                            "AND serverPinId = 10 AND pinScope = 2 AND pinnedAt = 500 " +
                            "AND pinnedUntil = 900 AND pinnedByUserId = 'alice' AND messageCreatedAt = 100"
                )
            ).isEqualTo(1L)
            assertThat(rowCount(migrated, DatabaseConstants.PENDING_PIN_TABLE)).isEqualTo(1L)
            assertThat(
                queryLong(
                    migrated,
                    "SELECT COUNT(*) FROM ${DatabaseConstants.PENDING_PIN_TABLE} " +
                            "WHERE messageTid = 2 AND channelId = 1 AND messageId = 102 " +
                            "AND isPin = 1 AND pinScope = 2 AND createdAt = 600"
                )
            ).isEqualTo(1L)
        }
    }

    @Test
    fun released33ToLatest_keepsMessagesConfirmedPinsAndPendingUnpins() {
        createReleasedDatabase(33).use { database ->
            database.insertMessage(tid = 1L, messageId = 101L, body = "pinned message")
            database.execSQL(
                """
                INSERT INTO ${DatabaseConstants.PINNED_MESSAGE_TABLE} (
                    messageTid, channelId, messageId, pinScope, pinnedAt, pinnedUntil,
                    pinnedByUserId, messageCreatedAt, serverPinId
                ) VALUES (1, 1, 101, 2, 500, 900, 'alice', 100, 10)
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO ${DatabaseConstants.PENDING_PIN_TABLE} (
                    id, messageTid, channelId, messageId, isPin, pinScope, createdAt
                ) VALUES (7, 1, 1, 101, 0, 2, 600)
                """.trimIndent()
            )
        }

        withLatestDatabase { migrated ->
            assertLatestSchema(migrated)
            assertThat(queryString(migrated, "SELECT body FROM ${DatabaseConstants.MESSAGE_TABLE}"))
                .isEqualTo("pinned message")
            assertThat(rowCount(migrated, DatabaseConstants.PINNED_MESSAGE_TABLE)).isEqualTo(1L)
            assertThat(queryLong(migrated, "SELECT serverPinId FROM ${DatabaseConstants.PINNED_MESSAGE_TABLE}"))
                .isEqualTo(10L)
            assertThat(rowCount(migrated, DatabaseConstants.PENDING_PIN_TABLE)).isEqualTo(1L)
            assertThat(
                queryLong(
                    migrated,
                    "SELECT COUNT(*) FROM ${DatabaseConstants.PENDING_PIN_TABLE} " +
                            "WHERE id = 7 AND messageTid = 1 AND channelId = 1 AND messageId = 101 " +
                            "AND isPin = 0 AND pinScope = 2 AND createdAt = 600"
                )
            ).isEqualTo(1L)
        }
    }

    private fun createReleasedDatabase(version: Int): SupportSQLiteDatabase {
        // Freeze the original schema inputs, so rewriting an old JSON cannot silently
        // change the upgrade path under test. v31/v32: ff192ae26; v33: 1cb274279.
        val expectedDigest = when (version) {
            31 -> "25d03c3cd3d5adc05e42cebf876277a38c9fab67d47530aea14c15e715f7c548"
            32 -> "e28f2d9e813c771092a6b6dae190e1915e2d05fcc63969d561dc5e3b55a52093"
            33 -> "7afa9062c2c3f612e43593cd0d6b2c974d798dbd337389b2101e18188e0a60bd"
            else -> error("Unsupported released schema version: $version")
        }
        val schema = InstrumentationRegistry.getInstrumentation().context.assets
            .open("${SceytDatabase::class.java.name}/$version.json")
            .use { it.readBytes() }
        val actualDigest = MessageDigest.getInstance("SHA-256").digest(schema)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        assertThat(actualDigest).isEqualTo(expectedDigest)
        return helper.createDatabase(TEST_DB_NAME, version)
    }

    private fun withLatestDatabase(assertions: (SupportSQLiteDatabase) -> Unit) {
        val database = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            SceytDatabase::class.java,
            TEST_DB_NAME,
        ).addMigrations(
            DatabaseMigrations.Migration_15_16,
            DatabaseMigrations.Migration_16_17,
            DatabaseMigrations.Migration_28_29,
        ).allowMainThreadQueries().build()
        try {
            assertions(database.openHelper.writableDatabase)
        } finally {
            database.close()
        }
    }
}

private const val TEST_DB_NAME = "released-pinned-schema-migration-test.db"

private fun assertLatestSchema(database: SupportSQLiteDatabase) {
    assertThat(database.version).isEqualTo(34)
    assertThat(rowCount(database, DatabaseConstants.PENDING_CHANNEL_AVATAR_TABLE)).isEqualTo(0L)
    database.query("PRAGMA foreign_key_check").use { cursor ->
        assertThat(cursor.count).isEqualTo(0)
    }
}

private fun SupportSQLiteDatabase.insertMessage(tid: Long, messageId: Long, body: String) {
    execSQL(
        """
        INSERT INTO ${DatabaseConstants.MESSAGE_TABLE} (
            tid, message_id, channelId, body, type, createdAt, updatedAt, incoming,
            isTransient, silent, deliveryStatus, state, replyCount, displayCount, unList
        ) VALUES (?, ?, 1, ?, 'text', 100, 100, 0, 0, 0, ?, ?, 0, 0, 0)
        """.trimIndent(),
        arrayOf<Any?>(tid, messageId, body, MessageDeliveryStatus.Displayed.ordinal, MessageState.Unmodified.ordinal)
    )
}

private fun rowCount(database: SupportSQLiteDatabase, table: String): Long =
    queryLong(database, "SELECT COUNT(*) FROM $table")

private fun queryLong(database: SupportSQLiteDatabase, sql: String): Long =
    database.query(sql).use { cursor ->
        check(cursor.moveToFirst()) { "Expected a row for query: $sql" }
        cursor.getLong(0)
    }

private fun queryString(database: SupportSQLiteDatabase, sql: String): String =
    database.query(sql).use { cursor ->
        check(cursor.moveToFirst()) { "Expected a row for query: $sql" }
        cursor.getString(0)
    }
