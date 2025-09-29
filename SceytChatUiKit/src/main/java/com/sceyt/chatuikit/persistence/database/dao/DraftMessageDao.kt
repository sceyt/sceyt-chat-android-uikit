package com.sceyt.chatuikit.persistence.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.CHANNEL_TABLE
import com.sceyt.chatuikit.persistence.database.DatabaseConstants.DRAFT_MESSAGE_TABLE
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftAttachmentEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageDb
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftMessageUserLinkEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.DraftVoiceAttachmentEntity

@Dao
internal abstract class DraftMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: DraftMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertDraftMessageUserLinks(links: List<DraftMessageUserLinkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertDraftAttachments(attachments: List<DraftAttachmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertVoiceAttachment(voiceAttachment: DraftVoiceAttachmentEntity)

    @Transaction
    open suspend fun insertDraftMessage(
            entity: DraftMessageEntity,
            links: List<DraftMessageUserLinkEntity>?,
            attachments: List<DraftAttachmentEntity>?,
            voiceAttachment: DraftVoiceAttachmentEntity?,
    ) {
        if (existsChannel(entity.chatId)) {
            insert(entity)

            links?.takeIf { it.isNotEmpty() }?.let {
                insertDraftMessageUserLinks(it)
            }

            attachments?.takeIf { it.isNotEmpty() }?.let {
                insertDraftAttachments(it)
            }

            voiceAttachment?.let {
                insertVoiceAttachment(it)
            }
        }
    }

    @Query("select exists(select 1 from $CHANNEL_TABLE where chat_id = :chatId)")
    abstract suspend fun existsChannel(chatId: Long): Boolean

    @Transaction
    @Query("select * from $DRAFT_MESSAGE_TABLE where chatId = :chatId")
    abstract suspend fun getDraftByChannelId(chatId: Long): DraftMessageDb?

    @Query("delete from $DRAFT_MESSAGE_TABLE where chatId = :chatId")
    abstract suspend fun deleteDraftByChannelId(chatId: Long)
}