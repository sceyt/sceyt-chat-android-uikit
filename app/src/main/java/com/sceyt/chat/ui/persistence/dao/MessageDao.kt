package com.sceyt.chat.ui.persistence.dao

import androidx.room.*
import com.sceyt.chat.ui.persistence.entity.messages.MessageEntity
import com.sceyt.chat.ui.sceytconfigs.SceytUIKitConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(vararg messages: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessages(messages: List<MessageEntity>)

    @Transaction
    @Query("select * from messages order by createdAt DESC LIMIT :limit")
    fun getMessagesFlow(limit: Int = SceytUIKitConfig.MESSAGES_LOAD_SIZE): Flow<List<MessageEntity>>
}