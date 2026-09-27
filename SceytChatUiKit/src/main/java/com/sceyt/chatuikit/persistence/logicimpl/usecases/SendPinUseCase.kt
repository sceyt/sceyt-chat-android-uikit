package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.SDKErrorTypeEnum
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.messages.SceytPinnedMessage
import com.sceyt.chatuikit.extensions.TAG
import com.sceyt.chatuikit.logger.SceytLog
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.database.dao.PinnedMessageDao
import com.sceyt.chatuikit.persistence.database.entity.messages.StoredPinScope
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinEntity
import com.sceyt.chatuikit.persistence.logic.SystemMessageSender
import com.sceyt.chatuikit.persistence.mappers.toPinType
import com.sceyt.chatuikit.persistence.mappers.toPinnedMessageEntity
import com.sceyt.chatuikit.persistence.repositories.PinRepository

internal class SendPinUseCase(
    private val pendingPinDao: PendingPinDao,
    private val pinnedMessageDao: PinnedMessageDao,
    private val pinRepository: PinRepository,
    private val systemMessageSender: SystemMessageSender,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
) {

    suspend operator fun invoke(pending: PendingPinEntity): SceytResponse<SceytPinnedMessage> {
        val response = pinRepository.pinMessages(
            channelId = pending.channelId,
            messageIds = listOf(pending.messageId),
            pinType = StoredPinScope.fromValue(pending.pinScope).toPinType(),
            pinTill = null,
        )
        if (response is SceytResponse.Error) {
            val errorType = SDKErrorTypeEnum.fromValue(response.exception?.type)
            SceytLog.e(TAG, "Pin failed: type=${errorType?.value}, message=${response.message}")
            if (errorType?.isResendable == false) {
                pendingPinDao.deleteById(pending.id)
                refreshPinnedMessageCache(pending.channelId, pending.messageTid)
            }
            return SceytResponse.Error(response.exception)
        }

        val pin = response.data?.firstOrNull { it.message.id == pending.messageId }
        pin?.let {
            pinnedMessageDao.insertIfMessageExists(it.toPinnedMessageEntity(pending.channelId, pending.messageTid))
        }
        val settled = pendingPinDao.deleteById(pending.id) > 0
        refreshPinnedMessageCache(pending.channelId, pending.messageTid)

        if (settled && pin?.scope == PinType.SHARED)
            systemMessageSender.sendMessagePinned(pending.channelId, pin.message)
        return SceytResponse.Success(pin.takeIf { settled })
    }
}