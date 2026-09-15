package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.managers.message.event.PinUpdateEvent
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.logic.PersistencePinLogic
import com.sceyt.chatuikit.persistence.logicimpl.usecases.PinMessageUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SendPendingPinsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SyncChannelPinsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UnpinMessageUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UpdatePinnedMessagesUseCase
import com.sceyt.chatuikit.persistence.mappers.toSceytPinnedMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.sync.withLock

internal class PersistencePinLogicImpl(
    private val pinnedMessageDao: PinnedMessageDao,
    private val pinMessageUseCase: PinMessageUseCase,
    private val unpinMessageUseCase: UnpinMessageUseCase,
    private val syncChannelPinsUseCase: SyncChannelPinsUseCase,
    private val sendPendingPinsUseCase: SendPendingPinsUseCase,
    private val updatePinnedMessagesUseCase: UpdatePinnedMessagesUseCase,
) : PersistencePinLogic {

    private val pinUpdateMutex get() = sendPendingPinsUseCase.localUpdateMutex

    override fun getPinnedMessagesFlow(channelId: Long): Flow<List<SceytPinnedMessage>> =
        pinnedMessageDao
            .getPinnedMessagesFlow(channelId, System.currentTimeMillis())
            .map { list -> list.mapNotNull { it.toSceytPinnedMessage() } }
            .observeUnexpiredPins()

    override suspend fun getPinnedMessages(channelId: Long): List<SceytPinnedMessage> =
        pinnedMessageDao
            .getPinnedMessages(channelId, System.currentTimeMillis())
            .mapNotNull { it.toSceytPinnedMessage() }
            .filterNot { it.isExpired }

    override suspend fun pinMessage(
        channelId: Long,
        messageTid: Long,
        pinType: PinType,
    ): SceytResponse<SceytPinnedMessage> = pinMessageUseCase(channelId, messageTid, pinType)

    override suspend fun unpinMessage(
        channelId: Long,
        messageTid: Long,
    ): SceytResponse<Boolean> = unpinMessageUseCase(channelId, messageTid)

    override suspend fun syncChannelPins(channelId: Long) {
        syncChannelPinsUseCase(channelId)
    }

    override suspend fun sendAllPendingPins() {
        sendPendingPinsUseCase()
    }

    override suspend fun onPinUpdated(event: PinUpdateEvent) {
        pinUpdateMutex.withLock { updatePinnedMessagesUseCase(event) }
    }

    override suspend fun onMessageDeleted(channelId: Long, messageTid: Long) {
        pinUpdateMutex.withLock { pinnedMessageDao.deleteWithMirror(messageTid, channelId) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun Flow<List<SceytPinnedMessage>>.observeUnexpiredPins(
    currentTime: () -> Long = System::currentTimeMillis,
): Flow<List<SceytPinnedMessage>> = transformLatest { pins ->
    while (true) {
        val now = currentTime()
        val active = pins.filter { pin ->
            pin.pinnedUntil == null || pin.pinnedUntil == 0L || pin.pinnedUntil > now
        }
        emit(active)
        val nextExpiry = active.mapNotNull { it.pinnedUntil?.takeIf { expiry -> expiry > now } }
            .minOrNull() ?: return@transformLatest
        delay(nextExpiry - now)
    }
}