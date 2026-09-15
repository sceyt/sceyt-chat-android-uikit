package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chatuikit.data.managers.message.event.PinUpdateEvent
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.PinSyncStateEntity

internal class UpdatePinnedMessagesUseCase(
    private val messageDao: MessageDao,
    private val pinnedMessageDao: PinnedMessageDao,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
    private val storePinsUseCase: StorePinsUseCase,
) {

    suspend operator fun invoke(event: PinUpdateEvent) {
        when (event) {
            is PinUpdateEvent.Pinned -> storePinsUseCase(event.channelId, event.messages)
            is PinUpdateEvent.Unpinned -> event.messages.forEach { pin ->
                val messageTid = messageDao.getMessageById(pin.messageId)?.messageEntity?.tid
                    ?: pin.messageTid
                val existing = pinnedMessageDao.getByTid(messageTid, event.channelId)
                if (existing?.syncState == PinSyncStateEntity.PendingPin.value) return@forEach
                pinnedMessageDao.deleteWithMirror(messageTid, event.channelId)
                refreshPinnedMessageCache(event.channelId, messageTid)
            }
        }
    }
}