package com.sceyt.chatuikit.persistence.database

import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.RenameTable
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.ATTACHMENT_PAYLOAD_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.ATTACHMENT_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.AUTO_DELETE_MESSAGES_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHANNEL_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHAT_USER_REACTION_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.DRAFT_MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.DRAFT_MESSAGE_USER_LINK_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.FILE_CHECKSUM_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LINK_DETAILS_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.LOAD_RANGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MARKER_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MENTION_USER_MESSAGE_LINK_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_MARKER_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_MESSAGE_STATE_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.PENDING_REACTION_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.REACTION_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.REACTION_TOTAL_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_CHAT_LINK_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_METADATA_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.USER_TABLE

internal object DatabaseMigrations {

    @RenameColumn(tableName = "messages", fromColumnName = "isParentMessage", toColumnName = "unList")
    class AutoMigrationSpec3to4 : AutoMigrationSpec

    @DeleteColumn(tableName = "DraftMessageEntity", columnName = "metadata")
    class AutoMigrationSpec5To6 : AutoMigrationSpec

    @DeleteColumn(tableName = "MentionUserMessageLink", columnName = "id")
    class AutoMigrationSpec10To11 : AutoMigrationSpec

    @DeleteColumn(tableName = "users", columnName = "metadata")
    class AutoMigrationSpec13To14 : AutoMigrationSpec

    val Migration_15_16: Migration = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Delete duplicate data
            db.execSQL("DELETE FROM channels WHERE chat_id NOT IN (SELECT MIN(chat_id) FROM channels GROUP BY uri)")
            // Start migration
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_channels` (`chat_id` INTEGER NOT NULL, `parentChannelId` INTEGER, `uri` TEXT, `type` TEXT NOT NULL, `subject` TEXT, `avatarUrl` TEXT, `metadata` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `messagesClearedAt` INTEGER NOT NULL, `memberCount` INTEGER NOT NULL, `createdById` TEXT, `userRole` TEXT, `unread` INTEGER NOT NULL, `newMessageCount` INTEGER NOT NULL, `newMentionCount` INTEGER NOT NULL, `newReactedMessageCount` INTEGER NOT NULL, `hidden` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `muted` INTEGER NOT NULL, `mutedTill` INTEGER, `pinnedAt` INTEGER, `lastReceivedMessageId` INTEGER NOT NULL, `lastDisplayedMessageId` INTEGER NOT NULL, `messageRetentionPeriod` INTEGER NOT NULL, `lastMessageTid` INTEGER, `lastMessageAt` INTEGER, `pending` INTEGER NOT NULL, `isSelf` INTEGER NOT NULL DEFAULT false, PRIMARY KEY(`chat_id`))")
            db.execSQL("INSERT INTO `_new_channels` (`chat_id`,`parentChannelId`,`uri`,`type`,`subject`,`avatarUrl`,`metadata`,`createdAt`,`updatedAt`,`messagesClearedAt`,`memberCount`,`createdById`,`userRole`,`unread`,`newMessageCount`,`newMentionCount`,`newReactedMessageCount`,`hidden`,`archived`,`muted`,`mutedTill`,`pinnedAt`,`lastReceivedMessageId`,`lastDisplayedMessageId`,`messageRetentionPeriod`,`lastMessageTid`,`lastMessageAt`,`pending`,`isSelf`) SELECT `chat_id`,`parentChannelId`,`uri`,`type`,`subject`,`avatarUrl`,`metadata`,`createdAt`,`updatedAt`,`messagesClearedAt`,`memberCount`,`createdById`,`userRole`,`unread`,`newMessageCount`,`newMentionCount`,`newReactedMessageCount`,`hidden`,`archived`,`muted`,`mutedTill`,`pinnedAt`,`lastReceivedMessageId`,`lastDisplayedMessageId`,`messageRetentionPeriod`,`lastMessageTid`,`lastMessageAt`,`pending`,`isSelf` FROM `channels`")
            db.execSQL("DROP TABLE `channels`")
            db.execSQL("ALTER TABLE `_new_channels` RENAME TO `channels`")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_channels_uri` ON `channels` (`uri`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_type` ON `channels` (`type`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_subject` ON `channels` (`subject`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_createdAt` ON `channels` (`createdAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_userRole` ON `channels` (`userRole`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_lastMessageAt` ON `channels` (`lastMessageAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_pending` ON `channels` (`pending`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_isSelf` ON `channels` (`isSelf`)")
        }
    }

    val Migration_16_17: Migration = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Clear all auto delete messages
            db.execSQL("DELETE FROM AutoDeleteMessages")
            // Start migration
            db.execSQL("CREATE TABLE IF NOT EXISTS `_new_AutoDeleteMessages` (`messageTid` INTEGER NOT NULL, `channelId` INTEGER NOT NULL, `autoDeleteAt` INTEGER NOT NULL, PRIMARY KEY(`messageTid`), FOREIGN KEY(`messageTid`) REFERENCES `messages`(`tid`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("DROP TABLE `AutoDeleteMessages`")
            db.execSQL("ALTER TABLE `_new_AutoDeleteMessages` RENAME TO `AutoDeleteMessages`")
        }
    }

    @DeleteColumn(tableName = "MarkerEntity", columnName = "primaryKey")
    @DeleteTable(tableName = "UserMarkerLink")
    class AutoMigrationSpec17To18 : AutoMigrationSpec

    @RenameTable(fromTableName = "channels", toTableName = CHANNEL_TABLE)
    @RenameTable(fromTableName = "users", toTableName = USER_TABLE)
    @RenameTable(fromTableName = "UserChatLink", toTableName = USER_CHAT_LINK_TABLE)
    @RenameTable(fromTableName = "messages", toTableName = MESSAGE_TABLE)
    @RenameTable(fromTableName = "MentionUserMessageLink", toTableName = MENTION_USER_MESSAGE_LINK_TABLE)
    @RenameTable(fromTableName = "DraftMessageEntity", toTableName = DRAFT_MESSAGE_TABLE)
    @RenameTable(fromTableName = "DraftMessageUserLink", toTableName = DRAFT_MESSAGE_USER_LINK_TABLE)
    @RenameTable(fromTableName = "AttachmentEntity", toTableName = ATTACHMENT_TABLE)
    @RenameTable(fromTableName = "MarkerEntity", toTableName = MARKER_TABLE)
    @RenameTable(fromTableName = "ReactionEntity", toTableName = REACTION_TABLE)
    @RenameTable(fromTableName = "ReactionTotalEntity", toTableName = REACTION_TOTAL_TABLE)
    @RenameTable(fromTableName = "ChatUserReactionEntity", toTableName = CHAT_USER_REACTION_TABLE)
    @RenameTable(fromTableName = "PendingMarker", toTableName = PENDING_MARKER_TABLE)
    @RenameTable(fromTableName = "pendingReaction", toTableName = PENDING_REACTION_TABLE)
    @RenameTable(fromTableName = "PendingMessageState", toTableName = PENDING_MESSAGE_STATE_TABLE)
    @RenameTable(fromTableName = "AttachmentPayLoad", toTableName = ATTACHMENT_PAYLOAD_TABLE)
    @RenameTable(fromTableName = "FileChecksum", toTableName = FILE_CHECKSUM_TABLE)
    @RenameTable(fromTableName = "LinkDetails", toTableName = LINK_DETAILS_TABLE)
    @RenameTable(fromTableName = "LoadRange", toTableName = LOAD_RANGE_TABLE)
    @RenameTable(fromTableName = "AutoDeleteMessages", toTableName = AUTO_DELETE_MESSAGES_TABLE)
    @RenameTable(fromTableName = "UserMetadata", toTableName = USER_METADATA_TABLE)
    class AutoMigrationSpec18To19 : AutoMigrationSpec
}