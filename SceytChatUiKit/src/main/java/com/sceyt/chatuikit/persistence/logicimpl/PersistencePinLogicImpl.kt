package com.sceyt.chatuikit.persistence.logicimpl

import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.managers.message.event.PinUpdateEvent
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.logic.PersistencePinLogic
import com.sceyt.chatuikit.persistence.logicimpl.usecases.PinMessageUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SendPendingPinsUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.SyncChannelPinsController
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UnpinMessageUseCase
import com.sceyt.chatuikit.persistence.logicimpl.usecases.UpdatePinnedMessagesUseCase
import com.sceyt.chatuikit.persistence.mappers.toSceytPinnedMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock

internal class PersistencePinLogicImpl(
    private val pinnedMessageDao: PinnedMessageDao,
    private val pinMessageUseCase: PinMessageUseCase,
    private val unpinMessageUseCase: UnpinMessageUseCase,
    private val syncChannelPinsController: SyncChannelPinsController,
    private val sendPendingPinsUseCase: SendPendingPinsUseCase,
    private val updatePinnedMessagesUseCase: UpdatePinnedMessagesUseCase,
) : PersistencePinLogic {

    private val pinUpdateMutex get() = sendPendingPinsUseCase.localUpdateMutex

    override fun getPinnedMessagesFlow(channelId: Long): Flow<List<SceytPinnedMessage>> =
        pinnedMessageDao
            .getPinnedMessagesFlow(channelId, System.currentTimeMillis())
            .map { list -> list.mapNotNull { it.toSceytPinnedMessage() } }

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
        syncChannelPinsController(channelId)
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