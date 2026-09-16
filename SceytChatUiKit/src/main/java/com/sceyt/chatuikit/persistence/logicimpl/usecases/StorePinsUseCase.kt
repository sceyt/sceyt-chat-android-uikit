package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chat.models.message.MessageState
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.data.models.messages.PinSyncState
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
        pins.forEach { pin ->
            val message = pin.message
            if (message.id == 0L || message.isDisappearing() ||
                message.state == MessageState.Deleted || message.state == MessageState.DeletedHard
            ) return@forEach

            val existingTid = messageDao.getMessageById(message.id)?.messageEntity?.tid
            val messageTid = existingTid ?: getTid(message.id, message.tid, message.incoming)
            val existingPin = pinnedMessageDao.getByTid(messageTid, channelId)
            if (existingPin?.syncState == PinSyncState.PendingPin.value ||
                existingPin?.syncState == PinSyncState.PendingUnpin.value
            ) return@forEach

            if (existingTid == null)
                messageDao.upsertMessage(message.copy(tid = messageTid).toMessageDb(unList = true))

            pinnedMessageDao.upsertWithMirror(pin.toPinnedMessageEntity(channelId, messageTid))
            refreshPinnedMessageCache(channelId, messageTid)
        }
    }
}