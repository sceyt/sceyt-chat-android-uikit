package com.sceyt.chatuikit.persistence

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

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
}