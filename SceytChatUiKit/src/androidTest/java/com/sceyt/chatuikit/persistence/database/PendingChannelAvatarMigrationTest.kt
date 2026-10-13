package com.sceyt.chatuikit.persistence.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingChannelAvatarMigrationTest {

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
    fun migrate31To32_marksAvatarsOfExistingPendingChannelsAsLocal() {
        helper.createDatabase(TEST_DB_NAME, 31).apply {
            insertChannelRow(id = 1L, avatarUrl = "/data/avatar.png", pending = true)
            insertChannelRow(id = 2L, avatarUrl = "https://example.com/real.png", pending = false)
            insertChannelRow(id = 3L, avatarUrl = "", pending = true)
            insertChannelRow(id = 4L, avatarUrl = null, pending = true)
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB_NAME, 32, true).use { migrated ->
            migrated.query(
                "SELECT channelId, filePath FROM ${DatabaseConstants.PENDING_CHANNEL_AVATAR_TABLE}"
            ).use { cursor ->
                assertThat(cursor.count).isEqualTo(1)
                cursor.moveToFirst()
                assertThat(cursor.getLong(0)).isEqualTo(1L)
                assertThat(cursor.getString(1)).isEqualTo("/data/avatar.png")
            }
        }
    }
}

private const val TEST_DB_NAME = "pending-channel-avatar-migration-test.db"

private fun SupportSQLiteDatabase.insertChannelRow(id: Long, avatarUrl: String?, pending: Boolean) {
    execSQL(
        """
        INSERT INTO ${DatabaseConstants.CHANNEL_TABLE} (
            chat_id, type, avatarUrl, createdAt, updatedAt, messagesClearedAt, memberCount,
            unread, newMessageCount, newMentionCount, newReactedMessageCount, hidden, archived,
            muted, lastReceivedMessageId, lastDisplayedMessageId, messageRetentionPeriod, pending, isSelf
        ) VALUES (?, 'group', ?, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ?, 0)
        """.trimIndent(),
        arrayOf<Any?>(id, avatarUrl, if (pending) 1 else 0)
    )
}
