package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.SDKErrorTypeEnum
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.StoredPinScope
import com.sceyt.chatuikit.data.models.messages.PinSyncState
import com.sceyt.chatuikit.persistence.database.entity.messages.PinnedMessageEntity
import com.sceyt.chatuikit.persistence.logic.SystemMessageSender
import com.sceyt.chatuikit.persistence.mappers.toPinType
import com.sceyt.chatuikit.persistence.repositories.PinRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shared request handling for immediate actions and reconnect retries. */
internal class SendPendingPinsUseCase(
    private val pinnedMessageDao: PinnedMessageDao,
    private val pinRepository: PinRepository,
    private val confirmPinUseCase: ConfirmPinUseCase,
    private val systemMessageSender: SystemMessageSender,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
) {

    // Network waits must never block a new local intent or its bubble refresh.
    val localUpdateMutex = Mutex()
    val requestMutex = Mutex()

    suspend operator fun invoke(channelId: Long? = null) {
        val pending = channelId?.let { pinnedMessageDao.getPendingByChannel(it) }
            ?: pinnedMessageDao.getAllPending()
        pending.forEach { entity ->
            when (entity.syncState) {
                PinSyncState.PendingPin.value -> sendPin(entity)
                PinSyncState.PendingUnpin.value -> sendUnpin(entity)
            }
        }
    }

    suspend fun sendPin(entity: PinnedMessageEntity): SceytResponse<SceytPinnedMessage> = requestMutex.withLock {
        if (!localUpdateMutex.withLock { isCurrent(entity) })
            return@withLock SceytResponse.Success(null)
        if (entity.messageId == 0L) return@withLock SceytResponse.Success(null)
        val pinType = StoredPinScope.fromValue(entity.pinScope).toPinType()
        val response = pinRepository.pinMessages(
            channelId = entity.channelId,
            messageIds = listOf(entity.messageId),
            pinType = pinType,
            pinTill = entity.pinnedUntil,
        )
        var announce = false
        val result: SceytResponse<SceytPinnedMessage> = localUpdateMutex.withLock applyResponse@{
            if (!isCurrent(entity)) return@applyResponse SceytResponse.Success(null)
            when (response) {
                is SceytResponse.Success -> {
                    val pin = response.data?.firstOrNull() ?: return@applyResponse SceytResponse.Success(null)
                    val confirmed = confirmPinUseCase(entity.channelId, entity.messageTid, pin.id)
                    announce = confirmed.didFlipPendingIntent && pinType == PinType.SHARED
                    SceytResponse.Success(confirmed.pinnedMessage)
                }
                is SceytResponse.Error -> {
                    handleError(entity, response, isPin = true)
                    SceytResponse.Error(response.exception)
                }
            }
        }
        if (announce) systemMessageSender.sendMessagePinned(entity.channelId, entity.messageId)
        result
    }

    suspend fun sendUnpin(entity: PinnedMessageEntity): SceytResponse<Boolean> = requestMutex.withLock {
        if (!localUpdateMutex.withLock { isCurrent(entity) })
            return@withLock SceytResponse.Success(false)
        if (entity.messageId == 0L) return@withLock SceytResponse.Success(false)
        val response = pinRepository.unpinMessages(
            channelId = entity.channelId,
            messageIds = listOf(entity.messageId),
        )
        localUpdateMutex.withLock applyResponse@{
            if (!isCurrent(entity)) return@applyResponse SceytResponse.Success(false)
            when (response) {
                is SceytResponse.Success -> {
                    pinnedMessageDao.deleteWithMirror(entity.messageTid, entity.channelId)
                    refreshPinnedMessageCache(entity.channelId, entity.messageTid)
                    SceytResponse.Success(true)
                }
                is SceytResponse.Error -> {
                    handleError(entity, response, isPin = false)
                    SceytResponse.Error(response.exception)
                }
            }
        }
    }

    private suspend fun isCurrent(entity: PinnedMessageEntity): Boolean =
        pinnedMessageDao.getByTid(entity.messageTid, entity.channelId) == entity

    private suspend fun handleError(
        entity: PinnedMessageEntity,
        response: SceytResponse.Error<*>,
        isPin: Boolean,
    ) {
        val errorType = SDKErrorTypeEnum.fromValue(response.exception?.type)
        if (errorType?.isResendable == false) {
            if (isPin || errorType == SDKErrorTypeEnum.NotFound) {
                pinnedMessageDao.deleteWithMirror(entity.messageTid, entity.channelId)
            } else {
                pinnedMessageDao.upsertWithMirror(
                    entity.copy(syncState = PinSyncState.Synced.value, retryCount = 0)
                )
            }
            refreshPinnedMessageCache(entity.channelId, entity.messageTid)
        } else {
            pinnedMessageDao.incrementRetry(entity.messageTid, entity.channelId, System.currentTimeMillis())
        }
        SceytLog.e(TAG, "Pin action failed: isPin=$isPin, type=${errorType?.value}, message=${response.message}")
    }
}