package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.managers.message.event.PinUpdateEvent
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.logic.PersistencePinLogic
import com.sceyt.chatuikit.persistence.logicimpl.usecases.PinMessageUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SendPendingPinsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SendPinUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SendUnpinUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SyncChannelPinsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UnpinMessageUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UpdatePinnedMessagesUseCase
import com.sceyt.chatuikit.persistence.mappers.mergePins
import com.sceyt.chatuikit.persistence.mappers.toSceytPinnedMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PersistencePinLogicImpl(
    private val pinnedMessageDao: PinnedMessageDao,
    private val pendingPinDao: PendingPinDao,
    private val pinMessageUseCase: PinMessageUseCase,
    private val unpinMessageUseCase: UnpinMessageUseCase,
    private val syncChannelPinsUseCase: SyncChannelPinsUseCase,
    private val sendPendingPinsUseCase: SendPendingPinsUseCase,
    private val sendPinUseCase: SendPinUseCase,
    private val sendUnpinUseCase: SendUnpinUseCase,
    private val updatePinnedMessagesUseCase: UpdatePinnedMessagesUseCase,
) : PersistencePinLogic {

    private val syncMutex = Mutex()

    override fun getPinnedMessagesFlow(channelId: Long): Flow<List<SceytPinnedMessage>> = combine(
        pinnedMessageDao.getPinnedMessagesFlow(channelId, System.currentTimeMillis()),
        pendingPinDao.getPendingPinsFlow(channelId),
    ) { confirmed, pending ->
        mergePins(confirmed.mapNotNull { it.toSceytPinnedMessage() }, pending)
    }

    override suspend fun pinMessage(
        channelId: Long,
        messageTid: Long,
        pinType: PinType,
    ): SceytResponse<SceytPinnedMessage> {
        val stored = pinMessageUseCase(channelId, messageTid, pinType)
        if (stored is SceytResponse.Error) return SceytResponse.Error(stored.exception)
        return syncMutex.withLock {
            pendingPinDao.getByTid(messageTid)?.takeIf { it.isPin }?.let { sendPinUseCase(it) }
                ?: SceytResponse.Success(null)
        }
    }

    override suspend fun unpinMessage(
        channelId: Long,
        messageTid: Long,
    ): SceytResponse<Boolean> {
        val stored = unpinMessageUseCase(channelId, messageTid)
        if (stored is SceytResponse.Error) return stored
        return syncMutex.withLock {
            pendingPinDao.getByTid(messageTid)?.takeIf { !it.isPin }?.let { sendUnpinUseCase(it) }
                ?: SceytResponse.Success(true)
        }
    }

    override suspend fun syncChannelPins(channelId: Long) {
        syncMutex.withLock {
            sendPendingPinsUseCase(channelId)
            syncChannelPinsUseCase(channelId)
        }
    }

    override suspend fun sendAllPendingPins() {
        syncMutex.withLock { sendPendingPinsUseCase() }
    }

    override suspend fun onPinUpdated(event: PinUpdateEvent) {
        updatePinnedMessagesUseCase(event)
    }
}