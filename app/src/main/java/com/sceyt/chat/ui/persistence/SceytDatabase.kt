package com.sceyt.chat.ui.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sceyt.chat.ui.persistence.converters.ChannelConverter
import com.sceyt.chat.ui.persistence.converters.MessageConverter
import com.sceyt.chat.ui.persistence.dao.ChannelDao
import com.sceyt.chat.ui.persistence.dao.MessageDao
import com.sceyt.chat.ui.persistence.dao.UserDao
import com.sceyt.chat.ui.persistence.entity.UserEntity
import com.sceyt.chat.ui.persistence.entity.channel.ChannelEntity
import com.sceyt.chat.ui.persistence.entity.channel.UserChatLink
import com.sceyt.chat.ui.persistence.entity.messages.MessageEntity

@Database(entities = [
    ChannelEntity::class,
    UserEntity::class,
    UserChatLink::class,
    MessageEntity::class
], version = 1, exportSchema = false)

@TypeConverters(ChannelConverter::class, MessageConverter::class)
abstract class SceytDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
}