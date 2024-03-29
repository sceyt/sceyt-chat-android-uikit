package com.sceyt.sceytchatuikit.persistence

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.sceyt.sceytchatuikit.persistence.converters.ChannelConverter
import com.sceyt.sceytchatuikit.persistence.converters.ListStringConverter
import com.sceyt.sceytchatuikit.persistence.converters.MessageConverter
import com.sceyt.sceytchatuikit.persistence.dao.AttachmentDao
import com.sceyt.sceytchatuikit.persistence.dao.ChannelDao
import com.sceyt.sceytchatuikit.persistence.dao.ChatUsersReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.DraftMessageDao
import com.sceyt.sceytchatuikit.persistence.dao.FileChecksumDao
import com.sceyt.sceytchatuikit.persistence.dao.LinkDao
import com.sceyt.sceytchatuikit.persistence.dao.LoadRangeDao
import com.sceyt.sceytchatuikit.persistence.dao.MembersDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.PendingMarkersDao
import com.sceyt.sceytchatuikit.persistence.dao.PendingMessageStateDao
import com.sceyt.sceytchatuikit.persistence.dao.PendingReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.ReactionDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.FileChecksumEntity
import com.sceyt.sceytchatuikit.persistence.entity.pendings.PendingMarkerEntity
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChatUserReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.UserChatLink
import com.sceyt.sceytchatuikit.persistence.entity.link.LinkDetailsEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentPayLoadEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.DraftMessageEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.DraftMessageUserLink
import com.sceyt.sceytchatuikit.persistence.entity.messages.LoadRangeEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MarkerEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MentionUserMessageLink
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.sceytchatuikit.persistence.entity.pendings.PendingReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionTotalEntity
import com.sceyt.sceytchatuikit.persistence.entity.pendings.PendingMessageStateEntity

@Database(entities = [
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
    LoadRangeEntity::class
], version = 9, autoMigrations = [
    AutoMigration(from = 1, to = 2),
    AutoMigration(from = 2, to = 3),
    AutoMigration(from = 3, to = 4, spec = SceytDatabase.AutoMigrationSpec3to4::class),
    AutoMigration(from = 4, to = 5),
    AutoMigration(from = 5, to = 6, spec = SceytDatabase.AutoMigrationSpec5To6::class),
    AutoMigration(from = 6, to = 7),
    AutoMigration(from = 7, to = 8),
    AutoMigration(from = 8, to = 9)
])

@TypeConverters(ChannelConverter::class, MessageConverter::class, ListStringConverter::class)
internal abstract class SceytDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentsDao(): AttachmentDao
    abstract fun draftMessageDao(): DraftMessageDao
    abstract fun membersDao(): MembersDao
    abstract fun userDao(): UserDao
    abstract fun reactionDao(): ReactionDao
    abstract fun channelUsersReactionDao(): ChatUsersReactionDao
    abstract fun pendingMarkersDao(): PendingMarkersDao
    abstract fun pendingReactionDao(): PendingReactionDao
    abstract fun pendingMessageStateDao(): PendingMessageStateDao
    abstract fun fileChecksumDao(): FileChecksumDao
    abstract fun linkDao(): LinkDao
    abstract fun loadRangeDao(): LoadRangeDao

    @RenameColumn(tableName = "messages", fromColumnName = "isParentMessage", toColumnName = "unList")
    class AutoMigrationSpec3to4 : AutoMigrationSpec

    @DeleteColumn(tableName = "DraftMessageEntity", columnName = "metadata")
    class AutoMigrationSpec5To6 : AutoMigrationSpec
}
