package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.SceytMessage
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.mappers.getTid
import com.sceyt.chatuikit.persistence.mappers.toMessageDb
import com.sceyt.chatuikit.persistence.mappers.toPinnedMessageEntity
import com.sceyt.chatuikit.presentation.extensions.isDisappearing

internal class StorePinsUseCase(
    private val messageDao: MessageDao,
    private val pinnedMessageDao: PinnedMessageDao,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
) {

    suspend operator fun invoke(channelId: Long, pins: List<SceytPinnedMessage>) {
        val entities = pins.filter { it.message.shouldStore() }.map { pin ->
            pin.toPinnedMessageEntity(channelId, messageTid = storeMessageIfMissing(pin.message))
        }
        if (entities.isEmpty()) return

        pinnedMessageDao.insertAllIfMessagesExist(entities)
        refreshPinnedMessageCache(channelId, *entities.map { it.messageTid }.toLongArray())
    }

    private suspend fun storeMessageIfMissing(message: SceytMessage): Long {
        messageDao.getMessageTidById(message.id)?.let { return it }
        val tid = getTid(message.id, message.tid, message.incoming)
        messageDao.upsertMessage(message.copy(tid = tid).toMessageDb(unList = true))
        return tid
    }

    private fun SceytMessage.shouldStore() = id != 0L && !isDisappearing() &&
            state != MessageState.Deleted && state != MessageState.DeletedHard
}