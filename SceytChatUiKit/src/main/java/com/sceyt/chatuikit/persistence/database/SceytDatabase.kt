package com.sceyt.chatuikit.persistence.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sceyt.chatuikit.persistence.database.converters.ChannelConverter
import com.sceyt.chatuikit.persistence.database.converters.ListStringConverter
import com.sceyt.chatuikit.persistence.database.converters.MessageConverter
import com.sceyt.chatuikit.persistence.database.dao.AttachmentDao
import com.sceyt.chatuikit.persistence.database.dao.AutoDeleteMessageDao
import com.sceyt.chatuikit.persistence.database.dao.ChannelDao
import com.sceyt.chatuikit.persistence.database.dao.ChatUserReactionDao
import com.sceyt.chatuikit.persistence.database.dao.DraftMessageDao
import com.sceyt.chatuikit.persistence.database.dao.FileChecksumDao
import com.sceyt.chatuikit.persistence.database.dao.LinkDao
import com.sceyt.chatuikit.persistence.database.dao.LoadRangeDao
import com.sceyt.chatuikit.persistence.database.dao.MarkerDao
import com.sceyt.chatuikit.persistence.database.dao.MemberDao
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMarkerDao
import com.sceyt.chatuikit.persistence.database.dao.PendingMessageStateDao
import com.sceyt.chatuikit.persistence.database.dao.PendingReactionDao
import com.sceyt.chatuikit.persistence.database.dao.ReactionDao
import com.sceyt.chatuikit.persistence.database.dao.UserDao
import com.sceyt.chatuikit.persistence.database.entity.FileChecksumEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.ChannelEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.ChatUserReactionEntity
import com.sceyt.chatuikit.persistence.database.entity.channel.UserChatLink
import com.sceyt.chatuikit.persistence.database.entity.link.LinkDetailsEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AttachmentPayLoadEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.AutoDeleteMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageUserLink
import com.sceyt.chatuikit.persistence.database.entity.messages.LoadRangeEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MarkerEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.MentionUserMessageLink
import com.sceyt.chatuikit.persistence.database.entity.messages.MessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.ReactionTotalEntity
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMarkerEntity
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingMessageStateEntity
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingReactionEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserEntity
import com.sceyt.chatuikit.persistence.database.entity.user.UserMetadataEntity

@Database(
    entities = [
        ChannelEntity::class,
        UserEntity::class,
        UserChatLink::class,
        MessageEntity::class,
        MentionUserMessageLink::class,
        DraftMessageEntity::class,
        DraftMessageUserLink::class,
        AttachmentEntity::class,
        MarkerEntity::class,
        ReactionEntity::class,
        ReactionTotalEntity::class,
        ChatUserReactionEntity::class,
        PendingMarkerEntity::class,
        PendingReactionEntity::class,
        PendingMessageStateEntity::class,
        AttachmentPayLoadEntity::class,
        FileChecksumEntity::class,
        LinkDetailsEntity::class,
        LoadRangeEntity::class,
        AutoDeleteMessageEntity::class,
        UserMetadataEntity::class,
    ],
    version = 18,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, spec = DatabaseMigrations.AutoMigrationSpec3to4::class),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = DatabaseMigrations.AutoMigrationSpec5To6::class),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11, spec = DatabaseMigrations.AutoMigrationSpec10To11::class),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
        AutoMigration(from = 13, to = 14, spec = DatabaseMigrations.AutoMigrationSpec13To14::class),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 17, to = 18, spec = DatabaseMigrations.AutoMigrationSpec17To18::class),
    ])

@TypeConverters(ChannelConverter::class, MessageConverter::class, ListStringConverter::class)
abstract class SceytDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentsDao(): AttachmentDao
    abstract fun draftMessageDao(): DraftMessageDao
    abstract fun membersDao(): MemberDao
    abstract fun userDao(): UserDao
    abstract fun reactionDao(): ReactionDao
    abstract fun channelUsersReactionDao(): ChatUserReactionDao
    abstract fun pendingMarkersDao(): PendingMarkerDao
    abstract fun pendingReactionDao(): PendingReactionDao
    abstract fun pendingMessageStateDao(): PendingMessageStateDao
    abstract fun fileChecksumDao(): FileChecksumDao
    abstract fun linkDao(): LinkDao
    abstract fun loadRangeDao(): LoadRangeDao
    abstract fun markerDao(): MarkerDao
    abstract fun autoDeleteMessageDao(): AutoDeleteMessageDao
}
