package com.sceyt.chatuikit.persistence.logicimpl.usecases

import com.sceyt.chat.models.message.PinDetails.PinType
import com.sceyt.chatuikit.data.models.SceytResponse
import com.sceyt.chatuikit.data.models.createErrorResponse
import com.sceyt.chatuikit.persistence.database.dao.MessageDao
import com.sceyt.chatuikit.persistence.database.dao.PendingPinDao
import com.sceyt.chatuikit.persistence.database.entity.pendings.PendingPinEntity
import com.sceyt.chatuikit.persistence.mappers.toSceytMessage
import com.sceyt.chatuikit.persistence.mappers.toStoredPinScope
import com.sceyt.chatuikit.presentation.extensions.isDisappearing

internal class PinMessageUseCase(
    private val messageDao: MessageDao,
    private val pendingPinDao: PendingPinDao,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
) {

    suspend operator fun invoke(
        channelId: Long,
        messageTid: Long,
        pinType: PinType,
    ): SceytResponse<Boolean> {
        val message = messageDao.getMessageByTid(messageTid)?.toSceytMessage()
            ?: return createErrorResponse("Message not found in database")

        if (message.isDisappearing())
            return createErrorResponse("Ephemeral messages cannot be pinned")

        if (message.id == 0L)
            return createErrorResponse("Pending messages cannot be pinned")

        if (message.pinDetails?.isPinned == true)
            return createErrorResponse("Message is already pinned")

        pendingPinDao.replace(
            PendingPinEntity(
                messageTid = messageTid,
                channelId = channelId,
                messageId = message.id,
                isPin = true,
                pinScope = pinType.toStoredPinScope().value,
                createdAt = System.currentTimeMillis(),
            )
        ) ?: return createErrorResponse("Message not found in database")

        refreshPinnedMessageCache(channelId, messageTid)
        return SceytResponse.Success(true)
    }
}