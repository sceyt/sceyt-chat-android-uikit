package com.sceyt.sceytchatuikit.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sceyt.sceytchatuikit.persistence.converters.ChannelConverter
import com.sceyt.sceytchatuikit.persistence.converters.MessageConverter
import com.sceyt.sceytchatuikit.persistence.dao.ChannelDao
import com.sceyt.sceytchatuikit.persistence.dao.MessageDao
import com.sceyt.sceytchatuikit.persistence.dao.UserDao
import com.sceyt.sceytchatuikit.persistence.entity.UserEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.ChannelEntity
import com.sceyt.sceytchatuikit.persistence.entity.channel.UserChatLink
import com.sceyt.sceytchatuikit.persistence.entity.messages.AttachmentEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.MessageEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionEntity
import com.sceyt.sceytchatuikit.persistence.entity.messages.ReactionScoreEntity

@Database(entities = [
    ChannelEntity::class,
    UserEntity::class,
    UserChatLink::class,
    MessageEntity::class,
    AttachmentEntity::class,
    ReactionEntity::class,
    ReactionScoreEntity::class
], version = 1, exportSchema = false)

@TypeConverters(ChannelConverter::class, MessageConverter::class)
abstract class SceytDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
}