package com.sceyt.sceytchatuikit.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sceyt.sceytchatuikit.persistence.converters.ChannelConverter
import com.sceyt.sceytchatuikit.persistence.converters.ListStringConverter
import com.sceyt.sceytchatuikit.persistence.converters.MessageConverter
import com.sceyt.sceytchatuikit.persistence.dao.*
import com.sceyt.sceytchatuikit.persistence.entity.PendingMarkersEntity
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChatUserReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.UserChatLink
import com.sceyt.sceytchatuikit.persistence.entity.messages.*

@Database(entities = [
    ChannelEntity::class,
    UserEntity::class,
    UserChatLink::class,
    MessageEntity::class,
    MentionUserMessageLink::class,
    DraftMessageEntity::class,
    DraftMessageUserLink::class,
    AttachmentEntity::class,
    ReactionEntity::class,
    ReactionScoreEntity::class,
    ChatUserReactionEntity::class,
    PendingMarkersEntity::class,
    AttachmentPayLoadEntity::class
], version = 23, exportSchema = false)

@TypeConverters(ChannelConverter::class, MessageConverter::class, ListStringConverter::class)
internal abstract class SceytDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun messageDao(): MessageDao
    abstract fun draftMessageDao(): DraftMessageDao
    abstract fun membersDao(): MembersDao
    abstract fun userDao(): UserDao
    abstract fun reactionDao(): ReactionDao
    abstract fun channelUsersReactionDao(): ChatUsersReactionDao
    abstract fun pendingMarkersDao(): PendingMarkersDao
    abstract fun attachmentsDao(): AttachmentDao
}