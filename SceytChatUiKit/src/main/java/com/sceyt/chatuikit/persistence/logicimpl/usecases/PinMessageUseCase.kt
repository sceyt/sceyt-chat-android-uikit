package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.data.models.messages.PinSyncState
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity.Companion.UNKNOWN_SERVER_PIN_ID
import com.sceyt.chatuikit.persistence.mappers.toStoredPinScope
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.presentation.extensions.isDisappearing
import com.sceyt.chatuikit.presentation.extensions.isPending
import kotlinx.coroutines.sync.withLock

internal class PinMessageUseCase(
    private val messageDao: MessageDao,
    private val pinnedMessageDao: PinnedMessageDao,
    private val sendPendingPinsUseCase: SendPendingPinsUseCase,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
) {

    suspend operator fun invoke(
        channelId: Long,
        messageTid: Long,
        pinType: PinType,
    ): SceytResponse<SceytPinnedMessage> {
        val prepared = sendPendingPinsUseCase.localUpdateMutex.withLock {
            prepare(channelId, messageTid, pinType)
        }
        return when (prepared) {
            is SceytResponse.Error -> SceytResponse.Error(prepared.exception)
            is SceytResponse.Success -> prepared.data?.let { sendPendingPinsUseCase.sendPin(it) }
                ?: SceytResponse.Success(null)
        }
    }

    private suspend fun prepare(
        channelId: Long,
        messageTid: Long,
        pinType: PinType,
    ): SceytResponse<PinnedMessageEntity> {
        val messageDb = messageDao.getMessageByTid(messageTid)
            ?: return createErrorResponse("Message not found in database")
        val message = messageDb.toSceytMessage()

        // Ephemeral messages must not outlive their disappearance in the pin list.
        if (message.isDisappearing())
            return createErrorResponse("Ephemeral messages cannot be pinned")

        val existing = pinnedMessageDao.getByTid(messageTid, channelId)
        if (existing != null && existing.syncState != PinSyncState.PendingUnpin.value)
            return createErrorResponse("Message is already pinned")

        // Distinguish rapid pin/unpin/pin intents even within the same millisecond.
        val now = maxOf(System.currentTimeMillis(), (existing?.lastAttemptAt ?: 0L) + 1)
        val entity = PinnedMessageEntity(
            messageTid = messageTid,
            channelId = channelId,
            messageId = message.id,
            pinScope = pinType.toStoredPinScope().value,
            pinnedAt = now,
            pinnedUntil = null,
            pinnedByUserId = null,
            messageCreatedAt = message.createdAt,
            serverPinId = UNKNOWN_SERVER_PIN_ID,
            syncState = PinSyncState.PendingPin.value,
            retryCount = 0,
            lastAttemptAt = now,
        )
        pinnedMessageDao.upsertWithMirror(entity)
        refreshPinnedMessageCache(channelId, messageTid)

        if (message.isPending() || message.id == 0L)
            return SceytResponse.Success(null)

        return SceytResponse.Success(entity)
    }
}
