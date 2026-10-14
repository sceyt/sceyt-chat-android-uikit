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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PinMessageUseCase(
    private val messageDao: MessageDao,
    private val pendingPinDao: PendingPinDao,
    private val refreshPinnedMessageCache: RefreshPinnedMessageCacheUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend operator fun invoke(
        channelId: Long,
        messageTid: Long,
        pinType: PinType,
    ): SceytResponse<Boolean> = withContext(ioDispatcher) {
        val message = messageDao.getMessageByTid(messageTid)?.toSceytMessage()
            ?: return@withContext createErrorResponse("Message not found in database")

        if (message.isDisappearing())
            return@withContext createErrorResponse("Ephemeral messages cannot be pinned")

        if (message.id == 0L)
            return@withContext createErrorResponse("Pending messages cannot be pinned")

        if (message.pinDetails?.isPinned == true)
            return@withContext createErrorResponse("Message is already pinned")

        pendingPinDao.replace(
            PendingPinEntity(
                messageTid = messageTid,
                channelId = channelId,
                messageId = message.id,
                isPin = true,
                pinScope = pinType.toStoredPinScope().value,
                createdAt = System.currentTimeMillis(),
            )
        ) ?: return@withContext createErrorResponse("Message not found in database")

        refreshPinnedMessageCache(channelId, messageTid)
        SceytResponse.Success(true)
    }
}
